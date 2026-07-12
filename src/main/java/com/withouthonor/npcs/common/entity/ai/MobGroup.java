package com.withouthonor.npcs.common.entity.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.WitherSkeleton;

import java.util.function.Predicate;

/**
 * Группа мобов для правил «Типа существа» (0.9.5 #4): кем может быть атакующий/защитник NPC.
 * Предикаты по маркерам/классам — модовые мобы, наследующие их (напр. Enemy), подхватываются сами.
 */
public enum MobGroup {
    /** Любой враждебный моб (маркер {@link Enemy}): зомби, скелеты, иллагеры, пиглины, визер-скелеты… */
    MONSTER("monster", e -> e instanceof Enemy),
    WITHER_SKELETON("wither_skeleton", e -> e instanceof WitherSkeleton),
    IRON_GOLEM("iron_golem", e -> e instanceof IronGolem),
    SNOW_GOLEM("snow_golem", e -> e instanceof SnowGolem);

    /** Группы, доступные для ручного выбора в кастом-типе (в порядке показа). */
    public static final MobGroup[] SELECTABLE = {MONSTER, WITHER_SKELETON, IRON_GOLEM, SNOW_GOLEM};

    private final String id;
    private final Predicate<Entity> test;

    MobGroup(String id, Predicate<Entity> test) {
        this.id = id;
        this.test = test;
    }

    public String id() {
        return id;
    }

    public String nameKey() {
        return "wh_npcs.ui.creature_type.group_" + id;
    }

    public boolean matches(Entity e) {
        return test.test(e);
    }

    public static MobGroup byId(String id) {
        for (MobGroup g : values()) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }
}
