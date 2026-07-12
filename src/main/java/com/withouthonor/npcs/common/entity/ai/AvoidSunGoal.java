package com.withouthonor.npcs.common.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;

/**
 * «Бояться солнца»: как ванильный {@link FleeSunGoal}, но БЕЗ требования гореть (isOnFire). Ваниль
 * убегает в тень только пока горит — а наш NPC не обязан быть нежитью. Поиск тени и движение
 * наследуем (setWantedPos/start/getHidePos), убираем лишь проверку isOnFire в canUse.
 * Шлем-проверку тоже снимаем: страх солнца не про защиту головы, а про сам дневной свет.
 */
public class AvoidSunGoal extends FleeSunGoal {

    public AvoidSunGoal(PathfinderMob mob, double speed) {
        super(mob, speed);
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) {
            return false;
        }
        if (!this.mob.level().isDay()) {
            return false;
        }
        if (!this.mob.level().canSeeSky(this.mob.blockPosition())) {
            return false;
        }
        return setWantedPos();
    }
}
