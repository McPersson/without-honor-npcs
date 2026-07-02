package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.ClientGlobalSchedule;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.network.GlobalSchedulePackets;
import com.withouthonor.npcs.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class GlobalScheduleScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 16;
    private static final int WIN_W = 360;
    private static final int WIN_H = 240;
    private static final int MAX_ROWS = 11;

    private final Screen parent;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private int scroll;
    private int winX, winY, winW, winH, listTop, bottomY;

    public GlobalScheduleScreen(@Nullable Screen parent) {
        super(Component.translatable("wh_npcs.ui.global_schedule.title"));
        this.parent = parent;
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
        listTop = winY + HEADER_H + 8;
        bottomY = winY + winH - PAD - 20;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        NetworkHandler.sendToServer(new GlobalSchedulePackets.Request());
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.global_schedule.title").getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        var list = ClientGlobalSchedule.get();
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.global_schedule.empty").getString(),
                    winX + winW / 2, winY + winH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - MAX_ROWS)));
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            var row = list.get(i);
            int y = listTop + (i - scroll) * ROW_H;
            boolean hovered = isOver(mouseX, mouseY, winX + 4, y, winW - 8, ROW_H);
            if (hovered) {
                g.fill(winX + 4, y, winX + winW - 4, y + ROW_H, VanillaUIHelper.BG_HOVERED);
            }
            g.drawString(font, row.loaded() ? "§a●" : "§8○", winX + 8, y + 4, -1, false);
            g.drawString(font, font.plainSubstrByWidth(row.name(), 112), winX + 20, y + 4,
                    VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, "§8" + row.x() + " " + row.y() + " " + row.z(),
                    winX + 142, y + 4, VanillaUIHelper.TEXT_DARK_GRAY, false);

            boolean tpHover = isOver(mouseX, mouseY, winX + winW - 122, y + 1, 62, 14);
            VanillaUIHelper.drawButton(g, winX + winW - 122, y + 1, 62, 14, tpHover);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.global_schedule.tp").getString(),
                    winX + winW - 91, y + 4, tpHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);

            boolean offHover = isOver(mouseX, mouseY, winX + winW - 56, y + 1, 38, 14);
            VanillaUIHelper.drawButton(g, winX + winW - 56, y + 1, 38, 14, offHover);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.global_schedule.off").getString(),
                    winX + winW - 37, y + 4, offHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_RED);
        }
        VanillaUIHelper.drawScrollbar(g, winX + winW - 10, listTop, MAX_ROWS * ROW_H,
                list.size(), MAX_ROWS, scroll, scrollbars, v -> scroll = v);

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(),
                winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0) {
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }
            var list = ClientGlobalSchedule.get();
            for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
                var row = list.get(i);
                int y = listTop + (i - scroll) * ROW_H;
                if (isOver(mouseX, mouseY, winX + winW - 122, y + 1, 62, 14)) {
                    NetworkHandler.sendToServer(new GlobalSchedulePackets.Action(
                            row.uuid(), GlobalSchedulePackets.ACTION_TP));
                    if (minecraft != null) {
                        minecraft.setScreen(null);
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, winX + winW - 56, y + 1, 38, 14)) {
                    NetworkHandler.sendToServer(new GlobalSchedulePackets.Action(
                            row.uuid(), GlobalSchedulePackets.ACTION_DISABLE));
                    return true;
                }
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta),
                Math.max(0, ClientGlobalSchedule.get().size() - MAX_ROWS)));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
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
