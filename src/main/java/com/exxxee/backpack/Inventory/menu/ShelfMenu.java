package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.Inventory.slot.ShelfSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShelfMenu extends AbstractBackpackMenu {

    public ShelfMenu(int windowId, Player player, Container container) {
        super(windowId, player);
        for (int i = 0; i < container.getContainerSize(); i++) {
            this.addSlot(new ShelfSlot(container, i, 4, 8 + i * 18));
        }
    }


    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

}
