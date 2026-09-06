package com.exxxee.backpack.client.guirender;

import com.exxxee.backpack.Network.paylo.ModuleIventoryPayload;
import com.exxxee.backpack.client.I_slot.BaseSlot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

/** 具体槽:只实现交互行为;坐标/索引/物品/渲染都继承 BaseSlot */
public class WidgetSlot extends BaseSlot {

    /** 按下标记：按下时打 true，释放时是 true 才发包（按下与释放同一槽位） */
    private boolean pressed;

    public WidgetSlot(int x, int y, ItemStack slotInfo, int slotIndex) {
        super(x, y, slotInfo, slotIndex);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 按下只打标记，不发包；左右键才接受
        if (event.button() == 0 || event.button() == 1) {
            this.pressed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // 释放时按下标记为真（按下与释放同一槽位）才发包；始终消费释放事件，避免原版在 widget 区域误处理
        if (this.pressed) {
            this.pressed = false;
            ClientPlayNetworking.send(new ModuleIventoryPayload(slotIndex(), event.button(), event.hasShiftDown() ? 1 : 0));
            return true;
        }
        return true;
    }
}
