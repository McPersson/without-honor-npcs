package com.withouthonor.npcs.common.dialogue;

import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;

public final class Placeholders {

    private Placeholders() {
    }

    public static String apply(String text, DialogueCondition.Context ctx) {
        String result = text.replace("{player}", ctx.player().getGameProfile().getName());
        if (ctx.npc() != null) {
            result = result.replace("{npc}", ctx.npc().getName().getString());
        }
        if (result.contains("{var:")) {
            result = applyVars(result, ctx);
        }
        return result;
    }

    private static String applyVars(String text, DialogueCondition.Context ctx) {
        var psm = com.withouthonor.npcs.common.storage.PlayerStateManager.get(ctx.player().server);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int start = text.indexOf("{var:", i);
            if (start < 0) {
                sb.append(text, i, text.length());
                break;
            }
            int end = text.indexOf('}', start + 5);
            if (end < 0) {
                sb.append(text, i, text.length());
                break;
            }
            sb.append(text, i, start);
            String name = text.substring(start + 5, end).trim();
            sb.append(psm.getVar(ctx.player().getUUID(), name));
            i = end + 1;
        }
        return sb.toString();
    }
}
