package com.withouthonor.npcs.client;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ClientEmoteAnim {

    private ClientEmoteAnim() {
    }

    public static void accept(int entityId, String emoteId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity e = mc.level.getEntity(entityId);
        if (e instanceof CompanionEntity npc) {
            if (emoteId == null || emoteId.isEmpty()) {
                Compat.emotecraft().stopOn(npc);
            } else {
                Compat.emotecraft().playOn(npc, emoteId, "", "");
            }
        }
    }
}
