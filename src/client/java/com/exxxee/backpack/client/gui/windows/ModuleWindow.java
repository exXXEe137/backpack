package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ModuleWindow extends AbstractBackpackWindows<ModuleMenu> {

    private static final Identifier SHELF_BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "module_background");

    private static final int WIDTH = 10 + 3 * 18;
    private static final int HEIGHT = 9 * 18 + 14;
    private static final int SHELF_AND_TAB = 24 + 7;

    public ModuleWindow(int x, int y, ModuleMenu menu) {
        super(x, y, menu, WIDTH, HEIGHT);
    }

    @Override
    public void extractBackground (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SHELF_BACKGROUND_SPRITE, windowLeft, windowTop,  WIDTH, HEIGHT);
    }

    @Override
    public void extractContents (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        this.windowLeft -= SHELF_AND_TAB;
//        this.windowRight -= SHELF_AND_TAB;
    }

}
