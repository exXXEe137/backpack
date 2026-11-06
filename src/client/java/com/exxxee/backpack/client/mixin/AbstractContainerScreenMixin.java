package com.exxxee.backpack.client.mixin;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.client.gui.windows.MenuWindows;
import com.exxxee.backpack.client.gui.windows.ModuleWindow;
import com.exxxee.backpack.client.gui.windows.ShelfWindow;
import com.exxxee.backpack.client.gui.windows.ShelfWindowHost;
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
public abstract class AbstractContainerScreenMixin implements IBackpackHost, ShelfWindowHost {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Unique private ShelfMenu shelfMenu;
    @Unique private ShelfWindow shelfWindow;
    @Unique private List<ModuleMenu> moduleMenus  = new ArrayList<>(4);
    @Unique private List<ModuleWindow> moduleWindows  = new ArrayList<>(4);

    @Override public AbstractBackpackMenu getShelfMenu() {
        return this.shelfMenu;
    }
    @Override public void setShelfMenu(AbstractBackpackMenu menu) {
        this.shelfMenu = (ShelfMenu)menu;
    }
    @Override public AbstractBackpackMenu getModuleMenu(int index) {
        return this.moduleMenus.get(index);
    }
    @Override public void setModuleMenu(AbstractBackpackMenu menu, int index) {
        this.moduleMenus.set(index, (ModuleMenu) menu);
    }

    @Override public void setShelfWindow (ShelfWindow window) {
        this.shelfWindow = (ShelfWindow)window;
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void backpack$init (CallbackInfo ci) {
        if (this.shelfWindow != null) return;
        AbstractBackpackMenu menu = ((IBackpackHost) Minecraft.getInstance().player).getShelfMenu();
        if (menu == null) return;
        this.shelfWindow = MenuWindows.shelf(menu);
        this.shelfMenu = (ShelfMenu) menu;
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    public void backpack$containerTick (CallbackInfo ci) {}

    @Inject(method = "extractContents", at = @At("TAIL"))
    public void backpack$extractRenderState (GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (this.shelfWindow == null) return;
        this.shelfWindow.init(this.leftPos, this.topPos);
        this.shelfWindow.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    public void backpack$mouseClicked (MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {

    }

    @Inject(method = "mouseReleased", at = @At("TAIL"))
    public void backpack$mouseReleased (MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
    }

    @Inject(method = "mouseDragged", at = @At("TAIL"))
    public void backpack$mouseDeagged (MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {

    }

}