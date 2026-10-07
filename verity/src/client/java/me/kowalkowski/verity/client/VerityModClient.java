package me.kowalkowski.verity.client;

import me.kowalkowski.verity.client.render.VerityRenderer;
import me.kowalkowski.verity.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class VerityModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.VERITY, VerityRenderer::new);
	}
}
