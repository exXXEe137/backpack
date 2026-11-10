package com.exxxee.backpack.Network.payload;

import com.exxxee.backpack.ExxxeeBackpack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.HashedStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;

public record ServerboundWindowClickPayload(int windowId, int slotindex, int buttonNum, ContainerInput containerInput, Int2ObjectMap<HashedStack> changedSlots) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerboundWindowClickPayload> STREAM_CODEC = CustomPacketPayload.codec(ServerboundWindowClickPayload::write, ServerboundWindowClickPayload::new);

    public static final CustomPacketPayload.Type<ServerboundWindowClickPayload> TYPE = new Type<ServerboundWindowClickPayload>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "window_click"));

    private ServerboundWindowClickPayload(final FriendlyByteBuf input) {
        this(input.readVarInt(), input.readVarInt(), input.readVarInt(), input.readEnum(ContainerInput.class), readChangedSlots((RegistryFriendlyByteBuf) input));
    }

    private static Int2ObjectMap<HashedStack> readChangedSlots(final RegistryFriendlyByteBuf input) {
        int size = input.readVarInt();
        Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < size; i++) {
            changedSlots.put(input.readVarInt(), HashedStack.STREAM_CODEC.decode(input));
        }
        return changedSlots;
    }

    private void write(final FriendlyByteBuf output) {
        output.writeVarInt(this.windowId);
        output.writeVarInt(this.slotindex);
        output.writeVarInt(this.buttonNum);
        output.writeEnum(this.containerInput);
        output.writeVarInt(this.changedSlots.size());
        for (Int2ObjectMap.Entry<HashedStack> entry : this.changedSlots.int2ObjectEntrySet()) {
            output.writeVarInt(entry.getIntKey());
            HashedStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) output, entry.getValue());
        }
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
