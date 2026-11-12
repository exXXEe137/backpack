package com.exxxee.backpack.Network.payload;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record WindowSlotPayload(int windowId, int slot, ItemStack item) implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, WindowSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            WindowSlotPayload::windowId,
            ByteBufCodecs.SHORT.map(Short::intValue, Integer::shortValue),
            WindowSlotPayload::slot,
            ItemStack.OPTIONAL_STREAM_CODEC,
            WindowSlotPayload::item,
            WindowSlotPayload::new
    );

    public static final CustomPacketPayload.Type<WindowSlotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "window_slot"));

    public WindowSlotPayload {
        item = item.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
