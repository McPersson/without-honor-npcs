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
    private static final int WIN_H = HEADER_H + 16 + 4 * 24 + 30 + 24;

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

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        // Поле сразу за меткой «Радиус подхода:», с небольшим зазором
        int labelW = font.width(Component.translatable("wh_npcs.ui.reactions.range_label").getString());
        rangeBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD + labelW + 6,
                winY + HEADER_H + 16 + EVENTS.length * 24 + 4, 36, 16, Component.empty()));
        rangeBox.setMaxLength(2);
        if (profileJson.has("react_approach_range") && profileJson.get("react_approach_range").getAsInt() > 0) {
            rangeBox.setValue(String.valueOf(profileJson.get("react_approach_range").getAsInt()));
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.reactions.header").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        String tooltip = null;
        int y0 = winY + HEADER_H + 12;
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

        int ry = y0 + EVENTS.length * 24 + 4;
        String rangeLabel = Component.translatable("wh_npcs.ui.reactions.range_label").getString();
        // Метка по вертикальному центру поля ввода (бокс стоит на ry+4, высота 16)
        g.drawString(font, rangeLabel, winX + PAD, ry + 8, VanillaUIHelper.TEXT_GRAY, false);
        // Тултип и на метке, и на поле ввода
        if (isOver(mouseX, mouseY, winX + PAD, ry + 4, font.width(rangeLabel) + 6 + 36, 16)) {
            tooltip = Component.translatable("wh_npcs.ui.reactions.range_tip").getString();
        }

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 70, winY + winH - PAD - 18, 70, mouseX, mouseY);
        hoverTooltip = tooltip;
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
        if (button == 0) {
            int y0 = winY + HEADER_H + 12;
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
