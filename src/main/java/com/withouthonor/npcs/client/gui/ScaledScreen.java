package com.withouthonor.npcs.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class ScaledScreen extends Screen {

    private static final int MARGIN = 6;

    private static float scissorScale = 1F;
    private static double scissorCx;
    private static double scissorCy;

    private List<Component> pendingTooltip;
    private int rawMouseX;
    private int rawMouseY;

    protected ScaledScreen(Component title) {
        super(title);
    }

    protected void queueTooltip(List<Component> lines) {
        this.pendingTooltip = lines;
    }

    public static void enableScissor(GuiGraphics g, int x1, int y1, int x2, int y2) {
        int sx1 = (int) Math.floor((x1 - scissorCx) * scissorScale + scissorCx);
        int sy1 = (int) Math.floor((y1 - scissorCy) * scissorScale + scissorCy);
        int sx2 = (int) Math.ceil((x2 - scissorCx) * scissorScale + scissorCx);
        int sy2 = (int) Math.ceil((y2 - scissorCy) * scissorScale + scissorCy);
        g.enableScissor(sx1, sy1, sx2, sy2);
    }

    protected abstract int designW();

    protected abstract int designH();

    protected final float uiScale() {
        int dw = designW();
        int dh = designH();
        if (dw <= 0 || dh <= 0) {
            return 1F;
        }
        float s = Math.min((width - MARGIN * 2F) / dw, (height - MARGIN * 2F) / dh);
        return Math.max(0.1F, Math.min(1F, s));
    }

    private double toDesignX(double x) {
        double cx = width / 2.0;
        return (x - cx) / uiScale() + cx;
    }

    private double toDesignY(double y) {
        double cy = height / 2.0;
        return (y - cy) / uiScale() + cy;
    }

    protected void renderDim(GuiGraphics g) {
        renderBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderDim(g);
        float s = uiScale();
        this.rawMouseX = mouseX;
        this.rawMouseY = mouseY;
        this.pendingTooltip = null;
        int mx = (int) toDesignX(mouseX);
        int my = (int) toDesignY(mouseY);
        PoseStack pose = g.pose();
        pose.pushPose();
        double cx = width / 2.0;
        double cy = height / 2.0;
        pose.translate(cx, cy, 0);
        pose.scale(s, s, 1F);
        pose.translate(-cx, -cy, 0);
        scissorScale = s;
        scissorCx = cx;
        scissorCy = cy;
        renderContent(g, mx, my, partialTick);
        for (Renderable r : this.renderables) {
            r.render(g, mx, my, partialTick);
        }
        renderOverlay(g, mx, my, partialTick);
        scissorScale = 1F;
        pose.popPose();
        if (this.pendingTooltip != null) {
            // Выше оверлеев на z=400 (тип-пикеры, превью), иначе их текст батчится поверх тултипа.
            pose.pushPose();
            pose.translate(0, 0, 500);
            g.renderComponentTooltip(this.font, this.pendingTooltip, this.rawMouseX, this.rawMouseY);
            pose.popPose();
            this.pendingTooltip = null;
        }
    }

    protected abstract void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClickedScaled(toDesignX(mouseX), toDesignY(mouseY), button);
    }

    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return mouseReleasedScaled(toDesignX(mouseX), toDesignY(mouseY), button);
    }

    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float s = uiScale();
        return mouseDraggedScaled(toDesignX(mouseX), toDesignY(mouseY), button, dragX / s, dragY / s);
    }

    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return mouseScrolledScaled(toDesignX(mouseX), toDesignY(mouseY), delta);
    }

    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mouseMovedScaled(toDesignX(mouseX), toDesignY(mouseY));
    }

    protected void mouseMovedScaled(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    protected final boolean superMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected final boolean superMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected final boolean superMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected final boolean superMouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        List<String> editBoxes = new ArrayList<>();
        List<String> richEditors = new ArrayList<>();
        for (GuiEventListener c : this.children()) {
            if (c instanceof EditBox box) {
                editBoxes.add(box.getValue());
            } else if (c instanceof RichTextEditor rich) {
                richEditors.add(rich.getValue());
            }
        }
        super.resize(mc, width, height);
        if (editBoxes.isEmpty() && richEditors.isEmpty()) {
            return;
        }
        int ei = 0;
        int ri = 0;
        for (GuiEventListener c : this.children()) {
            if (c instanceof EditBox box) {
                if (ei < editBoxes.size()) {
                    box.setValue(editBoxes.get(ei++));
                }
            } else if (c instanceof RichTextEditor rich) {
                if (ri < richEditors.size()) {
                    rich.setValue(richEditors.get(ri++));
                }
            }
        }
    }
}
