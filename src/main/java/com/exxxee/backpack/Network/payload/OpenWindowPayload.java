package com.exxxee.backpack.Network.payload;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenWindowPayload(int windowId, String menuType) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, OpenWindowPayload> STREAM_CODEC = CustomPacketPayload.codec(OpenWindowPayload::write, OpenWindowPayload::new);

    public static final CustomPacketPayload.Type<OpenWindowPayload> TYPE = new Type<OpenWindowPayload>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "open_window"));

    private OpenWindowPayload(final FriendlyByteBuf input) {
        this(input.readVarInt(), input.readUtf());
    }

    private void write(final FriendlyByteBuf output) {
        output.writeVarInt(this.windowId);
        output.writeUtf(this.menuType);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
