package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * «Живой» взгляд: NPC следит головой за ближайшим игроком, а если тот уходит
 * за спину — доворачивает корпус. Интерес угасает, когда игрок долго стоит на
 * месте, и возвращается, когда тот снова отходит от точки простоя.
 */
public class WatchPlayerGoal extends Goal {

    /** Сколько тиков игрок может стоять на месте, прежде чем NPC потеряет интерес. */
    private static final int BORED_TICKS = 5 * 20;
    /** Порог движения за тик (квадрат горизонтальной дистанции). */
    private static final double MOVE_EPS_SQR = 0.05 * 0.05;
    /** Отход от якоря, который тоже считается движением. */
    private static final double ANCHOR_DIST_SQR = 0.5 * 0.5;
    /** Насколько игрок должен отойти от точки простоя, чтобы интерес вернулся. */
    private static final double REACQUIRE_DIST_SQR = 1.5 * 1.5;

    private final CompanionEntity npc;
    @Nullable
    private Player target;
    @Nullable
    private Vec3 anchor;
    @Nullable
    private Vec3 lastPos;
    private int stillTicks;

    /** Игрок и точка, где интерес угас: не смотрим на него, пока не отойдёт. */
    @Nullable
    private Player boredPlayer;
    @Nullable
    private Vec3 boredPos;

    public WatchPlayerGoal(CompanionEntity npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (npc.getTarget() != null || npc.isScheduleYawFrozen()) {
            return false;
        }
        Player p = nearestPlayer();
        if (p == null) {
            return false;
        }
        if (p == boredPlayer && boredPos != null
                && p.position().distanceToSqr(boredPos) < REACQUIRE_DIST_SQR) {
            // Тот же игрок всё ещё топчется на месте — интерес не вернулся
            return false;
        }
        this.target = p;
        this.anchor = p.position();
        this.lastPos = p.position();
        this.stillTicks = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (npc.getTarget() != null || npc.isScheduleYawFrozen()) {
            return false;
        }
        float range = range();
        if (npc.distanceToSqr(target) > (double) ((range + 2.0F) * (range + 2.0F))) {
            return false;
        }
        return stillTicks <= BORED_TICKS;
    }

    @Override
    public void stop() {
        if (target != null && stillTicks > BORED_TICKS) {
            boredPlayer = target;
            boredPos = target.position();
        } else {
            boredPlayer = null;
            boredPos = null;
        }
        target = null;
        anchor = null;
        lastPos = null;
        stillTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        Vec3 pos = target.position();
        boolean moved = false;
        if (lastPos != null) {
            double dx = pos.x - lastPos.x;
            double dz = pos.z - lastPos.z;
            moved = dx * dx + dz * dz > MOVE_EPS_SQR;
        }
        if (anchor != null && pos.distanceToSqr(anchor) > ANCHOR_DIST_SQR) {
            moved = true;
        }
        if (moved) {
            stillTicks = 0;
            anchor = pos;
        } else {
            stillTicks++;
        }
        lastPos = pos;

        npc.getLookControl().setLookAt(target, 30.0F, (float) npc.getMaxHeadXRot());

        // Если игрок ушёл далеко за спину — доворачиваем корпус (~10°/тик)
        double tx = target.getX() - npc.getX();
        double tz = target.getZ() - npc.getZ();
        float desired = (float) (Mth.atan2(tz, tx) * (180.0D / Math.PI)) - 90.0F;
        if (Math.abs(Mth.wrapDegrees(desired - npc.yBodyRot)) > 60.0F) {
            float d = Mth.clamp(Mth.wrapDegrees(desired - npc.yBodyRot), -10.0F, 10.0F);
            npc.yBodyRot += d;
        }
    }

    /** Радиус из профиля (look_radius, 1..16); кламп на случай кривых данных. */
    private float range() {
        return net.minecraft.util.Mth.clamp(npc.cfgLookRadius(), 1, 16);
    }

    @Nullable
    private Player nearestPlayer() {
        Player best = null;
        float range = range();
        double bestDist = (double) (range * range);
        for (Player p : npc.level().players()) {
            // Креатив не фильтруем: авторы настраивают NPC в креативе и должны видеть «живой» взгляд.
            if (!p.isAlive() || p.isSpectator()) {
                continue;
            }
            double d = npc.distanceToSqr(p);
            if (d < bestDist) {
                best = p;
                bestDist = d;
            }
        }
        return best;
    }
}
