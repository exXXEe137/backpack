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
 * 只在原版 {@code add} 一点都放不进去（返回 false）时接管：本体还能叠、还有空位时不会走到这里。
 * 不打开 InventoryMixin 里原来那套 {@code extraContent}：那份数据和模块物品上的
 * MODULE_INVENTORY 不是同一份。写入在 {@link BackpackOverflow} 里，走组件，和 ModuleContainer.flush 同一条。
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void backpack$overflow(final int slot, final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Player player = ((Inventory) (Object) this).player;
        if (player == null) return;
        if (BackpackOverflow.insert(player, stack)) {
            cir.setReturnValue(true);
        }
    }
}
