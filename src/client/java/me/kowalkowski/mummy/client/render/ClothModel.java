package me.kowalkowski.mummy.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.util.Mth;

/**
 * Cloth (and other accessories) worn over the zombie model, animated like banner fabric.
 *
 * <p>The model has the zombie's skeleton (same part names and pivots) but no body cubes of its own,
 * so {@link #setupAnim} poses it exactly like the zombie model it is drawn over. Each
 * {@link ClothStrip} is a chain of segments: a wave travels down the chain, the first segment
 * cancels its parent's pitch so the cloth hangs with gravity (even from outstretched arms), and
 * walking, turning and falling push it back, sideways and up.
 */
public class ClothModel extends ZombieModel<ClothRenderState> {
	private record BakedStrip(ClothStrip strip, ModelPart parent, List<ModelPart> segments) {
	}

	private final List<BakedStrip> strips = new ArrayList<>();

	public ClothModel(final ModelPart root, final List<ClothStrip> strips) {
		super(root);
		for (ClothStrip strip : strips) {
			ModelPart parent = root.getChild(strip.parent());
			List<ModelPart> segments = new ArrayList<>();
			ModelPart current = parent;
			for (int i = 0; i < strip.segments(); i++) {
				current = current.getChild(strip.name() + "_" + i);
				segments.add(current);
			}
			this.strips.add(new BakedStrip(strip, parent, segments));
		}
	}

	/**
	 * @param extras adds static accessories (a crown...) to the skeleton parts, keyed by part name
	 */
	public static LayerDefinition createLayer(final boolean baby, final List<ClothStrip> strips, final Consumer<Map<String, PartDefinition>> extras) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		Map<String, PartDefinition> parts = new HashMap<>();
		// Same pivots as the vanilla adult zombie (HumanoidModel.createMesh) and BabyZombieModel.
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, baby ? 15.25F : 0.0F, 0.0F));
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		parts.put("head", head);
		parts.put("body", root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, baby ? 17.5F : 0.0F, 0.0F)));
		parts.put("right_arm", root.addOrReplaceChild("right_arm", CubeListBuilder.create(), baby ? PartPose.offset(-3.0F, 15.5F, 0.0F) : PartPose.offset(-5.0F, 2.0F, 0.0F)));
		parts.put("left_arm", root.addOrReplaceChild("left_arm", CubeListBuilder.create(), baby ? PartPose.offset(3.0F, 15.5F, 0.0F) : PartPose.offset(5.0F, 2.0F, 0.0F)));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create(), baby ? PartPose.offset(-1.0F, 20.0F, 0.0F) : PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create(), baby ? PartPose.offset(1.0F, 20.0F, 0.0F) : PartPose.offset(1.9F, 12.0F, 0.0F));
		extras.accept(parts);

		for (ClothStrip strip : strips) {
			PartDefinition current = parts.get(strip.parent());
			for (int i = 0; i < strip.segments(); i++) {
				CubeListBuilder cube = CubeListBuilder.create().texOffs(strip.u(), strip.v() + i * strip.textureRowsPerSegment());
				float half = strip.width() / 2.0F;
				// Flat planes have zero thickness: their front and back faces are culled from the wrong side, so they never fight.
				if (strip.sideways()) {
					cube.addBox(0.0F, 0.0F, -half, 0.0F, strip.segmentLength(), strip.width());
				} else {
					cube.addBox(-half, 0.0F, 0.0F, strip.width(), strip.segmentLength(), strip.thickness());
				}
				PartPose pose = i == 0
					? PartPose.offsetAndRotation(strip.x(), strip.y(), strip.z(), 0.0F, strip.yaw(), 0.0F)
					: PartPose.offset(0.0F, strip.segmentLength(), 0.0F);
				current = current.addOrReplaceChild(strip.name() + "_" + i, cube, pose);
			}
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(final ClothRenderState state) {
		super.setupAnim(state);
		float time = state.ageInTicks;
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float walkCycle = state.walkAnimationPos * 0.6662F;
		float push = Math.min(walk * 0.9F + state.clothLift, 1.1F);

		for (BakedStrip baked : this.strips) {
			ClothStrip strip = baked.strip();
			for (int i = 0; i < baked.segments().size(); i++) {
				ModelPart segment = baked.segments().get(i);
				float phase = strip.phase() + i * 1.15F;
				float growth = (1.0F + i * 0.35F) * strip.flutter(); // the loose end flaps more than the knot
				// A slow idle breeze (like a banner) plus a faster flap in step with the legs.
				float wave = (0.07F * Mth.sin(time * 0.11F - phase) + walk * 0.45F * Mth.sin(walkCycle - phase)) * growth;
				float sway = 0.05F * Mth.cos(time * 0.083F - phase) * growth - state.clothSway * (i == 0 ? 1.0F : 0.5F);

				float pitch = wave + strip.drag() * push * (i == 0 ? 1.0F : 0.25F);
				if (i == 0) {
					pitch -= baked.parent().xRot; // hang with gravity, whatever the parent's pitch
					if (strip.sideways()) {
						sway += strip.x() < 0.0F ? strip.flare() : -strip.flare(); // spread outwards, to the side
					} else if (strip.side() == ClothStrip.Side.BACK) {
						pitch = Math.max(pitch + 0.1F + strip.flare(), 0.15F);
						if (strip.clearsLegs()) {
							pitch = Math.max(pitch, Math.max(this.rightLeg.xRot, this.leftLeg.xRot) + 0.2F);
						}
					} else if (strip.side() == ClothStrip.Side.FRONT) {
						pitch = Math.min(pitch - 0.12F - strip.flare(), -0.15F);
						if (strip.clearsLegs()) {
							pitch = Math.min(pitch, Math.min(this.rightLeg.xRot, this.leftLeg.xRot) - 0.2F);
						}
					}
				}
				segment.xRot = pitch;
				segment.zRot = sway;
			}
		}
	}
}
