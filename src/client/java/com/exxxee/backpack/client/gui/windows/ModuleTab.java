package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Network.payload.ServerboundModuleInvokePayload;
import com.exxxee.backpack.api.IBackpackHost;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ModuleTab extends ImageButton {

    private static final WidgetSprites sprites = new WidgetSprites(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID,"shelf_tab"), Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID,"shelf_tab_selected"));
    private static final int width = 10;
    private static final int height = 19;
    private final int index;
    private boolean selected = false;
    private AbstractBackpackWindows<?> window;

    public ModuleTab(int x, int y,final int index, Button.OnPress onPress) {
        super(x, y, width, height, sprites, onPress);
        this.index = index;
    }

    public int index () {
        return this.index;
    }

//    private static void press(Button button) {
//        ModuleTab tab = (ModuleTab) button;
//        if (tab.isSelected()) {
//            tab.unselect();
//        } else {
//            tab.select();
//        }
//    }

    public void select() {
        this.selected = true;
        ModuleMenu moduleMenu = this.moduleMenu();
        if (moduleMenu == null) return;
        if (!moduleMenu.invoked) {
            moduleMenu.invoked = true;
            ClientPlayNetworking.send(new ServerboundModuleInvokePayload(moduleMenu.containerId, true));
        }
        if (!(Minecraft.getInstance().screen instanceof BackpackWindowHost host)) return;
        if (this.window != null) return;
        AbstractBackpackWindows<?> created = MenuWindows.window(moduleMenu);
        if (created == null) return;
        this.window = created;
        host.setBackpackWindow(moduleMenu.containerId, created);
    }

    public void unselect() {
        this.selected = false;
        ModuleMenu moduleMenu = this.moduleMenu();
        if (moduleMenu == null) return;
        if (moduleMenu.invoked) {
            moduleMenu.invoked = false;
            ClientPlayNetworking.send(new ServerboundModuleInvokePayload(moduleMenu.containerId, false));
        }
        if (!(Minecraft.getInstance().screen instanceof BackpackWindowHost host)) return;
        host.setBackpackWindow(moduleMenu.containerId, null);
    }

    private ModuleMenu moduleMenu() {
        for (AbstractBackpackMenu menu : ((IBackpackHost) Minecraft.getInstance().player).getBackpackMenus()) {
            if (menu instanceof ModuleMenu moduleMenu && moduleMenu.shelfSlot == this.index) {
                return moduleMenu;
            }
        }
        return null;
    }
    public boolean isSelected() {return selected;}

    @Override
    public void extractContents (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        Identifier sprite = this.sprites.get(this.isActive(), this.selected);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);

    }

}
