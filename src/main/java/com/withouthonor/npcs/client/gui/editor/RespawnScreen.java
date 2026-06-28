package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.RespawnHomePicker;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RespawnScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final String[] MODES = {"death", "home", "owner"};
    private static final String[] MODE_LABEL_KEYS = {"wh_npcs.ui.respawn.mode.death",
            "wh_npcs.ui.respawn.mode.home", "wh_npcs.ui.respawn.mode.owner"};

    private final Screen parent;
    private final JsonObject profileJson;

    @Nullable
    private EditBox delayMinBox;
    @Nullable
    private EditBox delayMaxBox;
    @Nullable
    private EditBox limitBox;

    private int winX, winY, winW, winH;
    private int top, bottomY;
    @Nullable
    private String hoverTooltip;

    public RespawnScreen(@Nullable Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.respawn.title"));
        this.parent = parent;
        this.profileJson = profileJson;
    }

    @Override
    protected int designW() {
        return 380;
    }

    @Override
    protected int designH() {
        return 172;
    }

    private void recalc() {
        winW = 380;
        winH = 172;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        top = winY + HEADER_H + 14;
        bottomY = winY + winH - PAD - 20;
    }

    private String mode() {
        return profileJson.has("respawn_location") ? profileJson.get("respawn_location").getAsString() : "death";
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        int x = winX + PAD;
        delayMinBox = numBox(x + 100, top - 2, 34, "respawn_seconds", 30, 1, 86400);
        delayMaxBox = numBox(x + 168, top - 2, 34, "respawn_seconds_max", 0, 0, 86400);
        limitBox = numBox(x + 56, top + 72, 34, "respawn_max", 0, 0, 9999);
    }

    private EditBox numBox(int x, int y, int w, String key, int def, int min, int max) {
        EditBox box = addRenderableWidget(new SelectableEditBox(font, x, y, w, 16, Component.literal(key)));
        box.setMaxLength(6);
        box.setValue(String.valueOf(profileJson.has(key) ? profileJson.get(key).getAsInt() : def));
        box.setResponder(v -> {
            try {
                int parsed = Math.max(min, Math.min(max, Integer.parseInt(v.trim())));
                profileJson.addProperty(key, parsed);
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.respawn.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        String tooltip = null;
        int x = winX + PAD;
        String mode = mode();

        g.drawString(font, Component.translatable("wh_npcs.ui.respawn.delay_from").getString(), x, top + 2, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.respawn.to").getString(), x + 140, top + 2, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.respawn.sec").getString(), x + 206, top + 2, VanillaUIHelper.TEXT_DARK_GRAY, false);

        g.drawString(font, Component.translatable("wh_npcs.ui.respawn.place").getString(), x, top + 28, VanillaUIHelper.TEXT_GRAY, false);
        boolean modeHover = isOver(mouseX, mouseY, x + 50, top + 24, 168, 18);
        VanillaUIHelper.drawButton(g, x + 50, top + 24, 168, 18, modeHover);
        g.drawCenteredString(font, modeLabel() + " ▾", x + 134, top + 29,
                modeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (modeHover) {
            tooltip = Component.translatable("wh_npcs.ui.respawn.tip.place").getString();
        }

        if ("home".equals(mode)) {
            String home = profileJson.has("respawn_home")
                    ? homeLabel() : Component.translatable("wh_npcs.ui.respawn.not_set").getString();
            g.drawString(font, Component.translatable("wh_npcs.ui.respawn.home_label", home).getString(), x, top + 52, VanillaUIHelper.TEXT_WHITE, false);
            boolean pickHover = isOver(mouseX, mouseY, x + 222, top + 48, 130, 18);
            VanillaUIHelper.drawButton(g, x + 222, top + 48, 130, 18, pickHover);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.respawn.mark_feather").getString(), x + 287, top + 53,
                    pickHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            if (pickHover) {
                tooltip = Component.translatable("wh_npcs.ui.respawn.tip.home").getString();
            }
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.respawn.limit").getString(), x, top + 74, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.respawn.limit_note").getString(), x + 96, top + 74,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        hoverTooltip = tooltip;
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    private String modeLabel() {
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(mode())) {
                return Component.translatable(MODE_LABEL_KEYS[i]).getString();
            }
        }
        return Component.translatable(MODE_LABEL_KEYS[0]).getString();
    }

    private String homeLabel() {
        JsonObject h = profileJson.getAsJsonObject("respawn_home");
        return "§b" + h.get("x").getAsInt() + " " + h.get("y").getAsInt() + " " + h.get("z").getAsInt();
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        int x = winX + PAD;
        if (button == 0 && isOver(mouseX, mouseY, x + 50, top + 24, 168, 18)) {
            cycleMode();
            return true;
        }
        if ("home".equals(mode()) && button == 0 && isOver(mouseX, mouseY, x + 222, top + 48, 130, 18)) {

            RespawnHomePicker.begin(parent, profileJson);
            if (minecraft != null) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.translatable("wh_npcs.msg.respawn.pick_home"), true);
                }
                minecraft.setScreen(null);
            }
            return true;
        }
        if (button == 0 && isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
            onClose();
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void cycleMode() {
        int idx = 0;
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(mode())) {
                idx = i;
            }
        }
        String next = MODES[(idx + 1) % MODES.length];
        if ("death".equals(next)) {
            profileJson.remove("respawn_location");
        } else {
            profileJson.addProperty("respawn_location", next);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void multilineTooltip(GuiGraphics g, String text, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
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
