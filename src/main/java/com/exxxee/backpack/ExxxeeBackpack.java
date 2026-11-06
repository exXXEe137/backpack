package com.exxxee.backpack;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Inventory.menu.WindowMenuType;
import com.exxxee.backpack.Network.ServerNetWorking;
import com.exxxee.backpack.item.ModCreativeModeTabs;
import com.exxxee.backpack.item.BackpackItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ExxxeeBackpack implements ModInitializer {
	public static final String MOD_ID = "exxxeebackpack";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BackpackItems.register();
		ModCreativeModeTabs.register();
		BackpackDataComponents.register();
		ServerNetWorking.register();
		Objects.requireNonNull(WindowMenuType.GENERIC_1x4);

		LOGGER.info("Hello Fabric world!");
	}
}
