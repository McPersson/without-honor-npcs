package com.withouthonor.npcs.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class VanillaUIHelper {

    public static int BG_DARK = 0xCC000000;
    public static int BG_PANEL = 0xFF2B2B2B;
    public static int BG_INNER = 0xFF1A1A1A;
    public static int BG_SELECTED = 0xFF4A4A4A;
    public static int BG_HOVERED = 0xFF333333;
    public static int BG_HEADER = 0xFF1E1E1E;
    public static int BG_ITEM_SLOT = 0xFF3A3A3A;

    public static int BORDER_LIGHT = 0xFF5A5A5A;
    public static int BORDER_DARK = 0xFF1E1E1E;
    public static int BORDER_OUTER = 0xFF000000;
    public static int BORDER_SEPARATOR = 0xFF3A3A3A;

    public static int TEXT_WHITE = 0xFFFFFFFF;
    public static int TEXT_GRAY = 0xFFA0A0A0;
    public static int TEXT_DARK_GRAY = 0xFF707070;
    public static int TEXT_YELLOW = 0xFFFFFF55;
    public static int TEXT_GREEN = 0xFF55FF55;
    public static int TEXT_RED = 0xFFFF5555;
    public static int TEXT_AQUA = 0xFF55FFFF;
    public static int TEXT_GOLD = 0xFFFFAA00;
    public static int TEXT_STATUS = 0xFF7F7F7F;

    private static int BTN_FACE = 0xFF555555;
    private static int BTN_FACE_HOVER = 0xFF6E6E6E;
    private static int BTN_EDGE_LIGHT = 0xFF8A8A8A;
    private static int BTN_EDGE_LIGHT_HOVER = 0xFF9E9E9E;
    private static int BTN_EDGE_DARK = 0xFF2A2A2A;
    private static int BTN_EDGE_DARK_HOVER = 0xFF3A3A3A;

    private static final net.minecraft.resources.ResourceLocation WIDGETS =
            new net.minecraft.resources.ResourceLocation("textures/gui/widgets.png");

    public enum Theme {DARK, VANILLA, COFFEE}

    private static Theme theme = Theme.DARK;

    public static Theme theme() {
        return theme;
    }

    public static void setTheme(Theme t) {
        theme = t;
        applyPalette(t);
    }

    private static void applyPalette(Theme t) {
        if (t == Theme.COFFEE) {
            BG_DARK = 0xCC000000;
            BG_PANEL = 0xFF2B2926;
            BG_INNER = 0xFF1E1C19;
            BG_SELECTED = 0xFF3F3B34;
            BG_HOVERED = 0xFF35322C;
            BG_HEADER = 0xFF211F1B;
            BG_ITEM_SLOT = 0xFF1A1815;
            BORDER_LIGHT = 0xFF4A463F;
            BORDER_DARK = 0xFF141210;
            BORDER_OUTER = 0xFF0C0B0A;
            BORDER_SEPARATOR = 0xFF3A3732;
            TEXT_WHITE = 0xFFECE9E0;
            TEXT_GRAY = 0xFFB0A99C;
            TEXT_DARK_GRAY = 0xFF8A8478;
            TEXT_YELLOW = 0xFFD97757;
            TEXT_GREEN = 0xFF8FBF6F;
            TEXT_RED = 0xFFE0705A;
            TEXT_AQUA = 0xFF7FB4C9;
            TEXT_GOLD = 0xFFE0A062;
            TEXT_STATUS = 0xFF8A8478;
            BTN_FACE = 0xFF3A3832;
            BTN_FACE_HOVER = 0xFF47433B;
            BTN_EDGE_LIGHT = 0xFF565249;
            BTN_EDGE_LIGHT_HOVER = 0xFF645F54;
            BTN_EDGE_DARK = 0xFF1E1C18;
            BTN_EDGE_DARK_HOVER = 0xFF28251F;
        } else {
            BG_DARK = 0xCC000000;
            BG_PANEL = 0xFF2B2B2B;
            BG_INNER = 0xFF1A1A1A;
            BG_SELECTED = 0xFF4A4A4A;
            BG_HOVERED = 0xFF333333;
            BG_HEADER = 0xFF1E1E1E;
            BG_ITEM_SLOT = 0xFF3A3A3A;
            BORDER_LIGHT = 0xFF5A5A5A;
            BORDER_DARK = 0xFF1E1E1E;
            BORDER_OUTER = 0xFF000000;
            BORDER_SEPARATOR = 0xFF3A3A3A;
            TEXT_WHITE = 0xFFFFFFFF;
            TEXT_GRAY = 0xFFA0A0A0;
            TEXT_DARK_GRAY = 0xFF707070;
            TEXT_YELLOW = 0xFFFFFF55;
            TEXT_GREEN = 0xFF55FF55;
            TEXT_RED = 0xFFFF5555;
            TEXT_AQUA = 0xFF55FFFF;
            TEXT_GOLD = 0xFFFFAA00;
            TEXT_STATUS = 0xFF7F7F7F;
            BTN_FACE = 0xFF555555;
            BTN_FACE_HOVER = 0xFF6E6E6E;
            BTN_EDGE_LIGHT = 0xFF8A8A8A;
            BTN_EDGE_LIGHT_HOVER = 0xFF9E9E9E;
            BTN_EDGE_DARK = 0xFF2A2A2A;
            BTN_EDGE_DARK_HOVER = 0xFF3A3A3A;
        }
    }

    public static void drawRaisedFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, BORDER_OUTER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER_OUTER);
        g.fill(x, y, x + 1, y + h, BORDER_OUTER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER_OUTER);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, BORDER_LIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, BORDER_LIGHT);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BORDER_DARK);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BORDER_DARK);
    }

    public static void drawInsetFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, BORDER_OUTER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER_OUTER);
        g.fill(x, y, x + 1, y + h, BORDER_OUTER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER_OUTER);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, BORDER_DARK);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, BORDER_DARK);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BORDER_LIGHT);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BORDER_LIGHT);
    }

    public static void drawWindow(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG_PANEL);
        drawRaisedFrame(g, x, y, w, h);
    }

    public static void drawContentPanel(GuiGraphics g, int x, int y, int w, int h) {
        drawInsetFrame(g, x, y, w, h);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, BG_INNER);
    }

    public static void drawButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        if (theme == Theme.VANILLA && w >= 16 && h >= 14) {

            g.setColor(1F, 1F, 1F, 1F);
            g.blitNineSliced(WIDGETS, x, y, w, h, 20, 4, 200, 20, 0, hovered ? 86 : 66);
            return;
        }
        int bg = hovered ? BTN_FACE_HOVER : BTN_FACE;
        g.fill(x, y, x + w, y + h, bg);
        int light = hovered ? BTN_EDGE_LIGHT_HOVER : BTN_EDGE_LIGHT;
        g.fill(x, y, x + w, y + 1, light);
        g.fill(x, y, x + 1, y + h, light);
        int dark = hovered ? BTN_EDGE_DARK_HOVER : BTN_EDGE_DARK;
        g.fill(x, y + h - 1, x + w, y + h, dark);
        g.fill(x + w - 1, y, x + w, y + h, dark);
    }

    public static void drawSeparator(GuiGraphics g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 1, BORDER_SEPARATOR);
    }

    public static void drawSmallButton(GuiGraphics g, Font font, String label,
                                       int x, int y, int w, boolean hovered, int color) {
        drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5, hovered ? TEXT_YELLOW : color);
    }

    public static void drawCheck(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y + 4, x + 2, y + 6, c);
        g.fill(x + 2, y + 6, x + 4, y + 8, c);
        g.fill(x + 4, y + 4, x + 6, y + 6, c);
        g.fill(x + 6, y + 2, x + 8, y + 4, c);
        g.fill(x + 8, y, x + 10, y + 2, c);
    }

    public static void drawRenameIcon(GuiGraphics g, Font font, int x, int y, int color) {
        g.drawString(font, "✎", x, y, color, false);
    }

    public static void drawItemSlot(GuiGraphics g, int x, int y) {
        drawItemSlot(g, x, y, false);
    }

    public static void drawItemSlot(GuiGraphics g, int x, int y, boolean highlighted) {
        int bg = highlighted ? 0xFF5A5A5A : BG_ITEM_SLOT;
        g.fill(x, y, x + 18, y + 18, bg);
        drawInsetFrame(g, x, y, 18, 18);
    }

    public static void drawProgressBar(GuiGraphics g, int x, int y, int w, int h,
                                       float progress, int fillColor) {
        drawInsetFrame(g, x, y, w, h);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, BG_INNER);
        int fillWidth = (int) ((w - 4) * Math.min(1.0f, Math.max(0.0f, progress)));
        if (fillWidth > 0) {
            g.fill(x + 2, y + 2, x + 2 + fillWidth, y + h - 2, fillColor);
        }
    }

    public static void drawTab(GuiGraphics g, int x, int y, int w, int h,
                               boolean active, boolean hovered) {
        drawButton(g, x, y, w, h, active || hovered);
    }

    public static void drawScrollbar(GuiGraphics g, int x, int y, int h,
                                     int totalRows, int visibleRows, int scroll) {
        if (totalRows <= visibleRows) {
            return;
        }
        g.fill(x, y, x + 3, y + h, BG_INNER);
        int barH = Math.max(10, h * visibleRows / totalRows);
        int barY = y + (h - barH) * scroll / Math.max(1, totalRows - visibleRows);
        g.fill(x, barY, x + 3, barY + barH, BORDER_LIGHT);
    }

    public static void drawScrollbar(GuiGraphics g, int x, int y, int h,
                                     int totalRows, int visibleRows, int scroll,
                                     ScrollDrag drag, java.util.function.IntConsumer apply) {
        drawScrollbar(g, x, y, h, totalRows, visibleRows, scroll);
        if (drag != null) {
            drag.arm(x, y, h, totalRows, visibleRows, apply);
        }
    }
}
