package com.exxxee.backpack.client.I_slot;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public interface ISlotWidget {
    int slotIndex();

    boolean isMouseOver(double x, double y);

    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick);

    boolean mouseReleased(MouseButtonEvent event);

    void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a);
}
