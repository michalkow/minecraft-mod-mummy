package me.kowalkowski.zombiekingdom.entity;

import me.kowalkowski.zombiekingdom.entity.ai.DefendRoyalsTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * A zombie in the royals' service (knights, archers). Hostile like any zombie, but first of all it
 * defends the king and princess: whatever hurts a royal nearby becomes its target. Guards are never
 * babies or chicken jockeys, don't burn in sunlight and don't turn into drowned.
 */
public abstract class RoyalGuard extends Zombie {
	protected RoyalGuard(final EntityType<? extends RoyalGuard> type, final Level level) {
		super(type, level);
	}

	@Override
	protected void addBehaviourGoals() {
		super.addBehaviourGoals();
		this.targetSelector.addGoal(1, new DefendRoyalsTargetGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(
		final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
	) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, new Zombie.ZombieGroupData(false, false));
		this.setCanPickUpLoot(false);
		return data;
	}

	/** Every guard gets its own fixed kit instead of the zombie's random weapon and armour. */
	@Override
	protected abstract void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty);

	/** Hands out the guard's kit, e.g. for a guard summoned with NBT (which skips the normal spawn setup). */
	public void equipKit(final ServerLevel level) {
		this.populateDefaultEquipmentSlots(this.random, level.getCurrentDifficultyAt(this.blockPosition()));
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
