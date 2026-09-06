package com.exxxee.backpack.client.I_slot;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public abstract class BaseSlot implements ISlotWidget {

    private static final Identifier WIDGET_SLOT = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "slot");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "slot_highlight_front");
    private static final int WIDTH = 18;
    private static final int HEIGHT = 18;

    private final int slotX;
    private final int slotY;
    private final int slotIndex;
    private ItemStack slotInfo;

    public BaseSlot(int x, int y, ItemStack slotInfo, int slotIndex) {
        this.slotX = x;
        this.slotY = y;
        this.slotIndex = slotIndex;
        this.slotInfo = slotInfo;
    }

    public int getX() {return slotX;}
    public int getY() {return slotY;}

    @Override
    public int slotIndex() {return this.slotIndex;}

    public ItemStack getSlotInfo() {return this.slotInfo;}
    public void setSlotInfo(ItemStack slotInfo) {this.slotInfo = slotInfo;}

    /** 公共命中检测：矩形判断,所有槽一致 */
    @Override
    public boolean isMouseOver(double x, double y) {
        return x >= this.slotX && y >= this.slotY && x < this.slotX + WIDTH && y < this.slotY + HEIGHT;
    }

    /** 公共渲染：底图 + 物品 + 数量 + 悬停高亮,所有槽一致 */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WIDGET_SLOT, slotX, slotY, WIDTH, HEIGHT);

        if (!this.slotInfo.isEmpty()) {
            graphics.item(this.slotInfo, slotX + 1, slotY + 1);
            graphics.itemDecorations(Minecraft.getInstance().font, this.slotInfo, slotX + 1, slotY + 1);
        }

        if (isMouseOver(mouseX, mouseY)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, slotX - 3, slotY - 3, 24, 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, slotX - 3, slotY - 3, 24, 24);
        }
    }

    /** 抽象交互：按下/释放的行为由具体槽实现(单格发包 / 真实 Slot 点击 / 拖拽收集) */
    @Override
    public abstract boolean mouseClicked(MouseButtonEvent event, boolean doubleClick);

    @Override
    public abstract boolean mouseReleased(MouseButtonEvent event);
}
