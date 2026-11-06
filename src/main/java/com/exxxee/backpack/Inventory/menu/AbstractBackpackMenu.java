package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.mixin.SlotAccessor;
import com.google.common.collect.Sets;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static net.minecraft.world.inventory.AbstractContainerMenu.*;

public abstract class AbstractBackpackMenu {

    protected final Player player;
    public final NonNullList<Slot> slots = NonNullList.create();

    private int quickcraftStatus;
    private final Set<Slot> quickcraftSlots = Sets.<Slot>newHashSet();
    @Nullable
    private int quickcraftType = 0;

    protected AbstractBackpackMenu(Player player) {
        this.player = player;
    }

    public void clicked(final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player) {
        try {
            this.doClick(slotIndex, buttonNum, containerInput, player);
        } catch (Exception var8) {
            CrashReport report = CrashReport.forThrowable(var8, "Container click");
            CrashReportCategory category = report.addCategory("Click info");
            category.setDetail("Menu Type", "BackpackMenu");
            category.setDetail("Menu Class", (CrashReportDetail<String>)(() -> this.getClass().getCanonicalName()));
            category.setDetail("Slot Count", this.slots.size());
            category.setDetail("Slot", slotIndex);
            category.setDetail("Button", buttonNum);
            category.setDetail("Type", containerInput);
            throw new ReportedException(report);
        }
    }

    public abstract ItemStack quickMoveStack(final Player player, final int slotIndex);
//===================================================doClick=====================================================//

    public void doClick(final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player) {
        Inventory inventory = player.getInventory();
        if (containerInput == ContainerInput.QUICK_CRAFT) {
            quickCraft(slotIndex, buttonNum, player);
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((containerInput == ContainerInput.PICKUP || containerInput == ContainerInput.QUICK_MOVE) && (buttonNum == 0 || buttonNum == 1)) {
            pickUpAndQuickMove(slotIndex, buttonNum, containerInput, player);
        } else if (containerInput == ContainerInput.SWAP && (buttonNum >= 0 && buttonNum < 9 || buttonNum == 40)) {
            swap(slotIndex, buttonNum, player, inventory);
        } else if (containerInput == ContainerInput.CLONE && player.hasInfiniteMaterials() && this.getCarried().isEmpty() && slotIndex >= 0) {
            itemClone(slotIndex, buttonNum, containerInput, player);
        } else if (containerInput == ContainerInput.THROW && this.getCarried().isEmpty() && slotIndex >= 0) {
            itemThrow(slotIndex, buttonNum, containerInput, player);
        } else if (containerInput == ContainerInput.PICKUP_ALL && slotIndex >= 0) {
            pickAll(slotIndex, buttonNum, containerInput, player);
        }
    }

    private void quickCraft (final int slotIndex, final int buttonNum, final Player player) {
        int expectedStatus = this.quickcraftStatus;
        this.quickcraftStatus = getQuickcraftHeader(buttonNum);
        if ((expectedStatus != 1 || this.quickcraftStatus != 2) && expectedStatus != this.quickcraftStatus) {
            this.resetQuickCraft();
        } else if (this.getCarried().isEmpty()) {
            this.resetQuickCraft();
        } else if (this.quickcraftStatus == 0) {
            this.quickcraftType = getQuickcraftType(buttonNum);
            if (isValidQuickcraftType(this.quickcraftType, player)) {
                this.quickcraftStatus = 1;
                this.quickcraftSlots.clear();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus == 1) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack carriedItemStack = this.getCarried();
            if (canItemQuickReplace(slot, carriedItemStack, true)
                    && slot.mayPlace(carriedItemStack)
                    && (this.quickcraftType == 2 || carriedItemStack.getCount() > this.quickcraftSlots.size())
                    && this.canDragTo(slot)) {
                this.quickcraftSlots.add(slot);
            }
        } else if (this.quickcraftStatus == 2) {
            if (!this.quickcraftSlots.isEmpty()) {
                if (this.quickcraftSlots.size() == 1) {
                    int slot = ((Slot)this.quickcraftSlots.iterator().next()).index;
                    this.resetQuickCraft();
                    this.doClick(slot, this.quickcraftType, ContainerInput.PICKUP, player);
                    return;
                }

                ItemStack source = this.getCarried().copy();
                if (source.isEmpty()) {
                    this.resetQuickCraft();
                    return;
                }

                int remaining = this.getCarried().getCount();

                for (Slot slot : this.quickcraftSlots) {
                    ItemStack carriedItemStack = this.getCarried();
                    if (slot != null
                            && canItemQuickReplace(slot, carriedItemStack, true)
                            && slot.mayPlace(carriedItemStack)
                            && (this.quickcraftType == 2 || carriedItemStack.getCount() >= this.quickcraftSlots.size())
                            && this.canDragTo(slot)) {
                        int carry = slot.hasItem() ? slot.getItem().getCount() : 0;
                        int maxSize = Math.min(source.getMaxStackSize(), slot.getMaxStackSize(source));
                        int newCount = Math.min(getQuickCraftPlaceCount(this.quickcraftSlots.size(), this.quickcraftType, source) + carry, maxSize);
                        remaining -= newCount - carry;
                        slot.setByPlayer(source.copyWithCount(newCount));
                    }
                }

                source.setCount(remaining);
                this.setCarried(source);
            }

            this.resetQuickCraft();
        } else {
            this.resetQuickCraft();
        }
    }
    protected void resetQuickCraft() {
        this.quickcraftStatus = 0;
        this.quickcraftSlots.clear();
    }
    private boolean canDragTo (final Slot slot) {
        return true;
    }

    private void pickUpAndQuickMove (final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player) {
        ClickAction clickAction = buttonNum == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
        if (slotIndex == -999) {
            if (!this.getCarried().isEmpty()) {
                if (clickAction == ClickAction.PRIMARY) {
                    player.drop(this.getCarried(), true);
                    this.setCarried(ItemStack.EMPTY);
                } else {
                    player.drop(this.getCarried().split(1), true);
                }
            }
        } else if (containerInput == ContainerInput.QUICK_MOVE) {
            if (slotIndex < 0) {
                return;
            }

            Slot slotx = this.slots.get(slotIndex);
            if (!slotx.mayPickup(player)) {
                return;
            }

            ItemStack clicked = this.quickMoveStack(player, slotIndex);

            while (!clicked.isEmpty() && ItemStack.isSameItem(slotx.getItem(), clicked)) {
                clicked = this.quickMoveStack(player, slotIndex);
            }
        } else {
            if (slotIndex < 0) {
                return;
            }

            Slot slotx = this.slots.get(slotIndex);
            ItemStack clicked = slotx.getItem();
            ItemStack carried = this.getCarried();
            player.updateTutorialInventoryAction(carried, slotx.getItem(), clickAction);
            if (!this.tryItemClickBehaviourOverride(player, clickAction, slotx, clicked, carried)) {
                if (clicked.isEmpty()) {
                    if (!carried.isEmpty()) {
                        int amount = clickAction == ClickAction.PRIMARY ? carried.getCount() : 1;
                        this.setCarried(slotx.safeInsert(carried, amount));
                    }
                } else if (slotx.mayPickup(player)) {
                    if (carried.isEmpty()) {
                        int amount = clickAction == ClickAction.PRIMARY ? clicked.getCount() : (clicked.getCount() + 1) / 2;
                        Optional<ItemStack> newCarried = slotx.tryRemove(amount, Integer.MAX_VALUE, player);
                        newCarried.ifPresent(itemsTaken -> {
                            this.setCarried(itemsTaken);
                            slotx.onTake(player, itemsTaken);
                        });
                    } else if (slotx.mayPlace(carried)) {
                        if (ItemStack.isSameItemSameComponents(clicked, carried)) {
                            int amount = clickAction == ClickAction.PRIMARY ? carried.getCount() : 1;
                            this.setCarried(slotx.safeInsert(carried, amount));
                        } else if (carried.getCount() <= slotx.getMaxStackSize(carried)) {
                            this.setCarried(clicked);
                            slotx.setByPlayer(carried);
                        }
                    } else if (ItemStack.isSameItemSameComponents(clicked, carried)) {
                        Optional<ItemStack> newCarried = slotx.tryRemove(clicked.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                        newCarried.ifPresent(itemsTaken -> {
                            carried.grow(itemsTaken.getCount());
                            slotx.onTake(player, itemsTaken);
                        });
                    }
                }
            }

            slotx.setChanged();
        }
    }
    private boolean tryItemClickBehaviourOverride(
            final Player player, final ClickAction clickAction, final Slot slot, final ItemStack clicked, final ItemStack carried
    ) {
        FeatureFlagSet enabledFeatures = player.level().enabledFeatures();
        return carried.isItemEnabled(enabledFeatures) && carried.overrideStackedOnOther(slot, clickAction, player)
                ? true
                : clicked.isItemEnabled(enabledFeatures) && clicked.overrideOtherStackedOnMe(carried, slot, clickAction, player, this.createCarriedSlotAccess());
    }
    private SlotAccess createCarriedSlotAccess() {
        return new SlotAccess() {
            {
                Objects.requireNonNull(AbstractBackpackMenu.this);
            }

            @Override
            public ItemStack get() {
                return AbstractBackpackMenu.this.getCarried();
            }

            @Override
            public boolean set(final ItemStack itemStack) {
                AbstractBackpackMenu.this.setCarried(itemStack);
                return true;
            }
        };
    }

    private void swap (final int slotIndex, final int buttonNum, final Player player, Inventory inventory) {
        ItemStack source = inventory.getItem(buttonNum);
        Slot target = this.slots.get(slotIndex);
        ItemStack targetItemStack = target.getItem();
        if (!source.isEmpty() || !targetItemStack.isEmpty()) {
            if (source.isEmpty()) {
                if (target.mayPickup(player)) {
                    inventory.setItem(buttonNum, targetItemStack);
                    ((SlotAccessor)(Object) target).invokeOnSwapCraft(targetItemStack.getCount());
                    target.setByPlayer(ItemStack.EMPTY);
                    target.onTake(player, targetItemStack);
                }
            } else if (targetItemStack.isEmpty()) {
                if (target.mayPlace(source)) {
                    int maxStackSize = target.getMaxStackSize(source);
                    if (source.getCount() > maxStackSize) {
                        target.setByPlayer(source.split(maxStackSize));
                    } else {
                        inventory.setItem(buttonNum, ItemStack.EMPTY);
                        target.setByPlayer(source);
                    }
                }
            } else if (target.mayPickup(player) && target.mayPlace(source)) {
                int maxStackSize = target.getMaxStackSize(source);
                if (source.getCount() > maxStackSize) {
                    target.setByPlayer(source.split(maxStackSize));
                    target.onTake(player, targetItemStack);
                    if (!inventory.add(targetItemStack)) {
                        player.drop(targetItemStack, true);
                    }
                } else {
                    inventory.setItem(buttonNum, targetItemStack);
                    target.setByPlayer(source);
                    target.onTake(player, targetItemStack);
                }
            }
        }
    }

    private void itemClone (final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player) {
        Slot slotx = this.slots.get(slotIndex);
        if (slotx.hasItem()) {
            ItemStack item = slotx.getItem();
            this.setCarried(item.copyWithCount(item.getMaxStackSize()));
        }
    }

    private void itemThrow (final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player) {
        Slot slotx = this.slots.get(slotIndex);
        int amount = buttonNum == 0 ? 1 : slotx.getItem().getCount();
        if (!player.canDropItems()) {
            return;
        }

        ItemStack itemStack = slotx.safeTake(amount, Integer.MAX_VALUE, player);
        player.drop(itemStack, true);
        player.handleCreativeModeItemDrop(itemStack);
        if (buttonNum == 1) {
            while (!itemStack.isEmpty() && ItemStack.isSameItem(slotx.getItem(), itemStack)) {
                if (!player.canDropItems()) {
                    return;
                }

                itemStack = slotx.safeTake(amount, Integer.MAX_VALUE, player);
                player.drop(itemStack, true);
                player.handleCreativeModeItemDrop(itemStack);
            }
        }
    }

    private void pickAll (final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player) {
        Slot slotxx = this.slots.get(slotIndex);
        ItemStack carried = this.getCarried();
        if (!carried.isEmpty() && (!slotxx.hasItem() || !slotxx.mayPickup(player))) {
            int start = buttonNum == 0 ? 0 : this.slots.size() - 1;
            int step = buttonNum == 0 ? 1 : -1;

            for (int pass = 0; pass < 2; pass++) {
                for (int i = start; i >= 0 && i < this.slots.size() && carried.getCount() < carried.getMaxStackSize(); i += step) {
                    Slot target = this.slots.get(i);
                    if (target.hasItem() && canItemQuickReplace(target, carried, true) && target.mayPickup(player) && this.canTakeItemForPickAll(carried, target)) {
                        ItemStack itemStack = target.getItem();
                        if (pass != 0 || itemStack.getCount() != itemStack.getMaxStackSize()) {
                            ItemStack removed = target.safeTake(itemStack.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                            carried.grow(removed.getCount());
                        }
                    }
                }
            }
        }
    }
    public boolean canTakeItemForPickAll(final ItemStack carried, final Slot target) {
        return true;
    }

//=======================================================================================================================//

    private ItemStack getCarried () {
        return player.containerMenu.getCarried();
    }
    private void setCarried (ItemStack itemStack) {
        player.containerMenu.setCarried(itemStack);
    }

}