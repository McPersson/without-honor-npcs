package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.PoseJson;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PoseEditorScreen extends ScaledScreen {

    private static final String[] PART_KEYS = {
            "wh_npcs.ui.pose_edit.part_head", "wh_npcs.ui.pose_edit.part_body",
            "wh_npcs.ui.pose_edit.part_right_arm", "wh_npcs.ui.pose_edit.part_left_arm",
            "wh_npcs.ui.pose_edit.part_right_leg", "wh_npcs.ui.pose_edit.part_left_leg"};

    private static String partName(int p) {
        return Component.translatable(PART_KEYS[p]).getString();
    }
    private static final String[] AXIS_LABELS = {"X", "Y", "Z"};

    private static final int[] AXIS_COLORS = {0xFFE06666, 0xFF6CC36C, 0xFF6E9BE6};

    private static final float[] SNAP_TARGETS = {0F, 90F, -90F, 180F, -180F};
    private static final float SNAP_THRESHOLD = 7F;
    private static final int BTN_W = 10, BOX_W = 28, IN_GAP = 2, CTRL_H = 16;
    private static final int UNIT_PITCH = 63, ROW_PITCH = 56, PV_W = 116;
    private static final int GROUP_W = 2 * UNIT_PITCH + BTN_W + IN_GAP + BOX_W + IN_GAP + BTN_W;
    private static final float STEP = 5F;
    private static final float[] LYING_BB = {1.6F, 0.6F};

    private final Screen parent;
    private final JsonObject profileJson;
    @Nullable
    private final CompanionEntity npc;

    private final PoseJson.Pose pose = new PoseJson.Pose();
    private final EditBox[] box = new EditBox[18];
    private boolean refreshing;
    private int hoverPart = -1;

    @Nullable
    private CompanionEntity previewNpc;
    private float spin = 180F;
    private long lastMs;
    private int frozenTick;
    private boolean rotDrag, paused = true;

    @Nullable
    private EditBox scrubBox;
    private int scrubIdx;
    private double scrubStartX;
    private float scrubStartVal;
    private boolean scrubbing;

    private int winX, winY, winW, winH;

    public PoseEditorScreen(Screen parent, JsonObject profileJson, @Nullable CompanionEntity npc) {
        super(Component.translatable("wh_npcs.ui.pose_edit.title_short"));
        this.parent = parent;
        this.profileJson = profileJson;
        this.npc = npc;
    }

    @Override
    protected int designW() {
        return 528;
    }

    @Override
    protected int designH() {
        return 270;
    }

    private void recalc() {
        winW = 528;
        winH = 270;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
    }

    private int prevX() {
        return winX + (winW - PV_W) / 2;
    }

    private int prevY() {
        return winY + 32;
    }

    private int prevH() {
        return winH - 84;
    }

    private int leftColX() {
        return winX + 14;
    }

    private int rightColX() {
        return prevX() + PV_W + 14;
    }

    private int colX(int part) {
        return (part % 2 == 0) ? leftColX() : rightColX();
    }

    private int rowTop(int part) {
        return winY + 32 + (part / 2) * ROW_PITCH;
    }

    private int minusX(int part, int axis) {
        return colX(part) + axis * UNIT_PITCH;
    }

    private int boxX(int part, int axis) {
        return minusX(part, axis) + BTN_W + IN_GAP;
    }

    private int plusX(int part, int axis) {
        return boxX(part, axis) + BOX_W + IN_GAP;
    }

    private int boxY(int part) {
        return rowTop(part) + 22;
    }

    private static String fmtNum(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        PoseJson.read(profileJson, pose);
        for (int p = 0; p < 6; p++) {
            for (int k = 0; k < 3; k++) {
                final int idx = p * 3 + k;
                EditBox b = addRenderableWidget(new SelectableEditBox(
                        font, boxX(p, k), boxY(p), BOX_W, CTRL_H, Component.literal("a" + idx)));
                b.setMaxLength(7);
                b.setValue(fmtNum(pose.angles[idx]));

                b.setFormatter((s, i) -> s.isEmpty()
                        ? net.minecraft.util.FormattedCharSequence.EMPTY
                        : net.minecraft.util.FormattedCharSequence.forward(
                                s + "°", net.minecraft.network.chat.Style.EMPTY));
                b.setResponder(v -> {
                    String s = v.trim().replace(',', '.');
                    float val = 0F;
                    if (!s.isEmpty()) {
                        try {
                            val = Math.max(-180F, Math.min(180F, Float.parseFloat(s)));
                        } catch (NumberFormatException ignored) {
                            return;
                        }
                    }
                    pose.angles[idx] = val;
                    if (!refreshing) {
                        sync();
                    }
                });
                box[idx] = b;
            }
        }
        pushPreview();
    }

    @Nullable
    private CompanionEntity preview() {
        if (previewNpc == null && minecraft != null && minecraft.level != null) {
            previewNpc = com.withouthonor.npcs.common.registry.ModEntities.COMPANION.get()
                    .create(minecraft.level);
        }
        return previewNpc;
    }

    private void sync() {
        PoseJson.write(profileJson, pose);
        pushPreview();
    }

    private void pushPreview() {
        if (npc != null) {
            npc.setPoseClient(pose);
        }
        if (previewNpc != null) {
            previewNpc.setPoseClient(pose);
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + 18, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.pose_edit.title").getString(), winX + 8, winY + 6, VanillaUIHelper.TEXT_YELLOW, false);
        boolean cHover = isOver(mouseX, mouseY, winX + winW - 16, winY + 4, 12, 12);
        g.drawString(font, "✕", winX + winW - 14, winY + 6, cHover ? 0xFFFF5555 : 0xFFC0C0C0, false);

        hoverPart = computeHoverPart(mouseX, mouseY);
        renderPreview(g, mouseX, mouseY);

        for (int p = 0; p < 6; p++) {
            int cx = colX(p), top = rowTop(p);
            boolean visible = !pose.hidden[p];
            if (p == hoverPart) {
                g.fill(cx - 2, top - 1, cx + GROUP_W, top + 38, 0x22FFFFFF);
            }
            boolean cbHover = isOver(mouseX, mouseY, cx, top, 10, 10);
            VanillaUIHelper.drawButton(g, cx, top, 10, 10, cbHover);
            if (visible) {
                VanillaUIHelper.drawCheck(g, cx + 1, top + 1, VanillaUIHelper.TEXT_GREEN);
            }
            g.drawString(font, partName(p), cx + 14, top + 1,
                    p == hoverPart ? VanillaUIHelper.TEXT_YELLOW
                            : (visible ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY), false);
            for (int k = 0; k < 3; k++) {
                g.drawCenteredString(font, AXIS_LABELS[k], boxX(p, k) + BOX_W / 2, top + 13, AXIS_COLORS[k]);
                stepper(g, p, k, mouseX, mouseY);
            }
        }

        int row1 = winY + winH - 46;
        drawCheckbox(g, leftColX(), row1, Component.translatable("wh_npcs.ui.pose_edit.freeze").getString(), pose.freeze, mouseX, mouseY);
        drawBtn(g, Component.translatable("wh_npcs.ui.pose_edit.mirror").getString(), winX + winW - 14 - 116 - 6 - 86, row1 - 3, 86, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        drawBtn(g, Component.translatable("wh_npcs.ui.pose_edit.symmetry").getString(), winX + winW - 14 - 116, row1 - 3, 116, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);

        int row2 = winY + winH - 24;
        drawCheckbox(g, leftColX(), row2, Component.translatable("wh_npcs.ui.pose_edit.lying_hitbox").getString(), pose.bb[0] > 0F, mouseX, mouseY);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - 14 - 64, row2 - 3, 64, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        drawBtn(g, Component.translatable("wh_npcs.ui.pose_edit.library").getString(), winX + winW - 14 - 64 - 6 - 110, row2 - 3, 110, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.reset").getString(), winX + winW - 14 - 64 - 6 - 110 - 6 - 84, row2 - 3, 84, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        int row1 = winY + winH - 46;
        int row2 = winY + winH - 24;
        renderTooltips(g, mouseX, mouseY, row1, row2);
    }

    private void renderTooltips(GuiGraphics g, int mouseX, int mouseY, int row1, int row2) {
        if (isOver(mouseX, mouseY, leftColX(), row1, 16 + font.width(Component.translatable("wh_npcs.ui.pose_edit.freeze").getString()), 12)) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.pose_edit.freeze_tip").getString(), mouseX, mouseY);
            return;
        }
        if (isOver(mouseX, mouseY, leftColX(), row2, 16 + font.width(Component.translatable("wh_npcs.ui.pose_edit.lying_hitbox").getString()), 12)) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.pose_edit.lying_hitbox_tip").getString(), mouseX, mouseY);
            return;
        }
        for (int p = 0; p < 6; p++) {
            if (isOver(mouseX, mouseY, colX(p), rowTop(p), 10, 10)) {
                multilineTooltip(g, Component.translatable("wh_npcs.ui.pose_edit.visibility_tip", partName(p)).getString(), mouseX, mouseY);
                return;
            }
        }
    }

    private void drawCheckbox(GuiGraphics g, int x, int y, String label, boolean on, int mouseX, int mouseY) {
        boolean hover = isOver(mouseX, mouseY, x, y, 12, 12);
        VanillaUIHelper.drawButton(g, x, y, 12, 12, hover);
        if (on) {
            VanillaUIHelper.drawCheck(g, x + 1, y + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, label, x + 16, y + 2, VanillaUIHelper.TEXT_GRAY, false);
    }

    private void stepper(GuiGraphics g, int part, int axis, int mouseX, int mouseY) {
        int mX = minusX(part, axis), pX = plusX(part, axis), y = boxY(part);
        boolean mh = isOver(mouseX, mouseY, mX, y, BTN_W, CTRL_H);
        VanillaUIHelper.drawButton(g, mX, y, BTN_W, CTRL_H, mh);
        g.drawCenteredString(font, "-", mX + BTN_W / 2 + 1, y + 4, mh ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean ph = isOver(mouseX, mouseY, pX, y, BTN_W, CTRL_H);
        VanillaUIHelper.drawButton(g, pX, y, BTN_W, CTRL_H, ph);
        g.drawCenteredString(font, "+", pX + BTN_W / 2 + 1, y + 4, ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
    }

    private void renderPreview(GuiGraphics g, int mouseX, int mouseY) {
        int x = prevX(), y = prevY(), w = PV_W, h = prevH();
        VanillaUIHelper.drawContentPanel(g, x, y, w, h);
        CompanionEntity p = preview();
        if (p == null || minecraft == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.pose_edit.no_preview").getString(), x + w / 2, y + h / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        p.setSkinName(str("skin_player_name"));
        p.setPoseClient(pose);
        long now = System.currentTimeMillis();
        if (!rotDrag && !paused) {
            spin = (spin + (now - lastMs) * 0.04F) % 360.0F;
        }
        lastMs = now;
        if (minecraft.level != null) {
            if (!paused) {
                frozenTick = (int) minecraft.level.getGameTime();
            }
            p.tickCount = frozenTick;
        }
        float scale = (h - 48.0F) / 2.2F;
        ScaledScreen.enableScissor(g, x + 2, y + 2, x + w - 2, y + h - 2);
        renderRotating(g, p, x + w / 2, y + h / 2 + (int) (scale * 0.9F), scale, spin);
        g.disableScissor();
        if (hoverPart >= 0) {
            g.drawString(font, "§e▸ " + partName(hoverPart), x + 4, y + 4, VanillaUIHelper.TEXT_WHITE, false);
        }
        int pbx = x + w - 20, pby = y + 4;
        boolean ph = isOver(mouseX, mouseY, pbx, pby, 16, 16);
        VanillaUIHelper.drawButton(g, pbx, pby, 16, 16, ph);
        int ic = ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY;
        if (paused) {
            g.fill(pbx + 6, pby + 4, pbx + 7, pby + 12, ic);
            g.fill(pbx + 7, pby + 5, pbx + 9, pby + 11, ic);
            g.fill(pbx + 9, pby + 6, pbx + 11, pby + 10, ic);
        } else {
            g.fill(pbx + 5, pby + 4, pbx + 7, pby + 12, ic);
            g.fill(pbx + 9, pby + 4, pbx + 11, pby + 12, ic);
        }
    }

    private void renderRotating(GuiGraphics g, CompanionEntity e, int x, int y, float scale, float angle) {
        e.yBodyRot = angle;
        e.yBodyRotO = angle;
        e.yHeadRot = angle;
        e.yHeadRotO = angle;
        e.setYRot(angle);
        e.yRotO = angle;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 100);
        pose.scale(scale, scale, -scale);
        pose.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.PI));
        pose.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(-12)));
        var dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        var buffer = minecraft.renderBuffers().bufferSource();
        try {
            dispatcher.render(e, 0, 0, 0, 0, 1.0F, pose, buffer,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        } catch (Exception ignored) {

        }
        buffer.endBatch();
        dispatcher.setRenderShadow(true);
        pose.popPose();
    }

    private String str(String key) {
        return profileJson.has(key) && !profileJson.get(key).isJsonNull()
                ? profileJson.get(key).getAsString() : "";
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5, hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    @Override
    protected boolean mouseClickedScaled(double mx, double my, int button) {
        recalc();
        if (button == 0 && isOver(mx, my, winX + winW - 16, winY + 4, 14, 12)) {
            onClose();
            return true;
        }
        if (button == 0 && isOver(mx, my, prevX() + PV_W - 20, prevY() + 4, 16, 16)) {
            paused = !paused;
            return true;
        }
        if (button == 0 && isOver(mx, my, prevX(), prevY(), PV_W, prevH())) {
            rotDrag = true;
            return true;
        }

        if (button == 0) {
            for (int p = 0; p < 6; p++) {
                if (isOver(mx, my, colX(p), rowTop(p), 10, 10)) {
                    pose.hidden[p] = !pose.hidden[p];
                    sync();
                    return true;
                }
            }
        }
        int row1 = winY + winH - 46;
        int row2 = winY + winH - 24;
        if (button == 0 && isOver(mx, my, leftColX(), row1, 12, 12)) {
            pose.freeze = !pose.freeze;
            sync();
            return true;
        }
        if (button == 0 && isOver(mx, my, winX + winW - 14 - 116 - 6 - 86, row1 - 3, 86, 18)) {
            mirrorAll();
            return true;
        }
        if (button == 0 && isOver(mx, my, winX + winW - 14 - 116, row1 - 3, 116, 18)) {
            symmetryRtoL();
            return true;
        }
        if (button == 0 && isOver(mx, my, leftColX(), row2, 12, 12)) {
            boolean on = pose.bb[0] > 0F;
            pose.bb[0] = on ? 0F : LYING_BB[0];
            pose.bb[1] = on ? 0F : LYING_BB[1];
            sync();
            return true;
        }
        if (button == 0 && isOver(mx, my, winX + winW - 14 - 64, row2 - 3, 64, 18)) {
            onClose();
            return true;
        }
        if (button == 0 && isOver(mx, my, winX + winW - 14 - 64 - 6 - 110, row2 - 3, 110, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new PoseLibraryScreen(this, profileJson, npc));
            }
            return true;
        }
        if (button == 0 && isOver(mx, my, winX + winW - 14 - 64 - 6 - 110 - 6 - 84, row2 - 3, 84, 18)) {
            resetAll();
            return true;
        }

        for (int p = 0; p < 6; p++) {
            for (int k = 0; k < 3; k++) {
                int idx = p * 3 + k;
                int y = boxY(p);
                if (button == 0 && isOver(mx, my, minusX(p, k), y, BTN_W, CTRL_H)) {
                    setBox(idx, pose.angles[idx] - STEP);
                    return true;
                }
                if (button == 0 && isOver(mx, my, plusX(p, k), y, BTN_W, CTRL_H)) {
                    setBox(idx, pose.angles[idx] + STEP);
                    return true;
                }
                if (isOver(mx, my, boxX(p, k), y, BOX_W, CTRL_H)) {
                    if (button == 1) {
                        setBox(idx, 0F);
                        return true;
                    }
                    if (button == 0) {
                        scrubBox = box[idx];
                        scrubIdx = idx;
                        scrubStartVal = pose.angles[idx];
                        scrubStartX = mx;
                        scrubbing = false;
                    }
                }
            }
        }
        return superMouseClicked(mx, my, button);
    }

    private void setBox(int idx, float v) {
        if (box[idx] == null) {
            return;
        }
        v = Math.round(Math.max(-180F, Math.min(180F, v)));
        box[idx].setValue(fmtNum(v));
    }

    @Override
    protected boolean mouseDraggedScaled(double mx, double my, int button, double dxr, double dyr) {
        if (rotDrag && button == 0) {
            spin = (spin + (float) dxr * 1.5F) % 360.0F;
            return true;
        }
        if (scrubBox != null && button == 0) {
            double delta = mx - scrubStartX;
            if (Math.abs(delta) > 3) {
                scrubbing = true;
                scrubBox.setFocused(false);
                setFocused(null);
            }
            if (scrubbing) {
                float raw = scrubStartVal + (float) delta;
                setBox(scrubIdx, hasShiftDown() ? raw : snapAngle(raw));
            }
            return true;
        }
        return superMouseDragged(mx, my, button, dxr, dyr);
    }

    @Override
    protected boolean mouseReleasedScaled(double mx, double my, int button) {
        rotDrag = false;
        scrubBox = null;
        scrubbing = false;
        return superMouseReleased(mx, my, button);
    }

    private void mirrorAll() {
        swapTriple(6, 9);
        swapTriple(12, 15);
        boolean t = pose.hidden[2];
        pose.hidden[2] = pose.hidden[3];
        pose.hidden[3] = t;
        t = pose.hidden[4];
        pose.hidden[4] = pose.hidden[5];
        pose.hidden[5] = t;
        for (int p = 0; p < 6; p++) {
            pose.angles[p * 3 + 1] = -pose.angles[p * 3 + 1];
            pose.angles[p * 3 + 2] = -pose.angles[p * 3 + 2];
        }
        refreshBoxes();
    }

    private void symmetryRtoL() {
        pose.angles[9] = pose.angles[6];
        pose.angles[10] = -pose.angles[7];
        pose.angles[11] = -pose.angles[8];
        pose.hidden[3] = pose.hidden[2];
        pose.angles[15] = pose.angles[12];
        pose.angles[16] = -pose.angles[13];
        pose.angles[17] = -pose.angles[14];
        pose.hidden[5] = pose.hidden[4];
        refreshBoxes();
    }

    private void swapTriple(int a, int b) {
        for (int k = 0; k < 3; k++) {
            float t = pose.angles[a + k];
            pose.angles[a + k] = pose.angles[b + k];
            pose.angles[b + k] = t;
        }
    }

    private void refreshBoxes() {
        refreshing = true;
        for (int i = 0; i < 18; i++) {
            if (box[i] != null) {
                box[i].setValue(fmtNum(pose.angles[i]));
            }
        }
        refreshing = false;
        sync();
    }

    private void resetAll() {
        refreshing = true;
        for (int i = 0; i < 18; i++) {
            if (box[i] != null) {
                box[i].setValue("0");
            }
        }
        refreshing = false;
        pose.clear();
        sync();
    }

    @Override
    public void onClose() {
        sync();
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

    private int computeHoverPart(int mouseX, int mouseY) {
        for (int p = 0; p < 6; p++) {
            if (isOver(mouseX, mouseY, colX(p), rowTop(p), GROUP_W, 38)) {
                return p;
            }
        }
        return -1;
    }

    private static float snapAngle(float v) {
        for (float t : SNAP_TARGETS) {
            if (Math.abs(v - t) <= SNAP_THRESHOLD) {
                return t;
            }
        }
        return v;
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
