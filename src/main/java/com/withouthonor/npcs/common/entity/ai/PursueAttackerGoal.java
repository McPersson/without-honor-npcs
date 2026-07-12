package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class PursueAttackerGoal extends Goal {

    private static final double LEASH_SQR = 40.0 * 40.0;

    private final CompanionEntity npc;
    private final PathNavigation nav;
    @Nullable
    private LivingEntity target;
    private int repath;

    public PursueAttackerGoal(CompanionEntity npc) {
        this.npc = npc;
        this.nav = npc.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private double followSqr() {
        double f = npc.getAttributeValue(Attributes.FOLLOW_RANGE);
        return f * f;
    }

    @Nullable
    private LivingEntity active() {
        LivingEntity t = npc.getPursuitTarget();
        if (t == null || !t.isAlive() || npc.tickCount > npc.getPursuitExpireTick()) {
            return null;
        }
        double d = npc.distanceToSqr(t);
        if (d <= followSqr() || d > LEASH_SQR) {
            return null;
        }
        return t;
    }

    @Override
    public boolean canUse() {
        // #3: сцена смерти (эмоция перед смертью) — NPC заморожен, погоня не стартует.
        if (npc.isDeathStaged()) {
            return false;
        }
        if (!npc.cfgPursueAttacker() || npc.cfgHoldPosition() || npc.getTarget() != null) {
            return false;
        }
        LivingEntity t = active();
        if (t == null) {
            return false;
        }
        this.target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // #3: сцена смерти началась посреди погони — немедленно останавливаемся.
        if (npc.isDeathStaged()) {
            return false;
        }
        return npc.cfgPursueAttacker() && npc.getTarget() == null && active() != null;
    }

    @Override
    public void start() {
        this.repath = 0;
    }

    @Override
    public void stop() {
        LivingEntity t = this.target;
        if (t != null && t.isAlive() && npc.cfgPursueAttacker()
                && npc.tickCount <= npc.getPursuitExpireTick()
                && npc.distanceToSqr(t) <= followSqr()) {
            npc.setTarget(t);
        }
        npc.clearPursuitTarget();
        this.target = null;
        this.nav.stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        npc.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        if (--this.repath <= 0) {
            this.repath = 10;
            this.nav.moveTo(this.target, 1.1D);
        }
    }
}
