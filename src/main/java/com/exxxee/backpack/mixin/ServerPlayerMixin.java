package com.exxxee.backpack.mixin;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Network.payload.CloseWindowPayload;
import com.exxxee.backpack.Network.payload.OpenWindowPayload;
import com.exxxee.backpack.Network.payload.ShelfContentPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;


@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends PlayerMixin {

    // @Unique private AbstractBackpackMenu lastShelfMenu;
    //
    // @Inject(method = "tick", at = @At("TAIL"))
    // private void backpack$tick(CallbackInfo ci) {
    //
    //     ServerPlayer player = (ServerPlayer)(Object)this;
    //
    //     if (lastShelfMenu != this.shelfMenu) {
    //         if (shelfMenu != null) {
    //             this.backpack$open(player);
    //         } else {
    //             ServerPlayNetworking.send(player, new CloseWindowPayload(this.lastShelfMenu.windowId));
    //         }
    //         lastShelfMenu = shelfMenu;
    //     }
    //
    // }
    //
    // @Unique private void backpack$open(ServerPlayer player) {
    //     ServerPlayNetworking.send(player, new OpenWindowPayload(this.shelfMenu.windowId, "generic_1x4"));
    //     List<ItemStack> items = new ArrayList<>();
    //     for (Slot slot : shelfMenu.slots) {
    //         items.add(slot.getItem().copy());
    //     }
    //     ServerPlayNetworking.send(player, new ShelfContentPayload(items));
    // }

}
