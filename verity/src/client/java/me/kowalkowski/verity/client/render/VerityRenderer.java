package me.kowalkowski.verity.client.render;

import me.kowalkowski.verity.VerityMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/** Verity: the vanilla zombie model (adult and baby), painted sunshine yellow with a big smiley face. */
public class VerityRenderer extends ZombieRenderer {
	private static final Identifier TEXTURE = VerityMod.id("textures/entity/verity/verity.png");
	private static final Identifier BABY_TEXTURE = VerityMod.id("textures/entity/verity/verity_baby.png");

	public VerityRenderer(final EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(final ZombieRenderState state) {
		return state.isBaby ? BABY_TEXTURE : TEXTURE;
	}
}
