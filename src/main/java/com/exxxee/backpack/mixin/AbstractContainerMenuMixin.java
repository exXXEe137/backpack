package com.exxxee.backpack.mixin;

import com.exxxee.backpack.Inventory.menu.QuickMove;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    @ModifyExpressionValue(
            method = "doClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack backpack$afterQuickMove(ItemStack moved, @Local(argsOnly = true, ordinal = 0) int slotIndex, @Local(argsOnly = true) Player player) {
        AbstractContainerMenu host = (AbstractContainerMenu) (Object) this;
        if (moved.isEmpty() || slotIndex < 0 || slotIndex >= host.slots.size() || !ItemStack.isSameItem(host.slots.get(slotIndex).getItem(), moved)) {
            QuickMove.offerPlayerInventoryRemainder(host, player, slotIndex);
        }
        return moved;
    }
}
