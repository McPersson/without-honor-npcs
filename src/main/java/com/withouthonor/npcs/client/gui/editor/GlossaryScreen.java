package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.ClientGlossary;
import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.client.cache.ClientSkinCache;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.RichTextEditor;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.glossary.GlossaryTerm;
import com.withouthonor.npcs.network.GlossaryPackets;
import com.withouthonor.npcs.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GlossaryScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int LIST_W = 210;
    private static final int ROW_H = 14;
    private static final int WIN_W = 500;
    private static final int WIN_H = 320;

    private final Screen parent;

    @Nullable
    private String selectedId;
    private String draftTitle = "";
    private String draftBody = "";
    private boolean requested;
    private int listScroll;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private boolean favOnly;
    private boolean sortDesc;
    @Nullable
    private String confirmDeleteId;

    private SelectableEditBox newTermBox;
    private SelectableEditBox titleBox;
    private RichTextEditor bodyEditor;

    private int winX, winY, winW, winH;
    private int contentY, listX, edX, edRight, listTop, listBottom;
    @Nullable
    private String hoverTooltip;

    public GlossaryScreen(Screen parent) {
        super(Component.translatable("wh_npcs.ui.glossary.title"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new GlossaryScreen(parent));
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
        contentY = winY + HEADER_H + 6;
        listX = winX + PAD;
        edX = listX + LIST_W + PAD;
        edRight = winX + winW - PAD;
        listTop = contentY + 18;
        listBottom = winY + winH - PAD;
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        if (titleBox != null) {
            draftTitle = titleBox.getValue();
        }
        if (bodyEditor != null) {
            draftBody = bodyEditor.getValue();
        }
        super.resize(mc, w, h);
    }

    @Override
    protected void init() {
        recalc();
        newTermBox = addRenderableWidget(new SelectableEditBox(font,
                listX + 2, listBottom - 38, LIST_W - 4, 16, Component.translatable("wh_npcs.ui.glossary.new_term")));
        newTermBox.setMaxLength(48);
        newTermBox.setHint(Component.translatable("wh_npcs.ui.glossary.new_term_hint"));
        if (selectedId != null) {
            titleBox = addRenderableWidget(new SelectableEditBox(font,
                    edX + 62, contentY + 2, edRight - (edX + 62), 16, Component.translatable("wh_npcs.ui.glossary.heading")));
            titleBox.setMaxLength(64);
            titleBox.setValue(draftTitle);
            titleBox.setHint(Component.translatable("wh_npcs.ui.glossary.heading_hint"));
            int bodyY = contentY + 44;
            int bodyH = (winY + winH - PAD - 22) - bodyY;
            bodyEditor = addRenderableWidget(new RichTextEditor(font, edX, bodyY, edRight - edX, bodyH));
            bodyEditor.setValue(draftBody);
        } else {
            titleBox = null;
            bodyEditor = null;
        }
        if (!requested) {
            requested = true;
            NetworkHandler.sendToServer(new GlossaryPackets.Request());
        }
    }

    private int maxListRows() {
        return Math.max(0, (listBottom - 42 - listTop) / ROW_H);
    }

    private List<GlossaryTerm> displayedTerms() {
        ClientPrefs prefs = ClientPrefs.get();
        List<GlossaryTerm> pinned = new ArrayList<>();
        List<GlossaryTerm> rest = new ArrayList<>();
        for (GlossaryTerm term : ClientGlossary.all()) {
            if (favOnly && !prefs.isFavoriteTerm(term.getId())) {
                continue;
            }
            (prefs.isPinnedTerm(term.getId()) ? pinned : rest).add(term);
        }
        Comparator<GlossaryTerm> cmp = Comparator.comparing(this::sortKey, String.CASE_INSENSITIVE_ORDER);
        if (sortDesc) {
            cmp = cmp.reversed();
        }
        pinned.sort(cmp);
        rest.sort(cmp);
        List<GlossaryTerm> out = new ArrayList<>(pinned);
        out.addAll(rest);
        return out;
    }

    private String sortKey(GlossaryTerm term) {
        return term.getTitle().isBlank() ? term.getId() : term.getTitle();
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        int mx = confirmDeleteId != null ? -1000 : mouseX;
        int my = confirmDeleteId != null ? -1000 : mouseY;
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.glossary.title").getString(), winX + PAD, winY + 8, VanillaUIHelper.TEXT_YELLOW, false);
        g.fill(listX + LIST_W + 2, winY + HEADER_H, listX + LIST_W + 3, winY + winH - PAD, 0xFF373737);

        String tooltip = renderList(g, mx, my);
        renderEditor(g, mx, my);

        boolean doneHover = isOver(mx, my, edRight - 70, winY + winH - PAD - 18, 70, 18);
        VanillaUIHelper.drawButton(g, edRight - 70, winY + winH - PAD - 18, 70, 18, doneHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.done").getString(), edRight - 35, winY + winH - PAD - 13,
                doneHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);

        hoverTooltip = tooltip;
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null && confirmDeleteId == null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
        if (confirmDeleteId != null) {
            renderConfirm(g, mouseX, mouseY);
        }
    }

    @Nullable
    private String renderList(GuiGraphics g, int mouseX, int mouseY) {
        ClientPrefs prefs = ClientPrefs.get();
        String tooltip = null;

        boolean favHover = isOver(mouseX, mouseY, listX + 2, contentY, 24, 16);
        VanillaUIHelper.drawButton(g, listX + 2, contentY, 24, 16, favHover || favOnly);
        drawHeart(g, listX + 2 + 9, contentY + 5, favOnly ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY);
        if (favHover) {
            tooltip = Component.translatable("wh_npcs.ui.glossary.fav_only").getString();
        }
        boolean sortHover = isOver(mouseX, mouseY, listX + 30, contentY, 40, 16);
        VanillaUIHelper.drawButton(g, listX + 30, contentY, 40, 16, sortHover);
        g.drawCenteredString(font, Component.translatable(sortDesc ? "wh_npcs.ui.glossary.sort_desc" : "wh_npcs.ui.glossary.sort_asc").getString(), listX + 50, contentY + 4, VanillaUIHelper.TEXT_AQUA);
        if (sortHover) {
            tooltip = Component.translatable("wh_npcs.ui.glossary.sort_tip").getString();
        }

        List<GlossaryTerm> terms = displayedTerms();
        int rows = maxListRows();
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, terms.size() - rows)));
        if (terms.isEmpty()) {
            g.drawString(font, Component.translatable(favOnly ? "wh_npcs.ui.glossary.empty_fav1" : "wh_npcs.ui.glossary.empty1").getString(),
                    listX + 2, listTop + 2, VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, Component.translatable(favOnly ? "wh_npcs.ui.glossary.empty_fav2" : "wh_npcs.ui.glossary.empty2").getString(),
                    listX + 2, listTop + 14, VanillaUIHelper.TEXT_WHITE, false);
        }
        int y = listTop;
        int rowRight = listX + LIST_W - 12;
        int iconsX = rowRight - 40;
        for (int i = listScroll; i < Math.min(terms.size(), listScroll + rows); i++) {
            GlossaryTerm term = terms.get(i);
            String id = term.getId();
            boolean selected = id.equals(selectedId);
            boolean hovered = isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H);
            boolean pinned = prefs.isPinnedTerm(id);
            boolean fav = prefs.isFavoriteTerm(id);
            if (selected || hovered) {
                g.fill(listX + 2, y, listX + LIST_W, y + ROW_H,
                        selected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            int x = listX + 4;
            if (pinned) {
                drawPin(g, x, y + 3, VanillaUIHelper.TEXT_GOLD);
                x += 10;
            }
            if (!term.getAuthor().isEmpty()) {
                drawAuthorHead(g, term.getAuthor(), x, y + 3);
                if (isOver(mouseX, mouseY, x, y + 3, 8, 8)) {
                    tooltip = Component.translatable("wh_npcs.ui.glossary.author", term.getAuthor()).getString();
                }
                x += 11;
            }
            String label = term.getTitle().isBlank() ? term.getId() : term.getTitle();
            g.drawString(font, font.plainSubstrByWidth(label, iconsX - x - 2), x, y + 3,
                    selected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (hovered) {
                boolean heartHover = isOver(mouseX, mouseY, iconsX, y + 3, 9, 9);
                drawHeart(g, iconsX, y + 4, fav ? 0xFFFF5555
                        : (heartHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
                boolean pinHover = isOver(mouseX, mouseY, iconsX + 14, y + 3, 10, 10);
                drawPin(g, iconsX + 14, y + 3, pinned ? VanillaUIHelper.TEXT_GOLD
                        : (pinHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
                boolean delHover = isOver(mouseX, mouseY, iconsX + 28, y + 3, 10, 10);
                g.drawString(font, "✕", iconsX + 28, y + 3,
                        delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
            } else if (fav) {
                drawHeart(g, iconsX, y + 4, 0xFFFF5555);
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + LIST_W - 8, listTop, rows * ROW_H,
                terms.size(), rows, listScroll, scrollbars, v -> listScroll = v);

        boolean createHover = isOver(mouseX, mouseY, listX + 2, listBottom - 18, LIST_W - 4, 18);
        VanillaUIHelper.drawButton(g, listX + 2, listBottom - 18, LIST_W - 4, 18, createHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.glossary.create").getString(), listX + 2 + (LIST_W - 4) / 2, listBottom - 13,
                createHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
        return tooltip;
    }

    private void renderEditor(GuiGraphics g, int mouseX, int mouseY) {
        if (selectedId == null) {
            g.drawString(font, Component.translatable("wh_npcs.ui.glossary.intro1").getString(), edX, contentY + 6, VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.glossary.intro2").getString(), edX, contentY + 24, VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.glossary.intro3").getString(), edX, contentY + 36, VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.glossary.intro4").getString(), edX, contentY + 48, VanillaUIHelper.TEXT_WHITE, false);
            return;
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.glossary.heading").getString(), edX, contentY + 7, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, "id: " + selectedId, edX, contentY + 24, VanillaUIHelper.TEXT_DARK_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.glossary.body_label").getString(), edX, contentY + 33,
                VanillaUIHelper.TEXT_GRAY, false);
    }

    private void renderConfirm(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        int w = 240;
        int h = 86;
        int x = winX + (winW - w) / 2;
        int y = winY + (winH - h) / 2;
        VanillaUIHelper.drawWindow(g, x, y, w, h);
        GlossaryTerm term = ClientGlossary.byId(confirmDeleteId);
        String title = term != null && !term.getTitle().isBlank() ? term.getTitle() : confirmDeleteId;
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.glossary.delete_q").getString(), x + w / 2, y + 12, VanillaUIHelper.TEXT_YELLOW);
        g.drawCenteredString(font, "§b" + font.plainSubstrByWidth(title, w - 20), x + w / 2, y + 30, VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.glossary.delete_warn").getString(), x + w / 2, y + 44, VanillaUIHelper.TEXT_WHITE);
        boolean delHover = isOver(mouseX, mouseY, x + w / 2 - 84, y + h - 28, 78, 18);
        boolean cancelHover = isOver(mouseX, mouseY, x + w / 2 + 6, y + h - 28, 78, 18);
        VanillaUIHelper.drawButton(g, x + w / 2 - 84, y + h - 28, 78, 18, delHover);
        VanillaUIHelper.drawButton(g, x + w / 2 + 6, y + h - 28, 78, 18, cancelHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.delete").getString(), x + w / 2 - 45, y + h - 23,
                delHover ? VanillaUIHelper.TEXT_YELLOW : 0xFFFF5555);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.cancel").getString(), x + w / 2 + 45, y + h - 23,
                cancelHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        g.pose().popPose();
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }
        if (confirmDeleteId != null) {
            if (button == 0) {
                int w = 240;
                int h = 86;
                int x = winX + (winW - w) / 2;
                int y = winY + (winH - h) / 2;
                if (isOver(mouseX, mouseY, x + w / 2 - 84, y + h - 28, 78, 18)) {
                    NetworkHandler.sendToServer(new GlossaryPackets.Delete(confirmDeleteId));
                    if (confirmDeleteId.equals(selectedId)) {
                        selectedId = null;
                        init(minecraft, width, height);
                    }
                    confirmDeleteId = null;
                } else if (isOver(mouseX, mouseY, x + w / 2 + 6, y + h - 28, 78, 18)
                        || !isOver(mouseX, mouseY, x, y, w, h)) {
                    confirmDeleteId = null;
                }
            }
            return true;
        }
        if (button == 0) {
            if (isOver(mouseX, mouseY, listX + 2, contentY, 24, 16)) {
                favOnly = !favOnly;
                return true;
            }
            if (isOver(mouseX, mouseY, listX + 30, contentY, 40, 16)) {
                sortDesc = !sortDesc;
                return true;
            }
            List<GlossaryTerm> terms = displayedTerms();
            int rows = maxListRows();
            int rowRight = listX + LIST_W - 12;
            int iconsX = rowRight - 40;
            int y = listTop;
            for (int i = listScroll; i < Math.min(terms.size(), listScroll + rows); i++) {
                if (isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H)) {
                    String id = terms.get(i).getId();
                    ClientPrefs prefs = ClientPrefs.get();
                    if (isOver(mouseX, mouseY, iconsX, y + 3, 9, 9)) {
                        prefs.toggleFavoriteTerm(id);
                    } else if (isOver(mouseX, mouseY, iconsX + 14, y + 3, 10, 10)) {
                        prefs.togglePinnedTerm(id);
                    } else if (isOver(mouseX, mouseY, iconsX + 28, y + 3, 10, 10)) {
                        confirmDeleteId = id;
                    } else {
                        selectTerm(id);
                    }
                    return true;
                }
                y += ROW_H;
            }
            if (isOver(mouseX, mouseY, listX + 2, listBottom - 18, LIST_W - 4, 18)) {
                createTerm();
                return true;
            }
            if (isOver(mouseX, mouseY, edRight - 70, winY + winH - PAD - 18, 70, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (mouseX < listX + LIST_W) {
            listScroll -= (int) Math.signum(delta);
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

    private void selectTerm(String id) {
        flush();
        selectedId = id;
        GlossaryTerm term = ClientGlossary.byId(id);
        draftTitle = term != null ? term.getTitle() : "";
        draftBody = term != null ? term.getBody() : "";
        init(minecraft, width, height);
    }

    private void createTerm() {
        String name = newTermBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        String base = NpcEditorScreen.slugify(name);
        if (base.isEmpty()) {
            base = "term";
        }
        String id = base;
        int n = 2;
        while (ClientGlossary.byId(id) != null || id.equals(selectedId)) {
            id = base + "_" + n++;
        }
        flush();
        NetworkHandler.sendToServer(new GlossaryPackets.Save(new GlossaryTerm(id, name, "").toJson()));
        newTermBox.setValue("");
        selectedId = id;
        draftTitle = name;
        draftBody = "";
        init(minecraft, width, height);
    }

    private void flush() {
        if (selectedId == null) {
            return;
        }
        if (titleBox != null) {
            draftTitle = titleBox.getValue();
        }
        if (bodyEditor != null) {
            draftBody = bodyEditor.getValue();
        }
        GlossaryTerm existing = ClientGlossary.byId(selectedId);
        if (existing != null && existing.getTitle().equals(draftTitle) && existing.getBody().equals(draftBody)) {
            return;
        }
        NetworkHandler.sendToServer(new GlossaryPackets.Save(
                new GlossaryTerm(selectedId, draftTitle, draftBody).toJson()));
    }

    @Override
    public void onClose() {
        flush();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void multilineTooltip(GuiGraphics g, String text, int x, int y) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, x, y);
    }

    private static void drawAuthorHead(GuiGraphics g, String author, int x, int y) {
        ClientSkinCache.Skin skin = ClientSkinCache.getInstance().get(author);
        if (skin != null) {
            g.blit(skin.location(), x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(skin.location(), x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + 8, y + 8, 0xFF6E5037);
            g.fill(x + 2, y + 4, x + 3, y + 5, 0xFF2B1F14);
            g.fill(x + 5, y + 4, x + 6, y + 5, 0xFF2B1F14);
        }
    }

    private static void drawHeart(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 3, y + 1, c);
        g.fill(x + 4, y, x + 6, y + 1, c);
        g.fill(x, y + 1, x + 7, y + 3, c);
        g.fill(x + 1, y + 3, x + 6, y + 4, c);
        g.fill(x + 2, y + 4, x + 5, y + 5, c);
        g.fill(x + 3, y + 5, x + 4, y + 6, c);
    }

    private static void drawPin(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 7, y + 4, c);
        g.fill(x + 3, y + 4, x + 5, y + 8, c);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
