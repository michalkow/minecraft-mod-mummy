package me.kowalkowski.mummy.client;

import me.kowalkowski.mummy.client.render.MummyRenderer;
import me.kowalkowski.mummy.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class MummyModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(MummyRenderer.BANDAGES, MummyRenderer::createBandagesLayer);
		ModelLayerRegistry.registerModelLayer(MummyRenderer.BABY_BANDAGES, MummyRenderer::createBabyBandagesLayer);
		EntityRenderers.register(ModEntities.MUMMY, MummyRenderer::new);
	}
}
