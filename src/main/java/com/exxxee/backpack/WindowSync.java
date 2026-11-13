package com.exxxee.backpack;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.WindowBuiltInRegistries;
import com.exxxee.backpack.Inventory.menu.WindowMenuType;
import com.exxxee.backpack.Network.payload.CloseWindowPayload;
import com.exxxee.backpack.Network.payload.OpenWindowPayload;
import com.exxxee.backpack.Network.payload.WindowContentPayload;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.api.WindowIdIssuer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WindowSync {
    private WindowSync() {
    }

    public static void sync(ServerPlayer player) {
        for (AbstractBackpackMenu menu : ((IBackpackHost) player).getBackpackMenus()) {
            sync(player, menu.windowId);
        }
    }

    public static void sync(ServerPlayer player, int windowId) {
        AbstractBackpackMenu menu = ((IBackpackHost) player).getBackpackMenu(windowId);
        if (menu == null) return;
        send(player, menu);
    }

    public static void sync(ServerPlayer player, WindowMenuType<?> menuType) {
        if (menuType == null) return;
        int windowId = ((WindowIdIssuer) player).nextWindowId();
        IBackpackHost host = (IBackpackHost) player;
        Container container = menuType.createContainer(player);
        host.setBackpackContainer(windowId, container);
        sync(player, menuType.create(windowId, player, container));
    }

    public static void sync(ServerPlayer player, AbstractBackpackMenu menu) {
        if (menu == null) return;
        ((IBackpackHost) player).setBackpackMenu(menu.windowId, menu);
        send(player, menu);
    }

    public static void close(ServerPlayer player, int windowId) {
        IBackpackHost host = (IBackpackHost) player;
        if (host.getBackpackMenu(windowId) == null) return;
        host.setBackpackContainer(windowId, null);
        host.setBackpackMenu(windowId, null);
        ServerPlayNetworking.send(player, new CloseWindowPayload(windowId));
    }

    private static void send(ServerPlayer player, AbstractBackpackMenu menu) {
        String menuType = typeName(menu.menuType);
        if (menuType == null) return;
        ServerPlayNetworking.send(player, new OpenWindowPayload(menu.windowId, menuType));
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : menu.slots) {
            items.add(slot.getItem().copy());
        }
        ServerPlayNetworking.send(player, new WindowContentPayload(menu.windowId, items));
    }

    private static String typeName(WindowMenuType<?> menuType) {
        if (menuType == null) return null;
        for (Map.Entry<String, WindowMenuType<?>> entry : WindowBuiltInRegistries.MENU.entrySet()) {
            if (entry.getValue() == menuType) return entry.getKey();
        }
        return null;
    }
}
