package com.exxxee.backpack.client.network;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.WindowBuiltInRegistries;
import com.exxxee.backpack.Inventory.menu.WindowMenuType;
import com.exxxee.backpack.Network.payload.CloseWindowPayload;
import com.exxxee.backpack.Network.payload.OpenWindowPayload;
import com.exxxee.backpack.Network.payload.ShelfContentPayload;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.client.gui.windows.MenuWindows;
import com.exxxee.backpack.client.gui.windows.ShelfWindowHost;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ClientNetWorking {
    private ClientNetWorking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenWindowPayload.TYPE, ClientNetWorking::openShelf);
        ClientPlayNetworking.registerGlobalReceiver(CloseWindowPayload.TYPE, ClientNetWorking::closeShelf);
        ClientPlayNetworking.registerGlobalReceiver(ShelfContentPayload.TYPE, ClientNetWorking::syncShelf);
    }

    private static void openShelf(OpenWindowPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            WindowMenuType<?> menuType = WindowBuiltInRegistries.MENU.get(payload.menuType());
            if (menuType == null) return;
            MenuWindows.create(menuType, context.client(), payload.windowId());
        });
    }

    private static void syncShelf(ShelfContentPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            AbstractBackpackMenu menu = ((IBackpackHost) context.player()).getShelfMenu();
            if (menu == null) return;
            List<ItemStack> items = payload.items();
            int count = Math.min(items.size(), menu.slots.size());
            for (int i = 0; i < count; i++) {
                menu.slots.get(i).set(items.get(i).copy());
            }
        });
    }

    private static void closeShelf(CloseWindowPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Player player = context.player();
            AbstractBackpackMenu menu = ((IBackpackHost) player).getShelfMenu();
            if (menu == null || menu.windowId != payload.window()) return;
            ((IBackpackHost) player).setShelfMenu(null);
            if (context.client().screen instanceof ShelfWindowHost host) {
                host.setShelfWindow(null);
            }
        });
    }

}
