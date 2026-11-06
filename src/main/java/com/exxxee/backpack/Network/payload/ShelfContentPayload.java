package com.exxxee.backpack.Network.payload;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ShelfContentPayload(List<ItemStack> items) implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ShelfContentPayload> STREAM_CODEC = StreamCodec.composite(ItemStack.OPTIONAL_LIST_STREAM_CODEC, ShelfContentPayload::items, ShelfContentPayload::new);

    public static final CustomPacketPayload.Type<ShelfContentPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "init_shelf_payload"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
