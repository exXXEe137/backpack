package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.container.ModuleContainer;
import com.exxxee.backpack.mixin.AbstractContainerMenuAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class QuickMove {
    private QuickMove() {
    }

    public static void moveOutOfBackpack(Player player, ItemStack stack) {
        AbstractContainerMenu host = player.containerMenu;
        if (host == null || stack.isEmpty()) return;
        if (!isCreativePicker(host) && host != player.inventoryMenu && moveRanges(host, player, stack, false, false)) {
            return;
        }
        moveRanges(host, player, stack, true, true);
    }

    /**
     * 创造物品栏那份菜单：前 45 格是物品列表（CONTAINER），不是真容器。
     * 把它当箱子往里塞，东西会掉进物品列表里，服务端也拿不到。
     */
    private static boolean isCreativePicker(AbstractContainerMenu menu) {
        return menu.getClass().getName().endsWith("CreativeModeInventoryScreen$ItemPickerMenu");
    }

    public static void offerPlayerInventoryRemainder(AbstractContainerMenu host, Player player, int slotIndex) {
        if (host instanceof AbstractBackpackMenu || player.containerMenu != host) return;
        if (slotIndex < 0 || slotIndex >= host.slots.size()) return;
        Slot slot = host.slots.get(slotIndex);
        if (slot.container != player.getInventory() || !slot.hasItem() || !slot.mayPickup(player)) return;
        ModuleMenu module = invoked(player);
        if (module == null || module.slots.isEmpty()) return;
        ItemStack stack = slot.getItem();
        int before = stack.getCount();
        moveIntoMenu(module, stack);
        if (stack.getCount() == before) return;
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (module.slots.get(0).container instanceof ModuleContainer modules) {
            modules.flush();
        }
        module.broadcastChanges();
    }

    private static ModuleMenu invoked(Player player) {
        if (!(player instanceof IBackpackHost host)) return null;
        for (AbstractBackpackMenu menu : host.getBackpackMenus()) {
            if (menu instanceof ModuleMenu moduleMenu && moduleMenu.invoked) {
                return moduleMenu;
            }
        }
        return null;
    }

    private static void moveIntoMenu(AbstractContainerMenu menu, ItemStack stack) {
        AbstractContainerMenuAccessor access = (AbstractContainerMenuAccessor) menu;
        int guard = menu.slots.size() + 1;
        while (!stack.isEmpty() && guard-- > 0) {
            int before = stack.getCount();
            access.invokeMoveItemStackTo(stack, 0, menu.slots.size(), false);
            if (stack.getCount() == before) return;
        }
    }

    private static boolean moveRanges(AbstractContainerMenu host, Player player, ItemStack stack, boolean inventorySlots, boolean reverse) {
        int before = stack.getCount();
        AbstractContainerMenuAccessor access = (AbstractContainerMenuAccessor) host;
        if (reverse) {
            int index = host.slots.size();
            while (index > 0 && !stack.isEmpty()) {
                if (isPlayerInventory(host.slots.get(index - 1), player) != inventorySlots) {
                    index--;
                    continue;
                }
                int end = index;
                while (index > 0 && isPlayerInventory(host.slots.get(index - 1), player) == inventorySlots) {
                    index--;
                }
                access.invokeMoveItemStackTo(stack, index, end, true);
            }
        } else {
            int index = 0;
            while (index < host.slots.size() && !stack.isEmpty()) {
                if (isPlayerInventory(host.slots.get(index), player) != inventorySlots) {
                    index++;
                    continue;
                }
                int start = index;
                while (index < host.slots.size() && isPlayerInventory(host.slots.get(index), player) == inventorySlots) {
                    index++;
                }
                access.invokeMoveItemStackTo(stack, start, index, false);
            }
        }
        return stack.getCount() != before;
    }

    private static boolean isPlayerInventory(Slot slot, Player player) {
        return slot.container == player.getInventory();
    }
}
