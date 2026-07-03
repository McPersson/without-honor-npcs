package com.withouthonor.npcs.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

import javax.annotation.Nullable;

/**
 * Стандартное числовое поле: EditBox + кнопки −/+ по бокам, драг-скраб по полю,
 * ПКМ — сброс. Экран создаёт EditBox сам (для persist при resize) и делегирует
 * сюда render/mouseClicked/mouseDragged/mouseReleased.
 */
public final class NumberScrub {

    private static final int BTN_W = 12;

    private final EditBox box;
    private final float min, max, step, sens;
    private final String resetValue;
    private final boolean integer;
    @Nullable
    private final Runnable clearFocus;

    private boolean armed;
    private boolean scrubbing;
    private double startX;
    private float startVal;

    public NumberScrub(EditBox box, float min, float max, float step, float sens,
                       String resetValue, boolean integer, @Nullable Runnable clearFocus) {
        this.box = box;
        this.min = min;
        this.max = max;
        this.step = step;
        this.sens = sens;
        this.resetValue = resetValue;
        this.integer = integer;
        this.clearFocus = clearFocus;
    }

    public static String fmt(float v, boolean integer) {
        if (integer || v == (int) v) {
            return String.valueOf((int) v);
        }
        return String.valueOf(Math.round(v * 100F) / 100F);
    }

    private float parsed() {
        try {
            return Float.parseFloat(box.getValue().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            try {
                return resetValue.isEmpty() ? clamp(0F) : Float.parseFloat(resetValue);
            } catch (NumberFormatException e2) {
                return clamp(0F);
            }
        }
    }

    private float clamp(float v) {
        return Math.max(min, Math.min(max, v));
    }

    private int minusX() {
        return box.getX() - BTN_W - 3;
    }

    private int plusX() {
        return box.getX() + box.getWidth() + 3;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        if (!box.visible) {
            return;
        }
        int y = box.getY();
        int h = box.getHeight();
        boolean mh = isOver(mouseX, mouseY, minusX(), y, BTN_W, h);
        VanillaUIHelper.drawButton(g, minusX(), y, BTN_W, h, mh);
        g.drawCenteredString(font, "-", minusX() + BTN_W / 2, y + (h - 8) / 2,
                mh ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean ph = isOver(mouseX, mouseY, plusX(), y, BTN_W, h);
        VanillaUIHelper.drawButton(g, plusX(), y, BTN_W, h, ph);
        g.drawCenteredString(font, "+", plusX() + BTN_W / 2, y + (h - 8) / 2,
                ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
    }

    /** true — клик обработан (−/+/ПКМ-сброс); нажатие по самому полю только «взводит» скраб. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!box.visible) {
            return false;
        }
        int y = box.getY();
        int h = box.getHeight();
        if (button == 0 && isOver(mx, my, minusX(), y, BTN_W, h)) {
            box.setValue(fmt(clamp(parsed() - step), integer));
            return true;
        }
        if (button == 0 && isOver(mx, my, plusX(), y, BTN_W, h)) {
            box.setValue(fmt(clamp(parsed() + step), integer));
            return true;
        }
        if (button == 1 && isOver(mx, my, box.getX(), y, box.getWidth(), h)) {
            box.setValue(resetValue);
            return true;
        }
        if (button == 0 && isOver(mx, my, box.getX(), y, box.getWidth(), h)) {
            armed = true;
            scrubbing = false;
            startX = mx;
            startVal = parsed();
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button) {
        if (!armed || button != 0) {
            return false;
        }
        double delta = mx - startX;
        if (!scrubbing && Math.abs(delta) > 3) {
            scrubbing = true;
            box.setFocused(false);
            if (clearFocus != null) {
                clearFocus.run();
            }
        }
        if (scrubbing) {
            box.setValue(fmt(clamp(startVal + (float) (delta * sens)), integer));
        }
        return true;
    }

    public void mouseReleased() {
        armed = false;
        scrubbing = false;
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
