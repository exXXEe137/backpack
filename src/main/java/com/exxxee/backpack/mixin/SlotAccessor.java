package com.exxxee.backpack.mixin;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Slot.class)
public interface SlotAccessor {

    @Invoker("onSwapCraft")
    void invokeOnSwapCraft(int slotNum);

    @Invoker("onQuickCraft")
    void invokeOnQuickCraft(ItemStack itemStack, int slotNum);

}
