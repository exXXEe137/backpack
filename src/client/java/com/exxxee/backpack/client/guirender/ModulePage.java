package com.exxxee.backpack.client.guirender;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.client.I_slot.ISlotWidget;
import com.exxxee.backpack.client.I_slot.SlotFactory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ModulePage implements Renderable, GuiEventListener {
    private static final Identifier pageBG = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "page_bg");
    private static final int WIDTH = 60;
    private static final int HEIGHT = 170;
    private final int pageX;
    private final int pageY;
    private final int tabIndex;

    private List<ItemStack> pageInfo;
    private final SlotFactory slotFactory;
    List<ISlotWidget> widgetSlots = new ArrayList<>();

    public ModulePage(int x, int y, List<ItemStack> pageInfo, int tabIndex, SlotFactory slotFactory) {
        this.pageX = x;
        this.pageY = y;
        this.tabIndex = tabIndex;
        this.pageInfo = pageInfo;
        this.slotFactory = slotFactory;
        this.slotVisuals();
    }

    public void slotVisuals () {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                int slotIndex = i * 9 + j;
                this.widgetSlots.add(slotFactory.create(pageX + 39 - i * 18, pageY + j * 18, pageInfo.get(slotIndex), slotIndex + tabIndex * 27));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, pageBG, pageX, pageY - 4, WIDTH, HEIGHT);
        for (ISlotWidget widgetSlot : this.widgetSlots) {
            widgetSlot.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (ISlotWidget widgetSlot : this.widgetSlots) {
            if (widgetSlot.isMouseOver(event.x(),  event.y())) {
                if (widgetSlot.mouseClicked(event, doubleClick)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (ISlotWidget widgetSlot : this.widgetSlots) {
            if (widgetSlot.isMouseOver(event.x(), event.y()) && widgetSlot.mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {

    }

    @Override
    public boolean isFocused() {
        return false;
    }

}
