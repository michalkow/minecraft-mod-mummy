package me.kowalkowski.mummy.entity.ai;

import java.util.EnumSet;
import me.kowalkowski.mummy.entity.KingdomAllies;
import me.kowalkowski.mummy.entity.RoyalGuard;
import me.kowalkowski.mummy.entity.RoyalZombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;

/** A guard turns on anything that recently hurt a king or princess nearby, like an iron golem defending its village. */
public class DefendRoyalsTargetGoal extends TargetGoal {
	private static final double GUARD_RANGE = 24.0;
	private static final int MEMORY_TICKS = 200;
	private final TargetingConditions attackable = TargetingConditions.forCombat().range(GUARD_RANGE * 1.5);
	private @Nullable LivingEntity attacker;

	public DefendRoyalsTargetGoal(final RoyalGuard guard) {
		super(guard, false, true);
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		for (RoyalZombie royal : this.mob.level().getEntitiesOfClass(RoyalZombie.class, this.mob.getBoundingBox().inflate(GUARD_RANGE))) {
			LivingEntity culprit = royal.getLastHurtByMob();
			if (culprit != null
				&& culprit.isAlive()
				&& !KingdomAllies.isKingdomMember(culprit)
				&& royal.tickCount - royal.getLastHurtByMobTimestamp() < MEMORY_TICKS
				&& this.canAttack(culprit, this.attackable)) {
				this.attacker = culprit;
				return true;
			}
		}
		return false;
	}

	@Override
	public void start() {
		this.mob.setTarget(this.attacker);
		super.start();
	}
}
