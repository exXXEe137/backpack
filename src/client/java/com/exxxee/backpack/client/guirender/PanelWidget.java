package com.exxxee.backpack.client.guirender;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.item.BackpackItems;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class PanelWidget implements Renderable, GuiEventListener {

    private List<ModulesButton> modulesButtons = Lists.<ModulesButton>newArrayList();
    private int topPos;
    private int bottomPos;
    private int leftPos;
    private int rightPos;

    @Nullable
    private ModulesButton selectedTab;

    public PanelWidget(int leftPos, int topPos) {
        this.leftPos = leftPos;
        this.topPos = topPos;
    }

    public void tick () {
        boolean hasShelf = this.getChest().getItem() == BackpackItems.BACKPACK_SHELF;
        if (hasShelf) {
            if (modulesButtons.isEmpty()) {
                tabVisuals();
            }
            if (selectedTab != null && selectedTab.slotIsEmpty()) {
                selectedTab.unselect();
                selectedTab = null;
            }
            for (ModulesButton button : modulesButtons) {
                button.pageVisuals();
            }
        } else if (!hasShelf) {
            modulesButtons.clear();
            selectedTab = null;
        }
    }

    public void tabVisuals () {
        if (this.getChest().getItem() == BackpackItems.BACKPACK_SHELF) {
        for (int tabIndex = 0; tabIndex < 4; tabIndex++) {
            this.modulesButtons.add(new ModulesButton(leftPos - 32, topPos + tabIndex * 20, tabIndex, this::onTabButtonPress, this));
            }
        }
    }

    private void onTabButtonPress (final Button button) {
        if (this.selectedTab == button) {
            selectedTab.unselect();
            selectedTab = null;
        } else if (button instanceof ModulesButton modulesButton) {
            this.replaceSelected(modulesButton);
        }
    }

    private void replaceSelected(final ModulesButton tabButton) {
        if (selectedTab != null) {
            selectedTab.unselect();
            selectedTab = null;
        }

        tabButton.select();
        selectedTab = tabButton;
    }

    @Override//(Renderable)
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!modulesButtons.isEmpty()) {
            for (ModulesButton modulesButton : modulesButtons) {
                modulesButton.extractRenderState(graphics, mouseX, mouseY, a);
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!modulesButtons.isEmpty()) {
            for (ModulesButton modulesButton : modulesButtons) {
                if (modulesButton.isMouseOver(event.x(), event.y()) && modulesButton.mouseClicked(event, doubleClick)) {
                    return true;
                }

                if (modulesButton.page != null && modulesButton.page.mouseClicked(event, doubleClick)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased (MouseButtonEvent event) {
        if (!modulesButtons.isEmpty()) {
            for (ModulesButton modulesButton : modulesButtons) {
                if (modulesButton.isMouseOver(event.x(), event.y()) && modulesButton.mouseReleased(event)) {
                    return true;
                }

                if (modulesButton.page != null && modulesButton.page.mouseReleased(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override//(GuiEventListener)
    public void setFocused(boolean focused) {

    }

    @Override//(GuiEventListener)
    public boolean isFocused() {return false;}

    /**获取玩家*/
    public Player getPlayer() {return Minecraft.getInstance().player;}

    /**模块插槽slot相关*/
    public ItemStack getChest () {return  getPlayer().getItemBySlot(EquipmentSlot.CHEST);}
    public List<ItemStack> getSHELF_MODULES () {
        List<ItemStack> modules = getChest().get(BackpackDataComponents.SHELF_MODULES);
        return modules != null ? modules : List.of();
    }

    /**鼠标手持carried物品相关方法*/
    public ItemStack carriedItemStack () {return getPlayer().containerMenu.getCarried();}
    public boolean carriedIsEmpty () {return carriedItemStack().isEmpty();}
    public boolean carriedModule() {return carriedItemStack().getItem() == BackpackItems.BACKPACK_MODULE;}

    public int getLeftPos() {
        return leftPos;
    }
    public int getTopPos() {
        return topPos;
    }
}
