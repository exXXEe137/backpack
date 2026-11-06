package com.exxxee.backpack.item;

import com.exxxee.backpack.ExxxeeBackpack;
import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Datacomponent.ModuleInventoryData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class BackpackItems {

    //物品注册列表
    public static final Item BACKPACK_SHELF = registerItem("backpacks/backpack_shelf",new Item.Properties().stacksTo(1).equippable(net.minecraft.world.entity.EquipmentSlot.CHEST).component(BackpackDataComponents.SHELF_MODULES, new ArrayList<>(Collections.nCopies(4, ItemStack.EMPTY))));
    public static final Item BACKPACK_MODULE = registerItem("backpacks/backpack_module",new Item.Properties().stacksTo(1).component(BackpackDataComponents.MODULE_INVENTORY, ModuleInventoryData.EMPTY));

    public static final TagKey<Item> MODULE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, "modules"));

    //物品注册方法
    private static Item registerItem(final String name, final Function<Item.Properties, Item> itemFactory, final Item.Properties properties) {

        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ExxxeeBackpack.MOD_ID, name));
        Item item = (Item)itemFactory.apply(properties.setId(key));

        if (item instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        }

        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static  Item registerItem(final String name, final Item.Properties properties) {
        return registerItem(name,BackpackItem::new, properties);
    }

    private static Item registerItem(final String name, final Function<Item.Properties, Item> itemFactory) {
        return registerItem(name,itemFactory,new Item.Properties());
    }

    private static Item registerItem(final String name) {
        return registerItem(name,BackpackItem::new,new Item.Properties());
    }

    public static void register() {
    }

}
