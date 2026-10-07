package me.kowalkowski.mummy.client;

import me.kowalkowski.mummy.client.render.GuardRenderer;
import me.kowalkowski.mummy.client.render.MummyRenderer;
import me.kowalkowski.mummy.client.render.VerityRenderer;
import me.kowalkowski.mummy.client.render.ZombieKingRenderer;
import me.kowalkowski.mummy.client.render.ZombiePrincessRenderer;
import me.kowalkowski.mummy.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class MummyModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(MummyRenderer.BANDAGES, MummyRenderer::createBandagesLayer);
		ModelLayerRegistry.registerModelLayer(MummyRenderer.BABY_BANDAGES, MummyRenderer::createBabyBandagesLayer);
		ModelLayerRegistry.registerModelLayer(ZombieKingRenderer.REGALIA, ZombieKingRenderer::createRegaliaLayer);
		ModelLayerRegistry.registerModelLayer(ZombieKingRenderer.BABY_REGALIA, ZombieKingRenderer::createBabyRegaliaLayer);
		ModelLayerRegistry.registerModelLayer(ZombiePrincessRenderer.REGALIA, ZombiePrincessRenderer::createRegaliaLayer);
		ModelLayerRegistry.registerModelLayer(ZombiePrincessRenderer.BABY_REGALIA, ZombiePrincessRenderer::createBabyRegaliaLayer);
		EntityRenderers.register(ModEntities.MUMMY, MummyRenderer::new);
		EntityRenderers.register(ModEntities.ZOMBIE_KING, ZombieKingRenderer::new);
		EntityRenderers.register(ModEntities.ZOMBIE_PRINCESS, ZombiePrincessRenderer::new);
		EntityRenderers.register(ModEntities.VERITY, VerityRenderer::new);
		EntityRenderers.register(ModEntities.ZOMBIE_KNIGHT, context -> new GuardRenderer<>(context, "zombie_knight"));
		EntityRenderers.register(ModEntities.ZOMBIE_ARCHER, context -> new GuardRenderer<>(context, "zombie_archer"));
	}
}
