package com.exxxee.backpack.item;

import com.exxxee.backpack.ExxxeeBackpack;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTabs {

    public static final ResourceKey<CreativeModeTab> CREATEBACKPACK_TAB =ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "createbackpack")
    );

    public static final CreativeModeTab CREATEBACKPACK = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(BackpackItems.BACKPACK_SHELF))
            .title(Component.translatable("itemGroup.createbackpack-fly-mod"))
            .displayItems((parameters, output) -> {
                output.accept(BackpackItems.BACKPACK_SHELF);
                output.accept(BackpackItems.BACKPACK_MODULE);
                }).build();

    public static void register() {
        ExxxeeBackpack.LOGGER.info("Registering CreativeModeTabs");
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATEBACKPACK_TAB, CREATEBACKPACK);
    }

}
