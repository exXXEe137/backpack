package com.exxxee.backpack.Inventory.menu;

import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface BackpackSynchronizer {
    void sendInitialData(AbstractBackpackMenu menu, List<ItemStack> slotItems, int[] dataSlots);

    void sendSlotChange(AbstractBackpackMenu menu, int slotIndex, ItemStack itemStack);

    void sendDataChange(AbstractBackpackMenu menu, int id, int value);

    RemoteSlot createSlot();
}
