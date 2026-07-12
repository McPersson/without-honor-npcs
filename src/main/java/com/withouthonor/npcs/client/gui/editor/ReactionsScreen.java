package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.action.ActionTypes;
import com.withouthonor.npcs.common.dialogue.action.DialogueAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ReactionsScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int WIN_W = 320;
    /** Двухколоночная сетка настроек: подписи PAD..PAD+LABEL_W, контролы — единый левый край PAD+LABEL_W. */
    private static final int LABEL_W = 110;
    /** Фикс-ширина кнопки-контрола (пикер эмоции смерти). */
    private static final int CTRL_W = 160;
    /** NumberScrub: бокс 60 + кнопки −/+ по 12 с зазорами 3 → полная ширина 90. */
    private static final int SCRUB_BOX_W = 60;
    private static final int SCRUB_W = SCRUB_BOX_W + 30;
    /** Зазор между блоком событий и сеткой настроек (разделительная линия посередине). */
    private static final int SETTINGS_GAP = 10;
    // Шапка + 4 строки событий + зазор + 3 строки сетки (радиус/эмоция/длительность) + «Готово»
    private static final int WIN_H = HEADER_H + 12 + 4 * 24 + SETTINGS_GAP + 3 * 24 + 8 + 18 + PAD;

    private record Event(String key, String id) {}

    private static final Event[] EVENTS = {
            new Event("react_hurt", "hurt"),
            new Event("react_death", "death"),
            new Event("react_interact", "interact"),
            new Event("react_approach", "approach"),
    };

    private static String evLabel(int i) {
        return Component.translatable("wh_npcs.ui.reactions.ev." + EVENTS[i].id + ".label").getString();
    }

    private static String evDesc(int i) {
        return Component.translatable("wh_npcs.ui.reactions.ev." + EVENTS[i].id + ".desc").getString();
    }

    private final Screen parent;
    private final JsonObject profileJson;
    private final List<List<DialogueAction>> lists = new ArrayList<>();
    @Nullable
    private EditBox rangeBox;
    @Nullable
    private EditBox deathSecsBox;
    private final java.util.List<com.withouthonor.npcs.client.gui.NumberScrub> scrubs = new ArrayList<>();

    private int winX, winY, winW, winH;
    @Nullable
    private String hoverTooltip;

    public ReactionsScreen(Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.reactions.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        for (Event e : EVENTS) {
            List<DialogueAction> list = new ArrayList<>();
            if (profileJson.has(e.key)) {
                try {
                    list.addAll(ActionTypes.parseList(profileJson.getAsJsonArray(e.key)));
                } catch (Exception ignored) {

                }
            }
            lists.add(list);
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
        winH = WIN_H;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
    }

    /** Y первой строки блока событий. */
    private int eventsY0() {
        return winY + HEADER_H + 12;
    }

    /**
     * Y i-й строки сетки настроек (0 — радиус, 1 — эмоция смерти, 2 — длительность).
     * Единая формула для рендера, кликов и init — клик-зоны всегда совпадают с рендером.
     */
    private int settingsRowY(int i) {
        return eventsY0() + EVENTS.length * 24 + SETTINGS_GAP + i * 24;
    }

    /** Левый край колонки контролов. */
    private int ctrlX() {
        return winX + PAD + LABEL_W;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        scrubs.clear();
        // Строка 0 сетки: радиус подхода — NumberScrub (кламп 0..16, 0 = выкл),
        // та же ширина бокса, что у скраба длительности; бокс на ctrlX()+15 (слева кнопка «−»)
        rangeBox = addRenderableWidget(new SelectableEditBox(font, ctrlX() + 15,
                settingsRowY(0) + 1, SCRUB_BOX_W, 16, Component.empty()));
        rangeBox.setMaxLength(2);
        if (profileJson.has("react_approach_range") && profileJson.get("react_approach_range").getAsInt() > 0) {
            rangeBox.setValue(String.valueOf(profileJson.get("react_approach_range").getAsInt()));
        }
        scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(rangeBox, 0F, 16F, 1F, 0.1F,
                "0", true, () -> setFocused(null)));

        // #3 Длительность эмоции смерти (сек) — строка 2 сетки.
        float secs = profileJson.has("death_emote_secs") ? profileJson.get("death_emote_secs").getAsFloat() : 3.0F;
        deathSecsBox = addRenderableWidget(new SelectableEditBox(font, ctrlX() + 15,
                settingsRowY(2) + 1, SCRUB_BOX_W, 16, Component.empty()));
        deathSecsBox.setMaxLength(5);
        deathSecsBox.setValue(com.withouthonor.npcs.client.gui.NumberScrub.fmt(secs, false));
        deathSecsBox.setResponder(v -> {
            try {
                float s = Math.max(0.5F, Math.min(10.0F, Float.parseFloat(v.trim().replace(',', '.'))));
                profileJson.addProperty("death_emote_secs", s);
            } catch (NumberFormatException ignored) {
            }
        });
        scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(deathSecsBox, 0.5F, 10.0F, 0.5F, 0.5F,
                "3", false, () -> setFocused(null)));
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.reactions.header").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        String tooltip = null;
        int y0 = eventsY0();
        for (int i = 0; i < EVENTS.length; i++) {
            int y = y0 + i * 24;
            boolean hov = isOver(mouseX, mouseY, winX + PAD, y, winW - PAD * 2, 18);
            VanillaUIHelper.drawButton(g, winX + PAD, y, winW - PAD * 2, 18, hov);
            int cnt = lists.get(i).size();
            String none = Component.translatable("wh_npcs.ui.common.none").getString();
            String label = evLabel(i) + (cnt > 0 ? " §7(" + cnt + ")" : " §8(" + none + ")") + " §r→";
            g.drawCenteredString(font, label, winX + winW / 2, y + 5,
                    hov ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            if (hov) {
                tooltip = "§e" + evLabel(i) + "\n" + evDesc(i);
            }
        }

        // Разделитель между блоком событий и сеткой настроек
        int sepY = y0 + EVENTS.length * 24 + SETTINGS_GAP / 2 - 1;
        g.fill(winX + PAD, sepY, winX + winW - PAD, sepY + 1, VanillaUIHelper.BORDER_SEPARATOR);

        // Сетка настроек: подписи на PAD (ellipsize до LABEL_W-4), контролы от ctrlX();
        // hover-зона тултипа = подпись + контрол, те же формулы, что у кликов
        int cx = ctrlX();

        // Строка 0: радиус подхода (NumberScrub, рендерится ниже вместе со скрабом длительности)
        int r0 = settingsRowY(0);
        g.drawString(font, font.plainSubstrByWidth(
                        Component.translatable("wh_npcs.ui.reactions.range_label").getString(), LABEL_W - 4),
                winX + PAD, r0 + 5, VanillaUIHelper.TEXT_GRAY, false);
        if (isOver(mouseX, mouseY, winX + PAD, r0, LABEL_W + SCRUB_W, 18)) {
            tooltip = Component.translatable("wh_npcs.ui.reactions.range_tip").getString();
        }

        // Строка 1: эмоция смерти — пикер фикс-ширины. Без Emotecraft строка серая, пикер не открывается.
        int r1 = settingsRowY(1);
        boolean emoLoaded = com.withouthonor.npcs.compat.Compat.emotecraftLoaded();
        g.drawString(font, font.plainSubstrByWidth(
                        Component.translatable("wh_npcs.ui.reactions.death_emote").getString(), LABEL_W - 4),
                winX + PAD, r1 + 5, emoLoaded ? VanillaUIHelper.TEXT_GRAY : VanillaUIHelper.TEXT_DARK_GRAY, false);
        String emoteName = deathEmoteLabel();
        boolean deHover = isOver(mouseX, mouseY, cx, r1, CTRL_W, 18);
        VanillaUIHelper.drawButton(g, cx, r1, CTRL_W, 18, emoLoaded && deHover);
        g.drawCenteredString(font, font.plainSubstrByWidth(emoteName, CTRL_W - 12) + " →",
                cx + CTRL_W / 2, r1 + 5, !emoLoaded ? VanillaUIHelper.TEXT_DARK_GRAY
                        : deHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (isOver(mouseX, mouseY, winX + PAD, r1, LABEL_W + CTRL_W, 18)) {
            tooltip = Component.translatable(emoLoaded ? "wh_npcs.ui.reactions.death_emote_tip"
                    : "wh_npcs.ui.reactions.need_emotecraft").getString();
        }

        // Строка 2: длительность эмоции смерти
        int r2 = settingsRowY(2);
        g.drawString(font, font.plainSubstrByWidth(
                        Component.translatable("wh_npcs.ui.reactions.death_secs").getString(), LABEL_W - 4),
                winX + PAD, r2 + 5, VanillaUIHelper.TEXT_GRAY, false);
        for (var s : scrubs) {
            s.render(g, font, mouseX, mouseY);
        }
        if (isOver(mouseX, mouseY, winX + PAD, r2, LABEL_W + SCRUB_W, 18)) {
            tooltip = Component.translatable("wh_npcs.ui.reactions.death_secs_tip").getString();
        }

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 70, winY + winH - PAD - 18, 70, mouseX, mouseY);
        hoverTooltip = tooltip;
    }

    /** Имя выбранной эмоции смерти для кнопки (имя → id → «нет»). */
    private String deathEmoteLabel() {
        if (profileJson.has("death_emote_name")
                && !profileJson.get("death_emote_name").getAsString().isEmpty()) {
            return profileJson.get("death_emote_name").getAsString();
        }
        if (profileJson.has("death_emote") && !profileJson.get("death_emote").getAsString().isEmpty()) {
            return profileJson.get("death_emote").getAsString();
        }
        return Component.translatable("wh_npcs.ui.common.none").getString();
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
        for (var s : scrubs) {
            if (s.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        // Пикер эмоции смерти: ЛКМ — выбрать, ПКМ — очистить. Без Emotecraft — не открываем.
        // Клик-зона == рендер-зоне кнопки (settingsRowY(1)/ctrlX()/CTRL_W).
        int dey = settingsRowY(1);
        if (isOver(mouseX, mouseY, ctrlX(), dey, CTRL_W, 18)) {
            if (!com.withouthonor.npcs.compat.Compat.emotecraftLoaded()) {
                return true;
            }
            if (button == 1) {
                profileJson.remove("death_emote");
                profileJson.remove("death_emote_name");
                profileJson.remove("death_emote_author");
                return true;
            }
            if (button == 0 && minecraft != null) {
                writeBack();
                minecraft.setScreen(EmotecraftScreen.forPicker(this, ref -> {
                    profileJson.addProperty("death_emote", ref.id());
                    profileJson.addProperty("death_emote_name", ref.name());
                    profileJson.addProperty("death_emote_author", ref.author());
                }));
                return true;
            }
        }
        if (button == 0) {
            int y0 = eventsY0();
            for (int i = 0; i < EVENTS.length; i++) {
                if (isOver(mouseX, mouseY, winX + PAD, y0 + i * 24, winW - PAD * 2, 18)) {
                    writeBack();
                    ActionsEditorScreen.open(this, lists.get(i));
                    return true;
                }
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 70, winY + winH - PAD - 18, 70, 18)) {
                onClose();
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

    private void writeBack() {
        for (int i = 0; i < EVENTS.length; i++) {
            List<DialogueAction> list = lists.get(i);
            if (list.isEmpty()) {
                profileJson.remove(EVENTS[i].key);
            } else {
                JsonArray arr = new JsonArray();
                for (DialogueAction a : list) {
                    arr.add(a.toJson());
                }
                profileJson.add(EVENTS[i].key, arr);
            }
        }
        int r = 0;
        if (rangeBox != null) {
            try {
                r = Math.max(0, Math.min(16, Integer.parseInt(rangeBox.getValue().trim())));
            } catch (NumberFormatException ignored) {
                r = 0;
            }
        }
        if (r > 0) {
            profileJson.addProperty("react_approach_range", r);
        } else {
            profileJson.remove("react_approach_range");
        }
    }

    @Override
    public void onClose() {
        writeBack();
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
        List<Component> lines = new ArrayList<>();
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
