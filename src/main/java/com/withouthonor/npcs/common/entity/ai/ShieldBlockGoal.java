package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class ShieldBlockGoal extends Goal {

    private static final double THREAT_RANGE_SQR = 9.0;

    private final CompanionEntity npc;
    private final int holdTicks;
    private final int cooldownTicks;
    private int holdTimer;
    private int cooldownTimer;

    public ShieldBlockGoal(CompanionEntity npc, int holdTicks, int cooldownTicks) {
        this.npc = npc;
        this.holdTicks = holdTicks;
        this.cooldownTicks = cooldownTicks;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Nullable
    private InteractionHand shieldHand() {
        // Forge-стандарт: любой предмет, умеющий блокировать (ваниль-щит реализует SHIELD_BLOCK) —
        // модовые щиты работают тоже. armPose увидит их сам через UseAnim.BLOCK.
        if (npc.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
            return InteractionHand.OFF_HAND;
        }
        if (npc.getMainHandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    @Override
    public boolean canUse() {
        if (cooldownTimer > 0) {
            cooldownTimer--;
            return false;
        }
        if (shieldHand() == null) {
            return false;
        }
        LivingEntity target = npc.getTarget();
        return target != null && target.isAlive()
                && npc.distanceToSqr(target) <= THREAT_RANGE_SQR;
    }

    @Override
    public void start() {
        InteractionHand hand = shieldHand();
        if (hand != null) {
            npc.startUsingItem(hand);
            holdTimer = holdTicks;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return holdTimer > 0 && shieldHand() != null
                && npc.getTarget() != null && npc.getTarget().isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (holdTimer > 0) {
            holdTimer--;
        }
    }

    @Override
    public void stop() {
        npc.stopUsingItem();
        cooldownTimer = cooldownTicks;
    }
}
