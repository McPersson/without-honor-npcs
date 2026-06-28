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

public class CreatureNatureScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;

    private final Screen parent;
    private final JsonObject profileJson;

    private boolean fallDamage, webSlow, canDrown, poisonImmune;
    @Nullable
    private EditBox aggroBox;

    private int winX, winY, winW, winH;
    private int top, bottomY;
    @Nullable
    private String hoverTooltip;

    public CreatureNatureScreen(@Nullable Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.creature_nature.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        this.fallDamage = boolDefault("fall_damage", true);
        this.webSlow = boolDefault("web_slow", true);
        this.canDrown = boolDefault("can_drown", true);
        this.poisonImmune = boolDefault("poison_immune", false);
    }

    private boolean boolDefault(String key, boolean def) {
        return profileJson.has(key) ? profileJson.get(key).getAsBoolean() : def;
    }

    @Override
    protected int designW() {
        return 320;
    }

    @Override
    protected int designH() {
        return 200;
    }

    private void recalc() {
        winW = 320;
        winH = 200;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        top = winY + HEADER_H + 12;
        bottomY = winY + winH - PAD - 20;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        EditBox b = addRenderableWidget(new SelectableEditBox(
                font, winX + PAD + 110, top + 92, 46, 16, Component.empty()));
        b.setMaxLength(4);
        b.setHint(Component.literal("16"));
        if (profileJson.has("aggro_range") && profileJson.get("aggro_range").getAsInt() > 0) {
            b.setValue(String.valueOf(profileJson.get("aggro_range").getAsInt()));
        }
        this.aggroBox = b;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.creature_nature.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        String tooltip = null;
        int x = winX + PAD;
        if (toggle(g, x, top, fallDamage, Component.translatable("wh_npcs.ui.creature_nature.fall").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.creature_nature.tip.fall").getString();
        }
        if (toggle(g, x, top + 22, webSlow, Component.translatable("wh_npcs.ui.creature_nature.web").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.creature_nature.tip.web").getString();
        }
        if (toggle(g, x, top + 44, canDrown, Component.translatable("wh_npcs.ui.creature_nature.drown").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.creature_nature.tip.drown").getString();
        }
        if (toggle(g, x, top + 66, poisonImmune, Component.translatable("wh_npcs.ui.creature_nature.poison").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.creature_nature.tip.poison").getString();
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.creature_nature.aggro_range").getString(), x, top + 96, VanillaUIHelper.TEXT_GRAY, false);
        if (isOver(mouseX, mouseY, x, top + 92, 100, 16)) {
            tooltip = Component.translatable("wh_npcs.ui.creature_nature.tip.aggro_range").getString();
        }

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        hoverTooltip = tooltip;
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    private boolean toggle(GuiGraphics g, int x, int y, boolean on, String label, int mouseX, int mouseY) {
        boolean boxHover = isOver(mouseX, mouseY, x, y, 12, 12);
        VanillaUIHelper.drawButton(g, x, y, 12, 12, boxHover);
        if (on) {
            VanillaUIHelper.drawCheck(g, x + 1, y + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, label, x + 16, y + 2, VanillaUIHelper.TEXT_GRAY, false);
        return boxHover || isOver(mouseX, mouseY, x + 16, y, Math.min(220, font.width(label) + 6), 12);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0) {
            int x = winX + PAD;
            if (isOver(mouseX, mouseY, x, top, 12, 12)) {
                fallDamage = !fallDamage;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 22, 12, 12)) {
                webSlow = !webSlow;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 44, 12, 12)) {
                canDrown = !canDrown;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 66, 12, 12)) {
                poisonImmune = !poisonImmune;
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void apply() {

        writeDefaultOn("fall_damage", fallDamage);
        writeDefaultOn("web_slow", webSlow);
        writeDefaultOn("can_drown", canDrown);

        if (poisonImmune) {
            profileJson.addProperty("poison_immune", true);
        } else {
            profileJson.remove("poison_immune");
        }
        int r = aggroBox != null ? parseIntSafe(aggroBox.getValue()) : 0;
        if (r > 0) {
            profileJson.addProperty("aggro_range", r);
        } else {
            profileJson.remove("aggro_range");
        }
    }

    private void writeDefaultOn(String key, boolean on) {
        if (on) {
            profileJson.remove(key);
        } else {
            profileJson.addProperty(key, false);
        }
    }

    private static int parseIntSafe(String s) {
        try {
            return Math.max(0, Math.min(128, Integer.parseInt(s.trim())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void onClose() {
        apply();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void multilineTooltip(GuiGraphics g, String text, int mouseX, int mouseY) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5, hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
