package com.exxxee.backpack.client.I_slot;

import net.minecraft.world.item.ItemStack;

/** 槽工厂:槽的创建从 ModulePage 解耦出去,由外部注入具体实现 */
@FunctionalInterface
public interface SlotFactory {
    ISlotWidget create(int x, int y, ItemStack stack, int globalSlotIndex);
}
