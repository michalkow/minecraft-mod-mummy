package me.kowalkowski.mummy.mixin;

import me.kowalkowski.mummy.entity.RoyalZombie;
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
	private EntityType<? extends Zombie> mummy$royalsCallSubjects(final EntityType<? extends Zombie> reinforcementType) {
		return (Object) this instanceof RoyalZombie ? EntityTypes.ZOMBIE : reinforcementType;
	}
}
