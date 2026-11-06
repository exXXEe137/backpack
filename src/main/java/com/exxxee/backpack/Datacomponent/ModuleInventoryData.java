package com.exxxee.backpack.Datacomponent;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

public record ModuleInventoryData(ItemContainerContents items) {

    public static final Codec<ModuleInventoryData> CODEC = ItemContainerContents.CODEC.xmap(ModuleInventoryData::new,ModuleInventoryData::items );

    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleInventoryData> STREAM_CODEC = ItemContainerContents.STREAM_CODEC.map(ModuleInventoryData::new,ModuleInventoryData::items);

    public static final ModuleInventoryData EMPTY = new ModuleInventoryData(ItemContainerContents.EMPTY);

    // 读:内部 27 格 → 拷成新的列表返回(调用方随意改,不碰内部)
    public NonNullList<ItemStack> toList () {
        NonNullList<ItemStack> list = NonNullList.withSize(27, ItemStack.EMPTY);
        items.copyInto(list);
        return list;
    }

    // 写:27 格列表 → 造新实例(record 不可变,所以是静态工厂,不是 setter)
    public static ModuleInventoryData fromList (List<ItemStack> list) {
        return new ModuleInventoryData(ItemContainerContents.fromItems(list));
    }

}