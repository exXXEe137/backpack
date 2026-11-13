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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(Player.class)
public abstract class PlayerMixin implements IBackpackHost, WindowIdIssuer {

    @Shadow public AbstractContainerMenu containerMenu;
    @Shadow
    @Final public InventoryMenu inventoryMenu;

    @Unique private int windowCounter;
    @Unique private final Map<Integer, Container> backpackContainers = new LinkedHashMap<>();
    @Unique private final Map<Integer, AbstractBackpackMenu> backpackMenus = new LinkedHashMap<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void backpack$onInit(CallbackInfo ci) {
        this.backpackContainers.clear();
        this.backpackMenus.clear();
    }

    @Unique public Collection<Container> getBackpackContainers() {
        return this.backpackContainers.values();
    }

    @Unique public Container getBackpackContainer(int windowId) {
        return this.backpackContainers.get(windowId);
    }

    @Unique public void setBackpackContainer(int windowId, Container container) {
        if (container == null) {
            this.backpackContainers.remove(windowId);
        } else {
            this.backpackContainers.put(windowId, container);
        }
    }

    @Unique public Collection<AbstractBackpackMenu> getBackpackMenus() {
        return this.backpackMenus.values();
    }

    @Unique public AbstractBackpackMenu getBackpackMenu(int windowId) {
        return this.backpackMenus.get(windowId);
    }

    @Unique public void setBackpackMenu(int windowId, AbstractBackpackMenu menu) {
        if (menu == null) {
            this.backpackMenus.remove(windowId);
        } else {
            this.backpackMenus.put(windowId, menu);
        }
    }

    @Override
    public int nextWindowId() {
        this.windowCounter = this.windowCounter % 100 + 1;
        return this.windowCounter;
    }

}