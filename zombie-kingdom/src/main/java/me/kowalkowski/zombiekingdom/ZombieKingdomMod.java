package me.kowalkowski.zombiekingdom;

import me.kowalkowski.zombiekingdom.registry.ModEntities;
import me.kowalkowski.zombiekingdom.registry.ModItems;
import me.kowalkowski.zombiekingdom.registry.ModStructures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZombieKingdomMod implements ModInitializer {
	public static final String MOD_ID = "zombie_kingdom";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModItems.init();
		ModStructures.init();
		LOGGER.info("Long live the Zombie King.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
