package com.exxxee.backpack.client.mixin;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.client.gui.windows.*;
import net.minecraft.client.Minecraft;
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

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements IBackpackHost, BackpackWindowHost {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Unique private final List<AbstractBackpackMenu> backpackMenus = new ArrayList<>();
    @Unique private final List<AbstractBackpackWindows<?>> backpackWindows = new ArrayList<>();

    @Override public List<AbstractBackpackMenu> getBackpackMenus() {
        return this.backpackMenus;
    }

    @Override public AbstractBackpackMenu getBackpackMenu (int windowId) {
        if (windowId < 0 || windowId >= this.backpackMenus.size()) return null;
        return this.backpackMenus.get(windowId);
    }

    @Override public void setBackpackMenu (int windowId, AbstractBackpackMenu backpackMenu) {
        while (this.backpackMenus.size() <= windowId) {
            this.backpackMenus.add(null);
        }
        this.backpackMenus.set(windowId, backpackMenu);
    }

    @Override public void setBackpackWindow(int windowId, AbstractBackpackWindows<?> window) {
        while (this.backpackWindows.size() <= windowId) {
            this.backpackWindows.add(null);
        }
        this.backpackWindows.set(windowId, window);
    }


    @Inject(method = "init", at = @At("TAIL"))
    public void backpack$init (CallbackInfo ci) {
        for (AbstractBackpackMenu menu : ((IBackpackHost) Minecraft.getInstance().player).getBackpackMenus()) {
            if (menu == null) continue;
            if (menu.windowId < this.backpackWindows.size() && this.backpackWindows.get(menu.windowId) != null)
                continue;
            AbstractBackpackWindows<?> window = MenuWindows.window(menu);
            if (window == null) continue;
            this.setBackpackWindow(menu.windowId, window);
        }

//        if (this.shelfWindow != null) return;
//        AbstractBackpackMenu menu = ((IBackpackHost) Minecraft.getInstance().player).getShelfMenu();
//        if (menu == null) return;
//        this.shelfWindow = MenuWindows.shelf(menu);
//        this.shelfMenu = (ShelfMenu) menu;

    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    public void backpack$containerTick (CallbackInfo ci) {}

    @Inject(method = "extractContents", at = @At("TAIL"))
    public void backpack$extractRenderState (GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        for (AbstractBackpackWindows<?> window : backpackWindows) {
            if (window != null) {
                window.init(this.leftPos, this.topPos);
                window.extractRenderState(graphics, mouseX, mouseY, a);
            }
        }

//        if (this.shelfWindow == null) return;
//        this.shelfWindow.init(this.leftPos, this.topPos);
//        this.shelfWindow.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"),  cancellable = true)
    public void backpack$mouseClicked (MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        for (AbstractBackpackWindows<?> window : backpackWindows) {
            if (window != null && window.isMouseOver(event.x(), event.y())) {
                cir.setReturnValue(window.mouseClicked(event, doubleClick));
            }
        }

//        if (this.shelfWindow != null && this.shelfWindow.isMouseOver(event.x(), event.y())) {
//            cir.setReturnValue(this.shelfWindow.mouseClicked(event, doubleClick));
//        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    public void backpack$mouseReleased (MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        for (AbstractBackpackWindows<?> window : backpackWindows) {
            if (window != null && window.isMouseOver(event.x(), event.y())) {
                cir.setReturnValue(window.mouseReleased(event));
            }
        }

//        if (this.shelfWindow != null && this.shelfWindow.isMouseOver(event.x(), event.y())) {
//            cir.setReturnValue(this.shelfWindow.mouseReleased(event));
//        }
    }
//
//    @Inject(method = "mouseDragged", at = @At("TAIL"))
//    public void backpack$mouseDragged (MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
//
//    }
//
//    @Inject(method = "mouseScrolled", at = @At("TAIL"))
//    public void backpack$mouseScrolled (double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
//
//    }

}