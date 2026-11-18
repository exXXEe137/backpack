package com.exxxee.backpack.mixin;

import com.exxxee.backpack.container.BackpackOverflow;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 物品栏溢出进背包。
 *
 * 拦在 HEAD：先问本体两个公开方法还有没有地方放，有就交回原版；没有才试模块。
 * 不能在 RETURN 兜底——创造模式下原版放不下时会直接把栈清零并返回 true（“创造模式不掉落”），
 * 返回 false 的分支根本走不到，所以捡起来就凭空没了。
 *
 * 显式指定 slot 的调用（{@code placeItemBackInInventory} 那种）不接管，只处理 {@code add(-1, stack)}。
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void backpack$overflow(final int slot, final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || slot >= 0) return;

        Inventory inventory = (Inventory) (Object) this;
        Player player = inventory.player;
        if (player == null || player.level().isClientSide()) return;

        if (inventory.getSlotWithRemainingSpace(stack) != -1) return;
        if (inventory.getFreeSlot() != -1) return;

        if (BackpackOverflow.insert(player, stack)) {
            cir.setReturnValue(true);
        }
    }
}
