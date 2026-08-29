package com.exxxee.backpack.api.helper.data;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import com.exxxee.backpack.api.IExtendedInventory;
import com.exxxee.backpack.item.BackpackItems;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BackpackDataHelper {
    private BackpackDataHelper() {}

    public static ItemStack getBackpack(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST);
    }

    public static boolean hasShelf (Player player) {
        return getBackpack(player).is(BackpackItems.BACKPACK_SHELF);
    }

/**==========================================module================================================**/

    public static List<ItemStack> getModules (Player player) {
        if (!hasShelf(player)) {return null;}
        return getBackpack(player).get(BackpackDataComponents.SHELF_MODULES);
    }
    public static ItemStack getModule (Player player, int moduleIndex) {
        List<ItemStack> modules = getModules(player);
        if (modules == null) {return null;}
        return modules.get(moduleIndex);
    }

//========================================ModuleUpDate=============================================//

    public static boolean isValidModuleIndex (Player player, int moduleIndex) {
        List<ItemStack> modules = getModules(player);
        return modules != null && moduleIndex >= 0 && moduleIndex < modules.size();
    }

    public static void updateBackpack (Player player, int moduleIndex, NonNullList<ItemStack> moduleInventoryData) {
        ItemStack module = getModule(player, moduleIndex);
        if (module == null) {return;}
        ItemStack newModule = module.copy();
        newModule.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(moduleInventoryData));
        updateBackpack(player, moduleIndex, newModule);
    }

    public static void updateBackpack (Player player,  int moduleIndex, ItemStack newModule) {
        if (!hasShelf(player) || !isValidModuleIndex(player, moduleIndex)) {return;}
        List<ItemStack> newModules = new ArrayList<ItemStack>(getModules(player));
        newModules.set(moduleIndex, newModule);
        updateBackpack(player, newModules);
    }
    public static void updateBackpack (Player player, List<ItemStack> newModules) {
        if (!hasShelf(player)) {return;}
        getBackpack(player).set(BackpackDataComponents.SHELF_MODULES, newModules);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(serverPlayer.getInventory().createInventoryUpdatePacket(EquipmentSlot.CHEST.getIndex(36)));
        }
        ((IExtendedInventory) player.getInventory()).backpack$syncFromModules();
    }

/**===========================================ModuleData================================================**/

    public static ModuleInventoryData getModuleData (Player player, int moduleIndex) {
        ItemStack module = getModule(player, moduleIndex);
        if (module == null) {return null;}
        return module.get(BackpackDataComponents.MODULE_INVENTORY);
    }

    public static NonNullList<ItemStack> getModuleDataInList (Player player, int moduleIndex) {
        ModuleInventoryData data = getModuleData(player, moduleIndex);
        if (data == null) {
            return NonNullList.withSize(27, ItemStack.EMPTY);
        }
        return data.toList();
    }

//===============================================update=======================================================//

    public static boolean isValidSlotIndex (int slotIndex) {
        return slotIndex >= 0 && slotIndex < 27;
    }

    public static void update (Player player, int moduleIndex, int slotIndex, ItemStack upDateData) {
        if (!hasShelf(player) || !isValidModuleIndex(player, moduleIndex) || !isValidSlotIndex(slotIndex)) {return;}
        ItemStack module = getModule(player, moduleIndex);
        ItemStack newModule = module.copy();
        NonNullList<ItemStack> moduleData = getModuleDataInList(player, moduleIndex);
        moduleData.set(slotIndex, upDateData);
        newModule.set(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.fromList(moduleData));
        updateBackpack(player, moduleIndex, newModule);
    }

}
