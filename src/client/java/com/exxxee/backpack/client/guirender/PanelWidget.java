package com.exxxee.backpack.client.guirender;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Network.paylo.ModuleActionPayload;
import com.exxxee.backpack.Network.paylo.ModuleDragPayload;
import com.exxxee.backpack.Network.paylo.ModuleIventoryPayload;
import com.exxxee.backpack.item.BackpackItems;
import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PanelWidget implements Renderable, GuiEventListener {

    private final List<ModulesButton> modulesButtons = Lists.newArrayList();
    private final int topPos;
    private final int leftPos;

    @Nullable
    private ModulesButton selectedTab;
    private int lastMouseX;
    private int lastMouseY;
    private int pressedButton = -1;
    private int pressedSlot = -1;
    private boolean pressedWithShift;
    private boolean externalPointer;
    // 左键先记录按下状态，只有确认没有拖拽时才执行普通点击。
    private boolean leftClickPending;
    // carried 为空时，记录从哪个自定义槽位取出拖拽物品。
    private int dragSourceSlot = -1;
    private final Set<Integer> draggedSlots = new LinkedHashSet<>();
    private int lastClickSlot = -1;
    private long lastClickTime;
    private int previewTicks;
    private boolean waitingForDragResult;
    private static final long DOUBLE_CLICK_WINDOW_MS = 300L;

    public PanelWidget(int leftPos, int topPos) {
        this.leftPos = leftPos;
        this.topPos = topPos;
    }

    public void tick() {
        boolean hasShelf = getChest().is(BackpackItems.BACKPACK_SHELF);
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
            if (waitingForDragResult) {
                if (!hasPreview()) {
                    waitingForDragResult = false;
                    previewTicks = 0;
                } else if (++previewTicks > 10) {
                    clearDragPreview();
                    waitingForDragResult = false;
                    previewTicks = 0;
                }
            }
        } else {
            modulesButtons.clear();
            selectedTab = null;
            resetPointer();
            clearDragPreview();
            waitingForDragResult = false;
            previewTicks = 0;
        }
    }

    private void tabVisuals() {
        if (getChest().is(BackpackItems.BACKPACK_SHELF)) {
            for (int tabIndex = 0; tabIndex < 4; tabIndex++) {
                modulesButtons.add(new ModulesButton(
                        leftPos - 32, topPos + tabIndex * 20, tabIndex, this::onTabButtonPress, this));
            }
        }
    }

    private void onTabButtonPress(Button button) {
        if (selectedTab == button) {
            selectedTab.unselect();
            selectedTab = null;
        } else if (button instanceof ModulesButton modulesButton) {
            replaceSelected(modulesButton);
        }
    }

    private void replaceSelected(ModulesButton tabButton) {
        if (selectedTab != null) {
            selectedTab.unselect();
        }
        tabButton.select();
        selectedTab = tabButton;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        for (ModulesButton modulesButton : modulesButtons) {
            modulesButton.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        lastMouseX = (int) event.x();
        lastMouseY = (int) event.y();

        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.isMouseOver(event.x(), event.y())
                    && modulesButton.mouseClicked(event, doubleClick)) {
                return true;
            }
        }

        WidgetSlot slot = getModuleSlotAt(event.x(), event.y());
        if (slot == null || !isSupportedButton(event.button())) {
            return false;
        }

        Player player = getPlayer();
        if (player == null || (event.button() == 2 && !player.getAbilities().instabuild)) {
            return false;
        }

        long now = System.currentTimeMillis();
        boolean customDoubleClick = event.button() == 0
                && slot.getSlotIndex() == lastClickSlot
                && now - lastClickTime <= DOUBLE_CLICK_WINDOW_MS;
        lastClickSlot = slot.getSlotIndex();
        lastClickTime = now;

        // 双击收集直接走独立操作，不进入普通点击/拖拽状态机。
        if ((doubleClick || customDoubleClick) && event.button() == 0) {
            ClientPlayNetworking.send(new ModuleActionPayload(
                    ModuleActionPayload.COLLECT, slot.getSlotIndex(), 0));
            resetPointer();
            return true;
        }

        pressedButton = event.button();
        pressedSlot = slot.getSlotIndex();
        pressedWithShift = event.hasShiftDown();
        externalPointer = false;
        leftClickPending = event.button() == 0;
        dragSourceSlot = -1;
        draggedSlots.clear();

        if (event.hasShiftDown()) {
            ClientPlayNetworking.send(new ModuleIventoryPayload(
                    pressedSlot, event.button(), 1));
            resetPointer();
            return true;
        }

        if (event.button() == 2) {
            ItemStack source = slot.getActualInfo();
            if (!source.isEmpty()) {
                draggedSlots.add(pressedSlot);
                player.containerMenu.setCarried(source.copyWithCount(source.getMaxStackSize()));
                ClientPlayNetworking.send(new ModuleActionPayload(
                        ModuleActionPayload.CREATIVE_CLONE, pressedSlot, 0));
            }
            return true;
        }

        // 右键仍然按下即执行，随后可以继续保留 carried 进行右键拖拽。
        if (event.button() == 1) {
            ItemStack predictedCarried = applyLocalClick(pressedSlot, event.button(), player.containerMenu.getCarried());
            player.containerMenu.setCarried(predictedCarried);
            ClientPlayNetworking.send(new ModuleIventoryPayload(pressedSlot, event.button(), 0));
            leftClickPending = false;
        }
        return true;
    }

    public void beginExternalPointer(MouseButtonEvent event) {
        if (pressedButton != -1 || !isSupportedButton(event.button())) {
            return;
        }
        Player player = getPlayer();
        if (player == null || (event.button() == 2 && !player.getAbilities().instabuild)) {
            return;
        }
        pressedButton = event.button();
        pressedSlot = -1;
        pressedWithShift = false;
        externalPointer = true;
        leftClickPending = false;
        dragSourceSlot = -1;
        draggedSlots.clear();
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        lastMouseX = (int) event.x();
        lastMouseY = (int) event.y();
        if (pressedButton == -1 || event.button() != pressedButton) {
            return false;
        }

        WidgetSlot slot = getModuleSlotAt(event.x(), event.y());
        if (slot == null) {
            return !externalPointer;
        }

        Player player = getPlayer();
        if (player == null) {
            return true;
        }

        // 左键第一次经过槽位时才决定进入拖拽；否则释放时仍是普通点击。
        if (pressedButton == 0 && leftClickPending && draggedSlots.isEmpty()) {
            ItemStack carried = player.containerMenu.getCarried();
            if (carried.isEmpty()) {
                WidgetSlot source = getModuleSlotAtIndex(pressedSlot);
                if (source == null || source.getActualInfo().isEmpty() || slot.getSlotIndex() == pressedSlot) {
                    return true;
                }
                carried = source.getActualInfo();
                if (!canAcceptDrag(slot, carried)) {
                    return true;
                }
                carried = applyLocalClick(pressedSlot, 0, ItemStack.EMPTY);
                player.containerMenu.setCarried(carried);
                dragSourceSlot = pressedSlot;
            } else if (!canAcceptDrag(slot, carried)) {
                return true;
            }
            leftClickPending = false;
        }

        if (pressedButton == 0 && slot.getSlotIndex() == dragSourceSlot) {
            return true;
        }

        if (pressedButton == 2 && !canAcceptDrag(slot, player.containerMenu.getCarried())) {
            return true;
        }

        if (pressedButton == 2 && player.containerMenu.getCarried().isEmpty()
                && !slot.getActualInfo().isEmpty() && player.getAbilities().instabuild) {
            pressedSlot = slot.getSlotIndex();
            player.containerMenu.setCarried(slot.getActualInfo().copyWithCount(slot.getActualInfo().getMaxStackSize()));
            draggedSlots.add(pressedSlot);
        } else {
            draggedSlots.add(slot.getSlotIndex());
        }
        updateDragPreview();
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        WidgetSlot slot = getModuleSlotAt(lastMouseX, lastMouseY);
        if (slot == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        for (int i = 0; i < minecraft.options.keyHotbarSlots.length; i++) {
            if (minecraft.options.keyHotbarSlots[i].matches(event)) {
                ClientPlayNetworking.send(new ModuleActionPayload(
                        ModuleActionPayload.HOTBAR_SWAP, slot.getSlotIndex(), i));
                return true;
            }
        }
        if (minecraft.options.keyDrop.matches(event)) {
            ClientPlayNetworking.send(new ModuleActionPayload(
                    ModuleActionPayload.DROP, slot.getSlotIndex(), event.hasControlDown() ? 1 : 0));
            return true;
        }
        if (minecraft.options.keySwapOffhand.matches(event)) {
            ClientPlayNetworking.send(new ModuleActionPayload(
                    ModuleActionPayload.OFFHAND_SWAP, slot.getSlotIndex(), 0));
            return true;
        }
        return false;
    }

    public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        WidgetSlot slot = getModuleSlotAt(mouseX, mouseY);
        if (slot != null && !slot.getSlotInfo().isEmpty()) {
            graphics.setTooltipForNextFrame(
                    Minecraft.getInstance().font, slot.getSlotInfo(), mouseX, mouseY);
            return;
        }

        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.isMouseOver(mouseX, mouseY)
                    && mouseX >= modulesButton.getX() + 14
                    && !modulesButton.getModuleSlot().isEmpty()) {
                graphics.setTooltipForNextFrame(
                        Minecraft.getInstance().font, modulesButton.getModuleSlot(), mouseX, mouseY);
                return;
            }
        }
    }

    public boolean hasInstalledModule() {
        return getSHELF_MODULES().stream().anyMatch(module -> module.is(BackpackItems.BACKPACK_MODULE));
    }

    public void cancelPointer() {
        resetPointer();
    }

    private boolean isSupportedButton(int button) {
        return button == 0 || button == 1 || button == 2;
    }

    @Nullable
    private WidgetSlot getModuleSlotAt(double mouseX, double mouseY) {
        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.page != null) {
                WidgetSlot slot = modulesButton.page.getSlotAt(mouseX, mouseY);
                if (slot != null) {
                    return slot;
                }
            }
        }
        return null;
    }

    private boolean finishPointer(MouseButtonEvent event) {
        if (pressedButton == -1 || event.button() != pressedButton) {
            return false;
        }

        WidgetSlot releaseSlot = getModuleSlotAt(event.x(), event.y());
        if (releaseSlot != null && externalPointer) {
            draggedSlots.add(releaseSlot.getSlotIndex());
        }

        boolean handled = pressedSlot != -1 || !draggedSlots.isEmpty();
        // 有经过的目标槽位就提交拖拽，否则提交延迟的普通左键点击。
        if (!draggedSlots.isEmpty()) {
            sendDrag();
            waitingForDragResult = true;
            previewTicks = 0;
        } else if (leftClickPending && pressedButton == 0 && !externalPointer) {
            commitPendingLeftClick();
            handled = true;
        }
        resetPointer();
        return handled;
    }

    private boolean canAcceptDrag(WidgetSlot slot, ItemStack carried) {
        if (carried.isEmpty()) {
            return false;
        }
        ItemStack target = slot.getActualInfo();
        return target.isEmpty()
                || (ItemStack.isSameItemSameComponents(target, carried)
                && target.getCount() < carried.getMaxStackSize());
    }

    // 执行没有转化为拖拽的普通左键点击，并同步本地预测状态。
    private void commitPendingLeftClick() {
        Player player = getPlayer();
        if (player == null || pressedSlot < 0) {
            return;
        }
        ItemStack predictedCarried = applyLocalClick(pressedSlot, 0, player.containerMenu.getCarried());
        player.containerMenu.setCarried(predictedCarried);
        ClientPlayNetworking.send(new ModuleIventoryPayload(pressedSlot, 0, 0));
    }

    @Nullable
    private WidgetSlot getModuleSlotAtIndex(int globalSlotIndex) {
        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.page != null && globalSlotIndex / 27 == modulesButton.page.getTabIndex()) {
                return modulesButton.page.getSlotAtIndex(globalSlotIndex);
            }
        }
        return null;
    }

    private ItemStack applyLocalClick(int globalSlotIndex, int button, ItemStack carried) {
        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.page != null && globalSlotIndex / 27 == modulesButton.page.getTabIndex()) {
                return modulesButton.page.applyClick(globalSlotIndex, button, carried);
            }
        }
        return carried.copy();
    }

    // sourceSlot 只在从自定义槽位取出物品时有效，普通 carried 拖拽使用 -1。
    private void sendDrag() {
        int dragType = pressedButton == 0 ? 0 : pressedButton == 1 ? 1 : 2;
        ClientPlayNetworking.send(new ModuleDragPayload(
                dragType, dragSourceSlot, draggedSlots.stream().mapToInt(Integer::intValue).toArray()));
    }

    // 使用本地 carried 和已经过的槽位生成即时预览，等待服务端确认。
    private void updateDragPreview() {
        Player player = getPlayer();
        if (player == null || player.containerMenu.getCarried().isEmpty()) {
            return;
        }
        int dragType = pressedButton == 0 ? 0 : pressedButton == 1 ? 1 : 2;
        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.page != null) {
                modulesButton.page.updateDragPreview(draggedSlots, dragType, player.containerMenu.getCarried());
            }
        }
    }

    private void clearDragPreview() {
        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.page != null) {
                modulesButton.page.clearPreview();
            }
        }
    }

    private boolean hasPreview() {
        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.page != null && modulesButton.page.hasPreview()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        lastMouseX = (int) event.x();
        lastMouseY = (int) event.y();
        if (finishPointer(event)) {
            return true;
        }

        for (ModulesButton modulesButton : modulesButtons) {
            if (modulesButton.isMouseOver(event.x(), event.y())
                    && modulesButton.mouseReleased(event)) {
                return true;
            }
            if (modulesButton.page != null && modulesButton.page.mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    private void resetPointer() {
        pressedButton = -1;
        pressedSlot = -1;
        pressedWithShift = false;
        externalPointer = false;
        draggedSlots.clear();
        leftClickPending = false;
        dragSourceSlot = -1;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    public Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public ItemStack getChest() {
        Player player = getPlayer();
        return player == null ? ItemStack.EMPTY : player.getItemBySlot(EquipmentSlot.CHEST);
    }

    public List<ItemStack> getSHELF_MODULES() {
        List<ItemStack> modules = getChest().get(BackpackDataComponents.SHELF_MODULES);
        return modules != null ? modules : List.of();
    }

    public ItemStack carriedItemStack() {
        Player player = getPlayer();
        return player == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
    }

    public boolean carriedIsEmpty() {
        return carriedItemStack().isEmpty();
    }

    public boolean carriedModule() {
        return carriedItemStack().is(BackpackItems.BACKPACK_MODULE);
    }

    public int getLeftPos() {
        return leftPos;
    }

    public int getTopPos() {
        return topPos;
    }
}
