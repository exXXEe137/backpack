package com.exxxee.backpack.client.gui.windows;

public interface BackpackWindowHost {
    void setBackpackWindow(int windowId, AbstractBackpackWindows<?> window);

    AbstractBackpackWindows<?> getBackpackWindow(int windowId);
}
