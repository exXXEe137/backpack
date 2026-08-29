package com.exxxee.backpack.client.mixin;

import com.exxxee.backpack.client.guirender.PanelWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.exxxee.backpack.Network.paylo.ModuleActionPayload;
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

    @Shadow
    protected AbstractContainerMenu menu;

    @Shadow
    public void clearDraggingState() {
    }

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

    @Inject(method = "extractTooltip", at = @At("TAIL"))
    public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        panel.extractTooltip(graphics, mouseX, mouseY);
    }

    // 自定义槽位不在原版 menu.slots 中，因此先由 PanelWidget 接管点击。
    @Inject (method = "mouseClicked", at = @At("HEAD"),  cancellable = true)
    public void mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (panel.mouseClicked(event, doubleClick)) {
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        if (!doubleClick && event.button() == 0 && event.hasShiftDown() && panel.hasInstalledModule()) {
            int slotIndex = getHoveredVanillaSlot(event.x(), event.y());
            if (slotIndex >= 0) {
                ClientPlayNetworking.send(new ModuleActionPayload(
                        ModuleActionPayload.QUICK_MOVE_TO_MODULES, slotIndex, 0));
                panel.cancelPointer();
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        if (!doubleClick) {
            panel.beginExternalPointer(event);
        }
    }

    // 拖拽状态由自定义面板维护，阻止原版槽位状态机误处理。
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void mouseDragged(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (panel.mouseDragged(event, dragX, dragY)) {
            clearDraggingState();
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    // 释放事件交给面板完成普通点击或提交拖拽。
    @Inject (method = "mouseReleased",at = @At("HEAD"), cancellable = true)
    public void mouseReleased (MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (panel.mouseReleased(event)) {
            clearDraggingState();
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (panel.keyPressed(event)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Unique
    private int getHoveredVanillaSlot(double mouseX, double mouseY) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.isActive()
                    && mouseX >= leftPos + slot.x - 1
                    && mouseX < leftPos + slot.x + 17
                    && mouseY >= topPos + slot.y - 1
                    && mouseY < topPos + slot.y + 17) {
                return i;
            }
        }
        return -1;
    }

}

