package com.exxxee.backpack.api;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;

public interface IBackpackHost {
    AbstractBackpackMenu getShelfMenu();
    void setShelfMenu(AbstractBackpackMenu menu);

    AbstractBackpackMenu getModuleMenu(int index);
    void setModuleMenu(AbstractBackpackMenu menu, int index);
}