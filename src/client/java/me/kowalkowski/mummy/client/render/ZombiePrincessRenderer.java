package me.kowalkowski.mummy.client.render;

import java.util.List;
import java.util.Map;
import me.kowalkowski.mummy.MummyMod;
import me.kowalkowski.mummy.entity.ZombiePrincess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** The Zombie Princess: a pink dress with a swishing, flared skirt and a little gold crown. */
public class ZombiePrincessRenderer extends ClothedZombieRenderer<ZombiePrincess> {
	public static final ModelLayerLocation REGALIA = new ModelLayerLocation(MummyMod.id("zombie_princess"), "regalia");
	public static final ModelLayerLocation BABY_REGALIA = new ModelLayerLocation(MummyMod.id("zombie_princess"), "regalia_baby");
	private static final Identifier REGALIA_TEXTURE = MummyMod.id("textures/entity/zombie_princess/zombie_princess_regalia.png");

	// Four skirt panels hanging from the waist. UV regions in zombie_princess_regalia.png;
	// tools/generate_textures.py paints the same layout.
	private static final List<ClothStrip> ADULT_SKIRT = List.of(
		ClothStrip.skirt("skirt_front", 0.0F, 11.0F, -2.3F, 10, 3, 2, 0, 0, ClothStrip.Side.FRONT, 0.0F),
		ClothStrip.skirt("skirt_back", 0.0F, 11.0F, 2.3F, 10, 3, 2, 0, 8, ClothStrip.Side.BACK, 1.6F),
		ClothStrip.skirt("skirt_right", -4.3F, 11.0F, 0.0F, 6, 3, 2, 24, 0, ClothStrip.Side.FREE, 0.8F),
		ClothStrip.skirt("skirt_left", 4.3F, 11.0F, 0.0F, 6, 3, 2, 40, 0, ClothStrip.Side.FREE, 2.4F)
	);
	private static final List<ClothStrip> BABY_SKIRT = List.of(
		ClothStrip.skirt("skirt_front", 0.0F, 2.0F, -1.2F, 5, 2, 2, 0, 20, ClothStrip.Side.FRONT, 0.0F),
		ClothStrip.skirt("skirt_back", 0.0F, 2.0F, 1.2F, 5, 2, 2, 0, 26, ClothStrip.Side.BACK, 1.6F),
		ClothStrip.skirt("skirt_right", -2.2F, 2.0F, 0.0F, 3, 2, 2, 24, 20, ClothStrip.Side.FREE, 0.8F),
		ClothStrip.skirt("skirt_left", 2.2F, 2.0F, 0.0F, 3, 2, 2, 32, 20, ClothStrip.Side.FREE, 2.4F)
	);

	public ZombiePrincessRenderer(final EntityRendererProvider.Context context) {
		super(
			context,
			MummyMod.id("textures/entity/zombie_princess/zombie_princess.png"),
			MummyMod.id("textures/entity/zombie_princess/zombie_princess_baby.png")
		);
		this.addLayer(new ClothLayer(
			this,
			new ClothModel(context.bakeLayer(REGALIA), ADULT_SKIRT),
			new ClothModel(context.bakeLayer(BABY_REGALIA), BABY_SKIRT),
			state -> REGALIA_TEXTURE
		));
	}

	public static LayerDefinition createRegaliaLayer() {
		return ClothModel.createLayer(false, ADULT_SKIRT, ZombiePrincessRenderer::addAdultCrown);
	}

	public static LayerDefinition createBabyRegaliaLayer() {
		return ClothModel.createLayer(true, BABY_SKIRT, ZombiePrincessRenderer::addBabyCrown);
	}

	/** A little crown perched on top of the head, with a taller jewelled point at the front. */
	private static void addAdultCrown(final Map<String, PartDefinition> parts) {
		parts.get("head").addOrReplaceChild(
			"crown",
			CubeListBuilder.create()
				.texOffs(0, 40).addBox(-3.0F, -9.0F, -3.0F, 6.0F, 1.0F, 1.0F)
				.texOffs(0, 40).addBox(-3.0F, -9.0F, 2.0F, 6.0F, 1.0F, 1.0F)
				.texOffs(16, 40).addBox(-3.0F, -9.0F, -2.0F, 1.0F, 1.0F, 4.0F)
				.texOffs(16, 40).addBox(2.0F, -9.0F, -2.0F, 1.0F, 1.0F, 4.0F)
				.texOffs(28, 40).addBox(-3.0F, -10.0F, -3.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(28, 40).addBox(2.0F, -10.0F, -3.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(28, 40).addBox(-3.0F, -10.0F, 2.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(28, 40).addBox(2.0F, -10.0F, 2.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(34, 40).addBox(-0.5F, -11.0F, -3.0F, 1.0F, 2.0F, 1.0F),
			PartPose.ZERO
		);
	}

	private static void addBabyCrown(final Map<String, PartDefinition> parts) {
		parts.get("head").addOrReplaceChild(
			"crown",
			CubeListBuilder.create()
				.texOffs(0, 48).addBox(-2.0F, -7.25F, -2.0F, 4.0F, 1.0F, 1.0F)
				.texOffs(0, 48).addBox(-2.0F, -7.25F, 1.0F, 4.0F, 1.0F, 1.0F)
				.texOffs(12, 48).addBox(-2.0F, -7.25F, -1.0F, 1.0F, 1.0F, 2.0F)
				.texOffs(12, 48).addBox(1.0F, -7.25F, -1.0F, 1.0F, 1.0F, 2.0F)
				.texOffs(20, 48).addBox(-2.0F, -8.25F, -2.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(20, 48).addBox(1.0F, -8.25F, -2.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(20, 48).addBox(-2.0F, -8.25F, 1.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(20, 48).addBox(1.0F, -8.25F, 1.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(26, 48).addBox(-0.5F, -9.25F, -2.0F, 1.0F, 2.0F, 1.0F),
			PartPose.ZERO
		);
	}
}
