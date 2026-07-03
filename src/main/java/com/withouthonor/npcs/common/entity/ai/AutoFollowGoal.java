package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

public class AutoFollowGoal extends Goal {

    private final CompanionEntity npc;
    private final PathNavigation navigation;
    private LivingEntity target;
    private int timeToRecalcPath;

    public AutoFollowGoal(CompanionEntity npc) {
        this.npc = npc;
        this.navigation = npc.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private float stopDist() {
        return switch (npc.cfgDistanceTier()) {
            case 0 -> 1.5F;
            case 2 -> 5.0F;
            default -> 2.5F;
        };
    }

    private float teleportDist() {
        return switch (npc.cfgDistanceTier()) {
            case 0 -> 14.0F;
            case 2 -> 22.0F;
            default -> 18.0F;
        };
    }

    @Override
    public boolean canUse() {
        if (npc.getFollowMode() != FollowMode.NONE) {
            return false;
        }
        LivingEntity t = resolveTarget();
        if (t == null) {
            return false;
        }
        this.target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.getFollowMode() != FollowMode.NONE) {
            return false;
        }
        LivingEntity t = resolveTarget();
        return t != null && t == this.target;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.target = null;
        this.navigation.stop();
        npc.setSprinting(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        npc.getLookControl().setLookAt(target, 10.0F, (float) npc.getMaxHeadXRot());
        double distSqr = npc.distanceToSqr(target);
        float stop = stopDist();
        if (distSqr <= (double) (stop * stop)) {
            this.navigation.stop();
            npc.setSprinting(false);
            return;
        }
        float teleport = teleportDist();
        npc.setSprinting(isSprintSpeed(distSqr));
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = adjustedTickDelay(10);
            if (distSqr >= (double) (teleport * teleport) && npc.shouldTeleportFollow()
                    && tryTeleportNearTarget()) {
                return;
            }
            navigation.moveTo(target, speedFor(distSqr));
        }
    }

    private double speedFor(double distSqr) {
        double sprintAt = teleportDist() * 0.5;
        if (npc.cfgRun() && distSqr >= sprintAt * sprintAt) {
            return 1.45D;
        }
        if (npc.cfgMatchSpeed() && npc.cfgRun() && target.isSprinting()) {
            return 1.3D;
        }
        return 1.0D;
    }

    private boolean isSprintSpeed(double distSqr) {
        double sprintAt = teleportDist() * 0.5;
        return npc.cfgRun() && (distSqr >= sprintAt * sprintAt
                || (npc.cfgMatchSpeed() && target.isSprinting()));
    }

    @Nullable
    private LivingEntity resolveTarget() {
        if (!(npc.level() instanceof ServerLevel level)) {
            return null;
        }
        String value = npc.cfgAutoFollowTarget();
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (npc.cfgAutoFollowMode()) {
            case "player" -> {
                ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(value);
                yield player != null && player.isAlive() && player.level() == level ? player : null;
            }
            case "entity" -> {
                LivingEntity e = entityByUuid(level, value);
                yield e != null && e != npc && e.isAlive() ? e : null;
            }
            default -> null;
        };
    }

    @Nullable
    private static LivingEntity entityByUuid(ServerLevel level, String uuid) {
        try {
            Entity e = level.getEntity(UUID.fromString(uuid));
            return e instanceof LivingEntity living ? living : null;
        } catch (IllegalArgumentException badUuid) {
            return null;
        }
    }

    private boolean tryTeleportNearTarget() {
        BlockPos base = target.blockPosition();
        for (int i = 0; i < 10; i++) {
            BlockPos pos = base.offset(randomInclusive(-3, 3), randomInclusive(-1, 1), randomInclusive(-3, 3));
            if (Math.abs(pos.getX() + 0.5D - target.getX()) < 2.0D
                    && Math.abs(pos.getZ() + 0.5D - target.getZ()) < 2.0D) {
                continue;
            }
            if (canTeleportTo(pos)) {
                doTeleport(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                return true;
            }
        }
        return false;
    }

    private void doTeleport(double x, double y, double z) {
        if (npc.cfgTeleportFx() && npc.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, npc.getX(), npc.getY() + 1.0D, npc.getZ(),
                    8, 0.25D, 0.4D, 0.25D, 0.01D);
        }
        npc.moveTo(x, y, z, npc.getYRot(), npc.getXRot());
        this.navigation.stop();
    }

    private boolean canTeleportTo(BlockPos pos) {
        if (WalkNodeEvaluator.getBlockPathTypeStatic(npc.level(), pos.mutable()) != BlockPathTypes.WALKABLE) {
            return false;
        }
        if (npc.level().getBlockState(pos.below()).getBlock() instanceof LeavesBlock) {
            return false;
        }
        BlockPos relative = pos.subtract(npc.blockPosition());
        return npc.level().noCollision(npc, npc.getBoundingBox().move(relative));
    }

    private int randomInclusive(int min, int max) {
        return npc.getRandom().nextInt(max - min + 1) + min;
    }
}
