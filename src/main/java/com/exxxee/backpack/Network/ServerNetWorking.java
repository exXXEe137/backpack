package com.exxxee.backpack.Network;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Inventory.menu.ModuleMenu;
import com.exxxee.backpack.Inventory.menu.ShelfMenu;
import com.exxxee.backpack.container.ShelfContainer;
import com.exxxee.backpack.Network.payload.CloseWindowPayload;
import com.exxxee.backpack.Network.payload.ModuleMenuPayload;
import com.exxxee.backpack.Network.payload.OpenWindowPayload;
import com.exxxee.backpack.Network.payload.ServerboundModuleInvokePayload;
import com.exxxee.backpack.Network.payload.ServerboundWindowClickPayload;
import com.exxxee.backpack.Network.payload.WindowContentPayload;
import com.exxxee.backpack.Network.payload.WindowSlotPayload;
import com.exxxee.backpack.api.IBackpackHost;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ServerNetWorking {
	private ServerNetWorking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(OpenWindowPayload.TYPE, OpenWindowPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CloseWindowPayload.TYPE, CloseWindowPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModuleMenuPayload.TYPE, ModuleMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(WindowContentPayload.TYPE, WindowContentPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(WindowSlotPayload.TYPE, WindowSlotPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ServerboundWindowClickPayload.TYPE, ServerboundWindowClickPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ServerboundModuleInvokePayload.TYPE, ServerboundModuleInvokePayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ServerboundWindowClickPayload.TYPE, ServerNetWorking::windowClicky);
		ServerPlayNetworking.registerGlobalReceiver(ServerboundModuleInvokePayload.TYPE, ServerNetWorking::moduleInvoke);
	}

	public static void windowClicky (ServerboundWindowClickPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			AbstractBackpackMenu menu = ((IBackpackHost) player).getBackpackMenu(payload.windowId());
			if (menu == null || menu.containerId != payload.windowId()) return;
			AbstractContainerMenu playerMenu = player.containerMenu;
			playerMenu.suppressRemoteUpdates();
			try {
				if (player.hasInfiniteMaterials() && playerMenu.getCarried().isEmpty()) {
					applyCreativeSlots(menu, payload.creativeSlots(), player);
					applyInventory(player, payload.creativeInventorySlots());
					if (payload.containerInput() == ContainerInput.QUICK_MOVE && playerMenu != player.inventoryMenu) {
						applyMenuSlots(playerMenu, payload.creativeMenuSlots(), player);
					}
					flushShelf(player);
					menu.broadcastChanges();
					return;
				}
				menu.clicked(payload.slotindex(), payload.buttonNum(), payload.containerInput(), player);
			} finally {
				playerMenu.resumeRemoteUpdates();
			}
			flushShelf(player);
			menu.broadcastChanges();
			playerMenu.broadcastChanges();
		});
	}

	private static void flushShelf(ServerPlayer player) {
		IBackpackHost host = (IBackpackHost) player;
		for (AbstractBackpackMenu menu : host.getBackpackMenus()) {
			if (menu instanceof ShelfMenu shelfMenu
					&& !shelfMenu.slots.isEmpty()
					&& shelfMenu.slots.get(0).container instanceof ShelfContainer shelf) {
				shelf.flush();
			}
		}
	}

	private static void moduleInvoke(ServerboundModuleInvokePayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			IBackpackHost host = (IBackpackHost) player;
			AbstractBackpackMenu menu = host.getBackpackMenu(payload.windowId());
			if (!(menu instanceof ModuleMenu moduleMenu)) return;
			if (!payload.invoked()) {
				moduleMenu.invoked = false;
				return;
			}
			for (AbstractBackpackMenu other : host.getBackpackMenus()) {
				if (other instanceof ModuleMenu otherMenu) {
					otherMenu.invoked = otherMenu == moduleMenu;
				}
			}
		});
	}

	private static void applyCreativeSlots(AbstractBackpackMenu menu, Int2ObjectMap<ItemStack> creativeSlots, ServerPlayer player) {
		boolean rejected = false;
		for (Int2ObjectMap.Entry<ItemStack> entry : creativeSlots.int2ObjectEntrySet()) {
			int index = entry.getIntKey();
			if (index < 0 || index >= menu.slots.size()) {
				rejected = true;
				continue;
			}
			ItemStack stack = entry.getValue();
			if (!stack.isEmpty() && (!stack.isItemEnabled(player.level().enabledFeatures()) || stack.getCount() > stack.getMaxStackSize())) {
				rejected = true;
				continue;
			}
			Slot slot = menu.slots.get(index);
			if (!stack.isEmpty() && !slot.mayPlace(stack)) {
				rejected = true;
				continue;
			}
			slot.setByPlayer(stack.copy());
		}
		if (rejected) {
			menu.sendAllDataToRemote();
		}
	}

	private static void applyInventory(ServerPlayer player, Int2ObjectMap<ItemStack> slots) {
		int size = player.getInventory().getContainerSize();
		for (Int2ObjectMap.Entry<ItemStack> entry : slots.int2ObjectEntrySet()) {
			int index = entry.getIntKey();
			if (index < 0 || index >= size) continue;
			ItemStack stack = entry.getValue();
			if (!stack.isEmpty() && (!stack.isItemEnabled(player.level().enabledFeatures()) || stack.getCount() > stack.getMaxStackSize())) {
				continue;
			}
			player.getInventory().setItem(index, stack.copy());
		}
	}

	private static void applyMenuSlots(AbstractContainerMenu menu, Int2ObjectMap<ItemStack> slots, ServerPlayer player) {
		for (Int2ObjectMap.Entry<ItemStack> entry : slots.int2ObjectEntrySet()) {
			int index = entry.getIntKey();
			if (index < 0 || index >= menu.slots.size()) continue;
			ItemStack stack = entry.getValue();
			if (!stack.isEmpty() && (!stack.isItemEnabled(player.level().enabledFeatures()) || stack.getCount() > stack.getMaxStackSize())) {
				continue;
			}
			Slot slot = menu.slots.get(index);
			if (!stack.isEmpty() && !slot.mayPlace(stack)) continue;
			slot.setByPlayer(stack.copy());
		}
	}

}
