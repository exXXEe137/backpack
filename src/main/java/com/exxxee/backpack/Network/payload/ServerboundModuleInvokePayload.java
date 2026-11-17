package com.exxxee.backpack.Network.payload;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ServerboundModuleInvokePayload(int windowId, boolean invoked) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerboundModuleInvokePayload> STREAM_CODEC = CustomPacketPayload.codec(ServerboundModuleInvokePayload::write, ServerboundModuleInvokePayload::new);

    public static final CustomPacketPayload.Type<ServerboundModuleInvokePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "module_invoke"));

    private ServerboundModuleInvokePayload(final FriendlyByteBuf input) {
        this(input.readVarInt(), input.readBoolean());
    }

    private void write(final FriendlyByteBuf output) {
        output.writeVarInt(this.windowId);
        output.writeBoolean(this.invoked);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
