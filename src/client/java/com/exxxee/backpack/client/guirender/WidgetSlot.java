package com.exxxee.backpack.client.guirender;

import com.exxxee.backpack.ExxxeeBackpack;
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

    private final int slotX;
    private final int slotY;
    private final int slotIndex;
    // 服务端最近一次确认的数据。
    private ItemStack authoritativeInfo;
    // 当前逻辑数据，可能暂时包含客户端预测。
    private ItemStack actualInfo;
    // 当前帧实际绘制的数据，拖拽预览只修改这里。
    private ItemStack renderedInfo;
    // 防止旧的服务端快照覆盖本地刚执行的操作。
    private boolean clientPrediction;
    private int predictionTicks;

    public WidgetSlot(int x, int y, ItemStack slotInfo, int slotIndex) {
        super(x, y, WIDTH, HEIGHT, CommonComponents.EMPTY);
        this.slotX = x;
        this.slotY = y;
        this.slotIndex = slotIndex;
        this.authoritativeInfo = slotInfo.copy();
        this.actualInfo = this.authoritativeInfo.copy();
        this.renderedInfo = this.actualInfo.copy();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        isHovered = isMouseOver(mouseX, mouseY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WIDGET_SLOT, slotX, slotY, WIDTH, HEIGHT);

        if (!renderedInfo.isEmpty()) {
            graphics.item(renderedInfo, slotX + 1, slotY + 1);
            graphics.itemDecorations(Minecraft.getInstance().font, renderedInfo, slotX + 1, slotY + 1);
        }

        if (isHovered) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, getX() - 3, getY() - 3, 24, 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, getX() - 3, getY() - 3, 24, 24);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return event.button() == 0 || event.button() == 1 || event.button() == 2;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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

    public int getSlotIndex() {
        return slotIndex;
    }

    public ItemStack getSlotInfo() {
        return renderedInfo;
    }

    public ItemStack getActualInfo() {
        return actualInfo;
    }

    // 同步时区分“确认预测”“旧快照”和“真正的新状态”。
    public boolean updateInfo(ItemStack slotInfo) {
        ItemStack next = slotInfo.copy();
        if (clientPrediction) {
            if (sameStack(actualInfo, next)) {
                authoritativeInfo = next;
                clientPrediction = false;
                predictionTicks = 0;
                actualInfo = next;
                return false;
            }
            if (sameStack(authoritativeInfo, next)) {
                if (++predictionTicks <= 20) {
                    return false;
                }
                clientPrediction = false;
                predictionTicks = 0;
                actualInfo = next;
                renderedInfo = next.copy();
                authoritativeInfo = next;
                return true;
            }
            clientPrediction = false;
            predictionTicks = 0;
        }

        boolean changed = !sameStack(actualInfo, next);
        authoritativeInfo = next;
        actualInfo = next;
        return changed;
    }

    public void refreshRenderedInfo() {
        if (!clientPrediction) {
            renderedInfo = actualInfo.copy();
        }
    }

    // 记录一次客户端预测，等待服务端返回权威结果。
    public void setClientInfo(ItemStack slotInfo) {
        actualInfo = slotInfo.copy();
        renderedInfo = actualInfo.copy();
        clientPrediction = true;
        predictionTicks = 0;
    }

    public void setPreview(ItemStack previewInfo) {
        renderedInfo = previewInfo.copy();
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount()
                && ItemStack.isSameItemSameComponents(first, second);
    }
}
