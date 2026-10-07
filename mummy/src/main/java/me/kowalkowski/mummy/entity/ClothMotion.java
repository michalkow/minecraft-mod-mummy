package me.kowalkowski.mummy.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client-side, smoothed motion that loose cloth (bandages, capes) reacts to. Eased towards the
 * entity's current turning and falling every tick, so the cloth trails behind the movement.
 */
public final class ClothMotion {
	private float sway;
	private float swayO;
	private float lift;
	private float liftO;

	public void tick(final LivingEntity entity) {
		float turn = Mth.wrapDegrees(entity.yBodyRot - entity.yBodyRotO);
		float fall = (float) (entity.getY() - entity.yo);
		this.swayO = this.sway;
		this.liftO = this.lift;
		this.sway += (Mth.clamp(turn * 0.04F, -0.7F, 0.7F) - this.sway) * 0.2F;
		this.lift += (Mth.clamp(-fall * 2.5F, 0.0F, 1.2F) - this.lift) * 0.25F;
	}

	/** Sideways swing from body turning, in radians. */
	public float sway(final float partialTicks) {
		return Mth.lerp(partialTicks, this.swayO, this.sway);
	}

	/** Upward lift from falling, in radians. */
	public float lift(final float partialTicks) {
		return Mth.lerp(partialTicks, this.liftO, this.lift);
	}

	/** Implemented by entities that wear animated cloth. */
	public interface Wearer {
		ClothMotion cloth();
	}
}
