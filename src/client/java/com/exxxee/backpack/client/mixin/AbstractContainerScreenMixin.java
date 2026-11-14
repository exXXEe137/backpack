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
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements IBackpackHost, BackpackWindowHost {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Unique private final Map<Integer, Container> backpackContainers = new LinkedHashMap<>();
    @Unique private final Map<Integer, AbstractBackpackMenu> backpackMenus = new LinkedHashMap<>();
    @Unique private final Map<Integer, AbstractBackpackWindows<?>> backpackWindows = new LinkedHashMap<>();

    @Override public Collection<Container> getBackpackContainers() {
        return this.backpackContainers.values();
    }

    @Override public Container getBackpackContainer(int windowId) {
        return this.backpackContainers.get(windowId);
    }

    @Override public void setBackpackContainer(int windowId, Container container) {
        if (container == null) {
            this.backpackContainers.remove(windowId);
        } else {
            this.backpackContainers.put(windowId, container);
        }
    }

    @Override public Collection<AbstractBackpackMenu> getBackpackMenus() {
        return this.backpackMenus.values();
    }

    @Override public AbstractBackpackMenu getBackpackMenu (int windowId) {
        return this.backpackMenus.get(windowId);
    }

    @Override public void setBackpackMenu (int windowId, AbstractBackpackMenu backpackMenu) {
        if (backpackMenu == null) {
            this.backpackMenus.remove(windowId);
        } else {
            this.backpackMenus.put(windowId, backpackMenu);
        }
    }

    @Override public void setBackpackWindow(int windowId, AbstractBackpackWindows<?> window) {
        if (window == null) {
            this.backpackWindows.remove(windowId);
        } else {
            this.backpackWindows.put(windowId, window);
        }
    }


    @Inject(method = "init", at = @At("TAIL"))
    public void backpack$init (CallbackInfo ci) {
        for (AbstractBackpackMenu menu : ((IBackpackHost) Minecraft.getInstance().player).getBackpackMenus()) {
            if (this.backpackWindows.containsKey(menu.windowId)) continue;
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
    public void backpack$containerTick (CallbackInfo ci) {
        for (AbstractBackpackWindows<?> window : this.backpackWindows.values()) {
            window.tick();
        }
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    public void backpack$extractRenderState (GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        for (AbstractBackpackWindows<?> window : backpackWindows.values()) {
            window.init(this.leftPos, this.topPos);
            window.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"),  cancellable = true)
    public void backpack$mouseClicked (MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        for (AbstractBackpackWindows<?> window : backpackWindows.values()) {
            if (window.isMouseOver(event.x(), event.y())) {
                cir.setReturnValue(window.mouseClicked(event, doubleClick));
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    public void backpack$mouseReleased (MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        for (AbstractBackpackWindows<?> window : backpackWindows.values()) {
            if (window.isMouseOver(event.x(), event.y())) {
                cir.setReturnValue(window.mouseReleased(event));
            }
        }
    }

}