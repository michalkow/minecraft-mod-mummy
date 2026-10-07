package me.kowalkowski.zombiekingdom.registry;

import me.kowalkowski.zombiekingdom.ZombieKingdomMod;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	public static final Item ZOMBIE_KING_SPAWN_EGG = registerSpawnEgg("zombie_king_spawn_egg", ModEntities.ZOMBIE_KING);
	public static final Item ZOMBIE_PRINCESS_SPAWN_EGG = registerSpawnEgg("zombie_princess_spawn_egg", ModEntities.ZOMBIE_PRINCESS);
	public static final Item ZOMBIE_KNIGHT_SPAWN_EGG = registerSpawnEgg("zombie_knight_spawn_egg", ModEntities.ZOMBIE_KNIGHT);
	public static final Item ZOMBIE_ARCHER_SPAWN_EGG = registerSpawnEgg("zombie_archer_spawn_egg", ModEntities.ZOMBIE_ARCHER);

	private ModItems() {
	}

	private static Item registerSpawnEgg(final String name, final EntityType<?> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ZombieKingdomMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key)));
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.insertAfter(
			Items.ZOMBIE_SPAWN_EGG, ZOMBIE_KING_SPAWN_EGG, ZOMBIE_PRINCESS_SPAWN_EGG, ZOMBIE_KNIGHT_SPAWN_EGG, ZOMBIE_ARCHER_SPAWN_EGG
		));
	}
}
