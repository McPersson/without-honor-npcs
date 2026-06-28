package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.client.SchedulePointPicker;
import com.withouthonor.npcs.network.PoseLibraryPackets.PoseEntry;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.ScheduleEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ScheduleScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int MAX_ROWS = 10;
    private static final int WIN_W = 478;
    private static final int WIN_H = 330;
    private static final String[] POSES = {"stand", "wander", "sit", "sleep"};
    private static final String[] POSE_LABEL_KEYS = {"wh_npcs.ui.schedule.pose.stand",
            "wh_npcs.ui.schedule.pose.wander", "wh_npcs.ui.schedule.pose.sit", "wh_npcs.ui.schedule.pose.sleep"};

    private final Screen parent;
    private final JsonObject profileJson;
    @Nullable
    private final CompanionEntity npc;

    private final List<ScheduleEntry> rows = new ArrayList<>();
    private boolean enabled;
    private final List<EditBox> timeBoxes = new ArrayList<>();
    private final List<EditBox> xBoxes = new ArrayList<>();
    private final List<EditBox> yBoxes = new ArrayList<>();
    private final List<EditBox> zBoxes = new ArrayList<>();
    private final List<EditBox> radiusBoxes = new ArrayList<>();

    private final java.util.Map<String, String> emoteNames = new java.util.HashMap<>();

    private int winX, winY, winW, winH;
    private int listTop, bottomY;
    @Nullable
    private String hoverTooltip;

    public ScheduleScreen(@Nullable Screen parent, JsonObject profileJson, @Nullable CompanionEntity npc) {
        super(Component.translatable("wh_npcs.ui.schedule.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        this.npc = npc;
        this.enabled = profileJson.has("schedule_enabled") && profileJson.get("schedule_enabled").getAsBoolean();
        if (profileJson.has("schedule")) {
            for (JsonElement e : profileJson.getAsJsonArray("schedule")) {
                rows.add(ScheduleEntry.fromJson(e.getAsJsonObject()));
            }
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
        listTop = winY + HEADER_H + 44;
        bottomY = winY + winH - PAD - 20;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        if (emoteNames.isEmpty()) {
            for (var ref : com.withouthonor.npcs.compat.Compat.emotecraft().listEmotes()) {
                emoteNames.put(ref.id(), ref.name());
            }
        }
        timeBoxes.clear();
        xBoxes.clear();
        yBoxes.clear();
        zBoxes.clear();
        radiusBoxes.clear();
        int base = winX + PAD;
        for (int i = 0; i < rows.size(); i++) {
            ScheduleEntry e = rows.get(i);
            int y = listTop + i * 22;
            timeBoxes.add(box(base, y, 42, hhmm(e.time())));
            xBoxes.add(box(base + 46, y, 36, String.valueOf(e.x())));
            yBoxes.add(box(base + 86, y, 36, String.valueOf(e.y())));
            zBoxes.add(box(base + 126, y, 36, String.valueOf(e.z())));
            EditBox rad = box(base + 240, y, 30, String.valueOf(e.radius()));
            rad.visible = isWander(e);
            radiusBoxes.add(rad);
        }
    }

    private EditBox box(int x, int y, int w, String value) {
        EditBox b = addRenderableWidget(new SelectableEditBox(font, x, y, w, 16, Component.empty()));
        b.setMaxLength(8);
        b.setValue(value);
        b.setResponder(v -> writeBackRows());
        return b;
    }

    private void writeBackRows() {
        for (int i = 0; i < rows.size() && i < timeBoxes.size(); i++) {
            ScheduleEntry old = rows.get(i);
            rows.set(i, new ScheduleEntry(
                    parseHHMM(timeBoxes.get(i).getValue()),
                    parseInt(xBoxes.get(i).getValue(), old.x()),
                    parseInt(yBoxes.get(i).getValue(), old.y()),
                    parseInt(zBoxes.get(i).getValue(), old.z()),
                    old.pose(),
                    Math.max(0, Math.min(32, parseInt(radiusBoxes.get(i).getValue(), old.radius()))),
                    old.poseName(), old.poseSnapshot(), old.emoteId(),
                    old.emoteName(), old.emoteAuthor()));
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.schedule.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        boolean enHover = isOver(mouseX, mouseY, winX + PAD, winY + HEADER_H + 4, 12, 12);
        VanillaUIHelper.drawButton(g, winX + PAD, winY + HEADER_H + 4, 12, 12, enHover);
        if (enabled) {
            VanillaUIHelper.drawCheck(g, winX + PAD + 1, winY + HEADER_H + 6, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, enabled ? Component.translatable("wh_npcs.ui.schedule.on").getString() : Component.translatable("wh_npcs.ui.schedule.off").getString(),
                winX + PAD + 18, winY + HEADER_H + 6, VanillaUIHelper.TEXT_GRAY, false);

        int base = winX + PAD;
        int hy = listTop - 16;
        g.drawString(font, Component.translatable("wh_npcs.ui.schedule.col.time").getString(), base, hy, VanillaUIHelper.TEXT_DARK_GRAY, false);
        g.drawString(font, "X", base + 46, hy, VanillaUIHelper.TEXT_DARK_GRAY, false);
        g.drawString(font, "Y", base + 86, hy, VanillaUIHelper.TEXT_DARK_GRAY, false);
        g.drawString(font, "Z", base + 126, hy, VanillaUIHelper.TEXT_DARK_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.schedule.col.pose").getString(), base + 166, hy, VanillaUIHelper.TEXT_DARK_GRAY, false);
        boolean anyWander = false;
        for (ScheduleEntry e : rows) {
            if (isWander(e)) {
                anyWander = true;
                break;
            }
        }
        if (anyWander) {
            g.drawString(font, Component.translatable("wh_npcs.ui.schedule.col.radius").getString(), base + 240, hy, VanillaUIHelper.TEXT_DARK_GRAY, false);
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.schedule.col.emote").getString(), base + 274, hy, VanillaUIHelper.TEXT_DARK_GRAY, false);

        String tooltip = null;
        for (int i = 0; i < rows.size(); i++) {
            int y = listTop + i * 22;
            ScheduleEntry e = rows.get(i);
            String poseLbl = e.isCustomPose() ? e.poseName() : poseLabel(e.pose());
            drawBtn(g, font.plainSubstrByWidth(poseLbl, 64), base + 166, y, 70, mouseX, mouseY,
                    e.isCustomPose() ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_AQUA);
            String emoteLbl = e.emoteId().isEmpty() ? Component.translatable("wh_npcs.ui.schedule.emote").getString()
                    : "♪ " + emoteNames.getOrDefault(e.emoteId(), e.emoteId());
            drawBtn(g, font.plainSubstrByWidth(emoteLbl, 60), base + 274, y, 66, mouseX, mouseY,
                    e.emoteId().isEmpty() ? VanillaUIHelper.TEXT_AQUA : VanillaUIHelper.TEXT_GOLD);
            drawBtn(g, Component.translatable("wh_npcs.ui.schedule.mark").getString(), base + 346, y, 60, mouseX, mouseY,
                    npc != null ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY);
            drawBtn(g, "✕", base + 410, y, 18, mouseX, mouseY, VanillaUIHelper.TEXT_RED);
            if (!e.emoteId().isEmpty() && isOver(mouseX, mouseY, base + 274, y, 66, 18)) {
                tooltip = Component.translatable("wh_npcs.ui.schedule.tip.emote",
                        emoteNames.getOrDefault(e.emoteId(), e.emoteId())).getString();
            }
            if (isOver(mouseX, mouseY, base, y, 42, 16)) {
                int min = parseHHMM(timeBoxes.get(i).getValue());
                tooltip = Component.translatable("wh_npcs.ui.schedule.tip.time", hhmm(min), timeToTicks(min)).getString();
            }
        }
        if (rows.size() < MAX_ROWS) {
            drawBtn(g, Component.translatable("wh_npcs.ui.schedule.add_row").getString(), base, listTop + rows.size() * 22, 90, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        }

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        hoverTooltip = tooltip;
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            g.renderTooltip(font, Component.literal(hoverTooltip), mouseX, mouseY);
        }
    }

    private static int timeToTicks(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return ((h - 6 + 24) % 24) * 1000 + m * 1000 / 60;
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0) {
            int base = winX + PAD;
            if (isOver(mouseX, mouseY, winX + PAD, winY + HEADER_H + 4, 130, 12)) {
                enabled = !enabled;
                return true;
            }
            for (int i = 0; i < rows.size(); i++) {
                int y = listTop + i * 22;
                if (isOver(mouseX, mouseY, base + 166, y, 70, 18)) {
                    writeBackRows();
                    final int row = i;
                    if (minecraft != null) {
                        minecraft.setScreen(PoseLibraryScreen.forPicker(this, profileJson, npc,
                                entry -> setRowActivity(row, "stand", entry.name(), buildSnapshot(entry)),
                                key -> setRowActivity(row, key, "", "")));
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, base + 274, y, 66, 18)) {
                    writeBackRows();
                    final int row = i;
                    if (minecraft != null) {
                        minecraft.setScreen(EmotecraftScreen.forPicker(this,
                                ref -> setRowEmote(row, ref.id(), ref.name(), ref.author())));
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, base + 346, y, 60, 18)) {
                    beginPick(i);
                    return true;
                }
                if (isOver(mouseX, mouseY, base + 410, y, 18, 18)) {
                    writeBackRows();
                    rows.remove(i);
                    init(minecraft, width, height);
                    return true;
                }
            }
            if (rows.size() < MAX_ROWS && isOver(mouseX, mouseY, base, listTop + rows.size() * 22, 90, 18)) {
                writeBackRows();
                int bx = npc != null ? npc.blockPosition().getX() : 0;
                int by = npc != null ? npc.blockPosition().getY() : 64;
                int bz = npc != null ? npc.blockPosition().getZ() : 0;
                rows.add(new ScheduleEntry(480, bx, by, bz, "stand", 4, "", "", ""));
                init(minecraft, width, height);
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }
        } else if (button == 1) {
            int base = winX + PAD;
            for (int i = 0; i < rows.size(); i++) {
                int y = listTop + i * 22;
                if (isOver(mouseX, mouseY, base + 274, y, 66, 18)) {
                    setRowEmote(i, "", "", "");
                    return true;
                }
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void beginPick(int i) {
        apply();
        SchedulePointPicker.begin(parent, profileJson, npc, i);
        if (minecraft != null) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("wh_npcs.msg.schedule.pick_point", (i + 1)), true);
            }
            minecraft.setScreen(null);
        }
    }

    public void setRowActivity(int i, String pose, String poseName, String snapshot) {
        if (i < 0 || i >= rows.size()) {
            return;
        }
        ScheduleEntry o = rows.get(i);
        rows.set(i, new ScheduleEntry(o.time(), o.x(), o.y(), o.z(), pose, o.radius(), poseName, snapshot,
                o.emoteId(), o.emoteName(), o.emoteAuthor()));
    }

    public void setRowEmote(int i, String emoteId, String emoteName, String emoteAuthor) {
        if (i < 0 || i >= rows.size()) {
            return;
        }
        ScheduleEntry o = rows.get(i);
        rows.set(i, new ScheduleEntry(o.time(), o.x(), o.y(), o.z(), o.pose(), o.radius(),
                o.poseName(), o.poseSnapshot(), emoteId == null ? "" : emoteId,
                emoteName == null ? "" : emoteName, emoteAuthor == null ? "" : emoteAuthor));
    }

    private static String buildSnapshot(PoseEntry e) {
        JsonObject snap = new JsonObject();
        try {
            snap.add("pose", JsonParser.parseString(e.pose()));
            if (e.transform() != null && !e.transform().isBlank()) {
                snap.add("transform", JsonParser.parseString(e.transform()));
            }
        } catch (Exception ignored) {

        }
        return snap.has("pose") ? snap.toString() : "";
    }

    private void apply() {
        writeBackRows();
        if (rows.isEmpty()) {
            profileJson.remove("schedule");
        } else {
            JsonArray arr = new JsonArray();
            for (ScheduleEntry e : rows) {
                arr.add(e.toJson());
            }
            profileJson.add("schedule", arr);
        }
        if (enabled) {
            profileJson.addProperty("schedule_enabled", true);
        } else {
            profileJson.remove("schedule_enabled");
        }
    }

    @Override
    public void onClose() {
        apply();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5, hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private static boolean isWander(ScheduleEntry e) {
        return !e.isCustomPose() && "wander".equals(e.pose());
    }

    private static String poseLabel(String pose) {
        for (int i = 0; i < POSES.length; i++) {
            if (POSES[i].equals(pose)) {
                return Component.translatable(POSE_LABEL_KEYS[i]).getString();
            }
        }
        return Component.translatable(POSE_LABEL_KEYS[0]).getString();
    }

    private static String hhmm(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private static int parseHHMM(String s) {
        s = s.trim();
        try {
            int c = s.indexOf(':');
            if (c < 0) {
                return clamp(Integer.parseInt(s) * 60, 0, 1439);
            }
            int h = Integer.parseInt(s.substring(0, c).trim());
            int m = Integer.parseInt(s.substring(c + 1).trim());
            return clamp(h * 60 + m, 0, 1439);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
