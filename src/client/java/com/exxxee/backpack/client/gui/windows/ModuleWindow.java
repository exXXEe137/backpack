package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.api.IBackpackHost;
import net.minecraft.client.Minecraft;
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
        this.windowRight -= SHELF_AND_TAB;
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) return true;
        ShelfWindow shelf = this.shelfWindow();
        if (shelf == null) return false;
        return shelf.scrollTab(scrollY);
    }

    private ShelfWindow shelfWindow() {
        if (!(Minecraft.getInstance().screen instanceof BackpackWindowHost host)) return null;
        for (AbstractBackpackMenu menu : ((IBackpackHost) Minecraft.getInstance().player).getBackpackMenus()) {
            if (!(menu instanceof ShelfMenu shelfMenu)) continue;
            if (host.getBackpackWindow(shelfMenu.containerId) instanceof ShelfWindow shelfWindow) return shelfWindow;
        }
        return null;
    }

}
