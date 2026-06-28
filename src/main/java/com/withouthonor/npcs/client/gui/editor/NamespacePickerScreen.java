package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class NamespacePickerScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 14;

    private final Screen parent;
    private final List<String> namespaces;
    private final Map<String, Integer> counts;
    private final int total;
    @Nullable
    private final String current;
    private final Consumer<String> onPick;

    private final List<String> filtered = new ArrayList<>();
    private final ScrollDrag scrollbars = new ScrollDrag();
    private EditBox searchBox;
    private int scroll;

    private int winX, winY, winW, winH;
    private int listX, listW, listY, listH;
    private int allRowY, bottomY;

    public NamespacePickerScreen(Screen parent, List<String> namespaces, Map<String, Integer> counts,
                                 int total, @Nullable String current, Consumer<String> onPick) {
        super(Component.translatable("wh_npcs.ui.namespace.title"));
        this.parent = parent;
        this.namespaces = namespaces;
        this.counts = counts;
        this.total = total;
        this.current = current;
        this.onPick = onPick;
        rebuild();
    }

    private void rebuild() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        filtered.clear();
        for (String ns : namespaces) {
            if (q.isEmpty() || ns.toLowerCase(Locale.ROOT).contains(q) || label(ns).toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(ns);
            }
        }
        scroll = 0;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, listX, winY + HEADER_H + 4, listW, 16,
                Component.translatable("wh_npcs.ui.namespace.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.namespace.search_hint"));
        searchBox.setResponder(v -> rebuild());
        setFocused(searchBox);
    }

    @Override
    protected int designW() {
        return 300;
    }

    @Override
    protected int designH() {
        return 300;
    }

    private void recalc() {
        winW = 300;
        winH = 300;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        bottomY = winY + winH - PAD - 20;
        listX = winX + PAD;
        listW = winW - 2 * PAD;
        allRowY = winY + HEADER_H + 26;
        listY = allRowY + 18;
        listH = bottomY - 8 - listY;
    }

    private String label(String ns) {
        return ns.equals("minecraft") ? Component.translatable("wh_npcs.ui.namespace.vanilla").getString() : ns;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.namespace.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        boolean allHover = isOver(mouseX, mouseY, listX, allRowY, listW, 16);
        VanillaUIHelper.drawButton(g, listX, allRowY, listW, 16, allHover || current == null);
        g.drawString(font, Component.translatable("wh_npcs.ui.namespace.all_sources").getString(), listX + 5, allRowY + 4,
                current == null ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
        String totalLabel = Component.translatable("wh_npcs.ui.namespace.count", total).getString();
        g.drawString(font, totalLabel, listX + listW - 5 - font.width(totalLabel), allRowY + 4,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        VanillaUIHelper.drawContentPanel(g, listX, listY, listW, listH);
        int visibleRows = (listH - 8) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - visibleRows)));
        if (filtered.isEmpty()) {
            g.drawString(font, Component.translatable("wh_npcs.ui.namespace.empty").getString(), listX + 5, listY + 5, VanillaUIHelper.TEXT_WHITE, false);
        }
        int y = listY + 4;
        for (int i = scroll; i < Math.min(filtered.size(), scroll + visibleRows); i++) {
            String ns = filtered.get(i);
            boolean selected = ns.equals(current);
            boolean hovered = isOver(mouseX, mouseY, listX + 2, y, listW - 4, ROW_H);
            if (selected || hovered) {
                g.fill(listX + 2, y, listX + listW - 2, y + ROW_H,
                        selected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            String cnt = Component.translatable("wh_npcs.ui.namespace.count", counts.getOrDefault(ns, 0)).getString();
            String name = font.plainSubstrByWidth(label(ns), listW - 18 - font.width(cnt));
            g.drawString(font, name, listX + 5, y + 3,
                    selected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, cnt, listX + listW - 8 - font.width(cnt), y + 3,
                    VanillaUIHelper.TEXT_DARK_GRAY, false);
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + listW - 6, listY + 3, listH - 6,
                filtered.size(), visibleRows, scroll, scrollbars, v -> scroll = v);

        drawBtn(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), winX + winW - PAD - 60, bottomY, 60, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0) {
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }
            if (isOver(mouseX, mouseY, listX, allRowY, listW, 16)) {
                onPick.accept(null);
                onClose();
                return true;
            }
            int visibleRows = (listH - 8) / ROW_H;
            int y = listY + 4;
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visibleRows); i++) {
                if (isOver(mouseX, mouseY, listX + 2, y, listW - 4, ROW_H)) {
                    onPick.accept(filtered.get(i));
                    onClose();
                    return true;
                }
                y += ROW_H;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 60, bottomY, 60, 18)) {
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
        recalc();
        if (isOver(mouseX, mouseY, listX, listY, listW, listH)) {
            scroll -= (int) Math.signum(delta) * 3;
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        VanillaUIHelper.drawSmallButton(g, font, label, x, y, w, isOver(mouseX, mouseY, x, y, w, 18), color);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
