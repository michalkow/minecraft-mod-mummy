package me.kowalkowski.mummy.client.render;

import me.kowalkowski.mummy.MummyMod;
import me.kowalkowski.mummy.entity.Mummy;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renders the mummy with the vanilla zombie model (adult and baby) and the mummy skin,
 * plus glowing eyes and loose animated bandages.
 */
public class MummyRenderer extends AbstractZombieRenderer<Mummy, MummyRenderState, ZombieModel<MummyRenderState>> {
	private static final Identifier MUMMY_LOCATION = MummyMod.id("textures/entity/mummy/mummy.png");
	private static final Identifier BABY_MUMMY_LOCATION = MummyMod.id("textures/entity/mummy/mummy_baby.png");

	public MummyRenderer(final EntityRendererProvider.Context context) {
		super(
			context,
			new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
			new BabyZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
			ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), ZombieModel::new),
			ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new)
		);
		this.addLayer(new MummyBandagesLayer(this, context.getModelSet()));
		this.addLayer(new MummyEyesLayer(this));
	}

	static Identifier texture(final MummyRenderState state) {
		return state.isBaby ? BABY_MUMMY_LOCATION : MUMMY_LOCATION;
	}

	@Override
	public MummyRenderState createRenderState() {
		return new MummyRenderState();
	}

	@Override
	public void extractRenderState(final Mummy entity, final MummyRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.bandageSway = Mth.lerp(partialTicks, entity.bandageSwayO, entity.bandageSway);
		state.bandageLift = Mth.lerp(partialTicks, entity.bandageLiftO, entity.bandageLift);
	}

	@Override
	public Identifier getTextureLocation(final MummyRenderState state) {
		return texture(state);
	}
}
