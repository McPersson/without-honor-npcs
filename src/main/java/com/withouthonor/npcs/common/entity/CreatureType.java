package com.withouthonor.npcs.common.entity;

import com.withouthonor.npcs.common.entity.ai.MobGroup;

import java.util.Set;

/**
 * «Тип существа» для реакции ванильных/модовых мобов на NPC (0.9.5 #4): как чужие мобы его
 * воспринимают — кто его атакует, кто защищает. Ортогонален {@link net.minecraft.world.entity.MobType}
 * (тот — про урон зачарований). Хранится в профиле строкой (id).
 *
 * ФАЗА 1: {@link #attackers} — группы мобов, которые нападают на NPC этого типа (читается
 * WhCreatureAggroGoal). Защитники и кастом-набор — ФАЗА 2.
 */
public enum CreatureType {
    // 4-й набор — npcEnemies: НАШИ NPC каких типов этот тип атакует по «природной вражде» (0.9.5).
    // Ссылки только на РАНЕЕ объявленные константы (VILLAGER) — иначе forward-reference не скомпилится.
    NEUTRAL("neutral", Set.of(), Set.of(), Set.of()),
    VILLAGER("villager", Set.of(MobGroup.MONSTER), Set.of(MobGroup.IRON_GOLEM), Set.of()),
    UNDEAD("undead", Set.of(MobGroup.IRON_GOLEM, MobGroup.SNOW_GOLEM), Set.of(), Set.of(VILLAGER)),
    PIGLIN("piglin", Set.of(MobGroup.WITHER_SKELETON), Set.of(), Set.of(VILLAGER)),
    BANDIT("bandit", Set.of(MobGroup.IRON_GOLEM), Set.of(), Set.of(VILLAGER)),
    // CUSTOM: группы-атакующие берутся из профиля (creature_custom_attackers); в природной вражде не участвует.
    CUSTOM("custom", Set.of(), Set.of(), Set.of());

    private final String id;
    private final Set<MobGroup> attackers;
    private final Set<MobGroup> defenders;
    private final Set<CreatureType> npcEnemies;

    CreatureType(String id, Set<MobGroup> attackers, Set<MobGroup> defenders, Set<CreatureType> npcEnemies) {
        this.id = id;
        this.attackers = attackers;
        this.defenders = defenders;
        this.npcEnemies = npcEnemies;
    }

    /** Группы мобов, нападающих на NPC этого типа (у «нейтрала» и «кастома» — пусто). */
    public Set<MobGroup> attackers() {
        return attackers;
    }

    /** Группы мобов, защищающих NPC этого типа (напр. железный голем у «жителя»). */
    public Set<MobGroup> defenders() {
        return defenders;
    }

    /** Типы НАШИХ NPC, которых этот тип атакует по «природной вражде» (напр. нежить → житель). */
    public Set<CreatureType> npcEnemies() {
        return npcEnemies;
    }

    /** Есть ли у типа-«жертвы» естественные хищники (кто-то держит его в своём npcEnemies) — для страха. */
    public static boolean hasPredators(CreatureType prey) {
        for (CreatureType t : values()) {
            if (t.npcEnemies.contains(prey)) {
                return true;
            }
        }
        return false;
    }

    public String id() {
        return id;
    }

    public String nameKey() {
        return "wh_npcs.ui.creature_type.type_" + id;
    }

    public String descKey() {
        return "wh_npcs.ui.creature_type.desc_" + id;
    }

    public static CreatureType byId(String id) {
        for (CreatureType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return NEUTRAL;
    }

    public CreatureType next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
