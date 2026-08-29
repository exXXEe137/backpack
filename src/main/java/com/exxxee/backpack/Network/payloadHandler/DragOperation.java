package com.exxxee.backpack.Network.payloadHandler;

import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DragOperation {
    public void Operation(ServerPlayer serverPlayer, int dragType, int[] slots) {
        ItemStack carriedStack = serverPlayer.containerMenu.getCarried();
        if (carriedStack.isEmpty()) return;
        int moduleIndex = slots[0] / 27;

        NonNullList<ItemStack> moduleStacks = BackpackDataHelper.getModuleDataInList(serverPlayer, moduleIndex);

        List<Integer> enable = this.slotToset(slots, moduleStacks, carriedStack);
        if (enable.isEmpty()) return;
        int countToSet = dragType == 0 ? carriedStack.getCount() / enable.size() : 1;

        for (int i = 0; i < enable.size(); i++) {
            int slotIndex = enable.get(i);
            int lostSize = moduleStacks.get(slotIndex).getMaxStackSize() - moduleStacks.get(slotIndex).getCount();
            int setCount = Math.min(lostSize, countToSet);
            int count = moduleStacks.get(slotIndex).getCount() + setCount;
            ItemStack carriedCopy = carriedStack.copyWithCount(count);
            BackpackDataHelper.update(serverPlayer, moduleIndex, slotIndex, carriedCopy);
            carriedStack.shrink(setCount);
        }
    }

    private List<Integer> slotToset (int[] slots, List<ItemStack> moduleStack, ItemStack carriedStack) {

        int slotIndex;
        List<Integer> enableSLots = new ArrayList<>();

        for (int num : slots) {
            slotIndex = num % 27;
            ItemStack slotStack = moduleStack.get(slotIndex);
            int setSize = slotStack.getMaxStackSize() - slotStack.getCount();
            if (slotStack.isEmpty() || (ItemStack.isSameItemSameComponents(slotStack, carriedStack) && setSize != 0)) {
                enableSLots.add(slotIndex);
            }
        }
        return enableSLots;
    }

}
