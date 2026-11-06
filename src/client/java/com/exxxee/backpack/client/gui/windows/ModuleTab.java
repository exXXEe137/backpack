package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ModuleTab extends ImageButton {

    private static final WidgetSprites sprites = new WidgetSprites(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID,"moduletab"), Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID,"moduletab_selected"));
    private static final int width = 32;
    private static final int height = 20;
    private static int index;
    private boolean selected = false;

    public ModuleTab(int x, int y, int index, OnPress onPress) {
        super(x, y, width, height, sprites, onPress);
        this.index = index;
    }

    public void select() {
        this.selected = true;
        //发包创建menu
    }
    public void unselect() {
        this.selected = false;
        //发包销毁menu
    }
    public boolean isSelected() {return selected;}

    @Override
    public void extractContents (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        Identifier sprite = this.sprites.get(this.isActive(), this.selected);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);

    }

}
