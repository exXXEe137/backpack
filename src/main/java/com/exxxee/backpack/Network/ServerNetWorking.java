package com.exxxee.backpack.Network;

import com.exxxee.backpack.Inventory.menu.AbstractBackpackMenu;
import com.exxxee.backpack.Network.payload.CloseWindowPayload;
import com.exxxee.backpack.Network.payload.ModuleMenuPayload;
import com.exxxee.backpack.Network.payload.OpenWindowPayload;
import com.exxxee.backpack.Network.payload.ServerboundWindowClickPayload;
import com.exxxee.backpack.Network.payload.WindowContentPayload;
import com.exxxee.backpack.Network.payload.WindowSlotPayload;
import com.exxxee.backpack.api.IBackpackHost;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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

		ServerPlayNetworking.registerGlobalReceiver(ServerboundWindowClickPayload.TYPE, ServerNetWorking::windowClicky);
	}

	public static void windowClicky (ServerboundWindowClickPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			AbstractBackpackMenu menu = ((IBackpackHost) context.player()).getBackpackMenu(payload.windowId());
			if (menu == null || menu.windowId != payload.windowId()) return;
			menu.clicked(payload.slotindex(), payload.buttonNum(), payload.containerInput(), context.player());
		});
	}

}
