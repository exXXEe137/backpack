package com.exxxee.backpack.client.guirender;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModulePage implements Renderable, GuiEventListener {
    private static final Identifier pageBG = Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "page_bg");
    private static final int WIDTH = 60;
    private static final int HEIGHT = 170;
    private final int pageX;
    private final int pageY;
    private final int tabIndex;
    private final List<WidgetSlot> widgetSlots = new ArrayList<>();
    private boolean previewActive;

    public ModulePage(int x, int y, List<ItemStack> pageInfo, int tabIndex) {
        this.pageX = x;
        this.pageY = y;
        this.tabIndex = tabIndex;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                int slotIndex = i * 9 + j;
                widgetSlots.add(new WidgetSlot(
                        pageX + 39 - i * 18, pageY + j * 18, pageInfo.get(slotIndex), slotIndex + tabIndex * 27));
            }
        }
    }

    // 页面对象和其中的槽位会复用，服务端同步只更新槽位数据。
    public void updateItems(List<ItemStack> pageInfo) {
        boolean changed = false;
        for (int i = 0; i < widgetSlots.size(); i++) {
            changed |= widgetSlots.get(i).updateInfo(pageInfo.get(i));
        }
        if (changed && previewActive) {
            clearPreview();
        } else if (!previewActive) {
            for (WidgetSlot widgetSlot : widgetSlots) {
                widgetSlot.refreshRenderedInfo();
            }
        }
    }

    // 根据拖拽类型计算经过槽位的临时显示，不直接写入服务端数据。
    public void updateDragPreview(Set<Integer> selectedSlots, int dragType, ItemStack carriedStack) {
        previewActive = true;
        int selectedCount = 0;
        for (int globalSlot : selectedSlots) {
            if (globalSlot / 27 == tabIndex) {
                selectedCount++;
            }
        }
        if (selectedCount == 0) {
            clearPreview();
            return;
        }

        int countToSet = dragType == 0 ? carriedStack.getCount() / selectedCount : 1;
        for (WidgetSlot widgetSlot : widgetSlots) {
            int globalSlot = widgetSlot.getSlotIndex();
            if (!selectedSlots.contains(globalSlot)) {
                widgetSlot.refreshRenderedInfo();
                continue;
            }

            ItemStack target = widgetSlot.getActualInfo();
            boolean valid = target.isEmpty() || ItemStack.isSameItemSameComponents(target, carriedStack);
            if (!valid) {
                widgetSlot.refreshRenderedInfo();
                continue;
            }

            int maxStackSize = carriedStack.getMaxStackSize();
            int space = target.isEmpty() ? maxStackSize : maxStackSize - target.getCount();
            int amount = dragType == 2 ? space : Math.min(space, countToSet);
            if (amount <= 0) {
                widgetSlot.refreshRenderedInfo();
                continue;
            }
            widgetSlot.setPreview(carriedStack.copyWithCount(target.getCount() + amount));
        }
    }

    // 在客户端预测一次点击结果，让 carried 和槽位立即显示正确状态。
    public ItemStack applyClick(int globalSlotIndex, int button, ItemStack carried) {
        if (globalSlotIndex / 27 != tabIndex) {
            return carried.copy();
        }

        WidgetSlot widgetSlot = widgetSlots.get(globalSlotIndex % 27);
        ItemStack slotStack = widgetSlot.getActualInfo().copy();
        ItemStack nextCarried = carried.copy();

        if (nextCarried.isEmpty()) {
            if (button == 0) {
                nextCarried = slotStack;
                slotStack = ItemStack.EMPTY;
            } else {
                int amount = (slotStack.getCount() + 1) / 2;
                nextCarried = slotStack.copyWithCount(amount);
                slotStack.shrink(amount);
                if (slotStack.isEmpty()) {
                    slotStack = ItemStack.EMPTY;
                }
            }
        } else if (slotStack.isEmpty()) {
            if (button == 0) {
                slotStack = nextCarried;
                nextCarried = ItemStack.EMPTY;
            } else {
                slotStack = nextCarried.copyWithCount(1);
                nextCarried.shrink(1);
                if (nextCarried.isEmpty()) {
                    nextCarried = ItemStack.EMPTY;
                }
            }
        } else if (ItemStack.isSameItemSameComponents(nextCarried, slotStack)) {
            int space = slotStack.getMaxStackSize() - slotStack.getCount();
            int amount = button == 0 ? Math.min(space, nextCarried.getCount()) : Math.min(space, 1);
            if (amount > 0) {
                slotStack.grow(amount);
                nextCarried.shrink(amount);
                if (nextCarried.isEmpty()) {
                    nextCarried = ItemStack.EMPTY;
                }
            }
        } else if (button == 0) {
            ItemStack previous = slotStack;
            slotStack = nextCarried;
            nextCarried = previous;
        }

        widgetSlot.setClientInfo(slotStack);
        return nextCarried;
    }

    public void clearPreview() {
        previewActive = false;
        for (WidgetSlot widgetSlot : widgetSlots) {
            widgetSlot.refreshRenderedInfo();
        }
    }

    public boolean hasPreview() {
        return previewActive;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, pageBG, pageX, pageY - 4, WIDTH, HEIGHT);
        for (WidgetSlot widgetSlot : widgetSlots) {
            widgetSlot.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return getSlotAt(event.x(), event.y()) != null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return getSlotAt(event.x(), event.y()) != null;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    // 按全局槽位编号查找稳定复用的 WidgetSlot 实例。
    public WidgetSlot getSlotAtIndex(int globalSlotIndex) {
        if (globalSlotIndex / 27 != tabIndex) {
            return null;
        }
        return widgetSlots.get(globalSlotIndex % 27);
    }

    public WidgetSlot getSlotAt(double mouseX, double mouseY) {
        for (WidgetSlot widgetSlot : widgetSlots) {
            if (widgetSlot.isMouseOver(mouseX, mouseY)) {
                return widgetSlot;
            }
        }
        return null;
    }
}
