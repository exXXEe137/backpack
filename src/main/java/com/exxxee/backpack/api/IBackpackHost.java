package com.exxxee.backpack.api;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;

/**
 * 任何持有 AbstractBackpackMenu 实例的"宿主"实现此接口。
 * 客户端 AbstractContainerScreen 的 Mixin 实现此接口，让外部能安全拿到/写入 backpackMenu，
 * 同时不直接引用 Mixin 类（避免 "Mixin class cannot be referenced directly"）。
 */
public interface IBackpackHost {
    AbstractBackpackMenu getBackpackMenu();
    void setBackpackMenu(AbstractBackpackMenu menu);
}