package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class FollowPlayerGoal extends Goal {

    private final CompanionEntity npc;
    private final PathNavigation navigation;
    private Player target;
    private int timeToRecalcPath;
    private int returnTicks;

    public FollowPlayerGoal(CompanionEntity npc) {
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

    private float startDist() {
        return switch (npc.cfgDistanceTier()) {
            case 0 -> 3.0F;
            case 2 -> 7.0F;
            default -> 4.5F;
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
        // #3: сцена смерти (эмоция перед смертью) — NPC заморожен, следование не стартует.
        if (npc.isDeathStaged()) {
            return false;
        }
        // В бою следование уступает: пока есть живая цель, не разворачиваем NPC к игроку («челнок»).
        if (npc.getTarget() != null && npc.getTarget().isAlive()) {
            return false;
        }
        Player p = npc.getFollowTarget();
        if (p == null || p.isSpectator() || npc.isPassenger()) {
            return false;
        }
        FollowMode mode = npc.getFollowMode();
        if (mode == FollowMode.WAIT || mode == FollowMode.NONE) {
            return false;
        }
        if (mode == FollowMode.RETURN) {
            this.target = p;
            return true;
        }
        if (npc.distanceToSqr(p) < (double) (startDist() * startDist())) {
            return false;
        }
        this.target = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // #3: сцена смерти началась посреди следования — немедленно останавливаемся.
        if (npc.isDeathStaged()) {
            return false;
        }
        if (npc.getTarget() != null && npc.getTarget().isAlive()) {
            return false; // бой начался посреди следования — уступаем боевой цели
        }
        Player p = npc.getFollowTarget();
        if (p == null || p != this.target || p.isSpectator() || npc.isPassenger()) {
            return false;
        }
        FollowMode mode = npc.getFollowMode();
        if (mode == FollowMode.RETURN) {
            return true;
        }
        if (mode != FollowMode.FOLLOW) {
            return false;
        }
        return npc.distanceToSqr(p) > (double) (stopDist() * stopDist());
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.returnTicks = 0;
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
        if (npc.getFollowMode() == FollowMode.RETURN) {
            tickReturn();
            return;
        }

        this.returnTicks = 0;
        npc.getLookControl().setLookAt(target, 10.0F, (float) npc.getMaxHeadXRot());
        double distSqr = npc.distanceToSqr(target);
        float teleport = teleportDist();
        boolean far = distSqr >= (double) (teleport * teleport);
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = adjustedTickDelay(10);
            if (far && npc.shouldTeleportFollow() && tryTeleportNearTarget()) {
                return;
            }
            moveToWithSpacing(speedFor(distSqr));
        }
        npc.setSprinting(isSprintSpeed(distSqr));
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

    private void moveToWithSpacing(double speed) {
        if (npc.cfgGroupSpacing()) {
            double ang = (npc.getId() % 8) / 8.0 * Math.PI * 2.0;
            double r = 1.3D;
            navigation.moveTo(target.getX() + Math.cos(ang) * r, target.getY(),
                    target.getZ() + Math.sin(ang) * r, speed);
        } else {
            navigation.moveTo(target, speed);
        }
    }

    private void tickReturn() {
        npc.setSprinting(false);
        this.returnTicks++;
        if (npc.distanceToSqr(target) >= 12.0 * 12.0 || this.returnTicks > 100) {
            teleportHomeAndFinish();
            return;
        }
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = adjustedTickDelay(10);
            Vec3 dir = npc.position().subtract(target.position());
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(1, 0, 0);
            }
            Vec3 goal = target.position().add(dir.normalize().scale(13.0));
            if (!navigation.moveTo(goal.x, goal.y, goal.z, 1.0D)) {
                teleportHomeAndFinish();
            }
        }
    }

    private void teleportHomeAndFinish() {
        BlockPos home = npc.getFollowReturnPos();
        if (home != null) {
            doTeleport(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        }
        npc.finishReturn();
    }

    private boolean tryTeleportNearTarget() {
        if (npc.cfgTeleportOutOfSight() && isVisibleToTarget()) {
            return false;
        }
        Vec3 back = target.getLookAngle().scale(-1.0);
        BlockPos base = target.blockPosition();
        for (int i = 0; i < 12; i++) {
            int dx = randomInclusive(-3, 3);
            int dy = randomInclusive(-1, 1);
            int dz = randomInclusive(-3, 3);
            if (dx * back.x + dz * back.z <= 0.0D) {
                continue;
            }
            if (tryTeleport(new BlockPos(base.getX() + dx, base.getY() + dy, base.getZ() + dz))) {
                return true;
            }
        }

        for (int i = 0; i < 8; i++) {
            BlockPos pos = base.offset(randomInclusive(-3, 3), randomInclusive(-1, 1), randomInclusive(-3, 3));
            if (tryTeleport(pos)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVisibleToTarget() {
        Vec3 eye = target.getEyePosition();
        Vec3 toNpc = npc.getEyePosition().subtract(eye);
        if (toNpc.lengthSqr() > 48.0 * 48.0) {
            return false;
        }
        if (toNpc.normalize().dot(target.getLookAngle()) < 0.5D) {
            return false;
        }
        HitResult hit = npc.level().clip(new ClipContext(eye, npc.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
        return hit.getType() == HitResult.Type.MISS;
    }

    private boolean tryTeleport(BlockPos pos) {
        if (Math.abs(pos.getX() + 0.5D - target.getX()) < 2.0D
                && Math.abs(pos.getZ() + 0.5D - target.getZ()) < 2.0D) {
            return false;
        }
        if (!canTeleportTo(pos)) {
            return false;
        }
        doTeleport(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return true;
    }

    private void doTeleport(double x, double y, double z) {
        if (npc.cfgTeleportFx() && npc.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, npc.getX(), npc.getY() + 1.0D, npc.getZ(),
                    8, 0.25D, 0.4D, 0.25D, 0.01D);
        }
        npc.moveTo(x, y, z, npc.getYRot(), npc.getXRot());
        this.navigation.stop();
        if (npc.cfgTeleportFx() && npc.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, x, y + 1.0D, z, 8, 0.25D, 0.4D, 0.25D, 0.01D);
            sl.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.35F, 1.4F);
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        BlockPathTypes type = WalkNodeEvaluator.getBlockPathTypeStatic(npc.level(), pos.mutable());
        if (type != BlockPathTypes.WALKABLE) {
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
