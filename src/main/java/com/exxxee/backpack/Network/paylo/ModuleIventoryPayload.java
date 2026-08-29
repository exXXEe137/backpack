package com.exxxee.backpack.Network.paylo;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ModuleIventoryPayload(int slotIndex, int button, int shift) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ModuleIventoryPayload> STREAM_CODEC = CustomPacketPayload.codec(ModuleIventoryPayload::write, ModuleIventoryPayload::new);

    public static final CustomPacketPayload.Type<ModuleIventoryPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "stack"));

    private ModuleIventoryPayload (final FriendlyByteBuf input) {
        this(input.readVarInt(), input.readVarInt(), input.readVarInt());
    }

    private void write(final FriendlyByteBuf output) {
        output.writeVarInt(slotIndex);
        output.writeVarInt(button);
        output.writeVarInt(shift);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
