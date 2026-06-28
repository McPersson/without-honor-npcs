package com.withouthonor.npcs.common.dialogue;

import javax.annotation.Nullable;

public enum EmoteIcon {
    EXCLAIM("exclaim", "wh_npcs.ui.emote_icon.exclaim"),
    QUESTION("question", "wh_npcs.ui.emote_icon.question"),
    HEART("heart", "wh_npcs.ui.emote_icon.heart"),
    ANGER("anger", "wh_npcs.ui.emote_icon.anger"),
    SWEAT("sweat", "wh_npcs.ui.emote_icon.sweat"),
    NOTE("note", "wh_npcs.ui.emote_icon.note"),
    ELLIPSIS("ellipsis", "wh_npcs.ui.emote_icon.ellipsis");

    public static final int COUNT = values().length;

    private final String id;
    private final String label;

    EmoteIcon(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public int atlasIndex() {
        return ordinal();
    }

    public static EmoteIcon byId(@Nullable String id) {
        if (id != null) {
            for (EmoteIcon icon : values()) {
                if (icon.id.equals(id)) {
                    return icon;
                }
            }
        }
        return EXCLAIM;
    }

    public static EmoteIcon byOrdinal(int ordinal) {
        EmoteIcon[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : EXCLAIM;
    }
}
