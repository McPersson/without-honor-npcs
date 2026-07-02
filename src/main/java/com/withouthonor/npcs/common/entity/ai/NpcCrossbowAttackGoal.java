package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.CrossbowItem;

import java.util.EnumSet;

public class NpcCrossbowAttackGoal extends Goal {

    private final CompanionEntity mob;
    private final double speedModifier;
    private final float attackRadiusSqr;
    private State state = State.UNCHARGED;
    private int seeTime;
    private int attackDelay;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public NpcCrossbowAttackGoal(CompanionEntity mob, double speed, float range) {
        this.mob = mob;
        this.speedModifier = speed;
        this.attackRadiusSqr = range * range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return isValidTarget() && isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(is -> is.getItem() instanceof CrossbowItem);
    }

    private InteractionHand crossbowHand() {
        return this.mob.getMainHandItem().getItem() instanceof CrossbowItem
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    @Override
    public boolean canContinueToUse() {
        return isValidTarget() && (canUse() || !this.mob.getNavigation().isDone()) && isHoldingCrossbow();
    }

    private boolean isValidTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
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
        this.strafingTime = -1;
        this.state = State.UNCHARGED;
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
            this.mob.setChargingCrossbow(false);
            CrossbowItem.setCharged(this.mob.getUseItem(), false);
        }
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

        if (distSqr <= (double) this.attackRadiusSqr && this.seeTime >= 5) {
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

        if (this.state == State.UNCHARGED) {
            this.mob.startUsingItem(crossbowHand());
            this.state = State.CHARGING;
            this.mob.setChargingCrossbow(true);
        } else if (this.state == State.CHARGING) {
            if (!this.mob.isUsingItem()) {
                this.state = State.UNCHARGED;
            } else if (this.mob.getTicksUsingItem() >= CrossbowItem.getChargeDuration(this.mob.getUseItem())) {
                this.mob.releaseUsingItem();
                this.state = State.CHARGED;
                this.attackDelay = 20 + this.mob.getRandom().nextInt(20);
                this.mob.setChargingCrossbow(false);
            }
        } else if (this.state == State.CHARGED) {
            --this.attackDelay;
            if (this.attackDelay <= 0) {
                this.state = State.READY_TO_ATTACK;
            }
        } else if (this.state == State.READY_TO_ATTACK && canSee) {
            this.mob.performCrossbowAttack(this.mob, 1.6F);
            CrossbowItem.setCharged(this.mob.getItemInHand(crossbowHand()), false);
            this.state = State.UNCHARGED;
        }
    }

    private enum State {
        UNCHARGED, CHARGING, CHARGED, READY_TO_ATTACK
    }
}
