package me.kowalkowski.mummy.registry;

import me.kowalkowski.mummy.MummyMod;
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
	private static final ResourceKey<Item> MUMMY_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, MummyMod.id("mummy_spawn_egg"));

	public static final Item MUMMY_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		MUMMY_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(ModEntities.MUMMY).setId(MUMMY_SPAWN_EGG_KEY))
	);

	private ModItems() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(output -> output.insertAfter(Items.HUSK_SPAWN_EGG, MUMMY_SPAWN_EGG));
	}
}
