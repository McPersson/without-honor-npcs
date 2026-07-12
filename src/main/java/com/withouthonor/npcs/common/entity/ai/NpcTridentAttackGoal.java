package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.TridentItem;

import java.util.EnumSet;

public class NpcTridentAttackGoal extends Goal {

    private final CompanionEntity mob;
    private final double speedModifier;
    private final int attackIntervalMin;
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public NpcTridentAttackGoal(CompanionEntity mob, double speed, int interval, float range) {
        this.mob = mob;
        this.speedModifier = speed;
        this.attackIntervalMin = interval;
        this.attackRadiusSqr = range * range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.getTarget() != null && isHoldingSpear();
    }

    /**
     * Держит метаемое копьё (ваниль или модовое), у которого метание НЕ помечено провалившимся.
     * ТОЛЬКО главная рука: performRangedAttack читает MAINHAND, off-hand дал бы замах
     * с «выстрелом стрелой из ничего»; гейт совпадает с rebuildCombatGoals.
     */
    private boolean isHoldingSpear() {
        return this.mob.canThrowSpear(this.mob.getMainHandItem());
    }

    private InteractionHand spearHand() {
        return InteractionHand.MAIN_HAND; // см. isHoldingSpear — off-hand не поддерживается
    }

    /** Ваниль-трезубец бросаем быстрее; модовым копьям даём больше заряда (порог releaseUsing мода). */
    private int windUp() {
        return this.mob.getItemInHand(spearHand()).getItem() instanceof TridentItem ? 12 : 20;
    }

    @Override
    public boolean canContinueToUse() {
        return (canUse() || !this.mob.getNavigation().isDone()) && isHoldingSpear();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.mob.setSprinting(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.strafingTime = -1;
        this.mob.stopUsingItem();
        this.mob.getNavigation().stop();
        this.mob.setDeltaMovement(0.0D, this.mob.getDeltaMovement().y, 0.0D);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.mob.getSensing().hasLineOfSight(target);
        boolean hadSee = this.seeTime > 0;
        if (canSee != hadSee) {
            this.seeTime = 0;
        }
        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        if (distSqr <= (double) this.attackRadiusSqr && this.seeTime >= 20) {
            this.mob.getNavigation().stop();
            ++this.strafingTime;
        } else {
            this.mob.getNavigation().moveTo(target, this.speedModifier);
            this.strafingTime = -1;
        }
        if (this.strafingTime >= 20) {
            if (this.mob.getRandom().nextFloat() < 0.3F) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            if (this.mob.getRandom().nextFloat() < 0.3F) {
                this.strafingBackwards = !this.strafingBackwards;
            }
            this.strafingTime = 0;
        }
        if (this.strafingTime > -1) {
            if (distSqr > (double) (this.attackRadiusSqr * 0.75F)) {
                this.strafingBackwards = false;
            } else if (distSqr < (double) (this.attackRadiusSqr * 0.25F)) {
                this.strafingBackwards = true;
            }
            this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F,
                    this.strafingClockwise ? 0.5F : -0.5F);
            this.mob.lookAt(target, 30.0F, 30.0F);
        } else {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (this.mob.isUsingItem()) {
            if (!canSee && this.seeTime < -60) {
                this.mob.stopUsingItem();
            } else if (canSee && this.mob.getTicksUsingItem() >= windUp()) {
                this.mob.stopUsingItem();
                this.mob.performRangedAttack(target, 1.0F);
                this.attackTime = this.attackIntervalMin;
            }
        } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
            this.mob.startUsingItem(spearHand());
        }
    }
}
