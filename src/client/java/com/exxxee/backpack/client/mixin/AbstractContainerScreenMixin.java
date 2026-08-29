package com.exxxee.backpack.client.mixin;

import com.exxxee.backpack.client.guirender.PanelWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;

    @Unique
    private PanelWidget panel;

    @Inject(method = "init", at = @At("TAIL"))
    public void init (CallbackInfo ci) {
        panel = new PanelWidget(leftPos, topPos);
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    public void containerTick (CallbackInfo ci) {
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

