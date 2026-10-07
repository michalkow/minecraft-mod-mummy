package me.kowalkowski.mummy.test;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.function.DoublePredicate;
import me.kowalkowski.mummy.MummyMod;
import me.kowalkowski.mummy.entity.RoyalGuard;
import me.kowalkowski.mummy.entity.RoyalZombie;
import me.kowalkowski.mummy.entity.Verity;
import me.kowalkowski.mummy.entity.ZombieArcher;
import me.kowalkowski.mummy.entity.ZombieKnight;
import me.kowalkowski.mummy.entity.ZombieKing;
import me.kowalkowski.mummy.entity.ZombiePrincess;
import me.kowalkowski.mummy.registry.ModEntities;
import me.kowalkowski.mummy.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("UnstableApiUsage")
public class MummyClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			server.runCommand("difficulty easy");
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				ServerPlayer player = singleplayer.getConnection().getServerPlayer();
				checkStatsMatchZombie(level, ModEntities.MUMMY);
				checkStatsMatchZombie(level, ModEntities.ZOMBIE_KING);
				checkStatsMatchZombie(level, ModEntities.ZOMBIE_PRINCESS);
				checkStatsMatchZombie(level, ModEntities.VERITY);
				checkStatsMatchZombie(level, ModEntities.ZOMBIE_KNIGHT);
				checkStatsMatchZombie(level, ModEntities.ZOMBIE_ARCHER);
				checkGuardKits(level);
				checkDesertSpawning(level);
				checkZombieVariants(level);
				checkTags();
				checkSpawnEgg(level, player, ModItems.MUMMY_SPAWN_EGG, ModEntities.MUMMY);
				checkSpawnEgg(level, player, ModItems.ZOMBIE_KING_SPAWN_EGG, ModEntities.ZOMBIE_KING);
				checkSpawnEgg(level, player, ModItems.ZOMBIE_PRINCESS_SPAWN_EGG, ModEntities.ZOMBIE_PRINCESS);
				checkSpawnEgg(level, player, ModItems.VERITY_SPAWN_EGG, ModEntities.VERITY);
				checkSpawnEgg(level, player, ModItems.ZOMBIE_KNIGHT_SPAWN_EGG, ModEntities.ZOMBIE_KNIGHT);
				checkSpawnEgg(level, player, ModItems.ZOMBIE_ARCHER_SPAWN_EGG, ModEntities.ZOMBIE_ARCHER);
			});

			server.runCommand("tp @e[type=!player] 0 -300 0"); // clear the stage, including generated animals
			context.waitTicks(5);
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			showcase(context, singleplayer, "mummy", ModEntities.MUMMY);
			showcase(context, singleplayer, "zombie_king", ModEntities.ZOMBIE_KING);
			showcase(context, singleplayer, "zombie_princess", ModEntities.ZOMBIE_PRINCESS);
			showcase(context, singleplayer, "verity", ModEntities.VERITY);
			showcase(context, singleplayer, "zombie_knight", ModEntities.ZOMBIE_KNIGHT);
			showcase(context, singleplayer, "zombie_archer", ModEntities.ZOMBIE_ARCHER);

			// The royal family in full sun, no roof: they must not catch fire.
			clear(context, server);
			server.runCommand("fill -8 -54 -4 8 -54 12 minecraft:air");
			server.runCommand("time set noon");
			server.runCommand("tp @a 0.5 -60 0.5 0 15");
			server.runCommand("summon mummy:zombie_king -1 -60 4 {NoAI:1b,Rotation:[190f,0f]}");
			server.runCommand("summon mummy:zombie_princess 1.5 -60 4 {NoAI:1b,Rotation:[170f,0f]}");
			context.waitTicks(100);
			server.runOnServer(minecraft -> check(
				minecraft.overworld().getEntities(EntityTypeTest.forClass(RoyalZombie.class), royal -> royal.isOnFire()).isEmpty(),
				"Zombie royalty must not burn in sunlight"
			));
			context.takeScreenshot("royals-in-the-sun");

			checkRoyalReunion(context, singleplayer);
			checkFireworksHurtPlayersOnly(context, singleplayer);
			checkKnightsDefendRoyals(context, singleplayer);
			checkMountedKnight(context, singleplayer);
			checkKingdom(context, singleplayer);

			// Night: the mummy's eyes glow.
			clear(context, server);
			server.runCommand("tp @a 0.5 -60 0.5 0 15");
			server.runCommand("summon mummy:mummy -0.6 -60 3.5 {NoAI:1b,Rotation:[180f,0f]}");
			server.runCommand("summon mummy:zombie_king 1.6 -60 3.5 {NoAI:1b,Rotation:[160f,0f]}");
			server.runCommand("time set midnight");
			context.waitTicks(20);
			context.takeScreenshot("mummy-night");
		}

		checkKingdomGeneratesNaturally(context);
	}

	/**
	 * In a normal (not flat) world, a kingdom must generate by itself: found the way /locate finds it,
	 * on flattened terrain, with its king present.
	 */
	private static void checkKingdomGeneratesNaturally(final ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder()
			.setUseConsistentSettings(false)
			.adjustSettings(settings -> {
				settings.setWorldType(settings.getNormalPresetList().getFirst());
				settings.setSeed("kingdom");
				settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			})
			.create()) {
			TestServerContext server = world.getServer();
			BlockPos found = server.computeOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				Holder<Structure> kingdom = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
					.getOrThrow(ResourceKey.create(Registries.STRUCTURE, MummyMod.id("kingdom")));
				Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
					.findNearestMapStructure(level, HolderSet.direct(kingdom), BlockPos.ZERO, 100, false);
				return result == null ? null : result.getFirst();
			});
			check(found != null, "No kingdom generated within 1600 blocks of spawn");
			System.out.println("Kingdom found at " + found);
			server.runCommand("gamemode spectator @a");
			server.runCommand("time set noon");
			server.runCommand("tp @a " + found.getX() + " 200 " + found.getZ());
			world.getConnection().waitForChunksRender();
			context.waitTicks(40);
			int ground = server.computeOnServer(minecraft -> minecraft.overworld().getHeight(Heightmap.Types.WORLD_SURFACE, found.getX(), found.getZ()));
			server.runCommand("tp @a " + found.getX() + " " + (ground + 30) + " " + (found.getZ() + 72) + " 180 25");
			world.getConnection().waitForChunksRender();
			context.waitTicks(60);
			context.takeScreenshot("kingdom-natural");
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				int kings = level.getEntities(ModEntities.ZOMBIE_KING, king -> king.blockPosition().closerThan(found.atY(king.getBlockY()), 60)).size();
				check(kings == 1, "The naturally generated kingdom should have its king, found " + kings);
			});
		}
	}

	/** Screenshots of a mob from the front (adult and baby), from behind, walking past and falling. */
	private static void showcase(final ClientGameTestContext context, final TestSingleplayerContext singleplayer, final String name, final EntityType<?> type) {
		TestServerContext server = singleplayer.getServer();
		String id = "mummy:" + name;
		clear(context, server);
		server.runCommand("time set noon");
		server.runCommand("tp @a 0.5 -60 0.5 0 15");
		server.runCommand("summon " + id + " -0.6 -60 3.5 {NoAI:1b,Rotation:[180f,0f]}");
		server.runCommand("summon " + id + " 1.4 -60 3.0 {NoAI:1b,IsBaby:1b,Rotation:[160f,0f]}");
		server.runCommand("item replace entity @a hotbar.0 with mummy:" + name + "_spawn_egg");
		server.runOnServer(minecraft -> minecraft.overworld().getEntities(EntityTypeTest.forClass(RoyalGuard.class), guard -> true)
			.forEach(guard -> guard.equipKit(minecraft.overworld())));
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

	/** Removes the mod's mobs without death smoke or drops: into the void, out of view. */
	private static void clear(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("tp @e[type=mummy:mummy] 0 -300 0");
		server.runCommand("tp @e[type=mummy:zombie_king] 0 -300 0");
		server.runCommand("tp @e[type=mummy:zombie_princess] 0 -300 0");
		server.runCommand("tp @e[type=mummy:verity] 0 -300 0");
		server.runCommand("tp @e[type=mummy:zombie_knight] 0 -300 0");
		server.runCommand("tp @e[type=mummy:zombie_archer] 0 -300 0");
		server.runCommand("tp @e[type=minecraft:zombie_horse] 0 -300 0");
		server.runCommand("kill @e[type=minecraft:firework_rocket]");
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

	private static void checkDesertSpawning(final ServerLevel level) {
		check(monsterSpawnsIn(level, Biomes.DESERT).contains(ModEntities.MUMMY), "Mummy is not in the desert monster spawn list");
		check(!monsterSpawnsIn(level, Biomes.PLAINS).contains(ModEntities.MUMMY), "Mummy should only spawn in deserts");
	}

	private static List<EntityType<?>> monsterSpawnsIn(final ServerLevel level, final net.minecraft.resources.ResourceKey<Biome> key) {
		Biome biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key).value();
		MobSpawnSettings settings = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		return settings.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream().<EntityType<?>>map(w -> w.value().type()).toList();
	}

	/**
	 * A king and a princess with their AI on, ten blocks apart: they bond, walk together and celebrate
	 * with (harmless) fireworks; split up, they find each other again.
	 */
	private static void checkRoyalReunion(final ClientGameTestContext context, final TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		clear(context, server);
		server.runCommand("gamemode creative @a"); // so they mind each other, not the player
		server.runCommand("time set noon");
		server.runCommand("tp @a 0.5 -60 -8 0 -12");
		server.runCommand("summon mummy:zombie_king -5 -60 8 {Rotation:[90f,0f]}");
		server.runCommand("summon mummy:zombie_princess 5 -60 8 {Rotation:[-90f,0f]}");
		server.waitFor(minecraft -> !celebrationRockets(minecraft.overworld()).isEmpty(), 400);
		context.waitTicks(14);
		context.takeScreenshot("royal-reunion");
		context.waitTicks(14);
		context.takeScreenshot("royal-fireworks");

		context.waitTicks(60); // rockets live at most ~45 ticks
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			check(celebrationRockets(level).isEmpty(), "Celebration rockets should be gone: " + celebrationRockets(level).stream()
				.map(r -> r.position() + " age " + r.tickCount).toList());
			ZombieKing king = onStage(level, ModEntities.ZOMBIE_KING);
			ZombiePrincess princess = onStage(level, ModEntities.ZOMBIE_PRINCESS);
			check(king.partner(level) == princess && princess.partner(level) == king, "The king and princess should have bonded");
			check(king.distanceTo(princess) < 6.0, "The royals should stay together, but are " + king.distanceTo(princess) + " apart");
			check(king.getHealth() == king.getMaxHealth() && princess.getHealth() == princess.getMaxHealth(), "Celebration fireworks must not hurt the royals");
		});

		// Split them up: they come back together like a pet and its owner.
		server.runCommand("tp @e[type=mummy:zombie_king] 35 -60 8");
		server.waitFor(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ZombieKing king = level.getEntities(ModEntities.ZOMBIE_KING, k -> k.getY() > -100).getFirst();
			return king.distanceTo(level.getEntities(ModEntities.ZOMBIE_PRINCESS, p -> p.getY() > -100).getFirst()) < 6.0;
		}, 300);
		server.runCommand("gamemode survival @a");
	}

	/** Knights wear full iron with a sword and shield; archers carry a bow. */
	private static void checkGuardKits(final ServerLevel level) {
		ZombieKnight knight = ModEntities.ZOMBIE_KNIGHT.create(level, EntitySpawnReason.COMMAND);
		knight.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), EntitySpawnReason.COMMAND, null);
		check(knight.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && knight.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
			&& knight.getItemBySlot(EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS) && knight.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS),
			"Knights should wear full iron armour");
		check(knight.getMainHandItem().is(Items.IRON_SWORD) && knight.getOffhandItem().is(Items.SHIELD), "Knights should carry a sword and a shield");
		check(!knight.isBaby(), "Knights are never babies");
		ZombieArcher archer = ModEntities.ZOMBIE_ARCHER.create(level, EntitySpawnReason.COMMAND);
		archer.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), EntitySpawnReason.COMMAND, null);
		check(archer.getMainHandItem().is(Items.BOW), "Archers should carry a bow");
	}

	/** A celebration rocket bursting between a player, a king and a knight hurts only the player. */
	private static void checkFireworksHurtPlayersOnly(final ClientGameTestContext context, final TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		clear(context, server);
		server.runCommand("gamemode survival @a");
		server.runCommand("effect clear @a");
		server.runCommand("tp @a 0.5 -60 0.5 0 0");
		server.runCommand("summon mummy:zombie_king 2.5 -60 0.5 {NoAI:1b}");
		server.runCommand("summon mummy:zombie_knight -1.5 -60 0.5 {NoAI:1b}");
		context.waitTicks(5);
		float before = server.computeOnServer(minecraft -> singleplayer.getConnection().getServerPlayer().getHealth());
		server.runCommand("summon minecraft:firework_rocket 0.5 -59 2.0 {LifeTime:1,Tags:[\"" + RoyalZombie.CELEBRATION_ROCKET_TAG
			+ "\"],FireworksItem:{id:\"minecraft:firework_rocket\",count:1,components:{\"minecraft:fireworks\":{explosions:[{shape:\"large_ball\",colors:[I;16733350]}]}}}}");
		context.waitTicks(10);
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			float after = singleplayer.getConnection().getServerPlayer().getHealth();
			check(after < before, "Celebration fireworks should hurt a player caught in the blast (" + before + " -> " + after + ")");
			List<? extends Zombie> nearby = level.getEntities(EntityTypeTest.forClass(Zombie.class), mob -> mob.blockPosition().closerThan(new BlockPos(0, -60, 0), 8));
			check(nearby.size() == 2, "Expected the king and the knight next to the blast, found " + nearby.size());
			for (Zombie mob : nearby) {
				check(mob.getHealth() == mob.getMaxHealth(), mob.getType() + " must not be hurt by celebration fireworks");
			}
		});
		server.runCommand("effect give @a minecraft:instant_health 1 10 true");
		server.runCommand("gamemode creative @a");
	}

	/** A knight from the spawn egg rides a saddled zombie horse in red armour, steers it at the player, and the horse doesn't burn. */
	private static void checkMountedKnight(final ClientGameTestContext context, final TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		clear(context, server);
		server.runCommand("tp @e[type=minecraft:zombie_horse] 0 -300 0");
		server.runCommand("time set noon");
		server.runCommand("gamemode survival @a");
		server.runCommand("effect give @a minecraft:resistance infinite 4 true");
		server.runCommand("tp @a 0.5 -60 0.5 0 5");
		context.waitTicks(5);
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ServerPlayer player = singleplayer.getConnection().getServerPlayer();
			ItemStack egg = new ItemStack(ModItems.ZOMBIE_KNIGHT_SPAWN_EGG);
			player.setItemInHand(InteractionHand.MAIN_HAND, egg);
			BlockPos ground = new BlockPos(0, -61, 14);
			egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
			ZombieKnight knight = onStage(level, ModEntities.ZOMBIE_KNIGHT);
			check(knight.getVehicle() instanceof ZombieHorse, "A knight from the spawn egg should ride a zombie horse");
			ZombieHorse horse = (ZombieHorse) knight.getVehicle();
			check(horse.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE), "The knight's horse should be saddled");
			check(horse.getItemBySlot(EquipmentSlot.BODY).is(Items.LEATHER_HORSE_ARMOR), "The knight's horse should wear the kingdom's armour");
		});
		context.waitTicks(10);
		context.takeScreenshot("zombie_knight-mounted");
		double start = server.computeOnServer(minecraft -> onStage(minecraft.overworld(), EntityTypes.ZOMBIE_HORSE).distanceTo(singleplayer.getConnection().getServerPlayer()));
		server.waitFor(minecraft -> onStage(minecraft.overworld(), EntityTypes.ZOMBIE_HORSE).distanceTo(singleplayer.getConnection().getServerPlayer()) < start - 6.0, 200);
		context.takeScreenshot("zombie_knight-charging");
		server.runOnServer(minecraft -> check(!onStage(minecraft.overworld(), EntityTypes.ZOMBIE_HORSE).isOnFire(), "An armoured zombie horse must not burn in the sun"));
		server.runCommand("gamemode creative @a");
		server.runCommand("effect clear @a");
		server.runCommand("effect give @a minecraft:instant_health 1 10 true");
	}

	/** Whatever hurts a royal becomes the knights' target. */
	private static void checkKnightsDefendRoyals(final ClientGameTestContext context, final TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		clear(context, server);
		server.runCommand("kill @e[type=minecraft:cow]");
		server.runCommand("gamemode creative @a");
		server.runCommand("summon mummy:zombie_king 0.5 -60 6.5 {NoAI:1b}");
		server.runCommand("summon mummy:zombie_knight 4.5 -60 6.5");
		server.runCommand("summon minecraft:cow -3.5 -60 6.5 {NoAI:1b}");
		context.waitTicks(5);
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ZombieKing king = onStage(level, ModEntities.ZOMBIE_KING);
			Cow cow = onStage(level, EntityTypes.COW);
			check(king.isAlliedTo(onStage(level, ModEntities.ZOMBIE_KNIGHT)), "Royals and knights should be allies");
			king.hurtServer(level, level.damageSources().mobAttack(cow), 1.0F);
		});
		server.waitFor(minecraft -> onStage(minecraft.overworld(), ModEntities.ZOMBIE_KNIGHT).getTarget() instanceof Cow, 100);
		server.runCommand("kill @e[type=minecraft:cow]");
	}

	/** Places a kingdom and checks who lives there; screenshots from the air, the gate and the throne room. */
	private static void checkKingdom(final ClientGameTestContext context, final TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		clear(context, server);
		server.runCommand("gamemode creative @a");
		server.runCommand("time set noon");
		server.runCommand("tp @a 400 -60 0");
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(20);
		server.runCommand("place structure mummy:kingdom 400 -60 0");
		context.waitTicks(10);
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			BlockPos center = new BlockPos(408, -60, 8); // the structure starts at the chunk's middle
			java.util.function.Function<EntityType<?>, Integer> count = type ->
				level.getEntities(EntityTypeTest.forClass(net.minecraft.world.entity.Entity.class), e -> e.getType() == type && e.blockPosition().closerThan(center, 60)).size();
			check(count.apply(ModEntities.ZOMBIE_KING) == 1, "A kingdom has exactly one king, found " + count.apply(ModEntities.ZOMBIE_KING));
			check(count.apply(ModEntities.ZOMBIE_PRINCESS) == 1, "A kingdom has exactly one princess, found " + count.apply(ModEntities.ZOMBIE_PRINCESS));
			check(count.apply(ModEntities.ZOMBIE_KNIGHT) >= 10, "A kingdom should have lots of knights, found " + count.apply(ModEntities.ZOMBIE_KNIGHT));
			check(count.apply(ModEntities.ZOMBIE_ARCHER) >= 6, "A kingdom should have archers, found " + count.apply(ModEntities.ZOMBIE_ARCHER));
			List<? extends ZombieHorse> horses = level.getEntities(EntityTypes.ZOMBIE_HORSE, h -> h.blockPosition().closerThan(center, 60));
			long ridden = horses.stream().filter(h -> h.getFirstPassenger() instanceof ZombieKnight).count();
			long stabled = horses.stream().filter(h -> !h.isVehicle() && h.getX() > center.getX() + 22 && h.getX() < center.getX() + 30).count();
			check(ridden == 5, "Five knights should patrol on zombie horses, found " + ridden);
			check(stabled == 4, "Four zombie horses should wait in the stable, found " + stabled);
			check(level.getBlockState(center.offset(23, 0, 3)).is(Blocks.OAK_FENCE_GATE), "The stable stalls should have gates");
			check(level.getBlockState(center.offset(-11, 5, 0)).is(Blocks.COBBLESTONE) || level.getBlockState(center.offset(-11, 5, 0)).is(Blocks.MOSSY_COBBLESTONE),
				"The castle should be cobblestone");
			check(level.getBlockState(center.offset(0, 4, -10)).is(Blocks.WALL_BANNER.pick(DyeColor.RED)), "The throne room should have red banners");
			check(level.getBlockState(center.offset(-36, 3, 0)).is(Blocks.COBBLESTONE) || level.getBlockState(center.offset(-36, 3, 0)).is(Blocks.MOSSY_COBBLESTONE),
				"The kingdom should be walled");
		});

		// The tour: from the air, at the gate, among the houses, in the courtyard, in the throne room.
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 408 -18 76 180 32");
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(40);
		context.takeScreenshot("kingdom-aerial");
		server.runCommand("tp @a 408 -57 66 180 0");
		context.waitTicks(30);
		context.takeScreenshot("kingdom-gate");
		server.runCommand("tp @a 384 -58.5 38 -141 -12");
		context.waitTicks(30);
		context.takeScreenshot("kingdom-courtyard");
		server.runCommand("tp @a 425 -57.5 14 -90 12");
		context.waitTicks(30);
		context.takeScreenshot("kingdom-stable");
		server.runCommand("tp @a 408 -59 13 180 5");
		context.waitTicks(30);
		context.takeScreenshot("kingdom-throne-room");
		server.runCommand("gamemode creative @a");
		server.runCommand("tp @a 0.5 -60 0.5");
	}

	/** The one mob of a type standing on the test stage (mobs cleared from earlier scenes are falling through the void). */
	private static <T extends net.minecraft.world.entity.Entity> T onStage(final ServerLevel level, final EntityType<T> type) {
		List<? extends T> found = level.getEntities(type, e -> e.getY() > -100 && e.blockPosition().closerThan(new BlockPos(0, -60, 0), 48));
		check(found.size() == 1, "Expected one " + type + " on the stage, found " + found.size());
		return found.getFirst();
	}

	private static List<? extends FireworkRocketEntity> celebrationRockets(final ServerLevel level) {
		return level.getEntities(EntityTypes.FIREWORK_ROCKET, rocket -> rocket.entityTags().contains(RoyalZombie.CELEBRATION_ROCKET_TAG)
			// only the stage: royals from earlier scenes (in the void) or spawned naturally nearby celebrate too
			&& rocket.getY() > -100 && Math.abs(rocket.getX()) < 24 && Math.abs(rocket.getZ() - 8) < 24);
	}

	/** The spawner hook turns 1% of zombies each into a king, a princess and Verity, and leaves everything else alone. */
	private static void checkZombieVariants(final ServerLevel level) {
		float king = ModEntities.ZOMBIE_KING_CHANCE;
		float princess = king + ModEntities.ZOMBIE_PRINCESS_CHANCE;
		float verity = princess + ModEntities.VERITY_CHANCE;
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.NATURAL);
		check(variant(level, zombie, seedWhereFirstRoll(r -> r < king)) instanceof ZombieKing, "A roll under 1% should make a Zombie King");
		check(variant(level, zombie, seedWhereFirstRoll(r -> r >= king && r < princess)) instanceof ZombiePrincess, "A roll of 1-2% should make a Zombie Princess");
		check(variant(level, zombie, seedWhereFirstRoll(r -> r >= princess && r < verity)) instanceof Verity, "A roll of 2-3% should make Verity");
		check(variant(level, zombie, seedWhereFirstRoll(r -> r >= verity)) == zombie, "Other rolls should stay a zombie");
		check(ModEntities.pickZombieVariant(level, EntityTypes.HUSK, husk, RandomSource.create(seedWhereFirstRoll(r -> r < king))) == husk, "Only plain zombies are replaced");

		// Roughly 1 in 100 each over many rolls.
		RandomSource random = RandomSource.create(42L);
		int kings = 0;
		int princesses = 0;
		int verities = 0;
		for (int i = 0; i < 20000; i++) {
			Mob mob = ModEntities.pickZombieVariant(level, EntityTypes.ZOMBIE, zombie, random);
			kings += mob instanceof ZombieKing ? 1 : 0;
			princesses += mob instanceof ZombiePrincess ? 1 : 0;
			verities += mob instanceof Verity ? 1 : 0;
		}
		for (int count : new int[] {kings, princesses, verities}) {
			check(count > 140 && count < 260, "Expected about 200 of each variant in 20000 zombie spawns, got " + kings + "/" + princesses + "/" + verities);
		}
	}

	private static Mob variant(final ServerLevel level, final Zombie zombie, final long seed) {
		return ModEntities.pickZombieVariant(level, EntityTypes.ZOMBIE, zombie, RandomSource.create(seed));
	}

	private static long seedWhereFirstRoll(final DoublePredicate condition) {
		for (long seed = 0; ; seed++) {
			if (condition.test(RandomSource.create(seed).nextFloat())) {
				return seed;
			}
		}
	}

	private static void checkTags() {
		Holder<EntityType<?>> mummy = ModEntities.MUMMY.builtInRegistryHolder();
		check(mummy.is(EntityTypeTags.ZOMBIES), "Mummy should be tagged #minecraft:zombies");
		check(mummy.is(EntityTypeTags.UNDEAD), "Mummy should be undead (smite, healing/harming)");
		check(!mummy.is(EntityTypeTags.BURN_IN_DAYLIGHT), "Mummy must not burn in daylight");
		check(SpawnPlacements.getPlacementType(ModEntities.MUMMY) == SpawnPlacementTypes.ON_GROUND, "Mummy spawn placement not registered");

		for (EntityType<?> royal : List.of(ModEntities.ZOMBIE_KING, ModEntities.ZOMBIE_PRINCESS, ModEntities.VERITY, ModEntities.ZOMBIE_KNIGHT, ModEntities.ZOMBIE_ARCHER)) {
			check(royal.builtInRegistryHolder().is(EntityTypeTags.UNDEAD), royal + " should be undead");
			check(!royal.builtInRegistryHolder().is(EntityTypeTags.BURN_IN_DAYLIGHT), royal + " must not burn in daylight");
		}
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
