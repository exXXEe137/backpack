package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.Inventory.slot.ShelfSlot;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class ShelfWindow extends AbstractBackpackWindows<ShelfMenu> {

    private static final Identifier SHELF_BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "shelf_background");
    private ShelfMenu menu;
    private static final int width = 24;
    private static final int height = 86;
    private List<ModuleTab> moduleTabs =  new ArrayList<>(4);

    protected ShelfWindow(int x, int y, ShelfMenu menu) {
        super(x, y, menu, width, height);
        this.menu = menu;
    }

    @Override
    protected void init() {
        int count = this.menu.slots.size();
        while (this.moduleTabs.size() < count) {
            this.moduleTabs.add(null);
        }
        for (Slot slot : this.menu.slots) {
            int index = slot.index;
            if (slot.getItem().is(BackpackItems.BACKPACK_MODULE) && this.moduleTabs.get(index) == null) {
                ModuleTab tab = new ModuleTab(this.windowLeft - 8, this.windowTop + 6 + index * 18, index);
                this.moduleTabs.set(index, tab);
                this.addRenderableWidget(tab);
            }
        }
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        for (ModuleTab tab : this.moduleTabs) {
            if (tab == null) continue;
            int index = tab.index();
            tab.setX(this.windowLeft - 8);
            tab.setY(this.windowTop + 6 + index * 18);
        }
    }

    @Override
    public void extractBackground (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SHELF_BACKGROUND_SPRITE, windowLeft, windowTop,  width, height);
    }

    @Override
    public void extractContents (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        for  (ModuleTab moduleTab : moduleTabs) {
            if(moduleTab == null) continue;
            moduleTab.extractContents(graphics, mouseX, mouseY, a);
        }

//        for (int i = 0; i < this.moduleTabs.size(); i++) {
//            ModuleTab moduleTab = this.moduleTabs.get(i);
//            if (moduleTab == null) continue;
//            moduleTab.extractContents(graphics, mouseX, mouseY, a);
//        }
    }

    @Override
    public boolean isMouseOver (final double mouseX, final double mouseY) {
        return mouseX >= this.windowLeft - 8 && mouseX < this.windowRight && mouseY >= this.windowTop && mouseY < this.windowBottom;
    }

    @Override
    public void tick() {
        int count = this.menu.slots.size();
        while (this.moduleTabs.size() < count) {
            this.moduleTabs.add(null);
        }

        for  (Slot slot : menu.slots) {
            int index = slot.index;
            if (slot.getItem().is(BackpackItems.BACKPACK_MODULE)) {
                if (this.moduleTabs.get(index) != null) continue;
                ModuleTab tab = new ModuleTab(this.windowLeft - 8, this.windowTop + 6 + index * 18, index);
                this.moduleTabs.set(index, tab);
                this.addRenderableWidget(tab);
            } else {
                ModuleTab tab = this.moduleTabs.get(index);
                if (tab == null) continue;
                this.removeWidget(tab);
                this.moduleTabs.set(index, null);
            }
        }
//        for (int i = 0; i < count; i++) {
//            Slot slot = this.menu.slots.get(i);
//            if (slot.getItem().is(BackpackItems.BACKPACK_MODULE)) {
//                if (this.moduleTabs.get(i) == null) {
//                    this.moduleTabs.set(i, new ModuleTab(this.windowLeft - 7, this.windowTop + 6 + i * 18, i));
//                }
//            } else {this.moduleTabs.set(i, null);}
//        }
    }

}
