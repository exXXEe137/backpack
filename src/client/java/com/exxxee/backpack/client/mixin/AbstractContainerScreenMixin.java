package com.exxxee.backpack.client.mixin;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import com.exxxee.backpack.client.guirender.PanelWidget;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements IBackpackHost {

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;

    @Unique
    private PanelWidget panel;

    @Unique
    private AbstractBackpackMenu backpackMenu;

    @Override
    public AbstractBackpackMenu getBackpackMenu() {
        return this.backpackMenu;
    }

    @Override
    public void setBackpackMenu(AbstractBackpackMenu menu) {
        this.backpackMenu = menu;
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void init (CallbackInfo ci) {
        // 任何 AbstractContainerScreen 打开都会走这里（含按 E 的背包界面）
        // 原版时序：handleOpenScreen 只覆盖实体容器；玩家背包(InventoryScreen)是本地打开，不走协议包
        Player player = Minecraft.getInstance().player;
        if (player != null && BackpackDataHelper.hasShelf(player)) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            this.setBackpackMenu(new ShelfMenu(player, chest));
            ExxxeeBackpack.LOGGER.info("[backpack] init: created ShelfMenu with chest={}", chest);
        } else {
            this.setBackpackMenu(null);
        }
        panel = new PanelWidget(leftPos, topPos, this.getBackpackMenu());
        ExxxeeBackpack.LOGGER.info("[backpack] panel created at leftPos={} topPos={}", leftPos, topPos);
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    public void containerTick (CallbackInfo ci) {
        ExxxeeBackpack.LOGGER.info("[backpack] containerTick, menu={}", this.getBackpackMenu());
        panel.tick();
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    public void extractRenderState (GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        panel.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Inject (method = "mouseClicked", at = @At("HEAD"),  cancellable = true)
    public void mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (panel.mouseClicked(event, doubleClick)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject (method = "mouseReleased",at = @At("HEAD"), cancellable = true)
    public void mouseReleased (MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
            if (panel.mouseReleased(event)) {
                cir.setReturnValue(true);
                cir.cancel();
            }
    }

}