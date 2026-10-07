package me.kowalkowski.mummy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kowalkowski.mummy.MummyMod;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Full-bright glowing eyes, like the spider's and enderman's. Unlike the vanilla
 * EyesLayer it picks a texture per model, because the baby model has its own UV layout.
 */
public class MummyEyesLayer extends RenderLayer<ClothRenderState, ZombieModel<ClothRenderState>> {
	private static final RenderType EYES = RenderTypes.eyes(MummyMod.id("textures/entity/mummy/mummy_eyes.png"));
	private static final RenderType BABY_EYES = RenderTypes.eyes(MummyMod.id("textures/entity/mummy/mummy_baby_eyes.png"));

	public MummyEyesLayer(final RenderLayerParent<ClothRenderState, ZombieModel<ClothRenderState>> renderer) {
		super(renderer);
	}

	@Override
	public void submit(
		final PoseStack poseStack,
		final SubmitNodeCollector submitNodeCollector,
		final int lightCoords,
		final ClothRenderState state,
		final float yRot,
		final float xRot
	) {
		if (state.isInvisible) {
			return;
		}

		submitNodeCollector.order(1)
			.submitModel(
				this.getParentModel(), state, poseStack, state.isBaby ? BABY_EYES : EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor
			);
	}
}
