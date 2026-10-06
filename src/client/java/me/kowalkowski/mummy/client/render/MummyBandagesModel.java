package me.kowalkowski.mummy.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.kowalkowski.mummy.MummyMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.util.Mth;

/**
 * Loose bandage strips that hang off the mummy and move like banner cloth.
 *
 * <p>The model has the zombie's skeleton (same part names and pivots) but no cubes of its own, so
 * {@link #setupAnim} poses it exactly like the zombie model it is drawn over. Each strip is a chain
 * of thin segments parented to the head, body or an arm. A wave travels down the chain, the first
 * segment cancels its parent's pitch so the strip hangs with gravity (even from outstretched arms),
 * and walking, turning and falling push the strips back, sideways and up.
 *
 * <p>The UV regions live in the free lower half of the mummy textures; tools/generate_textures.py
 * paints them from the same table.
 */
public class MummyBandagesModel extends ZombieModel<MummyRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(MummyMod.id("mummy"), "bandages");
	public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(MummyMod.id("mummy"), "bandages_baby");

	/** Which way a strip must not swing, so it never clips into the part it hangs from. */
	private enum Side {
		FREE, BACK, FRONT
	}

	/**
	 * @param sideways strip lies in the YZ plane (seen from the side) instead of the XY plane
	 * @param width strip width (depth for sideways strips)
	 * @param drag how strongly walking and falling push the strip backwards
	 * @param yaw turns the strip diagonally so its face shows from most angles
	 */
	private record Strip(
		String name, String parent, float x, float y, float z, int width, int segmentLength, int segments, int u, int v,
		boolean sideways, Side side, float drag, float phase, float yaw
	) {
		int textureRowsPerSegment() {
			return this.sideways ? this.width + this.segmentLength : this.segmentLength;
		}
	}

	private static final List<Strip> ADULT_STRIPS = List.of(
		new Strip("right_arm_bandage", "right_arm", -1.0F, 8.0F, 2.3F, 3, 3, 3, 0, 32, false, Side.FREE, 1.0F, 0.0F, 0.5F),
		new Strip("left_arm_bandage", "left_arm", 1.0F, 5.0F, 2.3F, 3, 4, 2, 6, 32, false, Side.FREE, 1.0F, 2.1F, -0.6F),
		new Strip("back_bandage", "body", 1.0F, 5.0F, 2.3F, 4, 4, 3, 12, 32, false, Side.BACK, 1.0F, 1.3F, 0.45F),
		new Strip("hip_bandage", "body", -4.3F, 9.0F, 0.0F, 2, 3, 2, 20, 32, true, Side.FREE, 0.8F, 3.4F, 0.4F),
		new Strip("head_bandage", "head", 1.5F, -5.0F, 4.8F, 3, 3, 3, 24, 32, false, Side.BACK, 1.0F, 0.7F, -0.5F),
		new Strip("front_bandage", "body", -2.0F, 5.0F, -2.3F, 3, 3, 2, 30, 32, false, Side.FRONT, 0.0F, 4.2F, -0.4F)
	);

	private static final List<Strip> BABY_STRIPS = List.of(
		new Strip("right_arm_bandage", "right_arm", 0.0F, 3.5F, 1.2F, 1, 2, 2, 0, 32, false, Side.FREE, 1.0F, 0.0F, 0.5F),
		new Strip("left_arm_bandage", "left_arm", 0.0F, 2.0F, 1.2F, 1, 2, 2, 2, 32, false, Side.FREE, 1.0F, 2.1F, -0.6F),
		new Strip("back_bandage", "body", 0.5F, -1.0F, 1.2F, 2, 2, 3, 4, 32, false, Side.BACK, 1.0F, 1.3F, 0.45F),
		new Strip("hip_bandage", "body", -2.2F, 1.0F, 0.0F, 1, 2, 2, 8, 32, true, Side.FREE, 0.8F, 3.4F, 0.4F),
		new Strip("head_bandage", "head", 1.0F, -3.0F, 3.5F, 1, 2, 2, 10, 32, false, Side.BACK, 1.0F, 0.7F, -0.5F),
		new Strip("front_bandage", "body", -1.0F, -1.0F, -1.2F, 1, 2, 2, 12, 32, false, Side.FRONT, 0.0F, 4.2F, -0.4F)
	);

	private record BakedStrip(Strip strip, ModelPart parent, List<ModelPart> segments) {
	}

	private final List<BakedStrip> strips = new ArrayList<>();

	public MummyBandagesModel(final ModelPart root, final boolean baby) {
		super(root);
		for (Strip strip : baby ? BABY_STRIPS : ADULT_STRIPS) {
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

	public static LayerDefinition createAdultLayer() {
		return createLayer(false);
	}

	public static LayerDefinition createBabyLayer() {
		return createLayer(true);
	}

	private static LayerDefinition createLayer(final boolean baby) {
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

		for (Strip strip : baby ? BABY_STRIPS : ADULT_STRIPS) {
			PartDefinition current = parts.get(strip.parent());
			for (int i = 0; i < strip.segments(); i++) {
				CubeListBuilder cube = CubeListBuilder.create().texOffs(strip.u(), strip.v() + i * strip.textureRowsPerSegment());
				float half = strip.width() / 2.0F;
				// Zero-thickness planes: the front and back faces are culled from the wrong side, so they never fight.
				if (strip.sideways()) {
					cube.addBox(0.0F, 0.0F, -half, 0.0F, strip.segmentLength(), strip.width());
				} else {
					cube.addBox(-half, 0.0F, 0.0F, strip.width(), strip.segmentLength(), 0.0F);
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
	public void setupAnim(final MummyRenderState state) {
		super.setupAnim(state);
		float time = state.ageInTicks;
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float walkCycle = state.walkAnimationPos * 0.6662F;
		float push = Math.min(walk * 0.9F + state.bandageLift, 1.1F);

		for (BakedStrip baked : this.strips) {
			Strip strip = baked.strip();
			for (int i = 0; i < baked.segments().size(); i++) {
				ModelPart segment = baked.segments().get(i);
				float phase = strip.phase() + i * 1.15F;
				float growth = 1.0F + i * 0.35F; // the loose end flaps more than the knot
				// A slow idle breeze (like a banner) plus a faster flap in step with the legs.
				float wave = (0.07F * Mth.sin(time * 0.11F - phase) + walk * 0.45F * Mth.sin(walkCycle - phase)) * growth;
				float sway = 0.05F * Mth.cos(time * 0.083F - phase) * growth - state.bandageSway * (i == 0 ? 1.0F : 0.5F);

				float pitch = wave + strip.drag() * push * (i == 0 ? 1.0F : 0.25F);
				if (i == 0) {
					pitch -= baked.parent().xRot; // hang with gravity, whatever the parent's pitch
					if (strip.side() == Side.BACK) {
						pitch = Math.max(pitch + 0.1F, 0.15F);
					} else if (strip.side() == Side.FRONT) {
						pitch = Math.min(pitch - 0.12F, -0.15F);
					}
				}
				segment.xRot = pitch;
				segment.zRot = sway;
			}
		}
	}
}
