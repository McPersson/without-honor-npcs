package com.withouthonor.npcs.common.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Как ванильный {@link AvoidEntityGoal}, но скан цели идёт не каждый тик, а в среднем раз в
 * {@code interval} тиков — тот же приём, что {@code randomInterval} у
 * {@link net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal}. Нужно, чтобы «страх»
 * у десятков NPC-жертв не звал {@code getEntitiesOfClass} каждый кадр (агро/защита уже троттлятся).
 * На поведение не влияет: задержка обнаружения хищника ≤ interval тиков, а раз побежав, цель
 * продолжает по {@code canContinueToUse} без учёта интервала.
 */
public class ThrottledAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {

    private final int interval;
    /** Доп. гейт по состоянию САМОГО убегающего (предикат AvoidEntityGoal видит только цель).
     *  null = без гейта. Пример: жертва в бою (getTarget() != null) дерётся, а не бежит. */
    @Nullable
    private final BooleanSupplier selfGate;

    public ThrottledAvoidEntityGoal(PathfinderMob mob, Class<T> avoidClass, float maxDist,
                                    double walkSpeed, double sprintSpeed,
                                    Predicate<LivingEntity> avoidPredicate, int interval) {
        this(mob, avoidClass, maxDist, walkSpeed, sprintSpeed, avoidPredicate, interval, null);
    }

    public ThrottledAvoidEntityGoal(PathfinderMob mob, Class<T> avoidClass, float maxDist,
                                    double walkSpeed, double sprintSpeed,
                                    Predicate<LivingEntity> avoidPredicate, int interval,
                                    @Nullable BooleanSupplier selfGate) {
        super(mob, avoidClass, maxDist, walkSpeed, sprintSpeed, avoidPredicate);
        this.interval = Math.max(1, reducedTickDelay(interval));
        this.selfGate = selfGate;
    }

    @Override
    public boolean canUse() {
        if (selfGate != null && !selfGate.getAsBoolean()) {
            return false;
        }
        if (interval > 1 && this.mob.getRandom().nextInt(interval) != 0) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Гейт и на продолжение: если посреди бегства жертва взяла боевую цель — дерёмся, не бежим.
        if (selfGate != null && !selfGate.getAsBoolean()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
