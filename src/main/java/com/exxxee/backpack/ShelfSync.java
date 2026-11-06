package com.exxxee.backpack;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.Network.payload.CloseWindowPayload;
import com.exxxee.backpack.Network.payload.OpenWindowPayload;
import com.exxxee.backpack.Network.payload.ShelfContentPayload;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.api.WindowIdIssuer;
import com.exxxee.backpack.container.ShelfContainer;
import com.exxxee.backpack.item.BackpackItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ShelfSync {
    private ShelfSync() {
    }

    public static void sync(ServerPlayer player) {
        IBackpackHost host = (IBackpackHost) player;
        boolean wearing = player.getItemBySlot(EquipmentSlot.CHEST).is(BackpackItems.BACKPACK_SHELF);
        AbstractBackpackMenu current = host.getShelfMenu();
        if (wearing) {
            if (current != null) return;
            ShelfMenu menu = new ShelfMenu(((WindowIdIssuer) player).nextWindowId(), player, new ShelfContainer(player));
            host.setShelfMenu(menu);
            sendOpen(player, menu);
            return;
        }
        if (current == null) return;
        int windowId = current.windowId;
        host.setShelfMenu(null);
        ServerPlayNetworking.send(player, new CloseWindowPayload(windowId));
    }

    private static void sendOpen(ServerPlayer player, AbstractBackpackMenu menu) {
        ServerPlayNetworking.send(player, new OpenWindowPayload(menu.windowId, "generic_1x4"));
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : menu.slots) {
            items.add(slot.getItem().copy());
        }
        ServerPlayNetworking.send(player, new ShelfContentPayload(items));
    }
}
