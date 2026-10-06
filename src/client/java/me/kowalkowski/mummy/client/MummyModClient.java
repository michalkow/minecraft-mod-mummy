package me.kowalkowski.mummy.client;

import me.kowalkowski.mummy.client.render.MummyBandagesModel;
import me.kowalkowski.mummy.client.render.MummyRenderer;
import me.kowalkowski.mummy.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class MummyModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(MummyBandagesModel.LAYER, MummyBandagesModel::createAdultLayer);
		ModelLayerRegistry.registerModelLayer(MummyBandagesModel.BABY_LAYER, MummyBandagesModel::createBabyLayer);
		EntityRenderers.register(ModEntities.MUMMY, MummyRenderer::new);
	}
}
