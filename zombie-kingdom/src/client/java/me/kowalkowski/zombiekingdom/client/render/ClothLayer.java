package me.kowalkowski.zombiekingdom.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** Draws a {@link ClothModel} (adult or baby) over the zombie model. */
public class ClothLayer extends RenderLayer<ClothRenderState, ZombieModel<ClothRenderState>> {
	private final ClothModel model;
	private final ClothModel babyModel;
	private final Function<ClothRenderState, Identifier> texture;

	public ClothLayer(
		final RenderLayerParent<ClothRenderState, ZombieModel<ClothRenderState>> renderer,
		final ClothModel model,
		final ClothModel babyModel,
		final Function<ClothRenderState, Identifier> texture
	) {
		super(renderer);
		this.model = model;
		this.babyModel = babyModel;
		this.texture = texture;
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
		coloredCutoutModelCopyLayerRender(
			state.isBaby ? this.babyModel : this.model, this.texture.apply(state), poseStack, submitNodeCollector, lightCoords, state, -1, 1
		);
	}
}
