package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.Inventory.slot.BackpackSlot;
import com.exxxee.backpack.container.ShelfContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShelfMenu extends AbstractBackpackMenu {

    public ShelfMenu(int windowId, Player player) {
        this(windowId, player, new ShelfContainer(player));
    }

    public ShelfMenu(int windowId, Player player, Container container) {
        super(windowId, player);
        for (int i = 0; i < container.getContainerSize(); i++) {
            slots.add(new BackpackSlot(player, container, i, 4, 8 + i * 18));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

}
