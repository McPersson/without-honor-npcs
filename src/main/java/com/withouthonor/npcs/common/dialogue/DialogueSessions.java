package com.withouthonor.npcs.common.dialogue;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DialogueSessions {

    public record Session(UUID npcUuid, String dialogueId, String nodeId) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private DialogueSessions() {
    }

    public static void put(UUID player, Session session) {
        SESSIONS.put(player, session);
    }

    @Nullable
    public static Session get(UUID player) {
        return SESSIONS.get(player);
    }

    public static void remove(UUID player) {
        SESSIONS.remove(player);
    }

    public static void clearAll() {
        SESSIONS.clear();
    }

    public static void renameDialogue(String oldId, String newId) {
        for (Map.Entry<UUID, Session> e : SESSIONS.entrySet()) {
            Session s = e.getValue();
            if (s.dialogueId().equals(oldId)) {
                e.setValue(new Session(s.npcUuid(), newId, s.nodeId()));
            }
        }
    }
}
