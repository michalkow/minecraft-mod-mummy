package me.kowalkowski.verity.mixin;

import me.kowalkowski.verity.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	/** Lets one in a hundred naturally spawning zombies be Verity instead. */
	@Inject(method = "getMobForSpawn", at = @At("RETURN"), cancellable = true)
	private static void verity$smileyZombie(final ServerLevel level, final EntityType<?> type, final CallbackInfoReturnable<Mob> cir) {
		Mob mob = cir.getReturnValue();
		if (mob != null) {
			cir.setReturnValue(ModEntities.smileyZombie(level, mob, level.getRandom()));
		}
	}
}
