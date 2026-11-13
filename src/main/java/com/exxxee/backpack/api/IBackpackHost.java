package com.exxxee.backpack.api;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import net.minecraft.world.Container;

import java.util.List;

public interface IBackpackHost {
    List<Container> getBackpackContainers();
    Container getBackpackContainer(int windowId);
    void setBackpackContainer(int windowId, Container container);
    List<AbstractBackpackMenu> getBackpackMenus();
    AbstractBackpackMenu getBackpackMenu(int windowId);
    void setBackpackMenu(int windowId, AbstractBackpackMenu menu);
}