package com.exxxee.backpack.client.guirender;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import com.exxxee.backpack.Network.paylo.SwitchModulePayload;
import com.exxxee.backpack.item.BackpackItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ModulesButton extends ImageButton {

    private static final WidgetSprites SPRITES = new WidgetSprites(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID,"moduletab"), Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID,"moduletab_selected"));
    private static final int WIDTH = 32;
    private static final int HEIGHT = 20;

    private final PanelWidget panel;
    private final int tabIndex;
    private boolean selected = false;
    private NonNullList<ItemStack> moduleInfo = NonNullList.withSize(27, ItemStack.EMPTY);

    ModulePage page;

    public ModulesButton(int x, int y, final int tabIndex, OnPress onPress, PanelWidget panel) {
        super(x, y, WIDTH, HEIGHT, SPRITES, onPress);
        this.tabIndex = tabIndex;
        this.panel = panel;
    }

    /** 标记此 tab 为当前选中态 */
    public void select() {this.selected = true;}
    public void unselect() {this.selected = false;}
    public boolean isSelected() {return selected;}

    // 只在选中模块变化时创建页面，正常 tick 期间复用原有槽位对象。
    public void pageVisuals () {
        if (this.selected && !this.slotIsEmpty()) {
            List<ItemStack> currentInfo = this.getModuleInfo();
            if (page == null) {
                page = new ModulePage(panel.getLeftPos() - 95, panel.getTopPos(), this.getModuleInfo(), tabIndex);
            } else {
                page.updateItems(currentInfo);
            }
        } else {
            if (page != null) {
                page.clearPreview();
            }
            page = null;
        }
    }



    @Override
    public void extractContents (final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {

        Identifier sprite = this.sprites.get(this.isActive(), this.selected);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);

        if (!slotIsEmpty()) {
            graphics.item(this.getModuleSlot(),getX() + 14,getY() + 2,0);
            if (page != null) {
                page.extractRenderState(graphics, mouseX, mouseY, a);
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        //tab点击逻辑
        if (event.x() - getX() < 14) {
            if (!slotIsEmpty()) {
                this.onPress.onPress(this);
                return true;
            }
        //tab模块存放判断
        }else if (event.x() - getX() >= 14 && (panel.carriedModule() || (slotIsModule() && panel.carriedIsEmpty()))) {

            ClientPlayNetworking.send(new SwitchModulePayload(tabIndex));
            return true;
        }
        return false;
    }

    public ItemStack getModuleSlot () {
        List<ItemStack> modules = panel.getSHELF_MODULES();
        if (tabIndex < modules.size()) {
            return modules.get(tabIndex);
        }
        return ItemStack.EMPTY;
    }

    public List<ItemStack> getModuleInfo () {
        ModuleInventoryData info = this.getModuleSlot().get(BackpackDataComponents.MODULE_INVENTORY);
        if (info != null) {
            info.items().copyInto(moduleInfo);
        }
        return moduleInfo;
    }

    public boolean slotIsEmpty () {return getModuleSlot().isEmpty();}
    public boolean slotIsModule () {return getModuleSlot().getItem() == BackpackItems.BACKPACK_MODULE;}
}


