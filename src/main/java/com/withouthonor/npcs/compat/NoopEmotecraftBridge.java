package com.withouthonor.npcs.compat;

import com.withouthonor.npcs.common.entity.CompanionEntity;

import java.util.List;

public final class NoopEmotecraftBridge implements EmotecraftBridge {

    public static final NoopEmotecraftBridge INSTANCE = new NoopEmotecraftBridge();

    private NoopEmotecraftBridge() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<EmoteRef> listEmotes() {
        return List.of();
    }

    @Override
    public void playOn(CompanionEntity npc, String emoteId, String emoteName, String emoteAuthor) {
    }

    @Override
    public void stopOn(CompanionEntity npc) {
    }
}
