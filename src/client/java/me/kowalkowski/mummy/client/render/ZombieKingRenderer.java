package me.kowalkowski.mummy.client.render;

import java.util.List;
import java.util.Map;
import me.kowalkowski.mummy.MummyMod;
import me.kowalkowski.mummy.entity.ZombieKing;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** The Zombie King: royal skin, a gold crown and a red cape that billows like a banner. */
public class ZombieKingRenderer extends ClothedZombieRenderer<ZombieKing> {
	public static final ModelLayerLocation REGALIA = new ModelLayerLocation(MummyMod.id("zombie_king"), "regalia");
	public static final ModelLayerLocation BABY_REGALIA = new ModelLayerLocation(MummyMod.id("zombie_king"), "regalia_baby");
	private static final Identifier REGALIA_TEXTURE = MummyMod.id("textures/entity/zombie_king/zombie_king_regalia.png");

	// UV regions in zombie_king_regalia.png; tools/generate_textures.py paints the same layout.
	private static final List<ClothStrip> ADULT_CAPE = List.of(ClothStrip.cape("cape", 0.0F, 0.0F, 2.1F, 10, 4, 4, 0, 0));
	private static final List<ClothStrip> BABY_CAPE = List.of(ClothStrip.cape("cape", 0.0F, -2.5F, 1.1F, 5, 2, 3, 24, 0));

	public ZombieKingRenderer(final EntityRendererProvider.Context context) {
		super(context, MummyMod.id("textures/entity/zombie_king/zombie_king.png"), MummyMod.id("textures/entity/zombie_king/zombie_king_baby.png"));
		this.addLayer(new ClothLayer(
			this,
			new ClothModel(context.bakeLayer(REGALIA), ADULT_CAPE),
			new ClothModel(context.bakeLayer(BABY_REGALIA), BABY_CAPE),
			state -> REGALIA_TEXTURE
		));
	}

	public static LayerDefinition createRegaliaLayer() {
		return ClothModel.createLayer(false, ADULT_CAPE, ZombieKingRenderer::addAdultCrown);
	}

	public static LayerDefinition createBabyRegaliaLayer() {
		return ClothModel.createLayer(true, BABY_CAPE, ZombieKingRenderer::addBabyCrown);
	}

	/** A ring of gold resting on top of the head (and its hat layer), with points at the corners and sides. */
	private static void addAdultCrown(final Map<String, PartDefinition> parts) {
		parts.get("head").addOrReplaceChild(
			"crown",
			CubeListBuilder.create()
				.texOffs(0, 40).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 2.0F, 1.0F)
				.texOffs(0, 40).addBox(-5.0F, -10.0F, 4.0F, 10.0F, 2.0F, 1.0F)
				.texOffs(24, 40).addBox(-5.0F, -10.0F, -4.0F, 1.0F, 2.0F, 8.0F)
				.texOffs(24, 40).addBox(4.0F, -10.0F, -4.0F, 1.0F, 2.0F, 8.0F)
				.texOffs(44, 40).addBox(-5.0F, -12.0F, -5.0F, 1.0F, 2.0F, 1.0F)
				.texOffs(44, 40).addBox(4.0F, -12.0F, -5.0F, 1.0F, 2.0F, 1.0F)
				.texOffs(44, 40).addBox(-5.0F, -12.0F, 4.0F, 1.0F, 2.0F, 1.0F)
				.texOffs(44, 40).addBox(4.0F, -12.0F, 4.0F, 1.0F, 2.0F, 1.0F)
				.texOffs(48, 40).addBox(-0.5F, -13.0F, -5.0F, 1.0F, 3.0F, 1.0F)
				.texOffs(48, 40).addBox(-0.5F, -13.0F, 4.0F, 1.0F, 3.0F, 1.0F)
				.texOffs(48, 40).addBox(-5.0F, -13.0F, -0.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(48, 40).addBox(4.0F, -13.0F, -0.5F, 1.0F, 3.0F, 1.0F),
			PartPose.ZERO
		);
	}

	private static void addBabyCrown(final Map<String, PartDefinition> parts) {
		parts.get("head").addOrReplaceChild(
			"crown",
			CubeListBuilder.create()
				.texOffs(0, 52).addBox(-4.0F, -8.25F, -4.0F, 8.0F, 2.0F, 1.0F)
				.texOffs(0, 52).addBox(-4.0F, -8.25F, 3.0F, 8.0F, 2.0F, 1.0F)
				.texOffs(20, 52).addBox(-4.0F, -8.25F, -3.0F, 1.0F, 2.0F, 6.0F)
				.texOffs(20, 52).addBox(3.0F, -8.25F, -3.0F, 1.0F, 2.0F, 6.0F)
				.texOffs(36, 52).addBox(-4.0F, -9.25F, -4.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(36, 52).addBox(3.0F, -9.25F, -4.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(36, 52).addBox(-4.0F, -9.25F, 3.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(36, 52).addBox(3.0F, -9.25F, 3.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(36, 52).addBox(-0.5F, -9.25F, -4.0F, 1.0F, 1.0F, 1.0F)
				.texOffs(36, 52).addBox(-0.5F, -9.25F, 3.0F, 1.0F, 1.0F, 1.0F),
			PartPose.ZERO
		);
	}
}
