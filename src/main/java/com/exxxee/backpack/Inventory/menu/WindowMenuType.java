package com.exxxee.backpack.Inventory.menu;

import net.minecraft.world.entity.player.Player;

public class WindowMenuType<T extends AbstractBackpackMenu> {

    public static final WindowMenuType<ShelfMenu> GENERIC_1x4 = new WindowMenuType<>(ShelfMenu::new);

    private final MenuSupplier<T> constructor;

    public static void register() {
        WindowBuiltInRegistries.MENU.put("generic_1x4", GENERIC_1x4);
    }

    private WindowMenuType(MenuSupplier<T> constructor) {
        this.constructor = constructor;
    }

    public T create(int windowId, Player player) {
        T menu = this.constructor.create(windowId, player);
        menu.menuType = this;
        return menu;
    }

    public interface MenuSupplier<T extends AbstractBackpackMenu> {
        T create(int windowId, Player player);
    }
}
