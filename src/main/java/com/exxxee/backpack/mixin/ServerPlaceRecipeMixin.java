package com.exxxee.backpack.mixin;

import com.exxxee.backpack.container.BackpackRecipeSupport;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.recipebook.ServerPlaceRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 点配方时先把模块里的材料搬进物品栏。
 *
 * 原版 {@code moveItemToGrid} 只在玩家物品栏的格子里找材料；找不到就整次摆放弃。
 * 这里在它找之前补一格：物品栏里没有匹配的，就从模块里搬一份进来，让它照常拿。
 */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {

    @Shadow
    @Final
    private Inventory inventory;

    @Inject(method = "moveItemToGrid(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/core/Holder;I)I", at = @At("HEAD"))
    private void backpack$pullFromModules(final Slot targetSlot, final Holder<Item> item, final int count,
                                          final CallbackInfoReturnable<Integer> cir) {
        if (this.inventory.findSlotMatchingCraftingIngredient(item, targetSlot.getItem()) != -1) return;
        BackpackRecipeSupport.pull(this.inventory, this.inventory.player, item, targetSlot.getItem(), count);
    }
}
