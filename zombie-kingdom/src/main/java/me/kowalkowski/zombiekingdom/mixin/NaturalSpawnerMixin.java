package me.kowalkowski.zombiekingdom.mixin;

import me.kowalkowski.zombiekingdom.registry.ModEntities;
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
	/** Lets a few naturally spawning zombies be a Zombie King or a Zombie Princess instead. */
	@Inject(method = "getMobForSpawn", at = @At("RETURN"), cancellable = true)
	private static void zombie_kingdom$crownZombie(final ServerLevel level, final EntityType<?> type, final CallbackInfoReturnable<Mob> cir) {
		Mob mob = cir.getReturnValue();
		if (mob != null) {
			cir.setReturnValue(ModEntities.crownZombie(level, mob, level.getRandom()));
		}
	}
}
