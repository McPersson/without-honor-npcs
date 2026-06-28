package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.block.SpawnerBlockEntity;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.SpawnerPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SpawnerEditScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 12;
    private static final int WIN_W = 360;
    private static final int WIN_H = 300;

    private final BlockPos pos;
    private final List<String> available;
    private final List<String> selected;
    private boolean random;
    private boolean enabled;
    private int availScroll, selScroll;
    private final ScrollDrag availBar = new ScrollDrag();
    private final ScrollDrag selBar = new ScrollDrag();

    private EditBox delayBox, maxAliveBox, rangeBox, xpBox, hardnessBox;
    private EditBox searchBox;
    private boolean favOnly;

    private int winX, winY, winW, winH;
    private int toggleY, num1Y, num2Y, captionY, searchY, listTop, listH, bottomY, colW;
    private String hoverTooltip;

    public SpawnerEditScreen(BlockPos pos, SpawnerBlockEntity.Config config, List<String> available) {
        super(Component.translatable("wh_npcs.ui.spawner_edit.title"));
        this.pos = pos;
        this.available = new ArrayList<>(available);
        this.selected = new ArrayList<>(config.presets());
        this.random = config.random();
        this.enabled = config.enabled();

        this.draftDelay = String.valueOf(config.delaySeconds());
        this.draftMaxAlive = String.valueOf(config.maxAlive());
        this.draftRange = String.valueOf(config.activationRange());
        this.draftXp = String.valueOf(config.breakXp());
        this.draftHardness = trimFloat(config.hardness());
    }

    private final String draftDelay, draftMaxAlive, draftRange, draftXp, draftHardness;

    private static String trimFloat(float f) {
        return f == Math.rint(f) ? String.valueOf((int) f) : String.valueOf(f);
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
        bottomY = winY + winH - PAD - 20;
        toggleY = winY + HEADER_H + 6;
        num1Y = toggleY + 22;
        num2Y = num1Y + 22;
        colW = (winW - PAD * 3) / 2;
        captionY = num2Y + 24;
        searchY = num2Y + 34;
        listTop = num2Y + 54;
        listH = bottomY - 6 - listTop;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        int bx = winX + PAD;
        delayBox = numBox(bx + 60, num1Y, 36, 4, draftDelay);
        maxAliveBox = numBox(bx + 60 + 110, num1Y, 30, 2, draftMaxAlive);
        rangeBox = numBox(bx + 60 + 110 + 96, num1Y, 30, 2, draftRange);
        xpBox = numBox(bx + 60, num2Y, 36, 4, draftXp);
        hardnessBox = numBox(bx + 60 + 110, num2Y, 36, 5, draftHardness);

        String oldSearch = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, bx, searchY, colW - 18, 14, Component.empty()));
        searchBox.setMaxLength(48);
        searchBox.setValue(oldSearch);
        searchBox.setHint(Component.translatable("wh_npcs.ui.spawner_edit.search"));
        searchBox.setResponder(v -> availScroll = 0);
    }

    private EditBox numBox(int x, int y, int w, int maxLen, String value) {
        EditBox b = addRenderableWidget(new SelectableEditBox(font, x, y, w, 16, Component.empty()));
        b.setMaxLength(maxLen);
        b.setValue(value);
        return b;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        availBar.beginFrame();
        selBar.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        String tooltip = null;
        int bx = winX + PAD;

        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.order").getString(), bx, toggleY + 5, VanillaUIHelper.TEXT_GRAY, false);
        boolean ordHover = isOver(mouseX, mouseY, bx + 56, toggleY, 110, 16);
        VanillaUIHelper.drawButton(g, bx + 56, toggleY, 110, 16, ordHover);
        g.drawCenteredString(font, Component.translatable(random ? "wh_npcs.ui.spawner_edit.order_random" : "wh_npcs.ui.spawner_edit.order_sequential").getString(), bx + 56 + 55, toggleY + 4,
                ordHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);

        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.spawn").getString(), bx + 178, toggleY + 5, VanillaUIHelper.TEXT_GRAY, false);
        boolean enHover = isOver(mouseX, mouseY, bx + 220, toggleY, 90, 16);
        VanillaUIHelper.drawButton(g, bx + 220, toggleY, 90, 16, enHover);
        g.drawCenteredString(font, Component.translatable(enabled ? "wh_npcs.ui.spawner_edit.on" : "wh_npcs.ui.spawner_edit.off").getString(), bx + 220 + 45, toggleY + 4,
                enHover ? VanillaUIHelper.TEXT_YELLOW
                        : (enabled ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_RED));
        if (enHover) {
            tooltip = Component.translatable("wh_npcs.ui.spawner_edit.tip_enabled").getString();
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.delay").getString(), bx, num1Y + 3, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.limit").getString(), bx + 110, num1Y + 3, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.radius").getString(), bx + 110 + 96, num1Y + 3, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.break_xp").getString(), bx, num2Y + 3, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.hardness").getString(), bx + 110, num2Y + 3, VanillaUIHelper.TEXT_GRAY, false);
        if (isOver(mouseX, mouseY, bx + 110, num2Y, 90, 14)) {
            tooltip = Component.translatable("wh_npcs.ui.spawner_edit.tip_hardness").getString();
        }

        int leftX = winX + PAD;
        int rightX = leftX + colW + PAD;
        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.col_available").getString(), leftX, captionY, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.spawner_edit.col_in_spawner").getString(), rightX, captionY, VanillaUIHelper.TEXT_GRAY, false);
        boolean favHover = isOver(mouseX, mouseY, leftX + colW - 16, searchY, 14, 14);
        VanillaUIHelper.drawButton(g, leftX + colW - 16, searchY, 14, 14, favHover || favOnly);
        drawHeart(g, leftX + colW - 16 + 3, searchY + 4, favOnly ? 0xFFFF5555
                : (favHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY));
        if (favHover) {
            tooltip = Component.translatable("wh_npcs.ui.spawner_edit.fav_filter").getString();
        }
        availScroll = renderList(g, mouseX, mouseY, leftX, colW, displayedAvailable(), availScroll, availBar, true);
        selScroll = renderList(g, mouseX, mouseY, rightX, colW, selected, selScroll, selBar, false);

        drawBtn(g, Component.translatable("wh_npcs.ui.common.save").getString(), winX + winW - PAD - 150, bottomY, 84, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), winX + winW - PAD - 62, bottomY, 62, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        hoverTooltip = tooltip;
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            List<Component> lines = new ArrayList<>();
            for (String line : hoverTooltip.split("\n")) {
                lines.add(Component.literal(line));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private List<String> displayedAvailable() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(java.util.Locale.ROOT).trim() : "";
        ClientPrefs prefs = ClientPrefs.get();
        List<String> out = new ArrayList<>();
        for (String a : available) {
            if (selected.contains(a)) {
                continue;
            }
            if (favOnly && !prefs.isFavoriteProfile(a)) {
                continue;
            }
            if (!q.isEmpty() && !a.toLowerCase(java.util.Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(a);
        }
        out.sort((x, y) -> {
            boolean fx = prefs.isFavoriteProfile(x);
            boolean fy = prefs.isFavoriteProfile(y);
            if (fx != fy) {
                return fx ? -1 : 1;
            }
            return x.compareToIgnoreCase(y);
        });
        return out;
    }

    private static void drawHeart(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 3, y + 1, c);
        g.fill(x + 4, y, x + 6, y + 1, c);
        g.fill(x, y + 1, x + 7, y + 3, c);
        g.fill(x + 1, y + 3, x + 6, y + 4, c);
        g.fill(x + 2, y + 4, x + 5, y + 5, c);
        g.fill(x + 3, y + 5, x + 4, y + 6, c);
    }

    private int renderList(GuiGraphics g, int mouseX, int mouseY, int x, int w, List<String> items,
                           int scroll, ScrollDrag bar, boolean addMode) {
        VanillaUIHelper.drawContentPanel(g, x, listTop, w, listH);
        int visible = Math.max(1, (listH - 6) / ROW_H);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, items.size() - visible)));
        if (items.isEmpty()) {
            g.drawCenteredString(font, Component.translatable(addMode ? "wh_npcs.ui.spawner_edit.empty_presets" : "wh_npcs.ui.spawner_edit.empty_spawner").getString(),
                    x + w / 2, listTop + listH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
        }
        int y = listTop + 3;
        for (int i = scroll; i < Math.min(items.size(), scroll + visible); i++) {
            boolean hov = isOver(mouseX, mouseY, x + 2, y, w - 6, ROW_H);
            if (hov) {
                g.fill(x + 2, y, x + w - 4, y + ROW_H, VanillaUIHelper.BG_HOVERED);
            }
            int col = hov ? VanillaUIHelper.TEXT_YELLOW
                    : (addMode && ClientPrefs.get().isFavoriteProfile(items.get(i))
                    ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_WHITE);
            g.drawString(font, font.plainSubstrByWidth(items.get(i), w - 10), x + 4, y + 2, col, false);
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, x + w - 5, listTop + 2, listH - 4,
                items.size(), visible, scroll, bar, v -> { });
        return scroll;
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (availBar.click(mouseX, mouseY) || selBar.click(mouseX, mouseY)) {
            return true;
        }
        if (button == 0) {
            int bx = winX + PAD;
            if (isOver(mouseX, mouseY, bx + 56, toggleY, 110, 16)) {
                random = !random;
                return true;
            }
            if (isOver(mouseX, mouseY, bx + 220, toggleY, 90, 16)) {
                enabled = !enabled;
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 150, bottomY, 84, 18)) {
                save();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 62, bottomY, 62, 18)) {
                onClose();
                return true;
            }
            int colW = (winW - PAD * 3) / 2;
            int leftX = winX + PAD;
            int rightX = leftX + colW + PAD;
            if (isOver(mouseX, mouseY, leftX + colW - 16, searchY, 14, 14)) {
                favOnly = !favOnly;
                availScroll = 0;
                return true;
            }
            String addPick = pick(mouseX, mouseY, leftX, colW, displayedAvailable(), availScroll);
            if (addPick != null) {
                selected.add(addPick);
                return true;
            }
            String remPick = pick(mouseX, mouseY, rightX, colW, selected, selScroll);
            if (remPick != null) {
                selected.remove(remPick);
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private String pick(double mouseX, double mouseY, int x, int w, List<String> items, int scroll) {
        int visible = Math.max(1, (listH - 6) / ROW_H);
        int y = listTop + 3;
        for (int i = scroll; i < Math.min(items.size(), scroll + visible); i++) {
            if (isOver(mouseX, mouseY, x + 2, y, w - 6, ROW_H)) {
                return items.get(i);
            }
            y += ROW_H;
        }
        return null;
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        int colW = (winW - PAD * 3) / 2;
        int leftX = winX + PAD;
        int rightX = leftX + colW + PAD;
        if (isOver(mouseX, mouseY, leftX, listTop, colW, listH)) {
            availScroll -= (int) Math.signum(delta);
            return true;
        }
        if (isOver(mouseX, mouseY, rightX, listTop, colW, listH)) {
            selScroll -= (int) Math.signum(delta);
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dx, double dy) {
        if (availBar.drag(mouseY) || selBar.drag(mouseY)) {
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        availBar.release();
        selBar.release();
        return superMouseReleased(mouseX, mouseY, button);
    }

    private void save() {
        SpawnerBlockEntity.Config cfg = new SpawnerBlockEntity.Config(
                new ArrayList<>(selected), random,
                parseInt(delayBox.getValue(), 10), parseInt(maxAliveBox.getValue(), 4),
                parseInt(rangeBox.getValue(), 16), parseInt(xpBox.getValue(), 0),
                parseFloat(hardnessBox.getValue(), 2.0F), enabled);
        NetworkHandler.sendToServer(new SpawnerPackets.Save(pos, cfg));
        onClose();
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        VanillaUIHelper.drawSmallButton(g, font, label, x, y, w, isOver(mouseX, mouseY, x, y, w, 18), color);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
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
