package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class ShelfWindow extends AbstractBackpackWindows<ShelfMenu> {

    private static final Identifier SHELF_BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "shelf_background");
    private ShelfMenu menu;
    private int width = 24;
    private int height = 96;
    private List<ModuleTab> moduleTabs;

    protected ShelfWindow(int x, int y, ShelfMenu menu) {
        super(x, y, menu, 24, 96);
        this.menu = menu;
    }

    @Override
    public void extractBackground (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SHELF_BACKGROUND_SPRITE, windowLeft, windowTop,  width, height);
    }

    @Override
    public void tick() {
        for (Slot slot  : menu.slots) {
            int index = slot.index;
            if (slot.getItem().is(BackpackItems.BACKPACK_MODULE) && moduleTabs.get(index) != null) {
                moduleTabs.set(index, new ModuleTab( 0, 0, index, null));
            }else {
                moduleTabs.set(index, null);
            }
        }
    }

}
