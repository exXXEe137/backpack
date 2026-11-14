package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
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

    public ModuleTab(int x, int y,final int index) {
        super(x, y, width, height, sprites, ModuleTab::press);
        this.index = index;
    }

    public int index () {
        return this.index;
    }

    private static void press(Button button) {
        ModuleTab tab = (ModuleTab) button;
        if (tab.isSelected()) {
            tab.unselect();
        } else {
            tab.select();
        }
    }

    public void select() {
        this.selected = true;
        //开widnows（index）
    }
    public void unselect() {
        this.selected = false;
        //关widnwos（index）
    }
    public boolean isSelected() {return selected;}

    @Override
    public void extractContents (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        Identifier sprite = this.sprites.get(this.isActive(), this.selected);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);

    }

}
