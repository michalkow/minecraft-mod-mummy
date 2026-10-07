package me.kowalkowski.mummy.entity;

import me.kowalkowski.mummy.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** A rare, crowned zombie in a flowing red cape, with a slightly deeper voice. Pairs up with a princess. */
public class ZombieKing extends RoyalZombie {
	public ZombieKing(final EntityType<? extends ZombieKing> type, final Level level) {
		super(type, level);
	}

	@Override
	protected EntityType<? extends RoyalZombie> partnerType() {
		return ModEntities.ZOMBIE_PRINCESS;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.8F;
	}
}
