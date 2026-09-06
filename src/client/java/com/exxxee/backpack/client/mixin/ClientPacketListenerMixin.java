package com.exxxee.backpack.client.mixin;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleOpenScreen", at = @At("TAIL"))
    public void backpack$OnHandleOpenScreen(final ClientboundOpenScreenPacket packet, final CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Screen screen = mc.screen;
        ExxxeeBackpack.LOGGER.info("[backpack] handleOpenScreen: player={} screen={} (class={})",
                player != null, screen != null, screen != null ? screen.getClass().getSimpleName() : "null");
        if (player == null || screen == null) return;
        if (!(screen instanceof AbstractContainerScreen)) return;
        if (!BackpackDataHelper.hasShelf(player)) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ExxxeeBackpack.LOGGER.info("[backpack] handleOpenScreen: hasShelf=true, chest={}, creating ShelfMenu", chest);
        ((IBackpackHost)(Object) screen).setBackpackMenu(new ShelfMenu(player, chest));
    }

}