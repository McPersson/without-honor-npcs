package com.withouthonor.npcs.client;

import com.withouthonor.npcs.client.gui.MonologueScreen;
import com.withouthonor.npcs.common.dialogue.action.MonologueLine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class ClientMonologue {

    private static final long INTRO_MS = 320L;
    private static final long OUTRO_MS = 260L;
    private static final long CHAR_MS = 25L;

    private static boolean active;
    private static List<MonologueLine> lines = List.of();
    private static int page;

    private static long openMs;
    private static long pageStartMs;
    private static boolean closing;
    private static long closeMs;

    private ClientMonologue() {
    }

    public static void start(List<MonologueLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        ClientMonologue.lines = lines;
        page = 0;
        openMs = System.currentTimeMillis();
        pageStartMs = openMs + INTRO_MS;
        closing = false;
        active = true;
    }

    public static void render(GuiGraphics g, Font font, int screenW, int screenH) {
        if (!active) {
            return;
        }
        long now = System.currentTimeMillis();

        int barH = 62;
        int barX = 20;
        int barW = screenW - 40;
        int barY = screenH - barH - 10;
        int blockH = screenH - (barY - 6);

        float slide;
        if (closing) {
            float t = clamp01((now - closeMs) / (float) OUTRO_MS);
            if (t >= 1f) {
                active = false;
                return;
            }
            slide = 1f - easeIn(t);
        } else {
            slide = easeOut(clamp01((now - openMs) / (float) INTRO_MS));

            String cur = lines.get(page).text();
            long typedDone = pageStartMs + (long) cur.length() * CHAR_MS;
            long hold = 1200L + 40L * cur.length();
            if (now >= typedDone + hold) {
                if (page + 1 < lines.size()) {
                    page++;
                    pageStartMs = now;
                } else {
                    closing = true;
                    closeMs = now;
                }
            }
        }

        int yOff = Math.round((1f - slide) * blockH);
        MonologueLine line = lines.get(page);
        String full = line.text();
        int shown = clamp(0, full.length(), (now - pageStartMs) / CHAR_MS);

        g.pose().pushPose();
        g.pose().translate(0f, 0f, 300f);
        MonologueScreen.drawBar(g, font, barX, barY + yOff, barW, barH,
                line.name(), line.portrait(), full.substring(0, shown));
        g.pose().popPose();
    }

    private static int clamp(int lo, int hi, long v) {
        return (int) Math.max(lo, Math.min(hi, v));
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float easeIn(float t) {
        return t * t * t;
    }
}
