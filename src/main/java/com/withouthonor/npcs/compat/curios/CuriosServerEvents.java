package com.withouthonor.npcs.compat.curios;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CuriosServerEvents {

    // HIGH: строго ДО applyCombatProfile в ServerEvents (NORMAL) — resolveExtraSpells при сборке
    // магических целей читает Curios-слоты, к этому моменту они должны быть восстановлены из NBT.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof CompanionEntity npc) {
            npc.reapplyCuriosOnLoad();
        }
    }
}
