package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.entity.CreatureType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;

import java.util.EnumSet;
import java.util.Set;

/**
 * Цель, которую вешаем МОБу-защитнику (напр. железному голему) на спавне: заставляет его нападать на
 * враждебного моба, который сейчас атакует защищаемого NPC (по «Типу существа», 0.9.5 #4).
 * Аналог ванильного DefendVillageTargetGoal, но без привязки к деревне — по нашим правилам типа.
 *
 * Цель ссылки на NPC не держит; кандидат — враждебный моб, чья текущая цель = защищаемый NPC поблизости.
 */
public class WhCreatureDefendGoal extends NearestAttackableTargetGoal<Mob> {

    /** Приоритет в targetSelector: как у ванильных target-целей голема. */
    public static final int PRIORITY = 3;

    /** Объединение всех групп-защитников по всем типам — фильтр «стоит ли вешать цель мобу». */
    private static final Set<MobGroup> DEFENDER_GROUPS;

    static {
        EnumSet<MobGroup> s = EnumSet.noneOf(MobGroup.class);
        for (CreatureType t : CreatureType.values()) {
            s.addAll(t.defenders());
        }
        DEFENDER_GROUPS = s;
    }

    public WhCreatureDefendGoal(Mob defender) {
        super(defender, Mob.class, 10, true, false,
                living -> living instanceof Mob attacker
                        && attacker instanceof Enemy
                        && attacker.getTarget() instanceof CompanionEntity npc
                        && defends(defender, npc));
    }

    @Override
    public boolean canUse() {
        return CreatureAggroState.anyLoaded() && super.canUse();
    }

    /** Может ли моб быть защитником хоть для какого-то типа — нужно ли вешать ему цель. */
    public static boolean isPotentialDefender(Entity e) {
        for (MobGroup g : DEFENDER_GROUPS) {
            if (g.matches(e)) {
                return true;
            }
        }
        return false;
    }

    /** Защищает ли этот моб данного NPC (его тип назначил группу моба защитником). */
    private static boolean defends(Mob defender, CompanionEntity npc) {
        for (MobGroup g : npc.effectiveDefenders()) {
            if (g.matches(defender)) {
                return true;
            }
        }
        return false;
    }
}
