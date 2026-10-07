package me.kowalkowski.mummy.registry;

import me.kowalkowski.mummy.MummyMod;
import me.kowalkowski.mummy.entity.Mummy;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	// Natural spawning in deserts. For reference, the husk uses weight 80 there.
	private static final int SPAWN_WEIGHT = 40;
	private static final int MIN_GROUP_SIZE = 1;
	private static final int MAX_GROUP_SIZE = 3;

	private static final ResourceKey<EntityType<?>> MUMMY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MummyMod.id("mummy"));

	// Same dimensions and tracking as the vanilla zombie.
	public static final EntityType<Mummy> MUMMY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MUMMY_KEY,
		EntityType.Builder.of(Mummy::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.74F)
			.passengerAttachments(2.075F)
			.ridingOffset(-0.7F)
			.clientTrackingRange(8)
			.notInPeaceful()
			.build(MUMMY_KEY)
	);

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(MUMMY, Zombie.createAttributes());

		SpawnPlacements.register(
			MUMMY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkSurfaceMonstersSpawnRules
		);

		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(Biomes.DESERT), MobCategory.MONSTER, MUMMY, SPAWN_WEIGHT, MIN_GROUP_SIZE, MAX_GROUP_SIZE
		);
	}
}
