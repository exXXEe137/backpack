package com.exxxee.backpack.Network.payload;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record ModuleMenuPayload(int windowId, int shelfSlot, ItemStack stack) implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleMenuPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ModuleMenuPayload::windowId,
            ByteBufCodecs.VAR_INT,
            ModuleMenuPayload::shelfSlot,
            ItemStack.OPTIONAL_STREAM_CODEC,
            ModuleMenuPayload::stack,
            ModuleMenuPayload::new
    );

    public static final CustomPacketPayload.Type<ModuleMenuPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "module_menu"));

    public ModuleMenuPayload {
        stack = stack.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
