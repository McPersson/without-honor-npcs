package com.withouthonor.npcs.client;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class ClientBubbles {

    private record Bubble(String text, long expireAtMillis) {
    }

    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();

    private ClientBubbles() {
    }

    public static void accept(int entityId, String text, int durationTicks) {
        BUBBLES.put(entityId, new Bubble(text, System.currentTimeMillis() + durationTicks * 50L));
    }

    @Nullable
    public static String get(int entityId) {
        Bubble bubble = BUBBLES.get(entityId);
        if (bubble == null) {
            return null;
        }
        if (System.currentTimeMillis() >= bubble.expireAtMillis()) {
            BUBBLES.remove(entityId);
            return null;
        }
        return bubble.text();
    }

    public static void remove(int entityId) {
        BUBBLES.remove(entityId);
    }

    public static void clear() {
        BUBBLES.clear();
    }
}
