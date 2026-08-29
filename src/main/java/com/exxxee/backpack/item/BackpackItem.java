package com.exxxee.backpack.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;

import net.minecraft.world.level.Level;

public class BackpackItem  extends Item {

    public BackpackItem(Properties properties) {
        super (properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        return InteractionResult.SUCCESS;
    }

}
