package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.client.multiplayer.BackpackMultiPlayerGameMode;
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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
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

    private AbstractBackpackWindows.@Nullable SnapbackData snapbackData;


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
    private final Minecraft minecraft;
    protected final Font font;

    private boolean initialized;

    protected AbstractBackpackWindows(int x, int y, T menu,  final int imageWidth, final int imageHeight) {
        this(Minecraft.getInstance() , x, y, menu, imageWidth, imageHeight, Minecraft.getInstance().font);
    }

    protected AbstractBackpackWindows(Minecraft minecraft,int x, int y, T menu,  final int imageWidth, final int imageHeight, Font font) {
        this.minecraft = minecraft;
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
    public boolean isMouseOver(final double mouseX, final double mouseY) {
        return mouseX >= this.windowLeft && mouseX < this.windowRight && mouseY >= this.windowTop && mouseY < this.windowBottom;
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

    @Nullable
    public Integer quickCraftPreview() {
        if (!this.isQuickCrafting || this.quickCraftSlots.size() <= 1) return null;
        return this.quickCraftingRemainder;
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

    protected boolean hasClickedOutside(final double mx, final double my, final int xo, final int yo) {
        return mx < xo || my < yo || mx >= xo + this.windowWidth || my >= yo + this.windowHeight;
    }

    void onMouseClickAction(@Nullable final Slot slot, final ContainerInput containerInput) {
        if (slot != null && slot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(slot)) {
                    itemMouseAction.onSlotClicked(slot, containerInput);
                }
            }
        }
    }

    protected void slotClicked(final Slot slot, int slotId, final int buttonNum, final ContainerInput containerInput) {
        if (slot != null) {
            slotId = slot.index;
        }

        this.onMouseClickAction(slot, containerInput);
        BackpackMultiPlayerGameMode.handleContainerInput(this.menu.windowId, slotId, buttonNum, containerInput, this.minecraft.player);
    }

    public boolean hasPendingRelease() {
        return this.isQuickCrafting || this.skipNextRelease || this.clickedSlot != null;
    }

    private void checkHotbarMouseClicked(final MouseButtonEvent event) {
        if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
            if (this.minecraft.options.keySwapOffhand.matchesMouse(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
                return;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(event)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        } else if (this.checkHotbarKeyPressed(event)) {
            return true;
        } else if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            if (this.minecraft.options.keyPickItem.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.CLONE);
                return true;
            } else if (this.minecraft.options.keyDrop.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, event.hasControlDown() ? 1 : 0, ContainerInput.THROW);
                return true;
            }
        }

        return false;
    }

    private boolean checkHotbarKeyPressed(final KeyEvent event) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            if (this.minecraft.options.keySwapOffhand.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
                return true;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldAddSlotToQuickCraft(final Slot slot, final ItemStack carried) {
        return this.isQuickCrafting
                && !carried.isEmpty()
                && (carried.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2)
                && AbstractBackpackMenu.canItemQuickReplace(slot, carried, true)
                && slot.mayPlace(carried)
                && this.menu.canDragTo(slot);
    }

    private void quickCraftToSlots() {
        this.slotClicked(null, -999, AbstractBackpackMenu.getQuickcraftMask(0, this.quickCraftingType), ContainerInput.QUICK_CRAFT);

        for (Slot quickSlot : this.quickCraftSlots) {
            this.slotClicked(quickSlot, quickSlot.index, AbstractBackpackMenu.getQuickcraftMask(1, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
        }

        this.slotClicked(null, -999, AbstractBackpackMenu.getQuickcraftMask(2, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        } else {
            boolean cloning = this.minecraft.options.keyPickItem.matchesMouse(event) && this.minecraft.player.hasInfiniteMaterials();
            Slot slot = this.getHoveredSlot(event.x(), event.y());
            this.doubleclick = this.lastClickSlot == slot && doubleClick;
            this.skipNextRelease = false;
            if (event.button() != 0 && event.button() != 1 && !cloning) {
                this.checkHotbarMouseClicked(event);
            } else {
                int xo = this.windowLeft;
                int yo = this.windowTop;
                boolean clickedOutside = this.hasClickedOutside(event.x(), event.y(), xo, yo);
                int slotId = -1;
                if (slot != null) {
                    slotId = slot.index;
                }

                if (clickedOutside) {
                    slotId = -999;
                }

//                if (this.minecraft.options.touchscreen().get() && clickedOutside && this.menu.getCarried().isEmpty()) {
//                    this.onClose();
//                    return true;
//                }

                if (slotId != -1) {
                    if (this.minecraft.options.touchscreen().get()) {
                        if (slot != null && slot.hasItem()) {
                            this.clickedSlot = slot;
                            this.draggingItem = ItemStack.EMPTY;
                            this.isSplittingStack = event.button() == 1;
                        } else {
                            this.clickedSlot = null;
                        }
                    } else if (!this.isQuickCrafting) {
                        if (this.menu.getCarried().isEmpty()) {
                            if (cloning) {
                                this.slotClicked(slot, slotId, event.button(), ContainerInput.CLONE);
                            } else {
                                boolean quickKey = slotId != -999 && event.hasShiftDown();
                                ContainerInput containerInput = ContainerInput.PICKUP;
                                if (quickKey) {
                                    this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                                    containerInput = ContainerInput.QUICK_MOVE;
                                } else if (slotId == -999) {
                                    containerInput = ContainerInput.THROW;
                                }

                                this.slotClicked(slot, slotId, event.button(), containerInput);
                            }

                            this.skipNextRelease = true;
                        } else {
                            this.isQuickCrafting = true;
                            this.quickCraftingButton = event.button();
                            this.quickCraftSlots.clear();
                            if (event.button() == 0) {
                                this.quickCraftingType = 0;
                            } else if (event.button() == 1) {
                                this.quickCraftingType = 1;
                            } else if (cloning) {
                                this.quickCraftingType = 2;
                            }
                        }
                    }
                }
            }

            this.lastClickSlot = slot;
            return true;
        }
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        ItemStack carried = this.menu.getCarried();
        if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
            if (event.button() == 0 || event.button() == 1) {
                if (this.draggingItem.isEmpty()) {
                    if (slot != this.clickedSlot && !this.clickedSlot.getItem().isEmpty()) {
                        this.draggingItem = this.clickedSlot.getItem().copy();
                    }
                } else if (this.draggingItem.getCount() > 1 && slot != null && AbstractBackpackMenu.canItemQuickReplace(slot, this.draggingItem, false)) {
                    long time = Util.getMillis();
                    if (this.quickdropSlot == slot) {
                        if (time - this.quickdropTime > 500L) {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ContainerInput.PICKUP);
                            this.slotClicked(slot, slot.index, 1, ContainerInput.PICKUP);
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ContainerInput.PICKUP);
                            this.quickdropTime = time + 750L;
                            this.draggingItem.shrink(1);
                        }
                    } else {
                        this.quickdropSlot = slot;
                        this.quickdropTime = time;
                    }
                }
            }

            return true;
        } else if (slot != null && this.shouldAddSlotToQuickCraft(slot, carried) && this.quickCraftSlots.add(slot)) {
            this.recalculateQuickCraftRemaining();
            return true;
        } else {
            return slot == null && this.menu.getCarried().isEmpty() ? super.mouseDragged(event, dx, dy) : true;
        }
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        int xo = this.windowLeft;
        int yo = this.windowTop;
        boolean clickedOutside = this.hasClickedOutside(event.x(), event.y(), xo, yo);
        int slotId = -1;
        if (slot != null) {
            slotId = slot.index;
        }

        if (clickedOutside) {
            slotId = -999;
        }

        if (this.doubleclick && slot != null && event.button() == 0 && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (event.hasShiftDown()) {
                if (!this.lastQuickMoved.isEmpty()) {
                    for (Slot target : this.menu.slots) {
                        if (target != null
                                && target.mayPickup(this.minecraft.player)
                                && target.hasItem()
                                && target.container == slot.container
                                && AbstractBackpackMenu.canItemQuickReplace(target, this.lastQuickMoved, true)) {
                            this.slotClicked(target, target.index, event.button(), ContainerInput.QUICK_MOVE);
                        }
                    }
                }
            } else {
                this.slotClicked(slot, slotId, event.button(), ContainerInput.PICKUP_ALL);
            }

            this.doubleclick = false;
        } else {
            if (this.isQuickCrafting && this.quickCraftingButton != event.button()) {
                this.isQuickCrafting = false;
                this.quickCraftSlots.clear();
                this.skipNextRelease = true;
                return true;
            }

            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            }

            if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
                if (event.button() == 0 || event.button() == 1) {
                    if (this.draggingItem.isEmpty() && slot != this.clickedSlot) {
                        this.draggingItem = this.clickedSlot.getItem();
                    }

                    boolean canReplace = AbstractBackpackMenu.canItemQuickReplace(slot, this.draggingItem, false);
                    if (slotId != -1 && !this.draggingItem.isEmpty() && canReplace) {
                        this.slotClicked(this.clickedSlot, this.clickedSlot.index, event.button(), ContainerInput.PICKUP);
                        this.slotClicked(slot, slotId, 0, ContainerInput.PICKUP);
                        if (this.menu.getCarried().isEmpty()) {
                            this.snapbackData = null;
                        } else {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, event.button(), ContainerInput.PICKUP);
                            this.snapbackData = new AbstractBackpackWindows.SnapbackData(
                                    this.draggingItem, new Vector2i((int)event.x(), (int)event.y()), new Vector2i(this.clickedSlot.x + xo, this.clickedSlot.y + yo), Util.getMillis()
                            );
                        }
                    } else if (!this.draggingItem.isEmpty()) {
                        this.snapbackData = new AbstractBackpackWindows.SnapbackData(
                                this.draggingItem, new Vector2i((int)event.x(), (int)event.y()), new Vector2i(this.clickedSlot.x + xo, this.clickedSlot.y + yo), Util.getMillis()
                        );
                    }

                    this.clearDraggingState();
                }
            } else if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
                this.quickCraftToSlots();
            } else if (!this.menu.getCarried().isEmpty()) {
                if (this.minecraft.options.keyPickItem.matchesMouse(event)) {
                    this.slotClicked(slot, slotId, event.button(), ContainerInput.CLONE);
                } else {
                    boolean quickKey = slotId != -999 && event.hasShiftDown();
                    if (quickKey) {
                        this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    }

                    this.slotClicked(slot, slotId, event.button(), quickKey ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
                }
            }
        }

        this.isQuickCrafting = false;
        return super.mouseReleased(event);
    }

    public void clearDraggingState() {
        this.draggingItem = ItemStack.EMPTY;
        this.clickedSlot = null;
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(this.hoveredSlot) && itemMouseAction.onMouseScrolled(scrollX, scrollY, this.hoveredSlot.index, this.hoveredSlot.getItem())) {
                    return true;
                }
            }
        }

        return false;
    }

    private record SnapbackData(ItemStack item, Vector2i start, Vector2i end, long time) {
    }

}
