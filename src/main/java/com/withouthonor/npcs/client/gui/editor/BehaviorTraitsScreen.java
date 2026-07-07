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
    /** Единый шаг сетки строк. */
    private static final int ROW = 24;
    /** Отступ вложенных строк-параметров (радиусы). */
    private static final int INDENT = 12;
    private static final String[] MODES = {"none", "player", "entity"};
    private static final String[] MODE_LABEL_KEYS = {"wh_npcs.ui.behavior.follow.none",
            "wh_npcs.ui.behavior.follow.player", "wh_npcs.ui.behavior.follow.entity"};
    private static final String[] LOOK_MODES = {"off", "cold", "lively"};
    private static final String[] LOOK_LABEL_KEYS = {"wh_npcs.ui.behavior.look.off",
            "wh_npcs.ui.behavior.look.cold", "wh_npcs.ui.behavior.look.lively"};

    private final Screen parent;
    private final JsonObject profileJson;

    private boolean idleWander, panic, avoidSun, burnInSun, pushable, passable, boatRide;
    private String lookMode;
    private String autoMode;
    @Nullable
    private EditBox autoTargetBox;
    @Nullable
    private EditBox wanderRadiusBox;
    @Nullable
    private EditBox lookRadiusBox;

    private int winX, winY, winW, winH;
    private int top, bottomY;
    @Nullable
    private String hoverTooltip;

    public BehaviorTraitsScreen(@Nullable Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.behavior.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        if (profileJson.has("look_mode")) {
            this.lookMode = profileJson.get("look_mode").getAsString();
        } else {
            // Миграция со старого флага idle_look
            this.lookMode = !profileJson.has("idle_look")
                    || profileJson.get("idle_look").getAsBoolean() ? "cold" : "off";
        }
        this.boatRide = !profileJson.has("boat_ride") || profileJson.get("boat_ride").getAsBoolean();
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
        return 310;
    }

    private void recalc() {
        winW = 360;
        winH = 310;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        top = winY + HEADER_H + 12;
        bottomY = winY + winH - PAD - 20;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        int x = winX + PAD;

        // Радиус «живого» взгляда — параметр строки «Взгляд», виден только в режиме lively
        EditBox lr = addRenderableWidget(new SelectableEditBox(
                font, x + 54, top + ROW, 46, 16, Component.empty()));
        lr.setMaxLength(2);
        lr.setHint(Component.literal("3"));
        if (profileJson.has("look_radius")) {
            lr.setValue(String.valueOf(profileJson.get("look_radius").getAsInt()));
        }
        lr.visible = "lively".equals(lookMode);
        this.lookRadiusBox = lr;

        // Радиус блуждания — параметр строки «Бродить без дела», виден только при включённой галочке
        EditBox rb = addRenderableWidget(new SelectableEditBox(
                font, x + INDENT, top + ROW * 3, 46, 16, Component.empty()));
        rb.setMaxLength(4);
        rb.setHint(Component.literal("0"));
        if (profileJson.has("idle_wander_radius")) {
            rb.setValue(String.valueOf(profileJson.get("idle_wander_radius").getAsInt()));
        }
        rb.visible = idleWander;
        this.wanderRadiusBox = rb;

        this.autoTargetBox = null;
        if (!"none".equals(autoMode)) {
            EditBox b = addRenderableWidget(new SelectableEditBox(
                    font, x, top + 224, winW - PAD * 2, 16, Component.empty()));
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

        // === Левая колонка: взгляд + характер ===
        boolean lookHover = isOver(mouseX, mouseY, x, top, 150, 18);
        VanillaUIHelper.drawButton(g, x, top, 150, 18, lookHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.behavior.look_mode",
                        lookModeLabel()).getString() + " ▾", x + 75, top + 5,
                lookHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (lookHover) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.look").getString();
        }
        if ("lively".equals(lookMode)) {
            // Выравнивание по левому краю колонки (как кнопка выше и чекбоксы ниже)
            g.drawString(font, Component.translatable("wh_npcs.ui.behavior.look_radius").getString(),
                    x, top + ROW + 4, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.behavior.look_radius_blocks").getString(),
                    x + 106, top + ROW + 4, VanillaUIHelper.TEXT_WHITE, false);
            if (isOver(mouseX, mouseY, x, top + ROW, 150, 16)) {
                tooltip = Component.translatable("wh_npcs.ui.behavior.tip.look_radius").getString();
            }
        }
        if (toggle(g, x, top + ROW * 2, idleWander, Component.translatable("wh_npcs.ui.behavior.wander").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.wander").getString();
        }
        if (idleWander) {
            g.drawString(font, Component.translatable("wh_npcs.ui.behavior.blocks_inf").getString(),
                    x + INDENT + 52, top + ROW * 3 + 4, VanillaUIHelper.TEXT_WHITE, false);
        }
        if (toggle(g, x, top + ROW * 4, panic, Component.translatable("wh_npcs.ui.behavior.panic").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.panic").getString();
        }
        if (toggle(g, x, top + ROW * 5, avoidSun, Component.translatable("wh_npcs.ui.behavior.avoid_sun").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.avoid_sun").getString();
        }
        if (toggle(g, x, top + ROW * 6, burnInSun, Component.translatable("wh_npcs.ui.behavior.burn_sun").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.burn_sun").getString();
        }

        // === Правая колонка: физика/транспорт ===
        int x2 = x + 180;
        if (toggle(g, x2, top, boatRide, Component.translatable("wh_npcs.ui.behavior.boat_ride").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.boat_ride").getString();
        }
        if (toggle(g, x2, top + ROW, pushable, Component.translatable("wh_npcs.ui.behavior.pushable").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.pushable").getString();
        }
        if (toggle(g, x2, top + ROW * 2, passable, Component.translatable("wh_npcs.ui.behavior.passable").getString(), mouseX, mouseY)) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.passable").getString();
        }

        // === Низ: постоянное следование ===
        g.drawString(font, Component.translatable("wh_npcs.ui.behavior.follow_label").getString(), x, top + 172, VanillaUIHelper.TEXT_GRAY, false);
        boolean modeHover = isOver(mouseX, mouseY, x, top + 186, 150, 18);
        VanillaUIHelper.drawButton(g, x, top + 186, 150, 18, modeHover);
        g.drawCenteredString(font, modeLabel() + " ▾", x + 75, top + 191,
                modeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (modeHover) {
            tooltip = Component.translatable("wh_npcs.ui.behavior.tip.follow").getString();
        }
        if (!"none".equals(autoMode)) {
            g.drawString(font, Component.translatable("player".equals(autoMode)
                            ? "wh_npcs.ui.behavior.player_nick" : "wh_npcs.ui.behavior.entity_uuid").getString(),
                    x, top + 212, VanillaUIHelper.TEXT_WHITE, false);
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
            if (isOver(mouseX, mouseY, x, top, 150, 18)) {
                cycleLookMode();
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + ROW * 2, 12, 12)) {
                idleWander = !idleWander;
                if (wanderRadiusBox != null) {
                    wanderRadiusBox.visible = idleWander;
                }
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + ROW * 4, 12, 12)) {
                panic = !panic;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + ROW * 5, 12, 12)) {
                avoidSun = !avoidSun;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + ROW * 6, 12, 12)) {
                burnInSun = !burnInSun;
                return true;
            }
            int x2 = x + 180;
            if (isOver(mouseX, mouseY, x2, top, 12, 12)) {
                boatRide = !boatRide;
                return true;
            }
            if (isOver(mouseX, mouseY, x2, top + ROW, 12, 12)) {
                pushable = !pushable;
                return true;
            }
            if (isOver(mouseX, mouseY, x2, top + ROW * 2, 12, 12)) {
                passable = !passable;
                return true;
            }
            if (isOver(mouseX, mouseY, x, top + 186, 150, 18)) {
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
        persistLookRadius();
        autoMode = MODES[(idx + 1) % MODES.length];
        init(minecraft, width, height);
    }

    private void cycleLookMode() {
        int idx = 0;
        for (int i = 0; i < LOOK_MODES.length; i++) {
            if (LOOK_MODES[i].equals(lookMode)) {
                idx = i;
            }
        }
        lookMode = LOOK_MODES[(idx + 1) % LOOK_MODES.length];
        if (lookRadiusBox != null) {
            lookRadiusBox.visible = "lively".equals(lookMode);
        }
    }

    private String lookModeLabel() {
        for (int i = 0; i < LOOK_MODES.length; i++) {
            if (LOOK_MODES[i].equals(lookMode)) {
                return Component.translatable(LOOK_LABEL_KEYS[i]).getString();
            }
        }
        return Component.translatable(LOOK_LABEL_KEYS[1]).getString();
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

        if ("cold".equals(lookMode)) {
            profileJson.remove("look_mode");
        } else {
            profileJson.addProperty("look_mode", lookMode);
        }
        // Легаси-флаг idle_look держим синхронно для совместимости старых версий
        if ("off".equals(lookMode)) {
            profileJson.addProperty("idle_look", false);
        } else {
            profileJson.remove("idle_look");
        }
        if (boatRide) {
            profileJson.remove("boat_ride");
        } else {
            profileJson.addProperty("boat_ride", false);
        }
        writeFlag("idle_wander", idleWander);
        writeFlag("panic_when_hurt", panic);
        writeFlag("avoid_sun", avoidSun);
        writeFlag("burn_in_sun", burnInSun);
        writeFlag("pushable", pushable);
        writeFlag("passable", passable);
        persistRadius();
        persistLookRadius();
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

    /** Радиус живого взгляда: 1..16, дефолт 3 (в json не пишем). */
    private void persistLookRadius() {
        int r = 3;
        if (lookRadiusBox != null && !lookRadiusBox.getValue().trim().isEmpty()) {
            try {
                r = Math.max(1, Math.min(16, Integer.parseInt(lookRadiusBox.getValue().trim())));
            } catch (NumberFormatException e) {
                r = 3;
            }
        }
        if (r != 3) {
            profileJson.addProperty("look_radius", r);
        } else {
            profileJson.remove("look_radius");
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
