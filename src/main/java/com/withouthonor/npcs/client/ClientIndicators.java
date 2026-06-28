package com.withouthonor.npcs.client;

import com.withouthonor.npcs.common.dialogue.EmoteIcon;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class ClientIndicators {

    private static final Map<Integer, EmoteIcon> MAP = new HashMap<>();

    private ClientIndicators() {
    }

    public static void accept(int entityId, int indicator) {
        if (indicator <= 0) {
            MAP.remove(entityId);
        } else {
            MAP.put(entityId, EmoteIcon.byOrdinal(indicator - 1));
        }
    }

    @Nullable
    public static EmoteIcon get(int entityId) {
        return MAP.get(entityId);
    }

    public static void remove(int entityId) {
        MAP.remove(entityId);
    }

    public static void clear() {
        MAP.clear();
    }
}
