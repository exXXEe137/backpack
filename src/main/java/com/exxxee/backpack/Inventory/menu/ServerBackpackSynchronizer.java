package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.Network.payload.WindowContentPayload;
import com.exxxee.backpack.Network.payload.WindowSlotPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.HashedStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ServerBackpackSynchronizer implements BackpackSynchronizer {

    private final ServerPlayer player;

    public ServerBackpackSynchronizer(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void sendInitialData(AbstractBackpackMenu menu, List<ItemStack> slotItems, int[] dataSlots) {
        List<ItemStack> items = new ArrayList<>(slotItems.size());
        for (ItemStack stack : slotItems) {
            items.add(stack.copy());
        }
        ServerPlayNetworking.send(this.player, new WindowContentPayload(menu.windowId, items));
    }

    @Override
    public void sendSlotChange(AbstractBackpackMenu menu, int slotIndex, ItemStack itemStack) {
        ServerPlayNetworking.send(this.player, new WindowSlotPayload(menu.windowId, slotIndex, itemStack));
    }

    @Override
    public void sendDataChange(AbstractBackpackMenu menu, int id, int value) {
    }

    @Override
    public RemoteSlot createSlot() {
        return new TrackedRemoteSlot();
    }

    private static final class TrackedRemoteSlot implements RemoteSlot {
        private ItemStack remote = ItemStack.EMPTY;
        private boolean hasRemote;

        @Override
        public void force(ItemStack outgoing) {
            this.remote = outgoing.copy();
            this.hasRemote = true;
        }

        @Override
        public void receive(HashedStack incoming) {
        }

        @Override
        public boolean matches(ItemStack local) {
            return this.hasRemote && ItemStack.matches(this.remote, local);
        }
    }
}
