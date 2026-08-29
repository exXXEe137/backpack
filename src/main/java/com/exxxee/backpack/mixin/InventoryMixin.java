package com.exxxee.backpack.mixin;

import com.exxxee.backpack.api.IExtendedInventory;
import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import com.exxxee.backpack.item.BackpackItems;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Inventory.class)
public abstract class InventoryMixin implements IExtendedInventory {
    @Shadow
    @Final
    public Player player;
    @Shadow public abstract int getContainerSize ();

    @Unique
    private final int extraSlot = 108;
    @Unique
    private final NonNullList<ItemStack> extraContent = NonNullList.withSize(extraSlot, ItemStack.EMPTY);
    @Unique boolean[] enabledSlots = new boolean[extraSlot];
    @Unique boolean dirty = false;

    @Inject(method = "load", at = @At("TAIL"))
    private void backpack$onLoad (ValueInput.TypedInputList<ItemStackWithSlot> input, CallbackInfo ci) {
        backpack$syncFromModules();
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private void backpack$onAddReturn (int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (dirty) {
            dirty =  false;
            syncToModules();
        }
    }

    @Unique
    public boolean backpackRemainingSpace (ItemStack itemInSlot, ItemStack stack) {
        return !itemInSlot.isEmpty()
                && ItemStack.isSameItemSameComponents(itemInSlot, stack )
                && itemInSlot.isStackable()
                && itemInSlot.getCount() < itemInSlot.getMaxStackSize();
    }

    @ModifyReturnValue(method = "getSlotWithRemainingSpace", at = @At("RETURN"))
    public int backpck$onGetSlotWithRemainingSpace (int ori, ItemStack stack) {
        if (ori != -1) {return ori;}

        for (int i = 0; i < this.extraContent.size(); i++) {
            ItemStack itemInSlot = this.extraContent.get(i);
            if (enabledSlots[i] && backpackRemainingSpace(itemInSlot, stack)) {
                dirty =  true;
                return getContainerSize() + i;
            }
        }
        return -1;
    }

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    public void backpack$onSetItems (int slot, ItemStack stack, CallbackInfo ci) {
        if (isExtraSlot(slot)) {
            extraContent.set(getExtraSlot(slot), stack);
            dirty =  true;
            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "getFreeSlot", at = @At("RETURN"))
    private int backpack$onGetFreeSlot (int ori) {
       if (ori != -1) {return ori;}
        for (int j = 0; j < extraSlot; j++) {
            if (enabledSlots[j] && extraContent.get(j).isEmpty()) {
                return this.getContainerSize() + j;
            }
        }
        return -1;
    }

    @Unique private boolean isExtraSlot (int slot) {
        return slot >= getContainerSize() && slot < getContainerSize() + 108;
    }

    @Unique private int getExtraSlot (int slot) {
        return slot - getContainerSize();
    }

    @Inject(method = "getItem", at = @At("HEAD"),  cancellable = true)
    public void backpackon$GetItem (int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (isExtraSlot(slot)) {
            cir.setReturnValue(extraContent.get(getExtraSlot(slot)));
        }
    }

    @Override
    public void backpack$syncFromModules () {
        for (int i = 0; i < 4; i++) {
            ItemStack module = BackpackDataHelper.getModule(player, i);
            if (module != null && module.getItem() == BackpackItems.BACKPACK_MODULE) {
                NonNullList<ItemStack> inv = BackpackDataHelper.getModuleDataInList(player, i);
                for (int j = 0; j < 27; j++) {
                    enabledSlots[j + i * 27] = true;
                    extraContent.set(i * 27 + j, inv.get(j));
                }
            } else {
                for (int j = 0; j < 27; j++) {
                    enabledSlots[j + i * 27] = false;
                    extraContent.set(i * 27 + j, ItemStack.EMPTY);
                }
            }
        }
    }
    @Unique private void syncToModules () {
        if (!(this.player instanceof ServerPlayer)) return;

        List<ItemStack> modules = BackpackDataHelper.getModules(player);
        if (modules == null) return;
        modules = new ArrayList<>(modules);
        for (int i = 0; i < 4; i++) {
            ItemStack module = BackpackDataHelper.getModule(player, i);
            if (module == null || module.getItem() != BackpackItems.BACKPACK_MODULE) continue;
            NonNullList<ItemStack> newModuleInventory = NonNullList.withSize(27, ItemStack.EMPTY);
            for (int j = 0; j < 27; j++) {
                newModuleInventory.set(j, this.extraContent.get(j + i * 27));
            }
            module = modules.get(i).copy();
            module.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(newModuleInventory));
            modules.set(i, module);
        }
        BackpackDataHelper.updateBackpack(player, modules);
    }

}

