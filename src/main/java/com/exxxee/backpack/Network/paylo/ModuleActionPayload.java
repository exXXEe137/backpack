package com.exxxee.backpack.Network.paylo;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ModuleActionPayload(int action, int slotIndex, int value) implements CustomPacketPayload {
    public static final int COLLECT = 0;
    public static final int HOTBAR_SWAP = 1;
    public static final int DROP = 2;
    public static final int OFFHAND_SWAP = 3;
    public static final int CREATIVE_CLONE = 4;
    public static final int QUICK_MOVE_TO_MODULES = 5;

    public static final StreamCodec<FriendlyByteBuf, ModuleActionPayload> STREAM_CODEC =
            CustomPacketPayload.codec(ModuleActionPayload::write, ModuleActionPayload::new);

    public static final Type<ModuleActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "module_action"));

    private ModuleActionPayload(FriendlyByteBuf input) {
        this(input.readVarInt(), input.readVarInt(), input.readVarInt());
    }

    private void write(FriendlyByteBuf output) {
        output.writeVarInt(action);
        output.writeVarInt(slotIndex);
        output.writeVarInt(value);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
