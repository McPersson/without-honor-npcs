package com.withouthonor.npcs.compat.curios;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CuriosServerEvents {

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof CompanionEntity npc) {
            npc.reapplyCuriosOnLoad();
        }
    }
}
