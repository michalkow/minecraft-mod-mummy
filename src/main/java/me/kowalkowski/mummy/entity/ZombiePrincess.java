package me.kowalkowski.mummy.entity;

import me.kowalkowski.mummy.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** A rare zombie princess with a little crown and a swishing pink dress, with a slightly higher voice. Pairs up with a king. */
public class ZombiePrincess extends RoyalZombie {
	public ZombiePrincess(final EntityType<? extends ZombiePrincess> type, final Level level) {
		super(type, level);
	}

	@Override
	protected EntityType<? extends RoyalZombie> partnerType() {
		return ModEntities.ZOMBIE_KING;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.2F;
	}
}
