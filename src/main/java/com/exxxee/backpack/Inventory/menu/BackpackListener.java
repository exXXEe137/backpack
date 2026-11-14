package com.exxxee.backpack.Inventory.menu;

import net.minecraft.world.item.ItemStack;

public interface BackpackListener {
    void slotChanged(AbstractBackpackMenu menu, int slotIndex, ItemStack itemStack);

    void dataChanged(AbstractBackpackMenu menu, int id, int value);
}
