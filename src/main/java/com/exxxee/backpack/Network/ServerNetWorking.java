package com.exxxee.backpack.Network;

import com.exxxee.backpack.Network.payload.CloseWindowPayload;
import com.exxxee.backpack.Network.payload.OpenWindowPayload;
import com.exxxee.backpack.Network.payload.ShelfContentPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ServerNetWorking {
	private ServerNetWorking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(OpenWindowPayload.TYPE, OpenWindowPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CloseWindowPayload.TYPE, CloseWindowPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ShelfContentPayload.TYPE, ShelfContentPayload.STREAM_CODEC);
	}
}
