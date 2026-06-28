package com.withouthonor.npcs.client;

import com.withouthonor.npcs.common.dialogue.EmoteIcon;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class ClientEmotes {

    private record Active(EmoteIcon icon, long expireAtMillis) {
    }

    private static final Map<Integer, Active> EMOTES = new HashMap<>();

    private ClientEmotes() {
    }

    public static void accept(int entityId, int iconOrdinal, int durationTicks) {
        EMOTES.put(entityId, new Active(EmoteIcon.byOrdinal(iconOrdinal),
                System.currentTimeMillis() + durationTicks * 50L));
    }

    @Nullable
    public static EmoteIcon get(int entityId) {
        Active active = EMOTES.get(entityId);
        if (active == null) {
            return null;
        }
        if (System.currentTimeMillis() >= active.expireAtMillis()) {
            EMOTES.remove(entityId);
            return null;
        }
        return active.icon();
    }

    public static void remove(int entityId) {
        EMOTES.remove(entityId);
    }

    public static void clear() {
        EMOTES.clear();
    }
}
