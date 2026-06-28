package com.withouthonor.npcs.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class ScrollDrag {

    private static final class Bar {
        int x, y, h, total, visible;
        IntConsumer apply;
    }

    private final List<Bar> armed = new ArrayList<>();
    private Bar active;

    public void beginFrame() {
        armed.clear();
    }

    public void arm(int x, int y, int h, int total, int visible, IntConsumer apply) {
        if (total <= visible || apply == null) {
            return;
        }
        Bar bar = new Bar();
        bar.x = x;
        bar.y = y;
        bar.h = h;
        bar.total = total;
        bar.visible = visible;
        bar.apply = apply;
        armed.add(bar);
    }

    public boolean click(double mouseX, double mouseY) {
        for (Bar bar : armed) {
            if (mouseX >= bar.x - 2 && mouseX <= bar.x + 5 && mouseY >= bar.y && mouseY <= bar.y + bar.h) {
                active = bar;
                bar.apply.accept(scrollAt(bar, mouseY));
                return true;
            }
        }
        return false;
    }

    public boolean drag(double mouseY) {
        if (active == null) {
            return false;
        }
        active.apply.accept(scrollAt(active, mouseY));
        return true;
    }

    public void release() {
        active = null;
    }

    public boolean isDragging() {
        return active != null;
    }

    private static int scrollAt(Bar bar, double mouseY) {
        int thumbH = Math.max(10, bar.h * bar.visible / bar.total);
        int travel = bar.h - thumbH;
        int maxScroll = bar.total - bar.visible;
        if (travel <= 0 || maxScroll <= 0) {
            return 0;
        }
        double rel = (mouseY - bar.y - thumbH / 2.0) / travel;
        int s = (int) Math.round(rel * maxScroll);
        return Math.max(0, Math.min(maxScroll, s));
    }
}
