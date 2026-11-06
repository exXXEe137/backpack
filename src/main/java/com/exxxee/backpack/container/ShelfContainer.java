package com.exxxee.backpack.container;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.exxxee.backpack.item.BackpackItems.MODULE;

public class ShelfContainer implements Container {

    private final Player player;
    private final ItemStack shelf;
    private final NonNullList<ItemStack> items;

    public ShelfContainer(Player player) {
        this.player = player;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        this.shelf = chest.is(BackpackItems.BACKPACK_SHELF) ? chest : ItemStack.EMPTY;
        List<ItemStack> modules = this.shelf.getOrDefault(BackpackDataComponents.SHELF_MODULES, List.of());
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
        if (slot < 0 || slot >= this.items.size() || this.shelf.isEmpty()) return;
        this.items.set(slot, stack);
        this.shelf.set(BackpackDataComponents.SHELF_MODULES, this.items);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(serverPlayer.getInventory().createInventoryUpdatePacket(EquipmentSlot.CHEST.getIndex(36)));
        }
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
