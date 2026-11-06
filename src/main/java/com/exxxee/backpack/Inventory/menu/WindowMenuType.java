package com.exxxee.backpack.Inventory.menu;

import net.minecraft.world.entity.player.Player;

public class WindowMenuType<T extends AbstractBackpackMenu> {

    public static final WindowMenuType<ShelfMenu> GENERIC_1x4 = register("generic_1x4", ShelfMenu::new);

    private final MenuSupplier<T> constructor;

    private static <T extends AbstractBackpackMenu> WindowMenuType<T> register(final String name, MenuSupplier<T> constructor) {
        WindowMenuType<T> type = new WindowMenuType<>(constructor);
        WindowBuiltInRegistries.MENU.put(name, type);
        return type;
    }

    private WindowMenuType(MenuSupplier<T> constructor) {
        this.constructor = constructor;
    }

    public T create(int windowId, Player player) {
        return this.constructor.create(windowId, player);
    }

    public interface MenuSupplier<T extends AbstractBackpackMenu> {
        T create(int windowId, Player player);
    }
}
