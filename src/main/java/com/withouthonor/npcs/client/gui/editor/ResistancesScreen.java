package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

public class ResistancesScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 24;
    private static final int WIN_W = 300;
    private static final int WIN_H = HEADER_H + 14 + 7 * ROW_H + 28;

    private record Row(String label, String key, int min, int max, boolean asFloat, String tooltip) {}

    private static final Row[] ROWS = {
            new Row("wh_npcs.ui.resist.row.recoil", "kb_resistance", 0, 100, true,
                    "wh_npcs.ui.resist.tip.recoil"),
            new Row("wh_npcs.ui.resist.row.melee", "res_melee", -100, 100, false,
                    "wh_npcs.ui.resist.tip.melee"),
            new Row("wh_npcs.ui.resist.row.projectile", "res_projectile", -100, 100, false,
                    "wh_npcs.ui.resist.tip.projectile"),
            new Row("wh_npcs.ui.resist.row.explosion", "res_explosion", -100, 100, false,
                    "wh_npcs.ui.resist.tip.explosion"),
            new Row("wh_npcs.ui.resist.row.fire", "res_fire", -100, 100, false,
                    "wh_npcs.ui.resist.tip.fire"),
            new Row("wh_npcs.ui.resist.row.fall", "res_fall", -100, 100, false,
                    "wh_npcs.ui.resist.tip.fall"),
            new Row("wh_npcs.ui.resist.row.magic", "res_magic", -100, 100, false,
                    "wh_npcs.ui.resist.tip.magic"),
    };

    private final Screen parent;
    private final JsonObject profileJson;
    private final int[] values = new int[ROWS.length];

    private int activeRow = -1;
    private int winX, winY, winW, winH, top, bottomY;
    private String hoverTooltip;

    public ResistancesScreen(@Nullable Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.resist.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        for (int i = 0; i < ROWS.length; i++) {
            Row r = ROWS[i];
            if (profileJson.has(r.key)) {
                values[i] = r.asFloat
                        ? Math.round(profileJson.get(r.key).getAsFloat() * 100F)
                        : profileJson.get(r.key).getAsInt();
            }
            values[i] = Mth.clamp(values[i], r.min, r.max);
        }
    }

    @Override
    protected int designW() {
        return WIN_W;
    }

    @Override
    protected int designH() {
        return WIN_H;
    }

    private void recalc() {
        winW = WIN_W;
        winH = HEADER_H + 14 + ROWS.length * ROW_H + 28;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        top = winY + HEADER_H + 10;
        bottomY = winY + winH - PAD - 18;
    }

    private int trackX() {
        return winX + PAD + 88;
    }

    private int trackW() {
        return winX + winW - PAD - 40 - trackX();
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.resist.title").getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        hoverTooltip = null;
        int tx = trackX();
        int tw = trackW();
        for (int i = 0; i < ROWS.length; i++) {
            Row r = ROWS[i];
            int y = top + i * ROW_H;
            int cy = y + 4;
            g.drawString(font, Component.translatable(r.label).getString(), winX + PAD, y, VanillaUIHelper.TEXT_GRAY, false);

            g.fill(tx, cy - 1, tx + tw, cy + 2, VanillaUIHelper.BG_INNER);
            g.fill(tx, cy - 1, tx + tw, cy, VanillaUIHelper.BORDER_OUTER);

            int handleX = tx + Math.round((values[i] - r.min) / (float) (r.max - r.min) * tw);
            int zeroX = tx + Math.round((0 - r.min) / (float) (r.max - r.min) * tw);
            if (!r.asFloat) {
                g.fill(zeroX, cy, zeroX + 1, cy + 1, VanillaUIHelper.BORDER_LIGHT);
            }
            int fillC = values[i] >= 0 ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_RED;
            int a = Math.min(zeroX, handleX);
            int b = Math.max(zeroX, handleX);
            if (b > a) {
                g.fill(a, cy - 1, b, cy + 2, fillC);
            }

            boolean rowHover = isOver(mouseX, mouseY, tx, y - 2, tw, ROW_H - 4);
            VanillaUIHelper.drawButton(g, handleX - 2, cy - 4, 5, 9, rowHover || activeRow == i);

            String vt = (values[i] > 0 && !r.asFloat ? "+" : "") + values[i] + "%";
            g.drawString(font, vt, tx + tw + 6, y, VanillaUIHelper.TEXT_AQUA, false);

            if (isOver(mouseX, mouseY, winX + PAD, y - 2, winW - PAD * 2, ROW_H - 4)) {
                hoverTooltip = Component.translatable(r.tooltip).getString();
            }
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.resist.hint").getString(),
                winX + PAD, bottomY + 4, VanillaUIHelper.TEXT_DARK_GRAY, false);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(),
                winX + winW - PAD - 70, bottomY, 70, mouseX, mouseY);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0 && isOver(mouseX, mouseY, winX + winW - PAD - 70, bottomY, 70, 18)) {
            onClose();
            return true;
        }
        int hit = rowAt(mouseX, mouseY);
        if (hit >= 0) {
            if (button == 1) {
                values[hit] = 0;
                return true;
            }
            if (button == 0) {
                activeRow = hit;
                setFromMouse(hit, mouseX);
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && activeRow >= 0) {
            setFromMouse(activeRow, mouseX);
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            activeRow = -1;
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    private int rowAt(double mouseX, double mouseY) {
        for (int i = 0; i < ROWS.length; i++) {
            int y = top + i * ROW_H;
            if (isOver(mouseX, mouseY, trackX() - 4, y - 2, trackW() + 8, ROW_H - 4)) {
                return i;
            }
        }
        return -1;
    }

    private void setFromMouse(int i, double mouseX) {
        Row r = ROWS[i];
        float frac = Mth.clamp((float) (mouseX - trackX()) / trackW(), 0F, 1F);
        float raw = r.min + frac * (r.max - r.min);
        int step = hasShiftDown() ? 1 : 5;
        values[i] = Mth.clamp(Math.round(raw / step) * step, r.min, r.max);
    }

    private void apply() {
        for (int i = 0; i < ROWS.length; i++) {
            Row r = ROWS[i];
            if (values[i] == 0) {
                profileJson.remove(r.key);
            } else if (r.asFloat) {
                profileJson.addProperty(r.key, values[i] / 100F);
            } else {
                profileJson.addProperty(r.key, values[i]);
            }
        }
    }

    @Override
    public void onClose() {
        apply();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
    }

    private void multilineTooltip(GuiGraphics g, String text, int mouseX, int mouseY) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
