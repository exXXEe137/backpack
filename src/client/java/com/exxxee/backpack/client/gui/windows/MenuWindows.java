package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.Inventory.menu.WindowBuiltInRegistries;
import com.exxxee.backpack.Inventory.menu.WindowMenuType;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.client.mixin.AbstractContainerScreenMixin;
import com.exxxee.backpack.mixin.PlayerMixin;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;

import java.util.Map;

import static com.exxxee.backpack.ExxxeeBackpack.LOGGER;

public class MenuWindows {

    private static final Map<WindowMenuType<?>, MenuWindows.WindowConstructor<?, ?>> SCREENS = Maps.<WindowMenuType<?>, MenuWindows.WindowConstructor<?, ?>>newHashMap();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void create (final WindowMenuType<?> type, final Minecraft minecraft, final int windowId) {
        MenuWindows.WindowConstructor constructor = getConstructor((WindowMenuType) type);
        if (constructor == null) {
            LOGGER.warn("Failed to create screen for menu type: {}", type);
        } else {
            constructor.fromPacket(type, minecraft, windowId);
        }
    }

    private static <T extends AbstractBackpackMenu> MenuWindows.WindowConstructor<T, ?> getConstructor(final WindowMenuType<T> type) {
        return (MenuWindows.WindowConstructor<T, ?>) SCREENS.get(type);
    }

    public static <M extends AbstractBackpackMenu, U extends AbstractBackpackWindows<M>> void register(
            final WindowMenuType<? extends M> type, final MenuWindows.WindowConstructor<M, U> factory
    ) {
        MenuWindows.WindowConstructor<?, ?> prev = (MenuWindows.WindowConstructor<?, ?>)SCREENS.put(type, factory);
        if (prev != null) {
            throw new IllegalStateException("Duplicate registration for " + WindowBuiltInRegistries.MENU.get(type));
        }
    }

    static {
        MenuWindows.<ShelfMenu, ShelfWindow>register(WindowMenuType.GENERIC_1x4, menu -> new ShelfWindow(0, 0, menu));
    }

    public static ShelfWindow shelf(AbstractBackpackMenu menu) {
        return new ShelfWindow(0, 0, (ShelfMenu) menu);
    }


    public interface WindowConstructor<T extends AbstractBackpackMenu, U extends AbstractBackpackWindows<T>> {
        default void fromPacket(final WindowMenuType<T> type, final Minecraft minecraft, final int windowId) {
            U window = this.create((T)type.create(windowId, minecraft.player));
            ((IBackpackHost) minecraft.player).setShelfMenu(window.menu);
            if (minecraft.screen instanceof ShelfWindowHost host) {
                host.setShelfWindow((ShelfWindow) window);
            }
        }

        U create(T menu);
    }

}
