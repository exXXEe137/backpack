package com.exxxee.backpack.mixin;

import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "onEquipItem", at = @At("TAIL"))
    public void backpack$onOnEquipItem(final EquipmentSlot slot, final ItemStack oldStack, final ItemStack stack, final CallbackInfo info) {
        if (slot != EquipmentSlot.CHEST) return;
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof Player player) {
            if (stack.is(BackpackItems.BACKPACK_SHELF)) {
                ((IBackpackHost)(Object) player).setBackpackMenu(new ShelfMenu(player, stack));
            } else {
                ((IBackpackHost)(Object) player).setBackpackMenu(null);
            }
        }
    }

}
