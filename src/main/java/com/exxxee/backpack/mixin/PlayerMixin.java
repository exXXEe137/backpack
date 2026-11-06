package com.exxxee.backpack.mixin;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.api.WindowIdIssuer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import org.apache.commons.io.input.TailerListener;
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

    @Shadow
    public AbstractContainerMenu containerMenu;
    @Shadow
    @Final
    public InventoryMenu inventoryMenu;
    @Unique
    public AbstractBackpackMenu shelfMenu;
    @Unique
    private int windowCounter;
    public List<AbstractBackpackMenu> moduleMenus = new ArrayList<>(4);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void backpack$onInit(CallbackInfo ci) {
        this.shelfMenu = null;
        for (int i = 0; i < 4; i++) {
            moduleMenus.add(null);
        }
    }

    @Override
    public AbstractBackpackMenu getShelfMenu() {
        return this.shelfMenu;
    }

    @Override
    public void setShelfMenu(AbstractBackpackMenu menu) {
        this.shelfMenu = menu;
    }

    @Override
    public int nextWindowId() {
        this.windowCounter = this.windowCounter % 100 + 1;
        return this.windowCounter;
    }

    @Override
    public AbstractBackpackMenu getModuleMenu(int index) {
        return this.moduleMenus.get(index);
    }

    @Override
    public void setModuleMenu(AbstractBackpackMenu menu, int index) {
        this.moduleMenus.set(index, menu);
    }
}