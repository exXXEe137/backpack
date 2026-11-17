package com.exxxee.backpack.Inventory.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractBackpackMenu extends AbstractContainerMenu {

    protected final Player player;
    public WindowMenuType<?> menuType;

    protected AbstractBackpackMenu(int windowId, Player player) {
        super(null, windowId);
        this.player = player;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
    }

    @Override
    public ItemStack getCarried() {
        return this.player.containerMenu.getCarried();
    }

    @Override
    public void setCarried(ItemStack itemStack) {
        this.player.containerMenu.setCarried(itemStack);
    }

    protected final ItemStack quickMoveToHost(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        AbstractContainerMenu host = player.containerMenu;
        if (host == null || host == this) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        QuickMove.moveOutOfBackpack(player, stack);
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
