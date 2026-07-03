package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.network.NpcListPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Пикер NPC из общего списка сервера — привязка NPC к блок-триггеру и т.п.
 * Выбор выгруженных NPC разрешён: привязка сохраняется, действия сработают,
 * когда NPC снова окажется в загруженных чанках.
 */
public class NpcPickScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 14;
    private static final int MAX_ROWS = 14;
    private static final int WIN_W = 360;
    private static final int WIN_H = 300;

    @Nullable
    private final Screen parent;
    private final List<NpcListPackets.NpcEntry> entries;
    private final Consumer<NpcListPackets.NpcEntry> callback;

    private EditBox searchBox;
    private int scroll;
    @Nullable
    private List<NpcListPackets.NpcEntry> displayedCache;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private int winX, winY, winW, winH, listTop, bottomY;

    public NpcPickScreen(@Nullable Screen parent, List<NpcListPackets.NpcEntry> entries,
                         Consumer<NpcListPackets.NpcEntry> callback) {
        super(Component.translatable("wh_npcs.ui.npc_pick.title"));
        this.parent = parent;
        this.entries = entries;
        this.callback = callback;
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
        listTop = winY + HEADER_H + 22;
        bottomY = winY + winH - PAD - 20;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, winY + HEADER_H + 2,
                winW - PAD * 2, 16, Component.translatable("wh_npcs.ui.npc_pick.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.npc_pick.search_hint"));
        searchBox.setResponder(v -> {
            scroll = 0;
            displayedCache = null;
        });
    }

    private List<NpcListPackets.NpcEntry> displayed() {
        if (displayedCache != null) {
            return displayedCache;
        }
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        List<NpcListPackets.NpcEntry> out = new ArrayList<>();
        for (NpcListPackets.NpcEntry e : entries) {
            if (!q.isEmpty() && !e.name().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(e);
        }
        displayedCache = out;
        return out;
    }

    /** Подпись расстояния: -1 = другое измерение. */
    private String distLabel(NpcListPackets.NpcEntry e) {
        return e.dist() < 0
                ? Component.translatable("wh_npcs.ui.npc_pick.other_dim").getString()
                : Component.translatable("wh_npcs.ui.npc_pick.dist", e.dist()).getString();
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.npc_pick.title").getString()
                        + "  §7(" + entries.size() + ")",
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        boolean xHover = isOver(mouseX, mouseY, winX + winW - 20, winY + 4, 14, 14);
        g.drawCenteredString(font, "✕", winX + winW - 13, winY + 7,
                xHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_RED);

        VanillaUIHelper.drawContentPanel(g, winX + PAD, listTop, winW - PAD * 2, MAX_ROWS * ROW_H + 6);
        List<NpcListPackets.NpcEntry> list = displayed();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - MAX_ROWS)));
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.npc_pick.empty").getString(),
                    winX + winW / 2, listTop + 30, VanillaUIHelper.TEXT_DARK_GRAY);
        }
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            NpcListPackets.NpcEntry e = list.get(i);
            int y = listTop + 3 + (i - scroll) * ROW_H;
            boolean hov = isOver(mouseX, mouseY, winX + PAD + 2, y - 1, winW - PAD * 2 - 8, ROW_H);
            if (hov) {
                g.fill(winX + PAD + 2, y - 1, winX + winW - PAD - 6, y - 1 + ROW_H, VanillaUIHelper.BG_HOVERED);
            }
            String dist = "§8" + distLabel(e);
            int distW = font.width(dist);
            String name = e.name() + (e.loaded() ? ""
                    : " " + Component.translatable("wh_npcs.ui.npc_pick.unloaded").getString());
            g.drawString(font, font.plainSubstrByWidth(name, winW - PAD * 2 - 16 - distW - 8),
                    winX + PAD + 6, y + 2,
                    e.loaded() ? (hov ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE)
                            : VanillaUIHelper.TEXT_DARK_GRAY, false);
            g.drawString(font, dist, winX + winW - PAD - 10 - distW, y + 2, VanillaUIHelper.TEXT_WHITE, false);
        }
        VanillaUIHelper.drawScrollbar(g, winX + winW - PAD - 6, listTop + 2, MAX_ROWS * ROW_H,
                list.size(), MAX_ROWS, scroll, scrollbars, v -> scroll = v);

        g.drawString(font, Component.translatable("wh_npcs.ui.npc_pick.note").getString(),
                winX + PAD, bottomY + 5, VanillaUIHelper.TEXT_DARK_GRAY, false);
        VanillaUIHelper.drawSmallButton(g, font, Component.translatable("wh_npcs.ui.common.close").getString(),
                winX + winW - PAD - 70, bottomY, 70,
                isOver(mouseX, mouseY, winX + winW - PAD - 70, bottomY, 70, 18), VanillaUIHelper.TEXT_WHITE);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button != 0) {
            return superMouseClicked(mouseX, mouseY, button);
        }
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }
        if (isOver(mouseX, mouseY, winX + winW - 20, winY + 4, 14, 14)) {
            onClose();
            return true;
        }
        List<NpcListPackets.NpcEntry> list = displayed();
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            int y = listTop + 3 + (i - scroll) * ROW_H;
            if (isOver(mouseX, mouseY, winX + PAD + 2, y - 1, winW - PAD * 2 - 8, ROW_H)) {
                callback.accept(list.get(i));
                onClose();
                return true;
            }
        }
        if (isOver(mouseX, mouseY, winX + winW - PAD - 70, bottomY, 70, 18)) {
            onClose();
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, winX + PAD, listTop, winW - PAD * 2, MAX_ROWS * ROW_H + 6)) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta),
                    Math.max(0, displayed().size() - MAX_ROWS)));
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
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
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
