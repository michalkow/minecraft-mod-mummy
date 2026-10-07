package me.kowalkowski.zombiekingdom.entity.ai;

import java.util.EnumSet;
import me.kowalkowski.zombiekingdom.entity.RoyalZombie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/** Keeps a royal close to its partner, the way a tamed wolf follows its owner, teleporting over if left far behind. */
public class FollowRoyalPartnerGoal extends Goal {
	private static final double START_DISTANCE = 5.0;
	private static final double TELEPORT_DISTANCE = 20.0;
	private static final double SPEED = 1.1;

	private final RoyalZombie royal;
	private @Nullable RoyalZombie partner;
	private int timeToRecalcPath;

	public FollowRoyalPartnerGoal(final RoyalZombie royal) {
		this.royal = royal;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!(this.royal.level() instanceof ServerLevel level) || this.royal.getTarget() != null) {
			return false;
		}
		RoyalZombie partner = this.royal.partner(level);
		if (partner == null || this.royal.distanceTo(partner) < START_DISTANCE) {
			return false;
		}
		this.partner = partner;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.partner != null
			&& this.partner.isAlive()
			&& this.royal.getTarget() == null
			&& this.royal.distanceTo(this.partner) > RoyalZombie.TOGETHER_DISTANCE - 1.0;
	}

	@Override
	public void start() {
		this.timeToRecalcPath = 0;
	}

	@Override
	public void stop() {
		this.partner = null;
		this.royal.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.partner == null) {
			return;
		}
		this.royal.getLookControl().setLookAt(this.partner, 10.0F, this.royal.getMaxHeadXRot());
		if (--this.timeToRecalcPath > 0) {
			return;
		}
		this.timeToRecalcPath = this.adjustedTickDelay(10);
		if (this.royal.distanceTo(this.partner) >= TELEPORT_DISTANCE) {
			this.teleportToPartner();
		} else {
			this.royal.getNavigation().moveTo(this.partner, SPEED);
		}
	}

	/** Like a pet left behind: hop next to the partner, onto solid ground, without landing in a wall or water. */
	private void teleportToPartner() {
		for (int attempt = 0; attempt < 10; attempt++) {
			double x = this.partner.getX() + Mth.nextInt(this.royal.getRandom(), -3, 3);
			double z = this.partner.getZ() + Mth.nextInt(this.royal.getRandom(), -3, 3);
			if (Math.abs(x - this.partner.getX()) >= 2 || Math.abs(z - this.partner.getZ()) >= 2) {
				if (this.royal.randomTeleport(x, this.partner.getY() + 1.0, z, true, state -> false)) {
					return;
				}
			}
		}
	}
}
