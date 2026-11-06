package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.Inventory.slot.BackpackSlot;
import com.exxxee.backpack.container.ModuleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public class ModuleMenu extends AbstractBackpackMenu {

//    public ModuleMenu(int windowId, Player player) {
//    }

    public ModuleMenu(int windowId, Player player, ModuleContainer container) {
        super(windowId, player);
        for (int i = 0; i < container.getContainerSize(); i++) {
            int col = i % 3;
            int row = i / 3;
            slots.add(new BackpackSlot(player, container, i, col * 18 + 16, row * 18));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

}