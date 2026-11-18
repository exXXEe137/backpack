package com.exxxee.backpack.container;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方书把已装备模块里的物品也算进去。
 *
 * 两件事：
 * 1. {@link #account}：给 StackedItemContents 记数，书上有材料、判定能做。只算模块里面的东西，模块物品本身不算材料。
 * 2. {@link #pull}：判定能做之后，原版会从玩家物品栏的格子里搬材料；模块里的不在物品栏里，
 *    所以摆之前先把缺口那一格的材料搬进物品栏，让原版照常去拿。
 */
public final class BackpackRecipeSupport {

    private BackpackRecipeSupport() {
    }

    /** 生存模式、穿着背架时，把每个模块里的物品记进配方书的可用清单。 */
    public static void account(final Player player, final StackedItemContents contents) {
        if (player == null || player.hasInfiniteMaterials()) return;   // 创造模式的配方书不看材料

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(BackpackItems.BACKPACK_SHELF)) return;

        for (ItemStack module : chest.getOrDefault(BackpackDataComponents.SHELF_MODULES, List.<ItemStack>of())) {
            if (module.isEmpty() || !module.is(BackpackItems.MODULE)) continue;
            ModuleInventoryData data = module.get(BackpackDataComponents.MODULE_INVENTORY);
            if (data == null) continue;
            for (ItemStack stack : data.toList()) {
                if (!stack.isEmpty()) contents.accountSimpleStack(stack);
            }
        }
    }

    /**
     * 把物品栏里找不到的那一格材料，从模块搬一份到物品栏。
     * 只在原版已经找不到匹配格子时调用，搬到物品栏之后原版自己会再去找。
     */
    public static boolean pull(final Inventory inventory, final Player player,
                               final Holder<Item> item, final ItemStack existing, final int max) {
        if (player == null || player.hasInfiniteMaterials() || max <= 0) return false;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(BackpackItems.BACKPACK_SHELF)) return false;

        List<ItemStack> modules = new ArrayList<>(chest.getOrDefault(BackpackDataComponents.SHELF_MODULES, List.<ItemStack>of()));
        boolean pulled = false;

        for (int i = 0; i < modules.size() && !pulled; i++) {
            ItemStack module = modules.get(i);
            if (module.isEmpty() || !module.is(BackpackItems.MODULE)) continue;

            ModuleContainer container = openContainer(player, i);
            boolean live = container != null;
            if (!live) container = new ModuleContainer(module.copy());

            int slot = findSlot(container, item, existing);
            if (slot >= 0) {
                ItemStack candidate = container.getItem(slot);
                int room = roomInInventory(inventory, candidate);
                if (room > 0) {
                    int take = Math.min(Math.min(max, candidate.getCount()), room);
                    ItemStack taken = container.removeItem(slot, take);
                    if (!taken.isEmpty()) {
                        int before = taken.getCount();
                        inventory.add(taken);
                        pulled = taken.getCount() < before;
                    }
                }
            }

            container.flush();
            modules.set(i, container.stack());
        }

        if (!pulled) return false;

        chest.set(BackpackDataComponents.SHELF_MODULES, modules);
        refreshShelfMenu(player, modules);
        return true;
    }

    private static ModuleContainer openContainer(final Player player, final int shelfSlot) {
        if (!(player instanceof IBackpackHost host)) return null;
        for (AbstractBackpackMenu menu : host.getBackpackMenus()) {
            if (menu instanceof ModuleMenu moduleMenu
                    && moduleMenu.shelfSlot == shelfSlot
                    && !moduleMenu.slots.isEmpty()
                    && moduleMenu.slots.get(0).container instanceof ModuleContainer container) {
                return container;
            }
        }
        return null;
    }

    private static int findSlot(final ModuleContainer container, final Holder<Item> item, final ItemStack existing) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) continue;
            if (!Inventory.isUsableForCrafting(stack)) continue;
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) continue;
            return i;
        }
        return -1;
    }

    private static int roomInInventory(final Inventory inventory, final ItemStack stack) {
        for (int i = 0; i < 36; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, stack)) continue;
            int room = Math.min(slot.getMaxStackSize(), stack.getMaxStackSize()) - slot.getCount();
            if (room > 0) return room;
        }
        return inventory.getFreeSlot() != -1 ? stack.getMaxStackSize() : 0;
    }

    private static void refreshShelfMenu(final Player player, final List<ItemStack> list) {
        if (!(player instanceof IBackpackHost host)) return;
        for (AbstractBackpackMenu menu : host.getBackpackMenus()) {
            if (menu instanceof ShelfMenu shelfMenu
                    && !shelfMenu.slots.isEmpty()
                    && shelfMenu.slots.get(0).container instanceof ShelfContainer shelf) {
                for (int i = 0; i < list.size(); i++) {
                    if (!ItemStack.matches(shelf.getItem(i), list.get(i))) {
                        shelf.setItem(i, list.get(i).copy());
                    }
                }
            }
        }
    }
}
