package me.kowalkowski.mummy.client.render;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;

/**
 * The zombie model for royal guards: zombie arms by default, but a proper bow-drawing pose when an
 * archer aims, and a raised shield arm when a knight blocks.
 */
public class GuardModel extends HumanoidModel<ZombieRenderState> {
	public GuardModel(final ModelPart root) {
		super(root);
	}

	@Override
	protected void setupAttackAnimation(final ZombieRenderState state) {
		super.setupAttackAnimation(state);
		if (state.rightArmPose == ArmPose.BOW_AND_ARROW || state.leftArmPose == ArmPose.BOW_AND_ARROW) {
			return; // keep the bow pose
		}
		AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, state.isAggressive, state);
		if (state.rightArmPose == ArmPose.BLOCK) {
			raiseShield(this.rightArm, true);
		}
		if (state.leftArmPose == ArmPose.BLOCK) {
			raiseShield(this.leftArm, false);
		}
	}

	private static void raiseShield(final ModelPart arm, final boolean right) {
		arm.xRot = -0.9424779F;
		arm.yRot = (right ? -1.0F : 1.0F) * 0.5235988F;
		arm.zRot = 0.0F;
	}
}
