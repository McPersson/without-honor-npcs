package com.withouthonor.npcs.client.gui.editor;

public final class EditorCodes {

    private static final String CODES = "0123456789abcdefklmnor";

    private EditorCodes() {
    }

    public static String toEditor(String stored) {
        return stored.replace("&", "&&").replace('§', '&');
    }

    public static String fromEditor(String edited) {
        StringBuilder out = new StringBuilder(edited.length());
        for (int i = 0; i < edited.length(); i++) {
            char c = edited.charAt(i);
            if (c == '&' && i + 1 < edited.length()) {
                char next = edited.charAt(i + 1);
                if (next == '&') {
                    out.append('&');
                    i++;
                    continue;
                }
                if (CODES.indexOf(Character.toLowerCase(next)) >= 0) {
                    out.append('§').append(next);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
