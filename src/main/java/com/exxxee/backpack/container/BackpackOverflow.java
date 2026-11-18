package com.exxxee.backpack.container;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家物品栏装不下时的溢出：只把本体已经放不下的剩余，按架子格子下标顺序塞进已装备的模块。
 *
 * 写入走模块物品上的 MODULE_INVENTORY，和 ModuleContainer.flush 同一条；
 * 这只模块正开着菜单时，直接写菜单里的那份 ModuleContainer，再 flush。
 */
public final class BackpackOverflow {

    private BackpackOverflow() {
    }

    /**
     * @return true 表示至少塞进去了一部分（和原版 {@code Inventory.add} 的返回值语义一致）。
     */
    public static boolean insert(final Player player, final ItemStack stack) {
        if (stack.isEmpty() || player.level().isClientSide()) return false;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(BackpackItems.BACKPACK_SHELF)) return false;

        List<ItemStack> modules = chest.getOrDefault(BackpackDataComponents.SHELF_MODULES, List.of());
        List<ItemStack> list = new ArrayList<>(modules);
        boolean moved = false;

        for (int i = 0; i < list.size() && !stack.isEmpty(); i++) {
            ItemStack module = list.get(i);
            if (module.isEmpty() || !module.is(BackpackItems.MODULE)) continue;

            ModuleContainer container = openMenuContainer(player, i);
            if (container == null) {
                container = new ModuleContainer(module.copy());
            }
            moved |= insertInto(container, stack);
            container.flush();
            list.set(i, container.stack());
        }

        if (!moved) return false;

        chest.set(BackpackDataComponents.SHELF_MODULES, list);
        refreshShelfMenu(player, list);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
        }
        return true;
    }

    /** 架子窗口开着时，把改过的模块格同步进那份菜单，免得下次开模块读到旧内容。 */
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

    /** 这只模块正的菜单开着时，拿菜单里那份容器；写它等于写玩家看得见的那份。 */
    private static ModuleContainer openMenuContainer(final Player player, final int shelfSlot) {
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

    /** 先叠到已有的同类，再占空格。容器自己的 canPlaceItem 说了算。 */
    private static boolean insertInto(final Container container, final ItemStack stack) {
        boolean moved = false;
        int size = container.getContainerSize();

        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int slot = 0; slot < size && !stack.isEmpty(); slot++) {
                ItemStack target = container.getItem(slot);
                if (pass == 0) {
                    if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, stack)) continue;
                    int max = Math.min(target.getMaxStackSize(), stack.getMaxStackSize());
                    if (target.getCount() >= max) continue;
                    int add = Math.min(stack.getCount(), max - target.getCount());
                    ItemStack merged = target.copy();
                    merged.grow(add);
                    container.setItem(slot, merged);
                    stack.shrink(add);
                    moved = true;
                } else {
                    if (!target.isEmpty() || !container.canPlaceItem(slot, stack)) continue;
                    container.setItem(slot, stack.copy());
                    stack.setCount(0);
                    moved = true;
                }
            }
        }
        return moved;
    }
}
