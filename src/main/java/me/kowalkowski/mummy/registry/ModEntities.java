package me.kowalkowski.mummy.registry;

import me.kowalkowski.mummy.MummyMod;
import me.kowalkowski.mummy.entity.Mummy;
import me.kowalkowski.mummy.entity.Verity;
import me.kowalkowski.mummy.entity.ZombieArcher;
import me.kowalkowski.mummy.entity.ZombieKnight;
import me.kowalkowski.mummy.entity.ZombieKing;
import me.kowalkowski.mummy.entity.ZombiePrincess;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
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

	/** Chances that a naturally spawning zombie is replaced by a Zombie King, a Zombie Princess or Verity. */
	public static final float ZOMBIE_KING_CHANCE = 0.01F;
	public static final float ZOMBIE_PRINCESS_CHANCE = 0.01F;
	public static final float VERITY_CHANCE = 0.01F;

	public static final EntityType<Mummy> MUMMY = register("mummy", EntityType.Builder.of(Mummy::new, MobCategory.MONSTER));
	public static final EntityType<ZombieKing> ZOMBIE_KING = register("zombie_king", EntityType.Builder.of(ZombieKing::new, MobCategory.MONSTER));
	public static final EntityType<ZombiePrincess> ZOMBIE_PRINCESS = register(
		"zombie_princess", EntityType.Builder.of(ZombiePrincess::new, MobCategory.MONSTER)
	);
	public static final EntityType<Verity> VERITY = register("verity", EntityType.Builder.of(Verity::new, MobCategory.MONSTER));
	public static final EntityType<ZombieKnight> ZOMBIE_KNIGHT = register("zombie_knight", EntityType.Builder.of(ZombieKnight::new, MobCategory.MONSTER));
	public static final EntityType<ZombieArcher> ZOMBIE_ARCHER = register("zombie_archer", EntityType.Builder.of(ZombieArcher::new, MobCategory.MONSTER));

	private ModEntities() {
	}

	/** Registers a zombie-shaped mob: same dimensions and tracking as the vanilla zombie. */
	private static <T extends Zombie> EntityType<T> register(final String name, final EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MummyMod.id(name));
		return Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			key,
			builder.sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.075F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful().build(key)
		);
	}

	/**
	 * Called by the natural spawner for every mob it creates. One in a hundred plain zombies becomes a
	 * Zombie King, another one in a hundred a Zombie Princess and another one in a hundred Verity.
	 * Returns the mob to spawn instead, or the original one.
	 */
	public static Mob pickZombieVariant(final ServerLevel level, final EntityType<?> type, final Mob mob, final RandomSource random) {
		if (type != EntityTypes.ZOMBIE) {
			return mob;
		}
		float roll = random.nextFloat();
		EntityType<? extends Zombie> variant = roll < ZOMBIE_KING_CHANCE ? ZOMBIE_KING
			: (roll -= ZOMBIE_KING_CHANCE) < ZOMBIE_PRINCESS_CHANCE ? ZOMBIE_PRINCESS
			: (roll -= ZOMBIE_PRINCESS_CHANCE) < VERITY_CHANCE ? VERITY
			: null;
		Mob replacement = variant != null ? variant.create(level, EntitySpawnReason.NATURAL) : null;
		return replacement != null ? replacement : mob;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(MUMMY, Zombie.createAttributes());
		FabricDefaultAttributeRegistry.register(ZOMBIE_KING, Zombie.createAttributes());
		FabricDefaultAttributeRegistry.register(ZOMBIE_PRINCESS, Zombie.createAttributes());
		FabricDefaultAttributeRegistry.register(VERITY, Zombie.createAttributes());
		FabricDefaultAttributeRegistry.register(ZOMBIE_KNIGHT, Zombie.createAttributes());
		FabricDefaultAttributeRegistry.register(ZOMBIE_ARCHER, Zombie.createAttributes());

		SpawnPlacements.register(
			MUMMY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkSurfaceMonstersSpawnRules
		);

		SpawnPlacements.register(
			ZOMBIE_KING, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules
		);
		SpawnPlacements.register(
			ZOMBIE_PRINCESS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules
		);
		SpawnPlacements.register(VERITY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(ZOMBIE_KNIGHT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(ZOMBIE_ARCHER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(Biomes.DESERT), MobCategory.MONSTER, MUMMY, SPAWN_WEIGHT, MIN_GROUP_SIZE, MAX_GROUP_SIZE
		);
	}
}
