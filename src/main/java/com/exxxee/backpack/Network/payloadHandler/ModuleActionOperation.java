package com.exxxee.backpack.Network.payloadHandler;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import com.exxxee.backpack.Network.paylo.ModuleActionPayload;
import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ModuleActionOperation {

    public void execute(ServerPlayer player, ModuleActionPayload payload) {
        switch (payload.action()) {
            case ModuleActionPayload.COLLECT -> collect(player, payload.slotIndex());
            case ModuleActionPayload.HOTBAR_SWAP -> hotbarSwap(player, payload.slotIndex(), payload.value());
            case ModuleActionPayload.DROP -> drop(player, payload.slotIndex(), payload.value() != 0);
            case ModuleActionPayload.OFFHAND_SWAP -> offhandSwap(player, payload.slotIndex());
            case ModuleActionPayload.CREATIVE_CLONE -> creativeClone(player, payload.slotIndex());
            case ModuleActionPayload.QUICK_MOVE_TO_MODULES -> quickMoveToModules(player, payload.slotIndex());
            default -> {
            }
        }
    }

    // 双击收集：以鼠标所在物品为目标，收集容器和所有模块中的同类物品。
    private void collect(ServerPlayer player, int globalSlotIndex) {
        if (!isValidModuleSlot(player, globalSlotIndex)) {
            return;
        }

        int moduleIndex = globalSlotIndex / 27;
        int slotIndex = globalSlotIndex % 27;
        List<ItemStack> modules = copyModules(player);
        List<NonNullList<ItemStack>> inventories = new ArrayList<>(modules.size());
        for (ItemStack module : modules) {
            inventories.add(module.is(BackpackItems.BACKPACK_MODULE) ? moduleData(module) : null);
        }

        NonNullList<ItemStack> anchorInventory = inventories.get(moduleIndex);
        ItemStack anchor = anchorInventory.get(slotIndex);
        ItemStack carried = player.containerMenu.getCarried();
        boolean changed = false;
        if (carried.isEmpty()) {
            if (anchor.isEmpty()) {
                return;
            }
            carried = anchor.copy();
            anchorInventory.set(slotIndex, ItemStack.EMPTY);
            changed = true;
        } else if (!anchor.isEmpty() && !ItemStack.isSameItemSameComponents(anchor, carried)) {
            return;
        }

        for (Slot slot : player.containerMenu.slots) {
            if (carried.getCount() >= carried.getMaxStackSize()) {
                break;
            }
            ItemStack source = slot.getItem();
            if (source.isEmpty()
                    || !ItemStack.isSameItemSameComponents(source, carried)
                    || !player.containerMenu.canTakeItemForPickAll(carried, slot)) {
                continue;
            }

            int amount = Math.min(source.getCount(), carried.getMaxStackSize() - carried.getCount());
            ItemStack taken = slot.remove(amount);
            if (!taken.isEmpty()) {
                carried.grow(taken.getCount());
                changed = true;
            }
        }

        for (NonNullList<ItemStack> inventory : inventories) {
            if (inventory == null) {
                continue;
            }
            for (int i = 0; i < inventory.size() && carried.getCount() < carried.getMaxStackSize(); i++) {
                ItemStack source = inventory.get(i);
                if (source.isEmpty() || !ItemStack.isSameItemSameComponents(source, carried)) {
                    continue;
                }
                int amount = Math.min(source.getCount(), carried.getMaxStackSize() - carried.getCount());
                source.shrink(amount);
                inventory.set(i, source.isEmpty() ? ItemStack.EMPTY : source);
                carried.grow(amount);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }
        for (int i = 0; i < modules.size(); i++) {
            NonNullList<ItemStack> inventory = inventories.get(i);
            if (inventory != null) {
                modules.get(i).set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(inventory));
            }
        }
        player.containerMenu.setCarried(carried);
        BackpackDataHelper.updateBackpack(player, modules);
        player.containerMenu.broadcastChanges();
    }

    // 数字键交换模块槽位与玩家快捷栏。
    private void hotbarSwap(ServerPlayer player, int globalSlotIndex, int hotbarSlot) {
        if (!isValidModuleSlot(player, globalSlotIndex) || !Inventory.isHotbarSlot(hotbarSlot)) {
            return;
        }

        int moduleIndex = globalSlotIndex / 27;
        int slotIndex = globalSlotIndex % 27;
        List<ItemStack> modules = copyModules(player);
        ItemStack module = modules.get(moduleIndex);
        NonNullList<ItemStack> data = moduleData(module);
        ItemStack moduleStack = data.get(slotIndex);
        ItemStack hotbarStack = player.getInventory().getItem(hotbarSlot);
        data.set(slotIndex, hotbarStack);
        module.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(data));
        modules.set(moduleIndex, module);
        player.getInventory().setItem(hotbarSlot, moduleStack);
        BackpackDataHelper.updateBackpack(player, modules);
        player.containerMenu.broadcastChanges();
    }

    // Q 丢一个，Ctrl+Q 丢出整叠。
    private void drop(ServerPlayer player, int globalSlotIndex, boolean dropAll) {
        if (!isValidModuleSlot(player, globalSlotIndex) || !player.canDropItems()) {
            return;
        }

        int moduleIndex = globalSlotIndex / 27;
        int slotIndex = globalSlotIndex % 27;
        List<ItemStack> modules = copyModules(player);
        ItemStack module = modules.get(moduleIndex);
        NonNullList<ItemStack> data = moduleData(module);
        ItemStack source = data.get(slotIndex);
        if (source.isEmpty()) {
            return;
        }

        int amount = dropAll ? source.getCount() : 1;
        ItemStack dropped = source.copyWithCount(amount);
        source.shrink(amount);
        data.set(slotIndex, source.isEmpty() ? ItemStack.EMPTY : source);
        module.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(data));
        modules.set(moduleIndex, module);
        BackpackDataHelper.updateBackpack(player, modules);
        player.drop(dropped, true);
        player.containerMenu.broadcastChanges();
    }

    // F 在模块槽位和副手之间交换物品。
    private void offhandSwap(ServerPlayer player, int globalSlotIndex) {
        if (!isValidModuleSlot(player, globalSlotIndex)) {
            return;
        }

        int moduleIndex = globalSlotIndex / 27;
        int slotIndex = globalSlotIndex % 27;
        List<ItemStack> modules = copyModules(player);
        ItemStack module = modules.get(moduleIndex);
        NonNullList<ItemStack> data = moduleData(module);
        ItemStack moduleStack = data.get(slotIndex);
        ItemStack offhandStack = player.getInventory().getItem(Inventory.SLOT_OFFHAND);
        data.set(slotIndex, offhandStack);
        module.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(data));
        modules.set(moduleIndex, module);
        player.getInventory().setItem(Inventory.SLOT_OFFHAND, moduleStack);
        BackpackDataHelper.updateBackpack(player, modules);
        player.containerMenu.broadcastChanges();
    }

    // 创造模式中键复制一整叠物品到鼠标。
    private void creativeClone(ServerPlayer player, int globalSlotIndex) {
        if (!player.getAbilities().instabuild || !isValidModuleSlot(player, globalSlotIndex)) {
            return;
        }

        ItemStack source = getModuleStack(player, globalSlotIndex);
        if (source.isEmpty()) {
            return;
        }

        player.containerMenu.setCarried(source.copyWithCount(source.getMaxStackSize()));
        player.containerMenu.broadcastChanges();
    }

    private void quickMoveToModules(ServerPlayer player, int sourceSlotIndex) {
        if (sourceSlotIndex < 0 || sourceSlotIndex >= player.containerMenu.slots.size()
                || !BackpackDataHelper.hasShelf(player)) {
            return;
        }

        Slot sourceSlot = player.containerMenu.getSlot(sourceSlotIndex);
        ItemStack sourceStack = sourceSlot.getItem();
        if (sourceStack.isEmpty()) {
            return;
        }

        List<ItemStack> modules = copyModules(player);
        ItemStack moving = sourceStack.copy();
        int moved = 0;

        for (int moduleIndex = 0; moduleIndex < modules.size() && !moving.isEmpty(); moduleIndex++) {
            ItemStack module = modules.get(moduleIndex);
            if (!module.is(BackpackItems.BACKPACK_MODULE)) {
                continue;
            }

            NonNullList<ItemStack> data = moduleData(module);
            moved += mergeIntoModule(data, moving, false);
            moved += mergeIntoModule(data, moving, true);
            module.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(data));
            modules.set(moduleIndex, module);
        }

        if (moved == 0) {
            player.containerMenu.quickMoveStack(player, sourceSlotIndex);
            return;
        }

        sourceSlot.set(moving.isEmpty() ? ItemStack.EMPTY : moving);
        sourceSlot.setChanged();
        BackpackDataHelper.updateBackpack(player, modules);
        player.containerMenu.broadcastChanges();
    }

    private int mergeIntoModule(NonNullList<ItemStack> data, ItemStack moving, boolean emptyOnly) {
        int moved = 0;
        for (int i = 0; i < data.size() && !moving.isEmpty(); i++) {
            ItemStack target = data.get(i);
            if (emptyOnly ? !target.isEmpty() : target.isEmpty()
                    || !ItemStack.isSameItemSameComponents(target, moving)) {
                continue;
            }

            if (!emptyOnly && target.isEmpty()) {
                continue;
            }

            int max = moving.getMaxStackSize();
            int space = max - target.getCount();
            if (space <= 0) {
                continue;
            }

            int amount = Math.min(space, moving.getCount());
            if (target.isEmpty()) {
                data.set(i, moving.copyWithCount(amount));
            } else {
                target.grow(amount);
                data.set(i, target);
            }
            moving.shrink(amount);
            moved += amount;
        }
        return moved;
    }

    private boolean isValidModuleSlot(ServerPlayer player, int globalSlotIndex) {
        if (globalSlotIndex < 0 || globalSlotIndex >= 108) {
            return false;
        }
        int moduleIndex = globalSlotIndex / 27;
        return BackpackDataHelper.isValidModuleIndex(player, moduleIndex)
                && BackpackDataHelper.getModule(player, moduleIndex).is(BackpackItems.BACKPACK_MODULE);
    }

    private ItemStack getModuleStack(ServerPlayer player, int globalSlotIndex) {
        int moduleIndex = globalSlotIndex / 27;
        int slotIndex = globalSlotIndex % 27;
        return BackpackDataHelper.getModuleDataInList(player, moduleIndex).get(slotIndex);
    }

    private List<ItemStack> copyModules(ServerPlayer player) {
        List<ItemStack> modules = new ArrayList<>();
        for (ItemStack module : BackpackDataHelper.getModules(player)) {
            modules.add(module.copy());
        }
        return modules;
    }

    private NonNullList<ItemStack> moduleData(ItemStack module) {
        ModuleInventoryData data = module.get(BackpackDataComponents.MODULE_INVENTORY);
        return data == null ? NonNullList.withSize(27, ItemStack.EMPTY) : data.toList();
    }
}
