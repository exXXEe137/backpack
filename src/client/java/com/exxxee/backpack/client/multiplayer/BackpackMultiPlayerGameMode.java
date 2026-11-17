package com.exxxee.backpack.client.multiplayer;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Network.payload.ServerboundWindowClickPayload;
import com.exxxee.backpack.api.IBackpackHost;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.exxxee.backpack.ExxxeeBackpack.LOGGER;

public class BackpackMultiPlayerGameMode {

    public static void handleContainerInput(final int windowId, final int slotNum, final int buttonNum, final ContainerInput containerInput, final Player player) {
        AbstractBackpackMenu bakcpackMenu = ((IBackpackHost)player).getBackpackMenu(windowId);
        if (windowId != bakcpackMenu.containerId) {
            LOGGER.warn("Ignoring click in mismatching window. Click in {}, player has {}.", windowId, bakcpackMenu.containerId);
        } else {
            NonNullList<Slot> slots = bakcpackMenu.slots;
            int slotCount = slots.size();

            List<ItemStack> itemsBeforeClick = Lists.<ItemStack>newArrayListWithCapacity(slotCount);
            boolean creative = player.hasInfiniteMaterials();
            AbstractContainerMenu hostMenu = player.containerMenu;
            List<ItemStack> hostBeforeClick = List.of();
            if (creative && containerInput == ContainerInput.QUICK_MOVE && hostMenu != null) {
                hostBeforeClick = Lists.<ItemStack>newArrayListWithCapacity(hostMenu.slots.size());
                for (Slot slot : hostMenu.slots) {
                    hostBeforeClick.add(slot.getItem().copy());
                }
            }

            for (Slot slot : slots) {
                itemsBeforeClick.add(slot.getItem().copy());
            }

            bakcpackMenu.clicked(slotNum, buttonNum, containerInput, player);

            Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();
            Int2ObjectMap<ItemStack> creativeSlots = new Int2ObjectOpenHashMap<>();
            Int2ObjectMap<ItemStack> creativeInventorySlots = new Int2ObjectOpenHashMap<>();
            Int2ObjectMap<ItemStack> creativeMenuSlots = new Int2ObjectOpenHashMap<>();

            for (int i = 0; i < slotCount; i++) {
                ItemStack before = (ItemStack)itemsBeforeClick.get(i);
                ItemStack after = ((Slot)slots.get(i)).getItem();
                if (!ItemStack.matches(before, after)) {
                    changedSlots.put(i,HashedStack.create(after, ((LocalPlayer) player).connection.decoratedHashOpsGenenerator()));
                    if (creative) {
                        creativeSlots.put(i, after.copy());
                    }
                }
            }
            if (creative && containerInput == ContainerInput.SWAP && (buttonNum >= 0 && buttonNum < 9 || buttonNum == 40)) {
                creativeInventorySlots.put(buttonNum, player.getInventory().getItem(buttonNum).copy());
            }
            if (creative && containerInput == ContainerInput.QUICK_MOVE && hostMenu != null && hostMenu.slots.size() == hostBeforeClick.size()) {
                for (int i = 0; i < hostBeforeClick.size(); i++) {
                    Slot slot = hostMenu.slots.get(i);
                    ItemStack after = slot.getItem();
                    if (!ItemStack.matches(hostBeforeClick.get(i), after)) {
                        if (slot.container == player.getInventory()) {
                            creativeInventorySlots.put(slot.getContainerSlot(), after.copy());
                        } else {
                            creativeMenuSlots.put(slot.index, after.copy());
                        }
                    }
                }
            }

            ClientPlayNetworking.send(new ServerboundWindowClickPayload(windowId, slotNum, buttonNum, containerInput, changedSlots, creativeSlots, creativeInventorySlots, creativeMenuSlots));
        }
    }

}
