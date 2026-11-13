package com.exxxee.backpack.mixin;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.api.WindowIdIssuer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Player.class)
public abstract class PlayerMixin implements IBackpackHost, WindowIdIssuer {

    @Shadow public AbstractContainerMenu containerMenu;
    @Shadow
    @Final public InventoryMenu inventoryMenu;

    @Unique private int windowCounter;
    @Unique private List<Container> backpackContainers = new ArrayList<>();
    @Unique private List<AbstractBackpackMenu> backpackMenus = new ArrayList<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void backpack$onInit(CallbackInfo ci) {
        this.backpackContainers.clear();
        this.backpackMenus.clear();
    }

    @Unique public List<Container> getBackpackContainers() {
        return this.backpackContainers;
    }

    @Unique public Container getBackpackContainer(int windowId) {
        if (windowId < 0 || windowId >= this.backpackContainers.size()) return null;
        return this.backpackContainers.get(windowId);
    }

    @Unique public void setBackpackContainer(int windowId, Container container) {
        while (this.backpackContainers.size() <= windowId) {
            this.backpackContainers.add(null);
        }
        this.backpackContainers.set(windowId, container);
    }

    @Unique public List<AbstractBackpackMenu> getBackpackMenus() {
        return this.backpackMenus;
    }

    @Unique public AbstractBackpackMenu getBackpackMenu(int windowId) {
        if (windowId < 0 || windowId >= this.backpackMenus.size()) return null;
        return this.backpackMenus.get(windowId);
    }

    @Unique public void setBackpackMenu(int windowId, AbstractBackpackMenu menu) {
        while (this.backpackMenus.size() <= windowId) {
            this.backpackMenus.add(null);
        }
        this.backpackMenus.set(windowId, menu);
    }

    @Override
    public int nextWindowId() {
        this.windowCounter = this.windowCounter % 100 + 1;
        return this.windowCounter;
    }

}