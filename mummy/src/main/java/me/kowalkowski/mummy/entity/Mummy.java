package me.kowalkowski.mummy.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * A desert undead wrapped in ancient linen. Shares the zombie's stats and AI,
 * but – like the husk – does not burn in sunlight and does not drown into a drowned.
 */
public class Mummy extends Zombie implements ClothMotion.Wearer {
	private final ClothMotion cloth = new ClothMotion();

	public Mummy(final EntityType<? extends Mummy> type, final Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.cloth.tick(this);
		}
	}

	@Override
	public ClothMotion cloth() {
		return this.cloth;
	}

	@Override
	protected boolean isSunSensitive() {
		return false;
	}

	@Override
	protected boolean convertsInWater() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.HUSK_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.HUSK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.HUSK_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.HUSK_STEP;
	}

	@Override
	public float getVoicePitch() {
		// A touch deeper than a husk, so mummies are recognisable by ear.
		return super.getVoicePitch() * 0.85F;
	}
}
