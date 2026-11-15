package com.exxxee.backpack.Inventory.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModuleSlot extends Slot {
    public ModuleSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(final ItemStack itemStack) {
        return this.container.canPlaceItem(this.getContainerSlot(), itemStack);
    }
}
