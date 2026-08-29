package com.exxxee.backpack.client.guirender;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Network.paylo.ModuleIventoryPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class WidgetSlot extends AbstractWidget {

    private static final Identifier WIDGET_SLOT = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "slot");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "slot_highlight_front");
    private static final int WIDTH = 18;
    private static final int HEIGHT = 18;
    private int slotX;
    private int slotY;
    private int slotIndex;
    private ItemStack slotInfo;

    public WidgetSlot(int x, int y, ItemStack slotInfo, int slotIndex) {
        super(x, y, WIDTH, HEIGHT, CommonComponents.EMPTY);
        this.slotX = x;
        this.slotY = y;
        this.slotIndex = slotIndex;
        this.slotInfo = slotInfo;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WIDGET_SLOT, slotX, slotY, WIDTH, HEIGHT);

        if (slotInfo != ItemStack.EMPTY) {
            graphics.item(slotInfo, slotX+1, slotY+1);
            graphics.itemDecorations(Minecraft.getInstance().font, slotInfo, slotX+1, slotY+1);
        }

        if(isHovered) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, this.getX()-3, this.getY()-3, 24, 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, this.getX()-3, this.getY()-3, 24, 24);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 只放行左右键（原版槽位逻辑同款过滤），侧键中键不触发；shift 时发 shift=1 走快速转移
        if (event.button() == 0 || event.button() == 1) {
            ClientPlayNetworking.send(new ModuleIventoryPayload(slotIndex, event.button(), event.hasShiftDown() ? 1 : 0));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased (MouseButtonEvent event) {
//        if (event.button() == 0 || event.button() == 1) {
//            return true;
//        }
        return true;
    }

    @Override
    public void setFocused(boolean focused) {

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }

    @Override
    public boolean isFocused() {
        return false;
    }
}
