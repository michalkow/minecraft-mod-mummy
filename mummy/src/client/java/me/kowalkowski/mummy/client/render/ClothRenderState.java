package me.kowalkowski.mummy.client.render;

import net.minecraft.client.renderer.entity.state.ZombieRenderState;

public class ClothRenderState extends ZombieRenderState {
	/** Sideways swing of loose cloth, from body turning (radians). */
	public float clothSway;
	/** Upward lift of loose cloth, from falling (radians). */
	public float clothLift;
}
