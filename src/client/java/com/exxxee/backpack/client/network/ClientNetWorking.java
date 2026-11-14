package com.exxxee.backpack.client.network;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Inventory.menu.WindowBuiltInRegistries;
import com.exxxee.backpack.Inventory.menu.WindowMenuType;
import com.exxxee.backpack.Network.payload.CloseWindowPayload;
import com.exxxee.backpack.Network.payload.ModuleMenuPayload;
import com.exxxee.backpack.Network.payload.OpenWindowPayload;
import com.exxxee.backpack.Network.payload.WindowContentPayload;
import com.exxxee.backpack.Network.payload.WindowSlotPayload;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.client.gui.windows.MenuWindows;
import com.exxxee.backpack.client.gui.windows.BackpackWindowHost;
import com.exxxee.backpack.container.ModuleContainer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ClientNetWorking {
    private ClientNetWorking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenWindowPayload.TYPE, ClientNetWorking::openWindow);
        ClientPlayNetworking.registerGlobalReceiver(CloseWindowPayload.TYPE, ClientNetWorking::closeWindow);
        ClientPlayNetworking.registerGlobalReceiver(ModuleMenuPayload.TYPE, ClientNetWorking::syncModuleMenu);
        ClientPlayNetworking.registerGlobalReceiver(WindowContentPayload.TYPE, ClientNetWorking::syncWindow);
        ClientPlayNetworking.registerGlobalReceiver(WindowSlotPayload.TYPE, ClientNetWorking::syncWindowSlot);
    }

    private static void openWindow(OpenWindowPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            WindowMenuType<?> menuType = WindowBuiltInRegistries.MENU.get(payload.menuType());
            if (menuType == null) return;
            MenuWindows.create(menuType, context.client(), payload.windowId());
        });
    }

    private static void syncModuleMenu(ModuleMenuPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Player player = context.player();
            IBackpackHost host = (IBackpackHost) player;
            AbstractBackpackMenu existing = host.getBackpackMenu(payload.windowId());
            if (existing instanceof ModuleMenu moduleMenu) {
                moduleMenu.shelfSlot = payload.shelfSlot();
                if (!moduleMenu.slots.isEmpty() && moduleMenu.slots.get(0).container instanceof ModuleContainer modules) {
                    modules.setStack(payload.stack().copy());
                }
                return;
            }
            ModuleContainer container = new ModuleContainer(payload.stack().copy());
            ModuleMenu menu = new ModuleMenu(payload.windowId(), player, container);
            menu.shelfSlot = payload.shelfSlot();
            host.setBackpackContainer(payload.windowId(), container);
            host.setBackpackMenu(payload.windowId(), menu);
        });
    }

    private static void syncWindow(WindowContentPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            AbstractBackpackMenu menu = ((IBackpackHost) context.player()).getBackpackMenu(payload.windowId());
            if (menu == null) return;
            List<ItemStack> items = payload.items();
            int count = Math.min(items.size(), menu.slots.size());
            for (int i = 0; i < count; i++) {
                menu.slots.get(i).set(items.get(i).copy());
            }
        });
    }

    private static void syncWindowSlot(WindowSlotPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            AbstractBackpackMenu menu = ((IBackpackHost) context.player()).getBackpackMenu(payload.windowId());
            if (menu == null) return;
            int slot = payload.slot();
            if (slot < 0 || slot >= menu.slots.size()) return;
            menu.slots.get(slot).set(payload.item().copy());
        });
    }

    private static void closeWindow(CloseWindowPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Player player = context.player();
            AbstractBackpackMenu menu = ((IBackpackHost) player).getBackpackMenu(payload.windowId());
            if (menu == null || menu.windowId != payload.windowId()) return;
            ((IBackpackHost) player).setBackpackContainer(menu.windowId, null);
            ((IBackpackHost) player).setBackpackMenu(menu.windowId,  null);
            if (context.client().screen instanceof BackpackWindowHost host) {
                host.setBackpackWindow(payload.windowId(), null);
            }
        });
    }

}
