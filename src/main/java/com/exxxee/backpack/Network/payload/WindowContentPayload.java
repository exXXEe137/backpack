package com.exxxee.backpack.Network.payload;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record WindowContentPayload(int windowId, List<ItemStack> items) implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, WindowContentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            WindowContentPayload::windowId,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC,
            WindowContentPayload::items,
            WindowContentPayload::new
    );

    public static final CustomPacketPayload.Type<WindowContentPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "window_content"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
