package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Inventory.slot.ShelfSlot;
import com.exxxee.backpack.Network.payload.ModuleMenuPayload;
import com.exxxee.backpack.WindowSync;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.api.WindowIdIssuer;
import com.exxxee.backpack.container.ModuleContainer;
import com.exxxee.backpack.container.ShelfContainer;
import com.exxxee.backpack.item.BackpackItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;

public class ShelfMenu extends AbstractBackpackMenu implements ContainerListener {

    public ShelfMenu(int windowId, Player player, Container container) {
        super(windowId, player);
        for (int i = 0; i < container.getContainerSize(); i++) {
            this.addSlot(new ShelfSlot(container, i, 4, 8 + i * 18));
        }
        if (container instanceof ShelfContainer shelf) {
            shelf.setMenu(this);
        }
        if (player instanceof ServerPlayer) {
            this.addSlotListener(this);
        }
    }

    public void release() {
        this.removeSlotListener(this);
        for (int i = 0; i < this.slots.size(); i++) {
            this.removeModule(i);
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack itemStack) {
        if (!(this.player instanceof ServerPlayer serverPlayer)) return;
        if (slotIndex < 0 || slotIndex >= this.slots.size()) return;
        ItemStack live = this.slots.get(slotIndex).getItem();
        if (live.is(BackpackItems.MODULE) && live.get(BackpackDataComponents.MODULE_INVENTORY) != null) {
            ModuleMenu existing = this.find(slotIndex);
            if (existing == null) {
                this.createModule(serverPlayer, slotIndex, live);
            } else {
                this.retarget(existing, live);
            }
            return;
        }
        this.removeModule(slotIndex);
    }

    @Override
    public void dataChanged(AbstractContainerMenu menu, int id, int value) {
    }

    private void createModule(ServerPlayer player, int shelfSlot, ItemStack stack) {
        int windowId = ((WindowIdIssuer) player).nextWindowId();
        IBackpackHost host = (IBackpackHost) player;
        ModuleContainer container = new ModuleContainer(stack);
        ModuleMenu menu = new ModuleMenu(windowId, player, container);
        menu.shelfSlot = shelfSlot;
        host.setBackpackContainer(windowId, container);
        host.setBackpackMenu(windowId, menu);
        this.sendModule(player, menu);
        menu.setSynchronizer(new ServerBackpackSynchronizer(player));
    }

    private void retarget(ModuleMenu menu, ItemStack stack) {
        if (menu.slots.isEmpty()) return;
        if (!(menu.slots.get(0).container instanceof ModuleContainer modules) || modules.stack() == stack) return;
        modules.setStack(stack);
        if (this.player instanceof ServerPlayer serverPlayer) {
            this.sendModule(serverPlayer, menu);
        }
    }

    private void removeModule(int shelfSlot) {
        ModuleMenu menu = this.find(shelfSlot);
        if (menu == null || !(this.player instanceof ServerPlayer serverPlayer)) return;
        WindowSync.close(serverPlayer, menu.containerId);
    }

    private void sendModule(ServerPlayer player, ModuleMenu menu) {
        ItemStack stack = ItemStack.EMPTY;
        if (!menu.slots.isEmpty() && menu.slots.get(0).container instanceof ModuleContainer modules) {
            stack = modules.stack();
        }
        ServerPlayNetworking.send(player, new ModuleMenuPayload(menu.containerId, menu.shelfSlot, stack));
    }

    private ModuleMenu find(int shelfSlot) {
        for (AbstractBackpackMenu menu : ((IBackpackHost) this.player).getBackpackMenus()) {
            if (menu instanceof ModuleMenu moduleMenu && moduleMenu.shelfSlot == shelfSlot) {
                return moduleMenu;
            }
        }
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) return ItemStack.EMPTY;
        if (!this.slots.get(slotIndex).getItem().is(BackpackItems.MODULE)) return ItemStack.EMPTY;
        return this.quickMoveToHost(player, slotIndex);
    }

}
