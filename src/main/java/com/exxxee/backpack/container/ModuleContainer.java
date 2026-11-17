package com.exxxee.backpack.container;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.exxxee.backpack.item.BackpackItems.MODULE;

public class ModuleContainer implements Container {

    private ItemStack itemStack;
    private final NonNullList<ItemStack> items;
    private AbstractBackpackMenu menu;

    public ModuleContainer(ItemStack stack) {
        this.itemStack = stack;
        this.items = NonNullList.withSize(27, ItemStack.EMPTY);
        this.load();
    }

    @Override
    public boolean canPlaceItem (int index, ItemStack stack) {
        return !stack.is(MODULE);
    }

    public ItemStack stack() {
        return this.itemStack;
    }

    public void setStack(ItemStack stack) {
        this.itemStack = stack;
        this.load();
        this.notifyMenu();
    }

    public void setMenu(AbstractBackpackMenu menu) {
        this.menu = menu;
    }

    private void load() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        ModuleInventoryData data = this.itemStack.get(BackpackDataComponents.MODULE_INVENTORY);
        if (data == null) return;
        NonNullList<ItemStack> loaded = data.toList();
        for (int i = 0; i < this.items.size() && i < loaded.size(); i++) {
            this.items.set(i, loaded.get(i));
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
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
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
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
        if (slot < 0 || slot >= this.items.size()) return;
        this.items.set(slot, stack);
        this.flush();
        this.notifyMenu();
    }

    private void notifyMenu() {
        if (this.menu != null) {
            this.menu.slotsChanged(this);
        }
    }

    public void flush() {
        if (this.itemStack.isEmpty()) return;
        List<ItemStack> snapshot = new ArrayList<>(this.items.size());
        for (int i = 0; i < this.items.size(); i++) {
            snapshot.add(this.items.get(i).copy());
        }
        this.itemStack.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(snapshot));
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
