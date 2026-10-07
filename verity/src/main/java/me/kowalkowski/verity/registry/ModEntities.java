package me.kowalkowski.verity.registry;

import me.kowalkowski.verity.VerityMod;
import me.kowalkowski.verity.entity.Verity;
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
	/** Chance that a naturally spawning zombie is Verity instead. */
	public static final float VERITY_CHANCE = 0.01F;

	private static final ResourceKey<EntityType<?>> VERITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, VerityMod.id("verity"));

	// Same dimensions and tracking as the vanilla zombie.
	public static final EntityType<Verity> VERITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		VERITY_KEY,
		EntityType.Builder.of(Verity::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.74F)
			.passengerAttachments(2.075F)
			.ridingOffset(-0.7F)
			.clientTrackingRange(8)
			.notInPeaceful()
			.build(VERITY_KEY)
	);

	private ModEntities() {
	}

	/**
	 * Called by the natural spawner for every mob it creates: one in a hundred plain zombies becomes
	 * Verity. Only a mob that is still a plain zombie is replaced, so other mods' zombie variants keep
	 * theirs. Returns the mob to spawn.
	 */
	public static Mob smileyZombie(final ServerLevel level, final Mob mob, final RandomSource random) {
		if (mob.getType() != EntityTypes.ZOMBIE || random.nextFloat() >= VERITY_CHANCE) {
			return mob;
		}
		Verity verity = VERITY.create(level, EntitySpawnReason.NATURAL);
		return verity != null ? verity : mob;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(VERITY, Zombie.createAttributes());
		SpawnPlacements.register(VERITY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
	}
}
