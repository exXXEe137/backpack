package com.exxxee.backpack.Network.payloadHandler;

import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DragOperation {
    // 服务端根据客户端经过的槽位重新计算拖拽结果，客户端数据只用于选槽。
    public void Operation(ServerPlayer player, int dragType, int sourceSlot, int[] slots) {
        if (dragType < 0 || dragType > 2 || slots == null || slots.length == 0
                || sourceSlot < -1 || sourceSlot >= 108) {
            return;
        }

        Set<Integer> uniqueSlots = new LinkedHashSet<>();
        for (int slot : slots) {
            if (slot < 0 || slot >= 108) {
                return;
            }
            uniqueSlots.add(slot);
        }
        if (uniqueSlots.isEmpty()) {
            return;
        }

        int moduleIndex = sourceSlot >= 0 ? sourceSlot / 27 : uniqueSlots.iterator().next() / 27;
        for (int slot : uniqueSlots) {
            if (slot / 27 != moduleIndex) {
                return;
            }
        }
        if (sourceSlot >= 0 && sourceSlot / 27 != moduleIndex) {
            return;
        }
        if (!BackpackDataHelper.isValidModuleIndex(player, moduleIndex)
                || !BackpackDataHelper.getModule(player, moduleIndex).is(BackpackItems.BACKPACK_MODULE)) {
            return;
        }

        NonNullList<ItemStack> moduleStacks = BackpackDataHelper.getModuleDataInList(player, moduleIndex);
        if (dragType == 2) {
            creativeDrag(player, moduleIndex, moduleStacks, sourceSlot, uniqueSlots);
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();
        // 从自定义槽位开始拖拽时，服务端在这里取出源物品。
        if (carried.isEmpty()) {
            if (sourceSlot < 0) {
                return;
            }
            int sourceIndex = sourceSlot % 27;
            ItemStack source = moduleStacks.get(sourceIndex);
            if (source.isEmpty()) {
                return;
            }
            carried = source.copy();
            moduleStacks.set(sourceIndex, ItemStack.EMPTY);
        }

        List<Integer> enabledSlots = slotToSet(uniqueSlots, moduleStacks, carried, sourceSlot);
        if (enabledSlots.isEmpty()) {
            return;
        }

        int baseAmount = dragType == 0 ? carried.getCount() / enabledSlots.size() : 1;
        int remainder = dragType == 0 ? carried.getCount() % enabledSlots.size() : 0;
        int moved = 0;
        for (int i = 0; i < enabledSlots.size(); i++) {
            int slotIndex = enabledSlots.get(i);
            ItemStack target = moduleStacks.get(slotIndex);
            int space = target.isEmpty() ? carried.getMaxStackSize() : carried.getMaxStackSize() - target.getCount();
            int amount = dragType == 0 ? baseAmount + (i < remainder ? 1 : 0) : 1;
            amount = Math.min(amount, space);
            if (amount <= 0) {
                continue;
            }

            moduleStacks.set(slotIndex, target.isEmpty()
                    ? carried.copyWithCount(amount)
                    : target.copyWithCount(target.getCount() + amount));
            moved += amount;
        }

        if (moved == 0) {
            return;
        }
        carried.shrink(moved);
        player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        BackpackDataHelper.updateBackpack(player, moduleIndex, moduleStacks);
        player.containerMenu.broadcastChanges();
    }

    // 创造模式中键拖拽复制整叠物品到可接受的目标槽位。
    private void creativeDrag(ServerPlayer player, int moduleIndex, NonNullList<ItemStack> moduleStacks,
                              int sourceSlot, Set<Integer> slots) {
        if (!player.getAbilities().instabuild) {
            return;
        }

        int sourceIndex = sourceSlot >= 0 ? sourceSlot % 27 : slots.iterator().next() % 27;
        ItemStack source = moduleStacks.get(sourceIndex);
        if (source.isEmpty()) {
            source = player.containerMenu.getCarried();
        }
        if (source.isEmpty()) {
            return;
        }

        ItemStack clone = source.copyWithCount(source.getMaxStackSize());
        boolean changed = false;
        for (int globalSlot : slots) {
            int slotIndex = globalSlot % 27;
            ItemStack target = moduleStacks.get(slotIndex);
            if (target.isEmpty() || ItemStack.isSameItemSameComponents(target, clone)) {
                moduleStacks.set(slotIndex, clone.copy());
                changed = true;
            }
        }
        if (!changed) {
            return;
        }

        player.containerMenu.setCarried(clone);
        BackpackDataHelper.updateBackpack(player, moduleIndex, moduleStacks);
        player.containerMenu.broadcastChanges();
    }

    private List<Integer> slotToSet(Set<Integer> slots, List<ItemStack> moduleStacks, ItemStack carried,
                                    int sourceSlot) {
        List<Integer> enabledSlots = new ArrayList<>();
        for (int globalSlot : slots) {
            if (globalSlot == sourceSlot) {
                continue;
            }
            int slotIndex = globalSlot % 27;
            ItemStack target = moduleStacks.get(slotIndex);
            int space = target.isEmpty() ? carried.getMaxStackSize() : carried.getMaxStackSize() - target.getCount();
            if (space > 0 && (target.isEmpty() || ItemStack.isSameItemSameComponents(target, carried))) {
                enabledSlots.add(slotIndex);
            }
        }
        return enabledSlots;
    }
}
