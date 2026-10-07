package me.kowalkowski.mummy.client.render;

import java.util.List;
import me.kowalkowski.mummy.MummyMod;
import me.kowalkowski.mummy.entity.Mummy;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The mummy: bandage skin, glowing eyes and loose bandages that hang off it like cloth. */
public class MummyRenderer extends ClothedZombieRenderer<Mummy> {
	public static final ModelLayerLocation BANDAGES = new ModelLayerLocation(MummyMod.id("mummy"), "bandages");
	public static final ModelLayerLocation BABY_BANDAGES = new ModelLayerLocation(MummyMod.id("mummy"), "bandages_baby");

	// UV regions are in the free lower half of the mummy textures; tools/generate_textures.py paints them from the same table.
	private static final List<ClothStrip> ADULT_STRIPS = List.of(
		ClothStrip.ribbon("right_arm_bandage", "right_arm", -1.0F, 8.0F, 2.3F, 3, 3, 3, 0, 32, ClothStrip.Side.FREE, 1.0F, 0.0F, 0.5F),
		ClothStrip.ribbon("left_arm_bandage", "left_arm", 1.0F, 5.0F, 2.3F, 3, 4, 2, 6, 32, ClothStrip.Side.FREE, 1.0F, 2.1F, -0.6F),
		ClothStrip.ribbon("back_bandage", "body", 1.0F, 5.0F, 2.3F, 4, 4, 3, 12, 32, ClothStrip.Side.BACK, 1.0F, 1.3F, 0.45F),
		ClothStrip.sidewaysRibbon("hip_bandage", "body", -4.3F, 9.0F, 0.0F, 2, 3, 2, 20, 32, 0.8F, 3.4F, 0.4F),
		ClothStrip.ribbon("head_bandage", "head", 1.5F, -5.0F, 4.8F, 3, 3, 3, 24, 32, ClothStrip.Side.BACK, 1.0F, 0.7F, -0.5F),
		ClothStrip.ribbon("front_bandage", "body", -2.0F, 5.0F, -2.3F, 3, 3, 2, 30, 32, ClothStrip.Side.FRONT, 0.0F, 4.2F, -0.4F)
	);

	private static final List<ClothStrip> BABY_STRIPS = List.of(
		ClothStrip.ribbon("right_arm_bandage", "right_arm", 0.0F, 3.5F, 1.2F, 1, 2, 2, 0, 32, ClothStrip.Side.FREE, 1.0F, 0.0F, 0.5F),
		ClothStrip.ribbon("left_arm_bandage", "left_arm", 0.0F, 2.0F, 1.2F, 1, 2, 2, 2, 32, ClothStrip.Side.FREE, 1.0F, 2.1F, -0.6F),
		ClothStrip.ribbon("back_bandage", "body", 0.5F, -1.0F, 1.2F, 2, 2, 3, 4, 32, ClothStrip.Side.BACK, 1.0F, 1.3F, 0.45F),
		ClothStrip.sidewaysRibbon("hip_bandage", "body", -2.2F, 1.0F, 0.0F, 1, 2, 2, 8, 32, 0.8F, 3.4F, 0.4F),
		ClothStrip.ribbon("head_bandage", "head", 1.0F, -3.0F, 3.5F, 1, 2, 2, 10, 32, ClothStrip.Side.BACK, 1.0F, 0.7F, -0.5F),
		ClothStrip.ribbon("front_bandage", "body", -1.0F, -1.0F, -1.2F, 1, 2, 2, 12, 32, ClothStrip.Side.FRONT, 0.0F, 4.2F, -0.4F)
	);

	public MummyRenderer(final EntityRendererProvider.Context context) {
		super(context, MummyMod.id("textures/entity/mummy/mummy.png"), MummyMod.id("textures/entity/mummy/mummy_baby.png"));
		this.addLayer(new ClothLayer(
			this,
			new ClothModel(context.bakeLayer(BANDAGES), ADULT_STRIPS),
			new ClothModel(context.bakeLayer(BABY_BANDAGES), BABY_STRIPS),
			this::getTextureLocation
		));
		this.addLayer(new MummyEyesLayer(this));
	}

	public static LayerDefinition createBandagesLayer() {
		return ClothModel.createLayer(false, ADULT_STRIPS, parts -> {});
	}

	public static LayerDefinition createBabyBandagesLayer() {
		return ClothModel.createLayer(true, BABY_STRIPS, parts -> {});
	}
}
