package com.exxxee.backpack;

import com.exxxee.backpack.Datacomponent.BackpackDataComponents;
import com.exxxee.backpack.Network.ServerNetWorking;
import com.exxxee.backpack.item.ModCreativeModeTabs;
import com.exxxee.backpack.item.BackpackItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExxxeeBackpack implements ModInitializer {
	public static final String MOD_ID = "createbackpack-fly-mod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BackpackItems.register();
		ModCreativeModeTabs.register();
		BackpackDataComponents.register();
		ServerNetWorking.init();

		LOGGER.info("Hello Fabric world!");
	}
}
