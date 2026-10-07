package me.kowalkowski.mummy.entity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import it.unimi.dsi.fastutil.ints.IntList;
import me.kowalkowski.mummy.entity.ai.FollowRoyalPartnerGoal;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Zombie royalty: zombie stats and AI, but it doesn't burn in sunlight and doesn't turn into a
 * drowned (it would lose its crown). Its reinforcements are ordinary zombies, see ZombieMixin.
 *
 * <p>A king and a princess who meet become partners for life: they follow each other around like a
 * dog follows its owner, and every time they come together they launch a burst of fireworks.
 */
public abstract class RoyalZombie extends Zombie implements ClothMotion.Wearer {
	/** Tag on celebration rockets; FireworkRocketEntityMixin lets them hurt players only. */
	public static final String CELEBRATION_ROCKET_TAG = "mummy_royal_celebration";
	/** How far a royal notices a possible partner. */
	public static final double MEETING_DISTANCE = 16.0;
	/** Closer than this counts as "together". */
	public static final double TOGETHER_DISTANCE = 3.0;
	/** Further than this counts as "apart": the next reunion is celebrated again. */
	private static final double APART_DISTANCE = 10.0;
	private static final int CELEBRATION_COOLDOWN = 600;
	private static final List<IntList> PALETTES = List.of(
		IntList.of(0xFF5AA0, 0xFFD24A),
		IntList.of(0xFFFFFF, 0xFF8CC6),
		IntList.of(0xE8304A, 0xFFD24A),
		IntList.of(0x7FD7FF, 0xFFFFFF),
		IntList.of(0xB06BFF, 0xFF5AA0)
	);

	private final ClothMotion cloth = new ClothMotion();
	private @Nullable UUID partnerId;
	private boolean apart = true;
	private int celebrationCooldown;

	protected RoyalZombie(final EntityType<? extends RoyalZombie> type, final Level level) {
		super(type, level);
	}

	/** The kind of royal this one pairs up with. */
	protected abstract EntityType<? extends RoyalZombie> partnerType();

	@Override
	protected void addBehaviourGoals() {
		super.addBehaviourGoals();
		// Below attacking (3), above wandering about (6, 7): like a dog, fights first, then comes back.
		this.goalSelector.addGoal(5, new FollowRoyalPartnerGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.cloth.tick(this);
		} else if (this.level() instanceof ServerLevel level) {
			this.tickPartnership(level);
		}
	}

	@Override
	public ClothMotion cloth() {
		return this.cloth;
	}

	/** The partner, if bonded and loaded nearby in this world; finds and bonds with a new one otherwise. */
	public @Nullable RoyalZombie partner(final ServerLevel level) {
		if (this.partnerId != null) {
			return level.getEntity(this.partnerId) instanceof RoyalZombie partner && partner.isAlive() ? partner : null;
		}

		RoyalZombie candidate = level.getEntitiesOfClass(
				RoyalZombie.class,
				this.getBoundingBox().inflate(MEETING_DISTANCE),
				royal -> royal.getType() == this.partnerType() && royal.isAlive() && (royal.partnerId == null || this.getUUID().equals(royal.partnerId))
			)
			.stream()
			.min(Comparator.comparingDouble(this::distanceToSqr))
			.orElse(null);
		if (candidate != null) {
			this.bondWith(candidate);
		}
		return candidate;
	}

	private void bondWith(final RoyalZombie other) {
		this.partnerId = other.getUUID();
		other.partnerId = this.getUUID();
		this.apart = true;
		other.apart = true;
		this.setPersistenceRequired();
		other.setPersistenceRequired();
	}

	private void tickPartnership(final ServerLevel level) {
		if (this.celebrationCooldown > 0) {
			this.celebrationCooldown--;
		}
		if (this.tickCount % 10 != 0) {
			return;
		}
		RoyalZombie partner = this.partner(level);
		if (partner == null) {
			return;
		}
		double distance = this.distanceTo(partner);
		if (distance > APART_DISTANCE) {
			this.apart = true;
		} else if (distance < TOGETHER_DISTANCE && this.apart && this.celebrationCooldown == 0) {
			// Reunited (or met for the first time): both of them celebrate.
			this.apart = false;
			partner.apart = false;
			this.celebrationCooldown = CELEBRATION_COOLDOWN;
			partner.celebrationCooldown = CELEBRATION_COOLDOWN;
			this.celebrate(level);
			partner.celebrate(level);
		}
	}

	/** Spews a fan of fireworks into the sky, with hearts. The rockets only hurt players. */
	public void celebrate(final ServerLevel level) {
		for (int i = 0; i < 3; i++) {
			IntList colours = PALETTES.get(this.random.nextInt(PALETTES.size()));
			FireworkExplosion.Shape shape = FireworkExplosion.Shape.values()[this.random.nextInt(FireworkExplosion.Shape.values().length)];
			ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
			rocket.set(DataComponents.FIREWORKS, new Fireworks(1 + this.random.nextInt(2), List.of(
				new FireworkExplosion(shape, colours, IntList.of(0xFFFFFF), true, this.random.nextBoolean())
			)));
			FireworkRocketEntity entity = new FireworkRocketEntity(level, this, this.getX(), this.getEyeY(), this.getZ(), rocket);
			entity.setDeltaMovement(this.random.triangle(0.0, 0.08), 0.05, this.random.triangle(0.0, 0.08));
			entity.addTag(CELEBRATION_ROCKET_TAG);
			level.addFreshEntity(entity);
		}
		level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.2, this.getZ(), 5, 0.5, 0.3, 0.5, 0.0);
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("Partner", UUIDUtil.CODEC, this.partnerId);
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.partnerId = input.read("Partner", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(
		final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
	) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		this.setCanPickUpLoot(false); // a helmet would hide the crown
		return data;
	}

	/** No random armour or weapons: nothing should cover the crown. */
	@Override
	protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
	}

	@Override
	protected boolean considersEntityAsAlly(final Entity other) {
		return KingdomAllies.isKingdomMember(other) || super.considersEntityAsAlly(other);
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		return !(source.getEntity() != null && KingdomAllies.isKingdomMember(source.getEntity())) && super.hurtServer(level, source, damage);
	}

	@Override
	protected boolean isSunSensitive() {
		return false;
	}

	@Override
	protected boolean convertsInWater() {
		return false;
	}
}
