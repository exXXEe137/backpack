package com.exxxee.backpack.container;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ModuleContainer implements Container {

    private ItemStack itemStack;

    public ModuleContainer(ItemStack stack) {
        this.itemStack = stack;
    }

    @Override
    public int getContainerSize() {
        return itemStack.get(BackpackDataComponents.MODULE_INVENTORY).toList().size();
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
        return itemStack.get(BackpackDataComponents.MODULE_INVENTORY).toList().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack current = getItem(slot);
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
        List<ItemStack> list = itemStack.get(BackpackDataComponents.MODULE_INVENTORY).toList();
        list.set(slot, stack);
        itemStack.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(list));
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
