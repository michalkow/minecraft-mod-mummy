package me.kowalkowski.zombiekingdom.client.render;

import me.kowalkowski.zombiekingdom.ZombieKingdomMod;
import me.kowalkowski.zombiekingdom.entity.RoyalGuard;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

/** Knights and archers: the zombie model with their own skins, real armour, and bow / shield poses. */
public class GuardRenderer<T extends RoyalGuard> extends AbstractZombieRenderer<T, ZombieRenderState, GuardModel> {
	private final Identifier texture;
	private final Identifier babyTexture;

	public GuardRenderer(final EntityRendererProvider.Context context, final String name) {
		super(
			context,
			new GuardModel(context.bakeLayer(ModelLayers.ZOMBIE)),
			new GuardModel(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
			ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), GuardModel::new),
			ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), GuardModel::new)
		);
		this.texture = ZombieKingdomMod.id("textures/entity/" + name + "/" + name + ".png");
		this.babyTexture = ZombieKingdomMod.id("textures/entity/" + name + "/" + name + "_baby.png");
	}

	@Override
	public ZombieRenderState createRenderState() {
		return new ZombieRenderState();
	}

	@Override
	public Identifier getTextureLocation(final ZombieRenderState state) {
		return state.isBaby ? this.babyTexture : this.texture;
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(final T mob, final HumanoidArm arm) {
		if (arm == mob.getMainArm() && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW)) {
			return HumanoidModel.ArmPose.BOW_AND_ARROW;
		}
		if (arm == mob.getMainArm().getOpposite() && mob.isBlocking()) {
			return HumanoidModel.ArmPose.BLOCK;
		}
		return super.getArmPose(mob, arm);
	}
}
