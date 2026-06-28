package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class WitchHealGoal extends Goal {

    private final CompanionEntity npc;
    private int cooldown;
    private int scanCooldown;
    @Nullable
    private LivingEntity ally;

    public WitchHealGoal(CompanionEntity npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
        }
        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }
        if (npc.getTarget() != null || cooldown > 0) {
            return false;
        }
        ally = findInjuredAlly();
        if (ally == null) {
            scanCooldown = 10;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        cooldown = 80;
        if (ally != null) {
            npc.getLookControl().setLookAt(ally, 30.0F, 30.0F);
            npc.throwSplashAt(ally, npc.beneficialBeltPotion());
            ally = null;
        }
    }

    @Nullable
    private LivingEntity findInjuredAlly() {
        AABB box = npc.getBoundingBox().inflate(8.0D);
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (LivingEntity e : npc.level().getEntitiesOfClass(LivingEntity.class, box, this::isInjuredAlly)) {
            double dsq = npc.distanceToSqr(e);
            if (dsq < bestSq) {
                bestSq = dsq;
                best = e;
            }
        }
        return best;
    }

    private boolean isInjuredAlly(LivingEntity e) {
        if (e == npc || !e.isAlive() || e.getHealth() >= e.getMaxHealth() * 0.75F) {
            return false;
        }
        if (e instanceof Player) {
            return true;
        }
        return e instanceof CompanionEntity other && npc.isSameFaction(other);
    }
}
