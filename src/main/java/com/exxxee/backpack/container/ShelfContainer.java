package com.exxxee.backpack.container;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.exxxee.backpack.item.BackpackItems.MODULE;

public class ShelfContainer implements Container {

    private final Player player;
    private final NonNullList<ItemStack> items;
    private AbstractBackpackMenu menu;

    public ShelfContainer(Player player) {
        this.player = player;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        List<ItemStack> modules = chest.is(BackpackItems.BACKPACK_SHELF)
                ? chest.getOrDefault(BackpackDataComponents.SHELF_MODULES, List.of())
                : List.of();
        int size = modules == null ? 0 : modules.size();
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        if (modules != null) {
            for (int i = 0; i < size; i++) {
                this.items.set(i, modules.get(i).copy());
            }
        }
    }

    @Override
    public boolean canPlaceItem (int index, ItemStack stack) {
        return stack.is(MODULE);
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
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.items.size()) return;
        this.items.set(slot, stack);
        this.flush();
        if (this.menu != null) {
            this.menu.slotsChanged(this);
        }
    }

    public void setMenu(AbstractBackpackMenu menu) {
        this.menu = menu;
    }

    /**
     * 只写数据，不发包、不广播。
     * 一次点击期间会被调用很多次，广播交给调用方在事务末尾统一做。
     * 身上没有背架时直接返回：不再往脱下来那件上写。
     */
    public void flush() {
        ItemStack chest = this.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(BackpackItems.BACKPACK_SHELF)) return;
        List<ItemStack> snapshot = new ArrayList<>(this.items.size());
        for (int i = 0; i < this.items.size(); i++) {
            snapshot.add(this.items.get(i).copy());
        }
        chest.set(BackpackDataComponents.SHELF_MODULES, snapshot);
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
