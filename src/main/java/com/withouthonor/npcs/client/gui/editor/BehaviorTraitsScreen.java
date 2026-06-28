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

public class BehaviorTraitsScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final String[] MODES = {"none", "player", "entity"};
    private static final String[] MODE_LABEL_KEYS = {"wh_npcs.ui.behavior.follow.none",
            "wh_npcs.ui.behavior.follow.player", "wh_npcs.ui.behavior.follow.entity"};

    private final Screen parent;
    private final JsonObject profileJson;

    private boolean idleLook, idleWander, panic, avoidSun, burnInSun, pushable, passable;
    private String autoMode;
    @Nullable
    private EditBox autoTargetBox;
    @Nullable
    private EditBox wanderRadiusBox;

    private int winX, winY, winW, winH;
    private int top, bottomY;
    @Nullable
    private String hoverTooltip;

    public BehaviorTraitsScreen(@Nullable Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.behavior.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        this.idleLook = !profileJson.has("idle_look") || profileJson.get("idle_look").getAsBoolean();
        this.idleWander = bool("idle_wander");
        this.panic = bool("panic_when_hurt");
        this.avoidSun = bool("avoid_sun");
        this.burnInSun = bool("burn_in_sun");
        this.pushable = bool("pushable");
        this.passable = bool("passable");
        this.autoMode = profileJson.has("auto_follow") ? profileJson.get("auto_follow").getAsString() : "none";
    }

    private boolean bool(String key) {
        return profileJson.has(key) && profileJson.get(key).getAsBoolean();
    }

    @Override
    protected int designW() {
        return 360;
    }

    @Override
    protected int designH() {
        return 266;
    }

    private void recalc() {
        winW = 360;
        winH = 266;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        top = winY + HEADER_H + 12;
        bottomY = winY + winH - PAD - 20;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();

        EditBox rb = addRenderableWidget(new SelectableEditBox(
                font, winX + PAD + 170, top + 20, 46, 16, Component.empty()));
        rb.setMaxLength(4);
        rb.setHint(Component.literal("0"));
        if (profileJson.has("idle_wander_radius")) {
            rb.setValue(String.valueOf(profileJson.get("idle_wander_radius").getAsInt()));
        }
        this.wanderRadiusBox = rb;
        this.autoTargetBox = null;
        if (!"none".equals(autoMode)) {
            EditBox b = addRenderableWidget(new SelectableEditBox(
                    font, winX + PAD, top + 174, winW - PAD * 2, 16, Component.empty()));
            b.setMaxLength(48);
            b.setHint(Component.translatable("player".equals(autoMode)
                    ? "wh_npcs.ui.behavior.hint.player" : "wh_npcs.ui.behavior.hint.entity"));
            if (profileJson.has("auto_follow_target")) {
                b.setValue(profileJson.get("auto_follow_target").getAsString());
            }
            this.autoTargetBox = b;
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.behavior.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        String tooltip = null;
        hoverTooltip = null;
        int x = winX + PAD;
        if (toggle(g, x, top, idleLook, Component.translatable("wh_npcs.ui.behavior.look").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.look").getString();
        }
        if (toggle(g, x, top + 22, idleWander, Component.translatable("wh_npcs.ui.behavior.wander").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.wander").getString();
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.behavior.blocks_inf").getString(), x + 222, top + 24, VanillaUIHelper.TEXT_WHITE, false);
        if (toggle(g, x, top + 44, panic, Component.translatable("wh_npcs.ui.behavior.panic").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.panic").getString();
        }
        if (toggle(g, x, top + 66, avoidSun, Component.translatable("wh_npcs.ui.behavior.avoid_sun").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.avoid_sun").getString();
        }
        if (toggle(g, x, top + 88, burnInSun, Component.translatable("wh_npcs.ui.behavior.burn_sun").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.burn_sun").getString();
        }
        int x2 = x + 180;
        if (toggle(g, x2, top + 44, pushable, Component.translatable("wh_npcs.ui.behavior.pushable").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.pushable").getString();
        }
        if (toggle(g, x2, top + 66, passable, Component.translatable("wh_npcs.ui.behavior.passable").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.passable").getString();
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.behavior.follow_label").getString(), x, top + 122, VanillaUIHelper.TEXT_GRAY, false);
        boolean modeHover = isOver(mouseX, mouseY, x, top + 138, 150, 18);
        VanillaUIHelper.drawButton(g, x, top + 138, 150, 18, modeHover);
        g.drawCenteredString(font, modeLabel() + " ▾", x + 75, top + 143,
                modeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (modeHover) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.follow").getString();
        }
        if (!"none".equals(autoMode)) {
            g.drawString(font, Component.translatable("player".equals(autoMode)
                            ? "wh_npcs.ui.behavior.player_nick" : "wh_npcs.ui.behavior.entity_uuid").getString(),
                    x, top + 162, VanillaUIHelper.TEXT_WHITE, false);
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
                idleLook = !idleLook;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 22, 12, 12)) {
                idleWander = !idleWander;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 44, 12, 12)) {
                panic = !panic;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 66, 12, 12)) {
                avoidSun = !avoidSun;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 88, 12, 12)) {
                burnInSun = !burnInSun;
                return true;
            }
            int x2 = x + 180;
            if (isOver(mouseX, mouseY, x2, top + 44, 12, 12)) {
                pushable = !pushable;
                return true;
            }
            if (isOver(mouseX, mouseY, x2, top + 66, 12, 12)) {
                passable = !passable;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 138, 150, 18)) {
                cycleMode();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void cycleMode() {
        int idx = 0;
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(autoMode)) {
                idx = i;
            }
        }

        if (autoTargetBox != null) {
            profileJson.addProperty("auto_follow_target", autoTargetBox.getValue());
        }
        persistRadius();
        autoMode = MODES[(idx + 1) % MODES.length];
        init(minecraft, width, height);
    }

    private String modeLabel() {
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(autoMode)) {
                return Component.translatable(MODE_LABEL_KEYS[i]).getString();
            }
        }
        return Component.translatable(MODE_LABEL_KEYS[0]).getString();
    }

    private void apply() {

        if (idleLook) {
            profileJson.remove("idle_look");
        } else {
            profileJson.addProperty("idle_look", false);
        }
        writeFlag("idle_wander", idleWander);
        writeFlag("panic_when_hurt", panic);
        writeFlag("avoid_sun", avoidSun);
        writeFlag("burn_in_sun", burnInSun);
        writeFlag("pushable", pushable);
        writeFlag("passable", passable);
        persistRadius();
        if ("none".equals(autoMode)) {
            profileJson.remove("auto_follow");
            profileJson.remove("auto_follow_target");
        } else {
            profileJson.addProperty("auto_follow", autoMode);
            profileJson.addProperty("auto_follow_target",
                    autoTargetBox != null ? autoTargetBox.getValue().trim() : "");
        }
    }

    private void writeFlag(String key, boolean on) {
        if (on) {
            profileJson.addProperty(key, true);
        } else {
            profileJson.remove(key);
        }
    }

    private void persistRadius() {
        int r = wanderRadiusBox != null ? parseIntSafe(wanderRadiusBox.getValue()) : 0;
        if (r > 0) {
            profileJson.addProperty("idle_wander_radius", r);
        } else {
            profileJson.remove("idle_wander_radius");
        }
    }

    private static int parseIntSafe(String s) {
        try {
            return Math.max(0, Math.min(256, Integer.parseInt(s.trim())));
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
