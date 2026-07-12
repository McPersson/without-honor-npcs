package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Цель, которую мы вешаем ЧУЖОМУ мобу (не нашему NPC) на спавне: заставляет его считать NPC жертвой
 * по «Типу существа» (0.9.5 #4). Ванильные мобы фильтруют цели по жёсткому классу и наш NPC не видят —
 * эта цель добавляет их class-фильтр на {@link CompanionEntity} с предикатом по типу.
 *
 * Цель НЕ держит ссылку на конкретный NPC (сканирует ближайших через базу) — течь по выгруженным NPC
 * нечему; живёт вместе с мобом-владельцем.
 */
public class WhCreatureAggroGoal extends NearestAttackableTargetGoal<CompanionEntity> {

    /** Приоритет в targetSelector: чуть НИЖЕ игрока (у большинства враждебных мобов игрок = 2). */
    public static final int PRIORITY = 3;

    public WhCreatureAggroGoal(Mob mob) {
        // randomInterval 10 (как у ванильных target-целей), mustSee true (агрессия по линии видимости),
        // mustReach false. Предикат: цель — CompanionEntity, чей тип назначил эту группу атакующей.
        super(mob, CompanionEntity.class, 10, true, false,
                living -> living instanceof CompanionEntity npc && mobAttacks(mob, npc));
    }

    @Override
    public boolean canUse() {
        // Пока в мире нет NPC с непустым «типом существа» — не сканируем вовсе (дёшево на мобофермах).
        return CreatureAggroState.anyLoaded() && super.canUse();
    }

    /** Может ли моб входить хоть в какую-то группу — нужно ли вообще вешать ему цель (кастом = любая группа). */
    public static boolean isPotentialAttacker(Entity e) {
        for (MobGroup g : MobGroup.values()) {
            if (g.matches(e)) {
                return true;
            }
        }
        return false;
    }

    /** Нападает ли этот моб на NPC такого «типа существа» (у кастома — группы из профиля). */
    private static boolean mobAttacks(Mob attacker, CompanionEntity npc) {
        for (MobGroup g : npc.effectiveAttackers()) {
            if (g.matches(attacker)) {
                return true;
            }
        }
        return false;
    }
}
