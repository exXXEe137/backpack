package com.exxxee.backpack.mixin;

import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends PlayerMixin {

    @Inject(method = "openMenu", at = @At("RETURN"))
    private void backpack$onOpenMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (cir.getReturnValue().isEmpty()) return;  // 失败不创建
        ServerPlayer self = (ServerPlayer)(Object)this;
        ItemStack chest = self.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(BackpackItems.BACKPACK_SHELF)) return;
        this.setBackpackMenu(new ShelfMenu(self, chest));
    }
}