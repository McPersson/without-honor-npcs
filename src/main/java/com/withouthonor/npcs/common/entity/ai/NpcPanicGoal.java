package com.withouthonor.npcs.common.entity.ai;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.PanicGoal;

/**
 * Паника NPC без опоры на lastHurtByMob. Ванильный {@link PanicGoal#shouldPanic()} проверяет
 * {@code getLastHurtByMob() != null || isFreezing() || isOnFire()}, но lastHurtByMob читает и
 * HurtByTargetGoal, поэтому провокация (suppressAggro) чистит его ВСЕГДА — иначе боевой паникёр
 * агрился с первого удара. Триггером паники служит метка фактического урона
 * {@link CompanionEntity#getRecentlyPanicHurtTick()} (ставится в hurt() от ЛЮБОГО урона, как ваниль);
 * ветки freezing/fire сохранены как в ванили.
 */
public class NpcPanicGoal extends PanicGoal {

    /** Окно паники после урона, тики. Дальше NPC успокаивается (ваниль гаснет по концу пути). */
    private static final int PANIC_WINDOW_TICKS = 40;

    private final CompanionEntity npc;

    public NpcPanicGoal(CompanionEntity npc, double speedModifier) {
        super(npc, speedModifier);
        this.npc = npc;
    }

    @Override
    protected boolean shouldPanic() {
        return npc.tickCount - npc.getRecentlyPanicHurtTick() < PANIC_WINDOW_TICKS
                || npc.isFreezing() || npc.isOnFire();
    }
}
