package me.kowalkowski.mummy.client.render;

import net.minecraft.client.renderer.entity.state.ZombieRenderState;

public class MummyRenderState extends ZombieRenderState {
	/** Sideways swing of the loose bandages, from body turning (radians). */
	public float bandageSway;
	/** Upward lift of the loose bandages, from falling (radians). */
	public float bandageLift;
}
