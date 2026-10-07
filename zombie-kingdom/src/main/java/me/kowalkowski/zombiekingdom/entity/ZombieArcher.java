package me.kowalkowski.zombiekingdom.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A royal guard in a green hood who shoots arrows like a skeleton, best placed on walls and towers. */
public class ZombieArcher extends RoyalGuard implements RangedAttackMob {
	public ZombieArcher(final EntityType<? extends ZombieArcher> type, final Level level) {
		super(type, level);
	}

	@Override
	protected void addBehaviourGoals() {
		super.addBehaviourGoals();
		// Swap the zombie's melee for the skeleton's bow attack.
		this.goalSelector.removeAllGoals(goal -> goal instanceof ZombieAttackGoal || goal instanceof SpearUseGoal);
		this.goalSelector.addGoal(3, new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F));
	}

	@Override
	protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
	}

	@Override
	public void performRangedAttack(final LivingEntity target, final float power) {
		ItemStack bow = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
		ItemStack projectile = this.getProjectile(bow);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectile, power, bow);
		double xd = target.getX() - this.getX();
		double yd = target.getY(0.3333333333333333) - arrow.getY();
		double zd = target.getZ() - this.getZ();
		double distance = Math.sqrt(xd * xd + zd * zd);
		if (this.level() instanceof ServerLevel level) {
			Projectile.spawnProjectileUsingShoot(arrow, level, projectile, xd, yd + distance * 0.2F, zd, 1.6F, this.rangedAttackUncertainty(level));
		}
		this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	@Override
	public boolean canUseNonMeleeWeapon(final ItemStack item) {
		return item.is(Items.BOW);
	}
}
