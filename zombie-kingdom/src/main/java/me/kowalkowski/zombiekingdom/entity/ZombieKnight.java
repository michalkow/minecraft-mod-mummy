package me.kowalkowski.zombiekingdom.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import org.jspecify.annotations.Nullable;

/**
 * A royal guard in full iron armour with an iron sword and a red shield. When struck it sometimes
 * raises its shield, blocking the next blows until it strikes back. Knights ride zombie horses: the
 * kingdom's patrols are mounted, and so is every knight hatched from a spawn egg.
 */
public class ZombieKnight extends RoyalGuard {
	private static final float BLOCK_CHANCE = 0.4F;
	private static final int BLOCK_TICKS = 40;
	private int blockTicks;

	public ZombieKnight(final EntityType<? extends ZombieKnight> type, final Level level) {
		super(type, level);
	}

	@Override
	protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
		this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		this.setItemSlot(EquipmentSlot.OFFHAND, this.kingdomShield());
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			this.setDropChance(slot, 0.05F);
		}
	}

	/** A red shield with a gold border: the kingdom's colours. */
	private ItemStack kingdomShield() {
		ItemStack shield = new ItemStack(Items.SHIELD);
		shield.set(DataComponents.BASE_COLOR, DyeColor.RED);
		this.registryAccess().lookup(Registries.BANNER_PATTERN)
			.flatMap(patterns -> patterns.get(BannerPatterns.BORDER))
			.ifPresent(border -> shield.set(
				DataComponents.BANNER_PATTERNS, new BannerPatternLayers.Builder().add(border, DyeColor.YELLOW).build()
			));
		return shield;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(
		final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
	) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		if (spawnReason == EntitySpawnReason.SPAWN_ITEM_USE) {
			this.mountHorse(level, false);
		}
		return data;
	}

	/**
	 * Puts the knight on a saddled zombie horse in red leather armour (the kingdom's colours; horse
	 * armour also keeps a zombie horse from burning in the sun) and adds the horse to the level.
	 * The knight steers it; if the knight falls, the horse can be tamed.
	 */
	public void mountHorse(final ServerLevelAccessor level, final boolean persistent) {
		ZombieHorse horse = EntityTypes.ZOMBIE_HORSE.create(level.getLevel(), EntitySpawnReason.JOCKEY);
		if (horse == null) {
			return;
		}
		horse.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
		horse.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()), EntitySpawnReason.JOCKEY, null);
		horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
		horse.setItemSlot(EquipmentSlot.BODY, kingdomHorseArmour());
		if (persistent) {
			horse.setPersistenceRequired();
		}
		this.startRiding(horse, true, false);
		level.addFreshEntity(horse);
	}

	/** Red leather horse armour: the kingdom's colours, and sun protection for zombie horses. */
	public static ItemStack kingdomHorseArmour() {
		ItemStack armour = new ItemStack(Items.LEATHER_HORSE_ARMOR);
		armour.set(DataComponents.DYED_COLOR, new DyedItemColor(DyeColor.RED.getTextureDiffuseColor()));
		return armour;
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && !this.isBlocking() && source.getEntity() != null && this.getOffhandItem().is(Items.SHIELD) && this.random.nextFloat() < BLOCK_CHANCE) {
			this.startUsingItem(InteractionHand.OFF_HAND);
			this.blockTicks = BLOCK_TICKS;
		}
		return hurt;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.blockTicks > 0 && --this.blockTicks == 0) {
			this.stopUsingItem();
		}
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		if (this.isUsingItem()) {
			this.stopUsingItem(); // lower the shield to strike
			this.blockTicks = 0;
		}
		return super.doHurtTarget(level, target);
	}
}
