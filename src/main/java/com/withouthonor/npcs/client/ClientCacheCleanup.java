package com.withouthonor.npcs.client;

import com.withouthonor.npcs.WHCompanions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WHCompanions.MODID, value = Dist.CLIENT)
public final class ClientCacheCleanup {

    private ClientCacheCleanup() {
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        int id = event.getEntity().getId();
        ClientBubbles.remove(id);
        ClientEmotes.remove(id);
        ClientIndicators.remove(id);
        com.withouthonor.npcs.client.render.CompanionOverlays.INSTANCE.clearEntity(id);
        if (com.withouthonor.npcs.compat.Compat.emotecraftLoaded()
                && com.withouthonor.npcs.compat.Compat.emotecraft()
                        instanceof com.withouthonor.npcs.compat.emotecraft.EmotecraftBridgeImpl emote) {
            emote.onEntityUnload(id);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientBubbles.clear();
            ClientEmotes.clear();
            ClientIndicators.clear();
            com.withouthonor.npcs.client.render.CompanionOverlays.INSTANCE.clearAll();
            if (com.withouthonor.npcs.compat.Compat.emotecraftLoaded()
                    && com.withouthonor.npcs.compat.Compat.emotecraft()
                            instanceof com.withouthonor.npcs.compat.emotecraft.EmotecraftBridgeImpl emote) {
                emote.clearLayers();
            }
        }
    }
}
