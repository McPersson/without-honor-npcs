package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ScheduleTransformScreen extends ScaledScreen {

    private static final int WIN_W = 496;
    private static final int WIN_H = 134;
    private static final int BTN_W = 12, BOX_W = 38, CTRL_H = 18;
    private static final int CELL_PITCH = 74;
    private static final String[] AXIS_LABELS = {"X", "Y", "Z"};

    private static final String[] KEYS = {"rot_x", "rot_y", "rot_z", "pos_x", "pos_y", "pos_z"};
    private static final float[] MIN = {-180F, -180F, -180F, -3F, -3F, -3F};
    private static final float[] MAX = {180F, 180F, 180F, 3F, 3F, 3F};
    private static final float[] STEP = {5F, 5F, 5F, 0.1F, 0.1F, 0.1F};
    private static final float[] SENS = {1F, 1F, 1F, 0.02F, 0.02F, 0.02F};

    private final Screen parent;
    private final JsonObject tf;
    @Nullable
    private final CompanionEntity npc;
    private final BiConsumer<JsonObject, Boolean> onApply;
    private boolean freeze;

    private final EditBox[] box = new EditBox[6];
    @Nullable
    private EditBox scrubBox;
    private int scrubIndex;
    private double scrubStartX;
    private float scrubStartVal;
    private boolean scrubbing;

    private int winX, winY, winW, winH;
    @Nullable
    private String tooltip;

    public ScheduleTransformScreen(Screen parent, JsonObject tf, @Nullable CompanionEntity npc,
                                   boolean freeze, BiConsumer<JsonObject, Boolean> onApply) {
        super(Component.translatable("wh_npcs.ui.schedule_tf.title"));
        this.parent = parent;
        this.tf = tf;
        this.npc = npc;
        this.freeze = freeze;
        this.onApply = onApply;
    }

    @Override
    protected int designW() {
        return WIN_W;
    }

    @Override
    protected int designH() {
        return WIN_H;
    }

    @Override
    protected void renderDim(GuiGraphics g) {
    }

    private void recalc() {
        winW = WIN_W;
        winH = WIN_H;
        winX = (width - winW) / 2;
        winY = 24;
    }

    private int cellX(int i) {
        int group = i / 3;
        int col = i % 3;
        int origin = group == 0 ? winX + 14 : winX + 260;
        return origin + col * CELL_PITCH;
    }

    private int boxX(int i) {
        return cellX(i) + BTN_W + 3;
    }

    private int plusX(int i) {
        return boxX(i) + BOX_W + 3;
    }

    private int rowY() {
        return winY + 38;
    }

    private int freezeY() {
        return winY + 84;
    }

    private int closeX() {
        return winX + winW - 18;
    }

    private int resetX() {
        return winX + 14;
    }

    private int doneX() {
        return winX + winW - 14 - 70;
    }

    private int bottomY() {
        return winY + winH - 26;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            final String key = KEYS[i];
            final float minV = MIN[i], maxV = MAX[i], defV = 0F;
            EditBox b = addRenderableWidget(new SelectableEditBox(
                    font, boxX(i), rowY(), BOX_W, CTRL_H, Component.literal(key)));
            b.setMaxLength(7);
            b.setValue(fmtNum(numF(key)));
            b.setResponder(v -> {
                String s = v.trim().replace(',', '.');
                if (s.isEmpty()) {
                    tf.remove(key);
                    pushPreview();
                    return;
                }
                try {
                    float p = Math.max(minV, Math.min(maxV, Float.parseFloat(s)));
                    if (p == defV) {
                        tf.remove(key);
                    } else {
                        tf.addProperty(key, p);
                    }
                } catch (NumberFormatException ignored) {
                    return;
                }
                pushPreview();
            });
            box[idx] = b;
        }
        pushPreview();
    }

    private float numF(String key) {
        return tf.has(key) ? tf.get(key).getAsFloat() : 0F;
    }

    private float scaleF(String key) {
        return tf.has(key) ? tf.get(key).getAsFloat() : 1F;
    }

    private static String fmtNum(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }

    private void pushPreview() {
        if (npc != null) {
            npc.setRenderTransformClient(
                    numF("rot_x"), numF("rot_y"), numF("rot_z"),
                    numF("pos_x"), numF("pos_y"), numF("pos_z"),
                    scaleF("scale_x"), scaleF("scale_y"), scaleF("scale_z"));
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        tooltip = null;

        if (npc != null && freeze) {
            npc.setYRot(0F);
            npc.yRotO = 0F;
            npc.yBodyRot = 0F;
            npc.yBodyRotO = 0F;
            npc.setYHeadRot(0F);
            npc.yHeadRotO = 0F;
        }

        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + 18, VanillaUIHelper.BG_HEADER);
        g.drawString(font, getTitle().getString(), winX + 8, winY + 6, VanillaUIHelper.TEXT_YELLOW, false);

        boolean cHover = isOver(mouseX, mouseY, closeX(), winY + 4, 14, 12);
        g.drawString(font, "§l✕", closeX() + 3, winY + 6, cHover ? 0xFFFF5555 : 0xFFC0C0C0, false);

        g.drawString(font, Component.translatable("wh_npcs.ui.schedule_tf.rot").getString(),
                winX + 14, winY + 24, VanillaUIHelper.TEXT_AQUA, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.schedule_tf.pos").getString(),
                winX + 260, winY + 24, VanillaUIHelper.TEXT_AQUA, false);

        for (int i = 0; i < 6; i++) {
            stepper(g, i, mouseX, mouseY);
            g.drawCenteredString(font, AXIS_LABELS[i % 3], boxX(i) + BOX_W / 2, rowY() + 22,
                    VanillaUIHelper.TEXT_DARK_GRAY);
        }

        boolean fHover = isOver(mouseX, mouseY, winX + 14, freezeY(), 12, 12);
        VanillaUIHelper.drawButton(g, winX + 14, freezeY(), 12, 12, fHover);
        if (freeze) {
            VanillaUIHelper.drawCheck(g, winX + 15, freezeY() + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.schedule_tf.freeze").getString(),
                winX + 32, freezeY() + 2, VanillaUIHelper.TEXT_GRAY, false);
        if (fHover || isOver(mouseX, mouseY, winX + 32, freezeY(), 150, 12)) {
            tooltip = Component.translatable("wh_npcs.ui.schedule_tf.freeze_tip").getString();
        }

        drawBtn(g, Component.translatable("wh_npcs.ui.schedule_tf.reset").getString(),
                resetX(), bottomY(), 70, mouseX, mouseY, VanillaUIHelper.TEXT_RED);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(),
                doneX(), bottomY(), 70, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (tooltip != null) {
            List<Component> lines = new ArrayList<>();
            for (String line : tooltip.split("\n")) {
                lines.add(Component.literal(line));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void stepper(GuiGraphics g, int i, int mouseX, int mouseY) {
        int minusX = cellX(i), px = plusX(i), y = rowY();
        boolean mh = isOver(mouseX, mouseY, minusX, y, BTN_W, CTRL_H);
        VanillaUIHelper.drawButton(g, minusX, y, BTN_W, CTRL_H, mh);
        g.drawCenteredString(font, "-", minusX + BTN_W / 2, y + 5,
                mh ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean ph = isOver(mouseX, mouseY, px, y, BTN_W, CTRL_H);
        VanillaUIHelper.drawButton(g, px, y, BTN_W, CTRL_H, ph);
        g.drawCenteredString(font, "+", px + BTN_W / 2, y + 5,
                ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
    }

    @Override
    protected boolean mouseClickedScaled(double mx, double my, int button) {
        recalc();
        if (button == 0 && isOver(mx, my, closeX(), winY + 4, 14, 12)) {
            onClose();
            return true;
        }
        if (button == 0 && isOver(mx, my, winX + 14, freezeY(), 150, 12)) {
            freeze = !freeze;
            return true;
        }
        if (button == 0 && isOver(mx, my, resetX(), bottomY(), 70, 18)) {
            for (int i = 0; i < 6; i++) {
                box[i].setValue("0");
            }
            freeze = true;
            return true;
        }
        if (button == 0 && isOver(mx, my, doneX(), bottomY(), 70, 18)) {
            onApply.accept(tf, freeze);
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
            return true;
        }
        int y = rowY();
        for (int i = 0; i < 6; i++) {
            String key = KEYS[i];
            if (button == 0 && isOver(mx, my, cellX(i), y, BTN_W, CTRL_H)) {
                adjust(i, numF(key), -STEP[i]);
                return true;
            }
            if (button == 0 && isOver(mx, my, plusX(i), y, BTN_W, CTRL_H)) {
                adjust(i, numF(key), STEP[i]);
                return true;
            }
            if (button == 1 && isOver(mx, my, boxX(i), y, BOX_W, CTRL_H)) {
                box[i].setValue("0");
                return true;
            }
            if (button == 0 && isOver(mx, my, boxX(i), y, BOX_W, CTRL_H)) {
                scrubBox = box[i];
                scrubIndex = i;
                scrubStartVal = numF(key);
                scrubStartX = mx;
                scrubbing = false;
            }
        }
        return superMouseClicked(mx, my, button);
    }

    private void adjust(int i, float base, float delta) {
        float v = Math.max(MIN[i], Math.min(MAX[i], base + delta));
        v = Math.round(v * 100F) / 100F;
        box[i].setValue(fmtNum(v));
    }

    @Override
    protected boolean mouseDraggedScaled(double mx, double my, int button, double dx, double dy) {
        if (scrubBox != null && button == 0) {
            double delta = mx - scrubStartX;
            if (Math.abs(delta) > 3) {
                scrubbing = true;
                scrubBox.setFocused(false);
                setFocused(null);
            }
            if (scrubbing) {
                float v = Math.max(MIN[scrubIndex], Math.min(MAX[scrubIndex],
                        scrubStartVal + (float) (delta * SENS[scrubIndex])));
                v = Math.round(v * 100F) / 100F;
                scrubBox.setValue(fmtNum(v));
            }
            return true;
        }
        return superMouseDragged(mx, my, button, dx, dy);
    }

    @Override
    protected boolean mouseReleasedScaled(double mx, double my, int button) {
        scrubBox = null;
        scrubbing = false;
        return superMouseReleased(mx, my, button);
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
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
