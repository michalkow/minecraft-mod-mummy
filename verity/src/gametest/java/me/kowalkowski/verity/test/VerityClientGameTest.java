package me.kowalkowski.verity.test;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.function.DoublePredicate;
import me.kowalkowski.verity.VerityMod;
import me.kowalkowski.verity.entity.Verity;
import me.kowalkowski.verity.registry.ModEntities;
import me.kowalkowski.verity.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("UnstableApiUsage")
public class VerityClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			server.runCommand("difficulty easy");
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				checkStatsMatchZombie(level, ModEntities.VERITY);
				checkZombieVariant(level);
				checkTags();
				checkSpawnEgg(level, singleplayer.getConnection().getServerPlayer(), ModItems.VERITY_SPAWN_EGG, ModEntities.VERITY);
			});

			server.runCommand("tp @e[type=!player] 0 -300 0"); // clear the stage, including generated animals
			context.waitTicks(5);
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			showcase(context, singleplayer, "verity", ModEntities.VERITY);
		}
	}

	private static void showcase(final ClientGameTestContext context, final TestSingleplayerContext singleplayer, final String name, final EntityType<?> type) {
		TestServerContext server = singleplayer.getServer();
		String id = "verity:" + name;
		clear(context, server);
		server.runCommand("time set noon");
		server.runCommand("tp @a 0.5 -60 0.5 0 15");
		server.runCommand("summon " + id + " -0.6 -60 3.5 {NoAI:1b,Rotation:[180f,0f]}");
		server.runCommand("summon " + id + " 1.4 -60 3.0 {NoAI:1b,IsBaby:1b,Rotation:[160f,0f]}");
		server.runCommand("item replace entity @a hotbar.0 with verity:" + name + "_spawn_egg");
		singleplayer.getConnection().waitForClientboundEntityUpdates(type);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(20);
		context.takeScreenshot(name + "-front");

		server.runCommand("tp @a 0.5 -60 7.5 180 15");
		context.waitTicks(10);
		context.takeScreenshot(name + "-back");

		// Walking sideways past the camera: the cloth should trail behind.
		clear(context, server);
		server.runCommand("tp @a 0.5 -60 0.5 0 10");
		server.runCommand("summon " + id + " 3.5 -60 5 {NoAI:1b,Tags:[\"walker\"],Rotation:[90f,0f]}");
		for (int tick = 0; tick < 24; tick++) {
			server.runCommand("execute as @e[tag=walker] at @s run tp @s ~-0.15 ~ ~ 90 0");
			context.waitTick();
		}
		context.takeScreenshot(name + "-walking");

		// Falling: the cloth lifts.
		clear(context, server);
		server.runCommand("tp @a 0.5 -60 0.5 0 -15");
		server.runCommand("summon " + id + " 0.5 -55 6 {Rotation:[180f,0f]}");
		context.waitTicks(6);
		context.takeScreenshot(name + "-falling");
	}

	/** Removes this mod's mobs without death smoke or drops: into the void, out of view. */
	private static void clear(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("tp @e[type=verity:verity] 0 -300 0");
		server.runCommand("kill @e[type=item]");
		context.waitTicks(5);
	}

	private static void checkStatsMatchZombie(final ServerLevel level, final EntityType<? extends Zombie> type) {
		Zombie mob = type.create(level, EntitySpawnReason.COMMAND);
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		for (Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute : List.of(
			Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.ARMOR, Attributes.FOLLOW_RANGE
		)) {
			double expected = zombie.getAttributeBaseValue(attribute);
			double actual = mob.getAttributeBaseValue(attribute);
			check(expected == actual, type + " " + attribute.getRegisteredName() + " is " + actual + ", zombie has " + expected);
		}
		check(mob.getBbWidth() == zombie.getBbWidth() && mob.getBbHeight() == zombie.getBbHeight(), type + " size differs from zombie");
	}

	/** The spawner hook turns 1% of plain zombies into Verity, and leaves everything else alone. */
	private static void checkZombieVariant(final ServerLevel level) {
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.NATURAL);
		long lucky = seedWhereFirstRoll(r -> r < ModEntities.VERITY_CHANCE);
		check(ModEntities.smileyZombie(level, zombie, RandomSource.create(lucky)) instanceof Verity, "A roll under 1% should make Verity");
		check(ModEntities.smileyZombie(level, zombie, RandomSource.create(seedWhereFirstRoll(r -> r >= ModEntities.VERITY_CHANCE))) == zombie, "Other rolls should stay a zombie");
		check(ModEntities.smileyZombie(level, husk, RandomSource.create(lucky)) == husk, "Only plain zombies are replaced");

		RandomSource random = RandomSource.create(42L);
		int verities = 0;
		for (int i = 0; i < 20000; i++) {
			verities += ModEntities.smileyZombie(level, zombie, random) instanceof Verity ? 1 : 0;
		}
		check(verities > 140 && verities < 260, "Expected about 200 Verities in 20000 zombie spawns, got " + verities);
	}

	private static long seedWhereFirstRoll(final DoublePredicate condition) {
		for (long seed = 0; ; seed++) {
			if (condition.test(RandomSource.create(seed).nextFloat())) {
				return seed;
			}
		}
	}

	private static void checkTags() {
		Holder<EntityType<?>> verity = ModEntities.VERITY.builtInRegistryHolder();
		check(verity.is(EntityTypeTags.ZOMBIES) && verity.is(EntityTypeTags.UNDEAD), "Verity should be an undead zombie");
		check(!verity.is(EntityTypeTags.BURN_IN_DAYLIGHT), "Verity must not burn in daylight");
	}

	private static void checkSpawnEgg(final ServerLevel level, final ServerPlayer player, final Item item, final EntityType<?> type) {
		ItemStack egg = new ItemStack(item);
		check(egg.get(DataComponents.ENTITY_DATA) != null && egg.get(DataComponents.ENTITY_DATA).type() == type, item + " is not bound to " + type);

		player.setItemInHand(InteractionHand.MAIN_HAND, egg);
		BlockPos ground = BlockPos.containing(player.position()).below().offset(0, 0, 6);
		int before = level.getEntities(type, m -> true).size();
		InteractionResult result = egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
		int after = level.getEntities(type, m -> true).size();
		check(result.consumesAction() && after == before + 1, "Using " + item + " did not spawn " + type + " (" + result + ")");
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
