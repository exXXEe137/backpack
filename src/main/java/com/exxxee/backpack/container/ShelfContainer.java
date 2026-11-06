package com.exxxee.backpack.container;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.exxxee.backpack.item.BackpackItems.MODULE;

public class ShelfContainer implements Container {

    private final ItemStack itemStack;

    public ShelfContainer(ItemStack stack) {
        this.itemStack = stack;
    }

    @Override
    public boolean canPlaceItem (int index, ItemStack stack) {
        return stack.is(MODULE);
    }

    @Override
    public int getContainerSize() {
        return itemStack.get(BackpackDataComponents.SHELF_MODULES).size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemStack.get(BackpackDataComponents.SHELF_MODULES).get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack current =  getItem(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = current.split(count);
        setItem(slot, current.isEmpty() ? ItemStack.EMPTY : current);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        setItem(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        List<ItemStack> list = new ArrayList<>(itemStack.get(BackpackDataComponents.SHELF_MODULES));
        list.set(slot, stack);
        itemStack.set(BackpackDataComponents.SHELF_MODULES, list);
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {

    }
}
