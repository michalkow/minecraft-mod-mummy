package me.kowalkowski.mummy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kowalkowski.mummy.entity.RoyalZombie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
	/** The royals' celebration fireworks hurt players caught in the blast, but never any mob. */
	@WrapOperation(
		method = "dealExplosionDamage",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z")
	)
	private boolean mummy$celebrationHurtsPlayersOnly(
		final LivingEntity target, final ServerLevel level, final DamageSource source, final float damage, final Operation<Boolean> original
	) {
		boolean celebration = ((FireworkRocketEntity) (Object) this).entityTags().contains(RoyalZombie.CELEBRATION_ROCKET_TAG);
		return celebration && !(target instanceof Player) ? false : original.call(target, level, source, damage);
	}
}
