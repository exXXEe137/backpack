package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractBackpackWindows<T extends AbstractBackpackMenu> extends AbstractContainerEventHandler implements Renderable {

    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "slot_highlight_front");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "slot_highlight_back");
    private int hostLeftPos;
    private int hostTopPos;

    protected final T menu;
    protected final int windowWidth;
    protected final int windowHeight;
    protected int windowTop;
    protected int windowLeft;
    protected int windowBottom;
    protected int windowRight;

    private final List<ItemSlotMouseAction> itemSlotMouseActions;
    @Nullable
    protected Slot hoveredSlot;
    @Nullable
    private Slot clickedSlot;
    @Nullable
    private Slot quickdropSlot;
    @Nullable
    private Slot lastClickSlot;

    private boolean isSplittingStack;
    private ItemStack draggingItem = ItemStack.EMPTY;
    private long quickdropTime;
    protected final Set<Slot> quickCraftSlots = Sets.<Slot>newHashSet();
    protected boolean isQuickCrafting;
    private int quickCraftingType;

    @MouseButtonInfo.MouseButton
    private int quickCraftingButton;
    private boolean skipNextRelease;
    private int quickCraftingRemainder;
    private boolean doubleclick;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;

    private final List<GuiEventListener> children = Lists.<GuiEventListener>newArrayList();
    private final List<Renderable> renderables = Lists.<Renderable>newArrayList();
    protected final Font font;

    private boolean initialized;

    protected AbstractBackpackWindows(int x, int y, T menu,  final int imageWidth, final int imageHeight) {
        this(x, y, menu, imageWidth, imageHeight, Minecraft.getInstance().font);
    }

    protected AbstractBackpackWindows(int x, int y, T menu,  final int imageWidth, final int imageHeight, Font font) {
        this.windowLeft = x;
        this.windowTop = y;
        this.menu = menu;
        this.windowWidth = imageWidth;
        this.windowHeight = imageHeight;
        this.font = font;
        this.itemSlotMouseActions = new ArrayList();
    }

    public final void init(final int hostLeftPos, final int hostTopPos) {
        this.hostLeftPos = hostLeftPos;
        this.hostTopPos = hostTopPos;
        if (!this.initialized) {
            this.init();
        }
        this.initialized = true;

        this.repositionElements();
    }

    protected void repositionElements() {
        this.windowLeft = this.hostLeftPos - this.windowWidth;
        this.windowBottom = this.hostTopPos + this.windowHeight;
        this.windowTop = this.hostTopPos;
        this.windowRight = this.hostLeftPos;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractBackground(graphics, mouseX, mouseY, a);
        this.extractContents(graphics, mouseX, mouseY, a);
    }

    private boolean isHovering(final Slot slot, final double xm, final double ym) {
        return this.isHovering(slot.x, slot.y, 16, 16, xm, ym);
    }

    protected boolean isHovering(final int left, final int top, final int w, final int h, double xm, double ym) {
        int xo = this.windowLeft;
        int yo = this.windowTop;
        xm -= xo;
        ym -= yo;
        return xm >= left - 1 && xm < left + w + 1 && ym >= top - 1 && ym < top + h + 1;
    }

    private void onStopHovering(final Slot slot) {
        if (slot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(slot)) {
                    itemMouseAction.onStopHovering(slot);
                }
            }
        }
    }

    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {}

    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        int xo = this.windowLeft;
        int yo = this.windowTop;
        graphics.pose().pushMatrix();
        graphics.pose().translate(xo, yo);
        Slot previouslyHoveredSlot = this.hoveredSlot;
        this.hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
        this.extractSlotHighlightBack(graphics);
        this.extractSlots(graphics, mouseX, mouseY);
        this.extractSlotHighlightFront(graphics);
        if (previouslyHoveredSlot != null && previouslyHoveredSlot != this.hoveredSlot) {
            this.onStopHovering(previouslyHoveredSlot);
        }

        graphics.pose().popMatrix();
    }

    protected void extractSlots(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                this.extractSlot(graphics, slot, mouseX, mouseY);
            }
        }
    }

    private void extractSlotHighlightBack(final GuiGraphicsExtractor graphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    private void extractSlotHighlightFront(final GuiGraphicsExtractor graphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    protected void extractSlot(final GuiGraphicsExtractor graphics, final Slot slot, final int mouseX, final int mouseY) {
        int x = slot.x;
        int y = slot.y;
        ItemStack itemStack = slot.getItem();
        boolean quickCraftStack = false;
        boolean done = slot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack carried = menu.getCarried();
        String itemCount = null;
        if (slot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemStack.isEmpty()) {
            itemStack = itemStack.copyWithCount(itemStack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !carried.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }

            if (AbstractBackpackMenu.canItemQuickReplace(slot, carried, true) && this.menu.canDragTo(slot)) {
                quickCraftStack = true;
                int maxSize = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                int carry = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int newCount = AbstractBackpackMenu.getQuickCraftPlaceCount(this.quickCraftSlots.size(), this.quickCraftingType, carried) + carry;
                if (newCount > maxSize) {
                    newCount = maxSize;
                    itemCount = ChatFormatting.YELLOW.toString() + maxSize;
                }

                itemStack = carried.copyWithCount(newCount);
            } else {
                this.quickCraftSlots.remove(slot);
                this.recalculateQuickCraftRemaining();
            }
        }

        if (itemStack.isEmpty() && slot.isActive()) {
            Identifier icon = slot.getNoItemIcon();
            if (icon != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, 16, 16);
                done = true;
            }
        }

        if (!done) {
            if (quickCraftStack) {
                graphics.fill(x, y, x + 16, y + 16, -2130706433);
            }

            int seed = slot.x + slot.y * this.windowWidth;
            if (slot.isFake()) {
                graphics.fakeItem(itemStack, x, y, seed);
            } else {
                graphics.item(itemStack, x, y, seed);
            }

            graphics.itemDecorations(this.font, itemStack, x, y, itemCount);
        }
    }

    @Nullable
    private Slot getHoveredSlot(final double x, final double y) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot, x, y)) {
                return slot;
            }
        }

        return null;
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack carried = this.menu.getCarried();
        if (!carried.isEmpty() && this.isQuickCrafting) {
            if (this.quickCraftingType == 2) {
                this.quickCraftingRemainder = carried.getMaxStackSize();
            } else {
                this.quickCraftingRemainder = carried.getCount();

                for (Slot slot : this.quickCraftSlots) {
                    ItemStack slotItemStack = slot.getItem();
                    int carry = slotItemStack.isEmpty() ? 0 : slotItemStack.getCount();
                    int maxSize = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                    int newCount = Math.min(AbstractBackpackMenu.getQuickCraftPlaceCount(this.quickCraftSlots.size(), this.quickCraftingType, carried) + carry, maxSize);
                    this.quickCraftingRemainder -= newCount - carry;
                }
            }
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    protected <T extends GuiEventListener & Renderable> T addRenderableWidget(final T widget) {
        this.renderables.add(widget);
        return this.addWidget(widget);
    }

    protected <T extends GuiEventListener> T addWidget(final T widget) {
        this.children.add(widget);
        return widget;
    }

    protected void init() {
    }

    public void tick() {
    }

    public void removed() {
    }

    public void added() {
    }

    protected void removeWidget(final GuiEventListener widget) {
        if (widget instanceof Renderable) {
            this.renderables.remove((Renderable)widget);
        }

        if (this.getFocused() == widget) {
            setFocused(null);
        }

        this.children.remove(widget);
    }

    public T getMenu() {
        return this.menu;
    }

}
