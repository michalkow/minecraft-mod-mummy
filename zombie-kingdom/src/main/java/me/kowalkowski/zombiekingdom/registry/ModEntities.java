package me.kowalkowski.zombiekingdom.registry;

import me.kowalkowski.zombiekingdom.ZombieKingdomMod;
import me.kowalkowski.zombiekingdom.entity.ZombieArcher;
import me.kowalkowski.zombiekingdom.entity.ZombieKing;
import me.kowalkowski.zombiekingdom.entity.ZombieKnight;
import me.kowalkowski.zombiekingdom.entity.ZombiePrincess;
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
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	/** Chances that a naturally spawning zombie is replaced by a Zombie King or a Zombie Princess. */
	public static final float ZOMBIE_KING_CHANCE = 0.01F;
	public static final float ZOMBIE_PRINCESS_CHANCE = 0.01F;

	public static final EntityType<ZombieKing> ZOMBIE_KING = register("zombie_king", EntityType.Builder.of(ZombieKing::new, MobCategory.MONSTER));
	public static final EntityType<ZombiePrincess> ZOMBIE_PRINCESS = register(
		"zombie_princess", EntityType.Builder.of(ZombiePrincess::new, MobCategory.MONSTER)
	);
	public static final EntityType<ZombieKnight> ZOMBIE_KNIGHT = register("zombie_knight", EntityType.Builder.of(ZombieKnight::new, MobCategory.MONSTER));
	public static final EntityType<ZombieArcher> ZOMBIE_ARCHER = register("zombie_archer", EntityType.Builder.of(ZombieArcher::new, MobCategory.MONSTER));

	private ModEntities() {
	}

	/** Registers a zombie-shaped mob: same dimensions and tracking as the vanilla zombie. */
	private static <T extends Zombie> EntityType<T> register(final String name, final EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ZombieKingdomMod.id(name));
		return Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			key,
			builder.sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.075F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful().build(key)
		);
	}

	/**
	 * Called by the natural spawner for every mob it creates: one in a hundred plain zombies becomes a
	 * Zombie King and another one in a hundred a Zombie Princess. Only a mob that is still a plain
	 * zombie is replaced, so other mods' zombie variants keep theirs. Returns the mob to spawn.
	 */
	public static Mob crownZombie(final ServerLevel level, final Mob mob, final RandomSource random) {
		if (mob.getType() != EntityTypes.ZOMBIE) {
			return mob;
		}
		float roll = random.nextFloat();
		EntityType<? extends Zombie> royal = roll < ZOMBIE_KING_CHANCE ? ZOMBIE_KING
			: roll < ZOMBIE_KING_CHANCE + ZOMBIE_PRINCESS_CHANCE ? ZOMBIE_PRINCESS
			: null;
		Mob replacement = royal != null ? royal.create(level, EntitySpawnReason.NATURAL) : null;
		return replacement != null ? replacement : mob;
	}

	public static void init() {
		for (EntityType<? extends Zombie> type : java.util.List.of(ZOMBIE_KING, ZOMBIE_PRINCESS, ZOMBIE_KNIGHT, ZOMBIE_ARCHER)) {
			FabricDefaultAttributeRegistry.register(type, Zombie.createAttributes());
		}
		SpawnPlacements.register(ZOMBIE_KING, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(ZOMBIE_PRINCESS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(ZOMBIE_KNIGHT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(ZOMBIE_ARCHER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
	}
}
