package com.exxxee.backpack.client.gui.windows;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.Inventory.menu.WindowBuiltInRegistries;
import com.exxxee.backpack.Inventory.menu.WindowMenuType;
import com.exxxee.backpack.api.IBackpackHost;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static AbstractBackpackWindows<?> window(AbstractBackpackMenu menu) {
        if (menu.menuType == null) return null;
        WindowConstructor constructor = getConstructor((WindowMenuType) menu.menuType);
        if (constructor == null) return null;
        return constructor.create(menu);
    }


    public interface WindowConstructor<T extends AbstractBackpackMenu, U extends AbstractBackpackWindows<T>> {
        default void fromPacket(final WindowMenuType<T> type, final Minecraft minecraft, final int windowId) {
            U window = this.create((T)type.create(windowId, minecraft.player));
            ((IBackpackHost) minecraft.player).setBackpackMenu(windowId, window.menu);
//            ((IBackpackHost) minecraft.player).setShelfMenu(window.menu);
            if (minecraft.screen instanceof BackpackWindowHost host) {
                host.setBackpackWindow(windowId, window);
            }
        }

        U create(T menu);
    }

}
