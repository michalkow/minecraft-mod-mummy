package me.kowalkowski.verity.registry;

import me.kowalkowski.verity.VerityMod;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	private static final ResourceKey<Item> VERITY_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, VerityMod.id("verity_spawn_egg"));

	public static final Item VERITY_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		VERITY_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(ModEntities.VERITY).setId(VERITY_SPAWN_EGG_KEY))
	);

	private ModItems() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(output -> output.insertAfter(Items.ZOMBIE_SPAWN_EGG, VERITY_SPAWN_EGG));
	}
}
