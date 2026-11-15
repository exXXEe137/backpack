package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.container.ShelfContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public class WindowMenuType<T extends AbstractBackpackMenu> {

    public static final WindowMenuType<ShelfMenu> GENERIC_1x4 = new WindowMenuType<>(ShelfMenu::new, ShelfContainer::new);
    public static final WindowMenuType<ModuleMenu> MODULE = new WindowMenuType<>(
            (windowId, player, container) -> {
                throw new IllegalStateException("module menu is created with its item");
            },
            player -> {
                throw new IllegalStateException("module container comes with the item");
            }
    );

    private final MenuSupplier<T> constructor;
    private final ContainerSupplier containerSupplier;

    public static void register() {
        WindowBuiltInRegistries.MENU.put("generic_1x4", GENERIC_1x4);
    }

    private WindowMenuType(MenuSupplier<T> constructor, ContainerSupplier containerSupplier) {
        this.constructor = constructor;
        this.containerSupplier = containerSupplier;
    }

    public Container createContainer(Player player) {
        return this.containerSupplier.create(player);
    }

    public T create(int windowId, Player player, Container container) {
        T menu = this.constructor.create(windowId, player, container);
        menu.menuType = this;
        return menu;
    }

    public interface MenuSupplier<T extends AbstractBackpackMenu> {
        T create(int windowId, Player player, Container container);
    }

    public interface ContainerSupplier {
        Container create(Player player);
    }
}
