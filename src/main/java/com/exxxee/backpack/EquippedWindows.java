package com.exxxee.backpack;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.WindowMenuType;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.container.ShelfContainer;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EquippedWindows {
    public record Key(EquipmentSlot slot, Item item) {}

    private static final Map<Key, WindowMenuType<?>> BY_EQUIP = new HashMap<>();

    static {
        register(EquipmentSlot.CHEST, BackpackItems.BACKPACK_SHELF, WindowMenuType.GENERIC_1x4);
    }

    private EquippedWindows() {
    }

    public static void register(EquipmentSlot slot, Item item, WindowMenuType<?> menuType) {
        BY_EQUIP.put(new Key(slot, item), menuType);
    }

    public static void sync(ServerPlayer player) {
        Set<WindowMenuType<?>> worn = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            WindowMenuType<?> type = BY_EQUIP.get(new Key(slot, stack.getItem()));
            if (type != null) worn.add(type);
        }

        IBackpackHost host = (IBackpackHost) player;
        for (WindowMenuType<?> type : worn) {
            if (!has(host, type)) {
                WindowSync.sync(player, type);
            }
        }

        List<Integer> closing = new ArrayList<>();
        for (AbstractBackpackMenu menu : host.getBackpackMenus()) {
            if (menu == null || menu.menuType == null) continue;
            if (BY_EQUIP.containsValue(menu.menuType) && !worn.contains(menu.menuType)) {
                closing.add(menu.windowId);
            }
        }
        for (int windowId : closing) {
            flush(host.getBackpackMenu(windowId));
            WindowSync.close(player, windowId);
        }
    }

    private static boolean has(IBackpackHost host, WindowMenuType<?> type) {
        for (AbstractBackpackMenu menu : host.getBackpackMenus()) {
            if (menu != null && menu.menuType == type) return true;
        }
        return false;
    }

    private static void flush(AbstractBackpackMenu menu) {
        if (menu == null || menu.slots.isEmpty()) return;
        if (menu.slots.get(0).container instanceof ShelfContainer shelf) {
            shelf.flush();
        }
    }
}
