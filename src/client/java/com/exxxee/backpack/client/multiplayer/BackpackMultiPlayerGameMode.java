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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.exxxee.backpack.ExxxeeBackpack.LOGGER;

public class BackpackMultiPlayerGameMode {

    public static void handleContainerInput(final int windowId, final int slotNum, final int buttonNum, final ContainerInput containerInput, final Player player) {
        AbstractBackpackMenu bakcpackMenu = ((IBackpackHost)player).getBackpackMenu(windowId);
        if (bakcpackMenu == null || windowId != bakcpackMenu.containerId) {
            LOGGER.warn("Ignoring click in mismatching window. Click in {}, player has {}.", windowId, bakcpackMenu == null ? -1 : bakcpackMenu.containerId);
            return;
        }

        // 创造模式的快速移动不做本地预测：交给服务端用它自己那份菜单走一次真正的 clicked。
        // 本地不搬、也不发“最终状态”表，就不会出现把玩家库存/盔甲当落点写死的可能。
        if (player.hasInfiniteMaterials() && containerInput == ContainerInput.QUICK_MOVE) {
            ClientPlayNetworking.send(new ServerboundWindowClickPayload(windowId, slotNum, buttonNum, containerInput,
                    EMPTY_HASHES, EMPTY_STACKS, EMPTY_STACKS, EMPTY_STACKS));
            return;
        }

        NonNullList<Slot> slots = bakcpackMenu.slots;
        int slotCount = slots.size();

        List<ItemStack> itemsBeforeClick = Lists.<ItemStack>newArrayListWithCapacity(slotCount);
        boolean creative = player.hasInfiniteMaterials();
        for (Slot slot : slots) {
            itemsBeforeClick.add(slot.getItem().copy());
        }

        bakcpackMenu.clicked(slotNum, buttonNum, containerInput, player);

        Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<ItemStack> creativeSlots = new Int2ObjectOpenHashMap<>();

        for (int i = 0; i < slotCount; i++) {
            ItemStack before = itemsBeforeClick.get(i);
            ItemStack after = slots.get(i).getItem();
            if (!ItemStack.matches(before, after)) {
                changedSlots.put(i, HashedStack.create(after, ((LocalPlayer) player).connection.decoratedHashOpsGenenerator()));
                if (creative) {
                    creativeSlots.put(i, after.copy());
                }
            }
        }

        // 创造模式只有“光标上拿着东西”这类点击需要把结果整格发过去；
        // 数量键（SWAP）走的是 player.getInventory()，背包这一侧只可能有背包格变化。
        Int2ObjectMap<ItemStack> creativeInventorySlots = new Int2ObjectOpenHashMap<>();
        if (creative && containerInput == ContainerInput.SWAP && (buttonNum >= 0 && buttonNum < 9 || buttonNum == 40)) {
            creativeInventorySlots.put(buttonNum, player.getInventory().getItem(buttonNum).copy());
        }

        ClientPlayNetworking.send(new ServerboundWindowClickPayload(windowId, slotNum, buttonNum, containerInput,
                changedSlots, creativeSlots, creativeInventorySlots, EMPTY_STACKS));
    }

    private static final Int2ObjectMap<HashedStack> EMPTY_HASHES = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<ItemStack> EMPTY_STACKS = new Int2ObjectOpenHashMap<>();
}
