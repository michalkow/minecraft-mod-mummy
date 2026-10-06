package me.kowalkowski.mummy.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * A desert undead wrapped in ancient linen. Shares the zombie's stats and AI,
 * but – like the husk – does not burn in sunlight and does not drown into a drowned.
 */
public class Mummy extends Zombie {
	/** Client-only, smoothed sideways swing of the loose bandages when the body turns. */
	public float bandageSway;
	public float bandageSwayO;
	/** Client-only, smoothed upward lift of the loose bandages while falling. */
	public float bandageLift;
	public float bandageLiftO;

	public Mummy(final EntityType<? extends Mummy> type, final Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.tickBandages();
		}
	}

	/** Eases the bandages towards the current motion so they trail behind it like cloth. */
	private void tickBandages() {
		float turn = Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO);
		float fall = (float) (this.getY() - this.yo);
		this.bandageSwayO = this.bandageSway;
		this.bandageLiftO = this.bandageLift;
		this.bandageSway += (Mth.clamp(turn * 0.04F, -0.7F, 0.7F) - this.bandageSway) * 0.2F;
		this.bandageLift += (Mth.clamp(-fall * 2.5F, 0.0F, 1.2F) - this.bandageLift) * 0.25F;
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
