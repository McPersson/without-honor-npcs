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

public class PoseQuickAdjustScreen extends ScaledScreen {

    private static final String[] AXIS = {"x", "y", "z"};
    private static final String[] AXIS_LABELS = {"X", "Y", "Z"};
    private static final int BTN_W = 12, BOX_W = 40, CTRL_H = 18;

    private final Screen parent;
    private final JsonObject profileJson;
    @Nullable
    private final CompanionEntity npc;
    private final boolean positionMode;
    private final String prefix;
    private final float sens, step, min, max, def;

    private final EditBox[] box = new EditBox[3];

    @Nullable
    private EditBox scrubBox;
    private double scrubStartX;
    private float scrubStartVal;
    private boolean scrubbing;

    private int winX, winY, winW, winH;

    public PoseQuickAdjustScreen(Screen parent, JsonObject profileJson,
                                 @Nullable CompanionEntity npc, boolean positionMode) {
        super(Component.translatable(positionMode ? "wh_npcs.ui.pose_quick.title_pos" : "wh_npcs.ui.pose_quick.title_rot"));
        this.parent = parent;
        this.profileJson = profileJson;
        this.npc = npc;
        this.positionMode = positionMode;
        this.prefix = positionMode ? "pos_" : "rot_";
        if (positionMode) {
            this.sens = 0.02F;
            this.step = 0.1F;
            this.min = -3F;
            this.max = 3F;
            this.def = 0F;
        } else {
            this.sens = 1.0F;
            this.step = 5F;
            this.min = -180F;
            this.max = 180F;
            this.def = 0F;
        }
    }

    private float numF(String key) {
        return profileJson.has(key) ? profileJson.get(key).getAsFloat() : def;
    }

    private static String fmtNum(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }

    private static final int WIN_W = 276;
    private static final int WIN_H = 72;

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
        return winX + 16 + i * 86;
    }

    private int boxX(int i) {
        return cellX(i) + BTN_W + 4;
    }

    private int plusX(int i) {
        return boxX(i) + BOX_W + 4;
    }

    private int closeX() {
        return winX + winW - 18;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        for (int i = 0; i < 3; i++) {
            String key = prefix + AXIS[i];
            float minV = min, maxV = max, defV = def;
            EditBox b = addRenderableWidget(new SelectableEditBox(
                    font, boxX(i), winY + 34, BOX_W, CTRL_H, Component.literal(key)));
            b.setMaxLength(7);
            b.setValue(fmtNum(numF(key)));
            b.setResponder(v -> {
                String s = v.trim().replace(',', '.');
                if (s.isEmpty()) {
                    profileJson.remove(key);
                    pushPreview();
                    return;
                }
                try {
                    float p = Math.max(minV, Math.min(maxV, Float.parseFloat(s)));
                    if (p == defV) {
                        profileJson.remove(key);
                    } else {
                        profileJson.addProperty(key, p);
                    }
                } catch (NumberFormatException ignored) {
                    return;
                }
                pushPreview();
            });
            box[i] = b;
        }
    }

    private void pushPreview() {
        if (npc != null) {
            npc.setRenderTransformClient(
                    f("rot_x"), f("rot_y"), f("rot_z"),
                    f("pos_x"), f("pos_y"), f("pos_z"),
                    g("scale_x"), g("scale_y"), g("scale_z"));
        }
    }

    private float f(String key) {
        return profileJson.has(key) ? profileJson.get(key).getAsFloat() : 0F;
    }

    private float g(String key) {
        return profileJson.has(key) ? profileJson.get(key).getAsFloat() : 1F;
    }

    @Override
    protected void renderContent(GuiGraphics gr, int mouseX, int mouseY, float partialTick) {

        recalc();
        VanillaUIHelper.drawWindow(gr, winX, winY, winW, winH);
        gr.fill(winX + 2, winY + 2, winX + winW - 2, winY + 18, VanillaUIHelper.BG_HEADER);
        gr.drawString(font, getTitle().getString(), winX + 8, winY + 6, VanillaUIHelper.TEXT_YELLOW, false);

        boolean cHover = isOver(mouseX, mouseY, closeX(), winY + 4, 14, 12);
        gr.drawString(font, "§l✕", closeX() + 3, winY + 6, cHover ? 0xFFFF5555 : 0xFFC0C0C0, false);

        for (int i = 0; i < 3; i++) {
            stepper(gr, i, mouseX, mouseY);
            gr.drawCenteredString(font, AXIS_LABELS[i], boxX(i) + BOX_W / 2, winY + 56,
                    VanillaUIHelper.TEXT_DARK_GRAY);
        }
    }

    private void stepper(GuiGraphics gr, int i, int mouseX, int mouseY) {
        int minusX = cellX(i), px = plusX(i), y = winY + 34;
        boolean mh = isOver(mouseX, mouseY, minusX, y, BTN_W, CTRL_H);
        VanillaUIHelper.drawButton(gr, minusX, y, BTN_W, CTRL_H, mh);
        gr.drawCenteredString(font, "-", minusX + BTN_W / 2, y + 5,
                mh ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean ph = isOver(mouseX, mouseY, px, y, BTN_W, CTRL_H);
        VanillaUIHelper.drawButton(gr, px, y, BTN_W, CTRL_H, ph);
        gr.drawCenteredString(font, "+", px + BTN_W / 2, y + 5,
                ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
    }

    @Override
    protected boolean mouseClickedScaled(double mx, double my, int button) {
        recalc();
        if (button == 0 && isOver(mx, my, closeX(), winY + 4, 14, 12)) {
            onClose();
            return true;
        }
        int y = winY + 34;
        for (int i = 0; i < 3; i++) {
            String key = prefix + AXIS[i];
            if (button == 0 && isOver(mx, my, cellX(i), y, BTN_W, CTRL_H)) {
                adjust(i, numF(key), -step);
                return true;
            }
            if (button == 0 && isOver(mx, my, plusX(i), y, BTN_W, CTRL_H)) {
                adjust(i, numF(key), step);
                return true;
            }
            if (button == 1 && isOver(mx, my, boxX(i), y, BOX_W, CTRL_H)) {
                box[i].setValue(fmtNum(def));
                return true;
            }
            if (button == 0 && isOver(mx, my, boxX(i), y, BOX_W, CTRL_H)) {
                scrubBox = box[i];
                scrubStartVal = numF(key);
                scrubStartX = mx;
                scrubbing = false;

            }
        }
        return superMouseClicked(mx, my, button);
    }

    private void adjust(int i, float base, float delta) {
        float v = Math.max(min, Math.min(max, base + delta));
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
                float v = Math.max(min, Math.min(max, scrubStartVal + (float) (delta * sens)));
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
