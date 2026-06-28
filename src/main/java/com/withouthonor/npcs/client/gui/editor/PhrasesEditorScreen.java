package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PhrasesEditorScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 13;
    private static final int WIN_W = 380;
    private static final int WIN_H = 300;

    private static final String[] CAT_KEYS = {
            "ambient_phrases", "combat_phrases", "interact_phrases", "death_phrases", "kill_phrases"};
    private static final String[] CAT_TABS = {
            "wh_npcs.ui.phrases.tab_ambient", "wh_npcs.ui.phrases.tab_combat",
            "wh_npcs.ui.phrases.tab_interact", "wh_npcs.ui.phrases.tab_death",
            "wh_npcs.ui.phrases.tab_kill"};
    private static final String[] CAT_HINT = {
            "wh_npcs.ui.phrases.hint_ambient",
            "wh_npcs.ui.phrases.hint_combat",
            "wh_npcs.ui.phrases.hint_interact",
            "wh_npcs.ui.phrases.hint_death",
            "wh_npcs.ui.phrases.hint_kill",
    };

    private final Screen parent;
    private final JsonObject profileJson;
    private final List<List<String>> pools = new ArrayList<>();
    private int activeCat;
    private boolean enabled;
    private boolean toChat;
    private String draftRadius;
    private String draftCooldown;
    private int scroll;
    private final ScrollDrag scrollbars = new ScrollDrag();

    private EditBox newPhraseBox;
    private EditBox radiusBox;
    private EditBox cooldownBox;

    private int winX, winY, winW, winH;
    private int tabRowY, listY, listH;
    private int bottomY;
    private String hoverTooltip;

    public PhrasesEditorScreen(Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.phrases.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        enabled = !profileJson.has("bubbles_enabled") || profileJson.get("bubbles_enabled").getAsBoolean();
        toChat = profileJson.has("bubbles_to_chat") && profileJson.get("bubbles_to_chat").getAsBoolean();
        for (String key : CAT_KEYS) {
            List<String> pool = new ArrayList<>();
            if (profileJson.has(key)) {
                for (JsonElement e : profileJson.getAsJsonArray(key)) {
                    pool.add(e.getAsString());
                }
            }
            pools.add(pool);
        }
        draftRadius = profileJson.has("ambient_radius")
                ? String.valueOf(profileJson.get("ambient_radius").getAsInt()) : "8";
        draftCooldown = profileJson.has("ambient_cooldown_s")
                ? String.valueOf(profileJson.get("ambient_cooldown_s").getAsInt()) : "25";
    }

    private List<String> pool() {
        return pools.get(activeCat);
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
        bottomY = winY + winH - PAD - 20;
        tabRowY = winY + HEADER_H + 28;
        listY = tabRowY + 20;
        listH = bottomY - 30 - listY;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        radiusBox = addRenderableWidget(new SelectableEditBox(font, winX + winW - PAD - 142,
                winY + HEADER_H + 4, 32, 16, Component.translatable("wh_npcs.ui.phrases.radius")));
        radiusBox.setMaxLength(2);
        radiusBox.setValue(draftRadius);
        radiusBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.phrases.radius_tip")));
        radiusBox.setResponder(v -> draftRadius = v);
        cooldownBox = addRenderableWidget(new SelectableEditBox(font, winX + winW - PAD - 36,
                winY + HEADER_H + 4, 36, 16, Component.translatable("wh_npcs.ui.phrases.cooldown")));
        cooldownBox.setMaxLength(4);
        cooldownBox.setValue(draftCooldown);
        cooldownBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.phrases.cooldown_tip")));
        cooldownBox.setResponder(v -> draftCooldown = v);
        newPhraseBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD,
                bottomY - 24, winW - PAD * 2 - 96, 16, Component.translatable("wh_npcs.ui.phrases.new_phrase")));
        newPhraseBox.setMaxLength(120);
        newPhraseBox.setHint(Component.translatable("wh_npcs.ui.phrases.new_phrase_hint"));
    }

    private void addPhrase() {
        String text = newPhraseBox.getValue().trim();
        if (!text.isEmpty() && pool().size() < 32) {
            pool().add(text);
            newPhraseBox.setValue("");
            scroll = Math.max(0, pool().size() - visibleRows());
        }
    }

    private int visibleRows() {
        return Math.max(1, listH / ROW_H);
    }

    private int tabLeft(int i) {
        return winX + PAD + i * (winW - PAD * 2) / CAT_TABS.length;
    }

    private void writeBack() {
        if (enabled) {
            profileJson.remove("bubbles_enabled");
        } else {
            profileJson.addProperty("bubbles_enabled", false);
        }
        if (toChat) {
            profileJson.addProperty("bubbles_to_chat", true);
        } else {
            profileJson.remove("bubbles_to_chat");
        }
        for (int i = 0; i < CAT_KEYS.length; i++) {
            List<String> p = pools.get(i);
            if (p.isEmpty()) {
                profileJson.remove(CAT_KEYS[i]);
            } else {
                JsonArray array = new JsonArray();
                p.forEach(array::add);
                profileJson.add(CAT_KEYS[i], array);
            }
        }
        try {
            profileJson.addProperty("ambient_radius",
                    Math.max(2, Math.min(32, Integer.parseInt(draftRadius.trim()))));
        } catch (NumberFormatException ignored) {
        }
        try {
            profileJson.addProperty("ambient_cooldown_s",
                    Math.max(5, Math.min(3600, Integer.parseInt(draftCooldown.trim()))));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        boolean ambient = activeCat == 0;
        radiusBox.visible = ambient;
        cooldownBox.visible = ambient;

        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.phrases.header").getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        String tooltip = null;

        boolean checkHover = isOver(mouseX, mouseY, winX + PAD, winY + HEADER_H + 6, 12, 12);
        VanillaUIHelper.drawButton(g, winX + PAD, winY + HEADER_H + 6, 12, 12, checkHover);
        if (enabled) {
            VanillaUIHelper.drawCheck(g, winX + PAD + 1, winY + HEADER_H + 8, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, Component.translatable(enabled
                        ? "wh_npcs.ui.phrases.bubbles" : "wh_npcs.ui.phrases.bubbles_off").getString(),
                winX + PAD + 16, winY + HEADER_H + 8, VanillaUIHelper.TEXT_GRAY, false);
        if (checkHover) {
            tooltip = Component.translatable("wh_npcs.ui.phrases.bubbles_tip").getString();
        }

        int chatX = winX + PAD + 62;
        boolean chatHover = isOver(mouseX, mouseY, chatX, winY + HEADER_H + 6, 12, 12);
        VanillaUIHelper.drawButton(g, chatX, winY + HEADER_H + 6, 12, 12, chatHover);
        if (toChat) {
            VanillaUIHelper.drawCheck(g, chatX + 1, winY + HEADER_H + 8, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.phrases.to_chat").getString(),
                chatX + 16, winY + HEADER_H + 8, VanillaUIHelper.TEXT_GRAY, false);
        if (chatHover) {
            tooltip = Component.translatable("wh_npcs.ui.phrases.to_chat_tip").getString();
        }
        if (ambient) {
            g.drawString(font, Component.translatable("wh_npcs.ui.phrases.radius_short").getString(),
                    winX + winW - PAD - 170, winY + HEADER_H + 8,
                    VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.phrases.cooldown_short").getString(),
                    winX + winW - PAD - 70, winY + HEADER_H + 8,
                    VanillaUIHelper.TEXT_GRAY, false);
        }

        for (int i = 0; i < CAT_TABS.length; i++) {
            int tx = tabLeft(i);
            int tw = tabLeft(i + 1) - tx;
            boolean hov = isOver(mouseX, mouseY, tx, tabRowY, tw, 16);
            VanillaUIHelper.drawTab(g, tx, tabRowY, tw, 16, activeCat == i, hov);
            int cnt = pools.get(i).size();
            String label = Component.translatable(CAT_TABS[i]).getString() + (cnt > 0 ? " " + cnt : "");
            g.drawCenteredString(font, font.plainSubstrByWidth(label, tw - 4), tx + tw / 2, tabRowY + 4,
                    activeCat == i ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        }

        VanillaUIHelper.drawContentPanel(g, winX + PAD, listY, winW - PAD * 2, listH);
        if (pool().isEmpty()) {
            g.drawCenteredString(font, "§8" + Component.translatable(CAT_HINT[activeCat]).getString(),
                    winX + winW / 2, listY + listH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
        }
        int visible = visibleRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, pool().size() - visible)));
        int y = listY + 3;
        for (int i = scroll; i < Math.min(pool().size(), scroll + visible); i++) {
            boolean hovered = isOver(mouseX, mouseY, winX + PAD + 2, y, winW - PAD * 2 - 4, ROW_H);
            if (hovered) {
                g.fill(winX + PAD + 2, y, winX + winW - PAD - 2, y + ROW_H, VanillaUIHelper.BG_HOVERED);
            }
            g.drawString(font, font.plainSubstrByWidth(pool().get(i), winW - PAD * 2 - 26),
                    winX + PAD + 6, y + 2, VanillaUIHelper.TEXT_WHITE, false);
            if (hovered) {
                boolean delHover = isOver(mouseX, mouseY, winX + winW - PAD - 16, y + 1, 10, 10);
                g.drawString(font, "✕", winX + winW - PAD - 16, y + 2,
                        delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, winX + winW - PAD - 4, listY + 2, listH - 4,
                pool().size(), visible, scroll, scrollbars, v -> scroll = v);

        boolean addHover = isOver(mouseX, mouseY, winX + winW - PAD - 90, bottomY - 25, 90, 18);
        VanillaUIHelper.drawButton(g, winX + winW - PAD - 90, bottomY - 25, 90, 18, addHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.add").getString(),
                winX + winW - PAD - 45, bottomY - 20,
                addHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);

        g.drawString(font, "§8" + Component.translatable("wh_npcs.ui.phrases.apply_note").getString(),
                winX + PAD, bottomY + 5, VanillaUIHelper.TEXT_WHITE, false);
        boolean doneHover = isOver(mouseX, mouseY, winX + winW - PAD - 70, bottomY, 70, 18);
        VanillaUIHelper.drawButton(g, winX + winW - PAD - 70, bottomY, 70, 18, doneHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.done").getString(),
                winX + winW - PAD - 35, bottomY + 5,
                doneHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);

        hoverTooltip = tooltip;
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            List<Component> lines = new ArrayList<>();
            for (String line : hoverTooltip.split("\n")) {
                lines.add(Component.literal(line));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }
        if (button == 0) {
            recalc();
            if (isOver(mouseX, mouseY, winX + PAD, winY + HEADER_H + 6, 12, 12)) {
                enabled = !enabled;
                return true;
            }
            if (isOver(mouseX, mouseY, winX + PAD + 62, winY + HEADER_H + 6, 12, 12)) {
                toChat = !toChat;
                return true;
            }
            for (int i = 0; i < CAT_TABS.length; i++) {
                int tx = tabLeft(i);
                int tw = tabLeft(i + 1) - tx;
                if (isOver(mouseX, mouseY, tx, tabRowY, tw, 16)) {
                    activeCat = i;
                    scroll = 0;
                    return true;
                }
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 90, bottomY - 25, 90, 18)) {
                addPhrase();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 70, bottomY, 70, 18)) {
                onClose();
                return true;
            }
            int visible = visibleRows();
            int y = listY + 3;
            for (int i = scroll; i < Math.min(pool().size(), scroll + visible); i++) {
                if (isOver(mouseX, mouseY, winX + winW - PAD - 16, y + 1, 10, 10)) {
                    pool().remove(i);
                    return true;
                }
                y += ROW_H;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (newPhraseBox.isFocused()
                && (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)) {
            addPhrase();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, winX + PAD, listY, winW - PAD * 2, listH)) {
            scroll -= (int) Math.signum(delta);
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        writeBack();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
