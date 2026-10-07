package me.kowalkowski.zombiekingdom.mixin;

import me.kowalkowski.zombiekingdom.entity.RoyalZombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Zombie.class)
public abstract class ZombieMixin {
	/** Zombie royalty calls ordinary zombies as reinforcements, not more royals. */
	@ModifyVariable(method = "hurtServer", at = @At("STORE"))
	private EntityType<? extends Zombie> zombie_kingdom$royalsCallSubjects(final EntityType<? extends Zombie> reinforcementType) {
		return (Object) this instanceof RoyalZombie ? EntityTypes.ZOMBIE : reinforcementType;
	}
}
