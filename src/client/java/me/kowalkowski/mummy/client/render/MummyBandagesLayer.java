package me.kowalkowski.mummy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Draws the animated loose bandages over the mummy, using the mummy's own texture. */
public class MummyBandagesLayer extends RenderLayer<MummyRenderState, ZombieModel<MummyRenderState>> {
	private final MummyBandagesModel model;
	private final MummyBandagesModel babyModel;

	public MummyBandagesLayer(final RenderLayerParent<MummyRenderState, ZombieModel<MummyRenderState>> renderer, final EntityModelSet modelSet) {
		super(renderer);
		this.model = new MummyBandagesModel(modelSet.bakeLayer(MummyBandagesModel.LAYER), false);
		this.babyModel = new MummyBandagesModel(modelSet.bakeLayer(MummyBandagesModel.BABY_LAYER), true);
	}

	@Override
	public void submit(
		final PoseStack poseStack,
		final SubmitNodeCollector submitNodeCollector,
		final int lightCoords,
		final MummyRenderState state,
		final float yRot,
		final float xRot
	) {
		coloredCutoutModelCopyLayerRender(
			state.isBaby ? this.babyModel : this.model,
			MummyRenderer.texture(state),
			poseStack,
			submitNodeCollector,
			lightCoords,
			state,
			-1,
			1
		);
	}
}
