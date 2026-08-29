package com.exxxee.backpack.Network.paylo;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record  ModuleDragPayload(int dragType, int[] slots) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, ModuleDragPayload> STREAM_CODEC = CustomPacketPayload.codec(ModuleDragPayload::write, ModuleDragPayload::new);

    public static final CustomPacketPayload.Type<ModuleDragPayload> TYPE = new CustomPacketPayload.Type<ModuleDragPayload>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "drag"));

    private ModuleDragPayload(final FriendlyByteBuf input) {
        this(input.readVarInt(), input.readVarIntArray(108));
    }

    private void write(final FriendlyByteBuf output) {
        output.writeVarInt(dragType);
        output.writeVarIntArray(slots);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
