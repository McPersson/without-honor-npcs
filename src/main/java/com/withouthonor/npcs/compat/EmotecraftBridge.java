package com.withouthonor.npcs.compat;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

public interface EmotecraftBridge {

    boolean isAvailable();

    List<EmoteRef> listEmotes();

    void playOn(CompanionEntity npc, String emoteId, String emoteName, String emoteAuthor);

    void stopOn(CompanionEntity npc);

    record EmoteRef(String id, String name, String author, String icon,
                    @Nullable ResourceLocation iconTexture) {
    }
}
