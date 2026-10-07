package me.kowalkowski.mummy.client.render;

import me.kowalkowski.mummy.entity.ClothMotion;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Renders a zombie-shaped mob with the vanilla zombie model (adult and baby) and its own skins. */
public abstract class ClothedZombieRenderer<T extends Zombie & ClothMotion.Wearer>
	extends AbstractZombieRenderer<T, ClothRenderState, ZombieModel<ClothRenderState>> {
	private final Identifier texture;
	private final Identifier babyTexture;

	protected ClothedZombieRenderer(final EntityRendererProvider.Context context, final Identifier texture, final Identifier babyTexture) {
		super(
			context,
			new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
			new BabyZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
			ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), ZombieModel::new),
			ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new)
		);
		this.texture = texture;
		this.babyTexture = babyTexture;
	}

	@Override
	public ClothRenderState createRenderState() {
		return new ClothRenderState();
	}

	@Override
	public void extractRenderState(final T entity, final ClothRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.clothSway = entity.cloth().sway(partialTicks);
		state.clothLift = entity.cloth().lift(partialTicks);
	}

	@Override
	public Identifier getTextureLocation(final ClothRenderState state) {
		return state.isBaby ? this.babyTexture : this.texture;
	}
}
