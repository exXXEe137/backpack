package com.exxxee.backpack.Inventory.slot;

import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public class BackpackSlot extends Slot {

    private final Player player;

    public BackpackSlot(Player player, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.player = player;
    }

    @Override
    public boolean isActive() {
        return BackpackDataHelper.hasShelf(player);
    }

}
