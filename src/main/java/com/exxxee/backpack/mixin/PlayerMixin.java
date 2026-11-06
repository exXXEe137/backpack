package com.exxxee.backpack.mixin;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.api.IBackpackHost;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerMixin implements IBackpackHost {

    /**
     * 服务端 BackpackMenu 实例挂这。
     * 客户端 Player 实例此字段永远 null（创建由 ClientPacketListenerMixin 在 Screen 上做）。
     * 生命周期：跟随玩家（登录时 null，登出 GC）。
     */
    @Unique
    private AbstractBackpackMenu backpackMenu;

    @Override
    public AbstractBackpackMenu getBackpackMenu() {
        return this.backpackMenu;
    }

    @Override
    public void setBackpackMenu(AbstractBackpackMenu menu) {
        this.backpackMenu = menu;
    }
}