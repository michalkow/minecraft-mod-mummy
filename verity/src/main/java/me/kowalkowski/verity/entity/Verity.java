package me.kowalkowski.verity.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * Verity: a sunshine-yellow zombie with a big smiley face. Zombie stats and AI; being made of
 * sunshine, it doesn't burn in daylight, and it stays yellow instead of turning into a drowned.
 */
public class Verity extends Zombie {
	public Verity(final EntityType<? extends Verity> type, final Level level) {
		super(type, level);
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
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.1F;
	}
}
