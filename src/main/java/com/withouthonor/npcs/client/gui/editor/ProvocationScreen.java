package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Провокация + «напарник не ранит игрока» (#5/#6): NPC терпит случайные удары игрока до порога.
 * Пишет в profileJson: provoke_enabled/hits/hp_pct/window_s + forgive_after_s + provoke_ignore_escort
 * + escort_no_harm_owner. Значения читает CompanionProfile.fromJson.
 */
public class ProvocationScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int WIN_W = 320;
    private static final int WIN_H = 236;

    /** Ключи тултипов 4 числовых строк (порядок = порядок строк на экране). */
    private static final String[] NUM_TIP_KEYS = {
            "wh_npcs.ui.provoke.hits_tip", "wh_npcs.ui.provoke.hp_pct_tip",
            "wh_npcs.ui.provoke.window_tip", "wh_npcs.ui.provoke.forgive_tip"};

    private final Screen parent;
    private final JsonObject profileJson;

    @Nullable
    private EditBox hitsBox, hpBox, windowBox, forgiveBox;
    private final java.util.List<com.withouthonor.npcs.client.gui.NumberScrub> scrubs = new java.util.ArrayList<>();
    private int winX, winY;
    @Nullable
    private String tip;

    public ProvocationScreen(Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.provoke.title"));
        this.parent = parent;
        this.profileJson = profileJson;
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
        winX = (width - WIN_W) / 2;
        winY = (height - WIN_H) / 2;
    }

    private boolean flag(String key, boolean def) {
        return profileJson.has(key) ? profileJson.get(key).getAsBoolean() : def;
    }

    /** Чекбокс: пишем только НЕ-дефолт (как guard_spare_creepers), чтобы JSON был чистым. */
    private void toggleFlag(String key, boolean def) {
        boolean cur = flag(key, def);
        boolean next = !cur;
        if (next == def) {
            profileJson.remove(key);
        } else {
            profileJson.addProperty(key, next);
        }
    }

    private EditBox intBox(int x, int y, String key, int def, int min, int max) {
        EditBox box = addRenderableWidget(new SelectableEditBox(font, x, y, 40, 16, Component.literal(key)));
        box.setMaxLength(3);
        int value = profileJson.has(key) ? profileJson.get(key).getAsInt() : def;
        box.setValue(String.valueOf(value));
        box.setResponder(v -> {
            try {
                int parsed = Math.max(min, Math.min(max, Integer.parseInt(v.trim())));
                profileJson.addProperty(key, parsed);
            } catch (NumberFormatException ignored) {
                // оставляем прежнее значение, пока поле невалидно
            }
        });
        // Стандарт числовых полей: драг-скраб + −/+ + ПКМ-сброс; клампы == серверным (CompanionProfile).
        scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(box, min, max, 1, 0.2F,
                String.valueOf(def), true, () -> setFocused(null)));
        return box;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        scrubs.clear();
        int x = winX + PAD;
        hitsBox = intBox(x + 130, winY + 62, "provoke_hits", 3, 1, 10);
        hpBox = intBox(x + 130, winY + 82, "provoke_hp_pct", 15, 0, 100);
        windowBox = intBox(x + 130, winY + 102, "provoke_window_s", 10, 5, 60);
        forgiveBox = intBox(x + 130, winY + 122, "forgive_after_s", 20, 0, 120);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        tip = null;
        VanillaUIHelper.drawWindow(g, winX, winY, WIN_W, WIN_H);
        g.fill(winX + 2, winY + 2, winX + WIN_W - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, tr("wh_npcs.ui.provoke.title"), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        int x = winX + PAD;

        // Главный тумблер
        checkbox(g, x, winY + HEADER_H + 8, flag("provoke_enabled", true),
                "wh_npcs.ui.provoke.enabled", "wh_npcs.ui.provoke.enabled_tip", mouseX, mouseY);

        boolean on = flag("provoke_enabled", true);
        int labelColor = on ? VanillaUIHelper.TEXT_GRAY : VanillaUIHelper.TEXT_DARK_GRAY;
        // Числовые пороги (поля всегда активны — редактируются даже при выкл., но применяются только при вкл.)
        g.drawString(font, tr("wh_npcs.ui.provoke.hits"), x, winY + 66, labelColor, false);
        g.drawString(font, tr("wh_npcs.ui.provoke.hp_pct"), x, winY + 86, labelColor, false);
        g.drawString(font, tr("wh_npcs.ui.provoke.window"), x, winY + 106, labelColor, false);
        g.drawString(font, tr("wh_npcs.ui.provoke.forgive"), x, winY + 126, labelColor, false);
        // Единицы правее кнопки «+» скраба (поле x+130 w40 → «+» кончается на x+185)
        g.drawString(font, "%", x + 189, winY + 86, labelColor, false);
        g.drawString(font, tr("wh_npcs.ui.provoke.sec"), x + 189, winY + 106, labelColor, false);
        g.drawString(font, tr("wh_npcs.ui.provoke.sec"), x + 189, winY + 126, labelColor, false);
        for (var s : scrubs) {
            s.render(g, font, mouseX, mouseY);
        }
        // Тултипы числовых строк: метка + скраб + единица одной hover-зоной
        for (int i = 0; i < NUM_TIP_KEYS.length; i++) {
            if (isOver(mouseX, mouseY, x, winY + 62 + i * 20, 196, 16)) {
                tip = tr(NUM_TIP_KEYS[i]);
            }
        }

        checkbox(g, x, winY + 150, flag("provoke_ignore_escort", true),
                "wh_npcs.ui.provoke.ignore_escort", "wh_npcs.ui.provoke.ignore_escort_tip", mouseX, mouseY);
        checkbox(g, x, winY + 170, flag("escort_no_harm_owner", true),
                "wh_npcs.ui.provoke.no_harm_owner", "wh_npcs.ui.provoke.no_harm_owner_tip", mouseX, mouseY);

        drawBtn(g, tr("wh_npcs.ui.common.done"), winX + WIN_W - PAD - 70, winY + WIN_H - PAD - 18, 70, mouseX, mouseY);
    }

    private void checkbox(GuiGraphics g, int x, int y, boolean on, String labelKey, String tipKey,
                          int mouseX, int mouseY) {
        boolean h = isOver(mouseX, mouseY, x, y, 12, 12);
        VanillaUIHelper.drawButton(g, x, y, 12, 12, h);
        if (on) {
            VanillaUIHelper.drawCheck(g, x + 1, y + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, tr(labelKey), x + 16, y + 2, VanillaUIHelper.TEXT_GRAY, false);
        if (overCheckboxRow(mouseX, mouseY, x, y)) {
            tip = tr(tipKey);
        }
    }

    /** Единая зона чекбокса: галка 12×12 + подпись; hover-тултип и клик используют её одинаково. */
    private static boolean overCheckboxRow(double mx, double my, int x, int y) {
        return isOver(mx, my, x, y, 12, 12) || isOver(mx, my, x + 16, y, 260, 12);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        // Скрабы первыми: −/+/ПКМ-сброс должны перехватывать клик до прочих веток
        for (var s : scrubs) {
            if (s.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        int x = winX + PAD;
        if (button == 0 && isOver(mouseX, mouseY, winX + WIN_W - PAD - 70, winY + WIN_H - PAD - 18, 70, 18)) {
            onClose();
            return true;
        }
        if (button == 0) {
            if (overCheckboxRow(mouseX, mouseY, x, winY + HEADER_H + 8)) {
                toggleFlag("provoke_enabled", true);
                return true;
            }
            if (overCheckboxRow(mouseX, mouseY, x, winY + 150)) {
                toggleFlag("provoke_ignore_escort", true);
                return true;
            }
            if (overCheckboxRow(mouseX, mouseY, x, winY + 170)) {
                toggleFlag("escort_no_harm_owner", true);
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dx, double dy) {
        for (var s : scrubs) {
            if (s.mouseDragged(mouseX, mouseY, button)) {
                return true;
            }
        }
        return superMouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        for (var s : scrubs) {
            s.mouseReleased();
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (tip != null) {
            multilineTooltip(g, tip, mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY) {
        VanillaUIHelper.drawSmallButton(g, font, label, x, y, w,
                isOver(mouseX, mouseY, x, y, w, 18), VanillaUIHelper.TEXT_GREEN);
    }

    private void multilineTooltip(GuiGraphics g, String text, int mouseX, int mouseY) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        queueTooltip(lines);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
