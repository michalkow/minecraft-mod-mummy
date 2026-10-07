package me.kowalkowski.mummy.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.ZombieHorse;

/** The royals, their guards and the guards' zombie horses are one household: they never target or hurt each other. */
public final class KingdomAllies {
	private KingdomAllies() {
	}

	public static boolean isKingdomMember(final Entity entity) {
		return entity instanceof RoyalZombie || entity instanceof RoyalGuard || entity instanceof ZombieHorse;
	}
}
