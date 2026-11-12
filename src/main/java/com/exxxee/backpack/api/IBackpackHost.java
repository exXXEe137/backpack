package com.exxxee.backpack.api;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;

import java.util.List;

public interface IBackpackHost {
    List<AbstractBackpackMenu> getBackpackMenus();
    AbstractBackpackMenu getBackpackMenu(int windowId);
    void setBackpackMenu(int windowId, AbstractBackpackMenu menu);
}