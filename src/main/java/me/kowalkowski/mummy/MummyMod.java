package me.kowalkowski.mummy;

import me.kowalkowski.mummy.registry.ModEntities;
import me.kowalkowski.mummy.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MummyMod implements ModInitializer {
	public static final String MOD_ID = "mummy";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModItems.init();
		LOGGER.info("The mummies have awoken.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
