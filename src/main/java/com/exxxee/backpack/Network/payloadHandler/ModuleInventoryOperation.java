package com.exxxee.backpack.Network.payloadHandler;

import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import com.exxxee.backpack.mixin.AbstractContainerMenuAccessor;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 27 格存储区操作。纯数据逻辑，服务端专用。
 * 入口 operation 只做分发，每个动作独立方法。
 * button: 0=左键(整叠) 1=右键(半叠/放一)  shift: 1=快速转移
 */
public class ModuleInventoryOperation {

    public void Operation(ServerPlayer player, int slotIndex, int button, int shift) {
        if (slotIndex < 0 || slotIndex >= 108) {
            return;
        }
        // 包层全局编号 → 数据层按模块拆分(每模块 27 格独立编码)
        int tabIndex = slotIndex / 27;
        int slotInTab = slotIndex % 27;

        NonNullList<ItemStack> items = readItems(player, tabIndex);
        if (items == null) {
            return;
        }

        if (shift == 1) {
            quickMove(player, items, slotInTab);
        } else if (button == 0) {
            clickLeft(player, items, slotInTab);
        } else if (button == 1) {
            clickRight(player, items, slotInTab);
        }

        writeBack(player, tabIndex, slotInTab, items);
    }

    /** 校验模块索引 + 读当前 27 格(不可变 contents → 可变缓存),失败返回 null */
    private NonNullList<ItemStack> readItems(ServerPlayer player, int tabIndex) {
        if (!BackpackDataHelper.isValidModuleIndex(player, tabIndex)) {
            return null;
        }
        ModuleInventoryData data = BackpackDataHelper.getModuleData(player, tabIndex);
        if (data == null) {
            return null;
        }
        return data.toList();
    }

    /** shift 快速转移：整叠进玩家背包 */
    private void quickMove(ServerPlayer player, NonNullList<ItemStack> items, int slotIndex) {
        ItemStack slotStack = items.get(slotIndex);
        if (slotStack.isEmpty()) return;
        AbstractContainerMenuAccessor menu = (AbstractContainerMenuAccessor) player.containerMenu;
        if (menu instanceof InventoryMenu) {
            boolean movedHotBar = menu.invokeMoveItemStackTo(slotStack, 36, 45, false);
            boolean movedInven = menu.invokeMoveItemStackTo(slotStack, 0, 36, false);
            if (!movedHotBar && !movedInven) return;
        } else {
            int end = player.containerMenu.slots.size() - 36;
            boolean moved = menu.invokeMoveItemStackTo(slotStack, 0, end, false);
            if (!moved) return;
        }
        items.set(slotIndex, slotStack);
    }

    /** 左键：整叠取 / 放 / 合并 / 交换 */
    private void clickLeft(ServerPlayer player, NonNullList<ItemStack> items, int slotIndex) {
        ItemStack slotStack = items.get(slotIndex);
        ItemStack carried = player.containerMenu.getCarried();

        if (carried.isEmpty()) {
            items.set(slotIndex, ItemStack.EMPTY);
            player.containerMenu.setCarried(slotStack);
        } else if (slotStack.isEmpty()) {
            items.set(slotIndex, carried);
            player.containerMenu.setCarried(ItemStack.EMPTY);
        } else if (ItemStack.isSameItemSameComponents(carried, slotStack)) {
            int space = slotStack.getMaxStackSize() - slotStack.getCount();
            int move = Math.min(space, carried.getCount());
            if (move > 0) {
                slotStack.grow(move);
                carried.shrink(move);
            }
            items.set(slotIndex, slotStack);
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else {
            items.set(slotIndex, carried);
            player.containerMenu.setCarried(slotStack);
        }
    }

    /** 右键：取半叠 / 放一个 / 合并一个 / 交换 */
    private void clickRight(ServerPlayer player, NonNullList<ItemStack> items, int slotIndex) {
        ItemStack slotStack = items.get(slotIndex);
        ItemStack carried = player.containerMenu.getCarried();

        if (carried.isEmpty()) {
            int half = (slotStack.getCount() + 1) / 2;
            ItemStack taken = slotStack.split(half);
            items.set(slotIndex, slotStack.isEmpty() ? ItemStack.EMPTY : slotStack);
            player.containerMenu.setCarried(taken);
        } else if (slotStack.isEmpty()) {
            ItemStack one = carried.split(1);
            items.set(slotIndex, one);
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else if (ItemStack.isSameItemSameComponents(carried, slotStack)) {
            if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                slotStack.grow(1);
                carried.shrink(1);
            }
            items.set(slotIndex, slotStack);
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else {
            items.set(slotIndex, carried);
            player.containerMenu.setCarried(slotStack);
        }
    }

    /** 写回：副本替换 + 发包 + 刷新影子都在 helper */
    private void writeBack(ServerPlayer player, int tabIndex, int slotIndex, NonNullList<ItemStack> items) {
        BackpackDataHelper.update(player, tabIndex, slotIndex, items.get(slotIndex));
    }
}
