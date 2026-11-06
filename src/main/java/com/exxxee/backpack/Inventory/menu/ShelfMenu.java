package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.Inventory.slot.BackpackSlot;
import com.exxxee.backpack.container.ShelfContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShelfMenu extends AbstractBackpackMenu {

    public ShelfMenu(Player player, ItemStack itemStack) {
        super(player);
        ShelfContainer container = new ShelfContainer(itemStack);
        for (int i = 0; i < container.getContainerSize(); i++) {
            slots.add(new BackpackSlot(player, container, i, 16, i * 18));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

}
