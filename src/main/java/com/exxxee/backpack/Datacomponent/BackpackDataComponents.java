package com.exxxee.backpack.Datacomponent;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import com.exxxee.backpack.ExxxeeBackpack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import com.mojang.serialization.Codec;

import java.util.List;


public class BackpackDataComponents {

    public static final DataComponentType<ModuleInventoryData> MODULE_INVENTORY = registerDatacomponent(
            "module_inventory",
            DataComponentType
                    .<ModuleInventoryData>builder()
                    .persistent(ModuleInventoryData.CODEC)
                    .networkSynchronized(ModuleInventoryData.STREAM_CODEC)
    );

    public static final DataComponentType<List<ItemStack>> SHELF_MODULES = registerDatacomponent(
            "shelf_module",
            DataComponentType
                    .<List<ItemStack>>builder()
                    .persistent(Codec.list(ItemStack.OPTIONAL_CODEC))
                    .networkSynchronized(ItemStack.OPTIONAL_LIST_STREAM_CODEC)
    );

    private static <T> DataComponentType<T> registerDatacomponent (
            final String name,
            final DataComponentType
                    .Builder<T> builder) {
        ResourceKey<DataComponentType<?>> key = ResourceKey
                .create(Registries
                        .DATA_COMPONENT_TYPE,
                        Identifier.
                                fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, name));
        return (DataComponentType<T>) Registry
                .register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, builder.build());
    }

    public static void register () {
    }

}
