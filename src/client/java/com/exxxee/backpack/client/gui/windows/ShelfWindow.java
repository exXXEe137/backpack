package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.Inventory.slot.ShelfSlot;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
    private ModuleTab selectedTab;
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
                ModuleTab tab = new ModuleTab(this.windowLeft - 8, this.windowTop + 6 + index * 18, index, this::onTabPress);
                this.moduleTabs.set(index, tab);
                this.addRenderableWidget(tab);
                if (this.isInvoked(index)) {
                    tab.select();
                    this.selectedTab = tab;
                }
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
    }

    @Override
    public boolean isMouseOver (final double mouseX, final double mouseY) {
        return mouseX >= this.windowLeft - 8 && mouseX < this.windowRight && mouseY >= this.windowTop && mouseY < this.windowBottom;
    }

    private boolean isInvoked(int shelfSlot) {
        for (AbstractBackpackMenu menu : ((IBackpackHost) Minecraft.getInstance().player).getBackpackMenus()) {
            if (menu instanceof ModuleMenu moduleMenu && moduleMenu.shelfSlot == shelfSlot) {
                return moduleMenu.invoked;
            }
        }
        return false;
    }

    private void onTabPress (Button button) {
        ModuleTab tab = (ModuleTab) button;
        if (tab.isSelected()) {
            tab.unselect();
            this.selectedTab = null;
        } else {
            if (this.selectedTab != null) {
                this.selectedTab.unselect();
                this.selectedTab = null;
            }
            tab.select();
            this.selectedTab = tab;
        }
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
                ModuleTab tab = this.moduleTabs.get(index);
                if (tab == null) {
                    tab = new ModuleTab(this.windowLeft - 8, this.windowTop + 6 + index * 18, index, this::onTabPress);
                    this.moduleTabs.set(index, tab);
                    this.addRenderableWidget(tab);
                    if (this.isInvoked(index)) {
                        tab.select();
                        this.selectedTab = tab;
                    }
                } else if (tab.isSelected()) {
                    tab.select();
                }
            } else {
                ModuleTab tab = this.moduleTabs.get(index);
                if (tab == null) continue;
                if (tab == this.selectedTab) {
                    tab.unselect();
                    this.selectedTab = null;
                }
                this.removeWidget(tab);
                this.moduleTabs.set(index, null);
            }
        }
    }

}
