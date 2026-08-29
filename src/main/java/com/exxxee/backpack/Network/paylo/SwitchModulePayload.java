package com.exxxee.backpack.Network.paylo;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SwitchModulePayload(int tabIndex) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, SwitchModulePayload> STREAM_CODEC = CustomPacketPayload.codec(SwitchModulePayload::write, SwitchModulePayload::new);

    public static final CustomPacketPayload.Type<SwitchModulePayload> TYPE = new Type<SwitchModulePayload>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "module"));

    private SwitchModulePayload(final FriendlyByteBuf input) {
        this(input.readVarInt());
    }

    private void write(final FriendlyByteBuf output) {
        output.writeVarInt(this.tabIndex);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
