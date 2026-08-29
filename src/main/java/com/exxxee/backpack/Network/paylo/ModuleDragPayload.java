package com.exxxee.backpack.Network.paylo;

import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// dragType：0 左键，1 右键，2 创造模式中键；sourceSlot 为自定义槽位拖拽源。
public record ModuleDragPayload(int dragType, int sourceSlot, int[] slots) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, ModuleDragPayload> STREAM_CODEC =
            CustomPacketPayload.codec(ModuleDragPayload::write, ModuleDragPayload::new);

    public static final CustomPacketPayload.Type<ModuleDragPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "drag"));

    // 网络字段顺序必须与 write 保持一致。
    private ModuleDragPayload(FriendlyByteBuf input) {
        this(input.readVarInt(), input.readVarInt(), input.readVarIntArray(108));
    }

    private void write(FriendlyByteBuf output) {
        output.writeVarInt(dragType);
        output.writeVarInt(sourceSlot);
        output.writeVarIntArray(slots);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
