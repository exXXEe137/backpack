package com.exxxee.backpack.Network.payload;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CloseWindowPayload (int window) implements CustomPacketPayload{

    public static final StreamCodec<FriendlyByteBuf, CloseWindowPayload> STREAM_CODEC = CustomPacketPayload.codec(CloseWindowPayload::write, CloseWindowPayload::new);

    public static final CustomPacketPayload.Type<CloseWindowPayload> TYPE = new Type<CloseWindowPayload>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "close_window"));

    private CloseWindowPayload(final FriendlyByteBuf input) {
        this(input.readVarInt());
    }

    private void write(final FriendlyByteBuf output) {
        output.writeVarInt(this.window);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
