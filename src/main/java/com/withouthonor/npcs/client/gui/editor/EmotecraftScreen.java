package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.client.EmoteThumbnails;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.compat.Compat;
import com.withouthonor.npcs.compat.EmotecraftBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmotecraftScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int CW = 78;
    private static final int CH = 92;
    private static final int IMG = 64;

    private static final int EYE_BTN = 16;

    private static final int CB_X = 8;
    private static final int CB_Y = 8;
    private static final int CB_W = 210;
    private static final int CB_H = 24;

    private final Screen parent;
    private final JsonObject profileJson;
    @Nullable
    private final CompanionEntity npc;

    private List<EmotecraftBridge.EmoteRef> emotes = List.of();
    private final ScrollDrag scrollbars = new ScrollDrag();
    private final Map<String, Float> bounce = new HashMap<>();

    private EditBox searchBox;
    private boolean favTab;
    private boolean collapsed;
    private int sortMode;
    private int scroll;
    @Nullable
    private EmotecraftBridge.EmoteRef selected;
    @Nullable
    private String lastClickId;
    private long lastClickMs;
    private long lastFrameMs;

    private int winX, winY, winW, winH;
    private int controlsY, gridX, gridY, gridW, gridH, bottomY;
    private int cols, visibleRows;

    @Nullable
    private java.util.function.Consumer<EmotecraftBridge.EmoteRef> onPick;

    public EmotecraftScreen(Screen parent, JsonObject profileJson, @Nullable CompanionEntity npc) {
        super(Component.translatable("wh_npcs.ui.emotecraft.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        this.npc = npc;
    }

    public static EmotecraftScreen forPicker(Screen parent,
                                             java.util.function.Consumer<EmotecraftBridge.EmoteRef> onPick) {
        EmotecraftScreen s = new EmotecraftScreen(parent, new JsonObject(), null);
        s.onPick = onPick;
        return s;
    }

    private boolean pickMode() {
        return onPick != null;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, gridX, controlsY, 140, 16,
                Component.translatable("wh_npcs.ui.emotecraft.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.emotecraft.search_hint"));
        searchBox.setResponder(v -> scroll = 0);
        searchBox.visible = !collapsed;
        emotes = Compat.emotecraft().listEmotes();
        if (!pickMode() && selected == null && profileJson.has("idle_emote")) {
            String id = profileJson.get("idle_emote").getAsString();
            for (EmotecraftBridge.EmoteRef e : emotes) {
                if (e.id().equals(id)) {
                    selected = e;
                    break;
                }
            }
        }
    }

    @Override
    protected int designW() {
        return 480;
    }

    @Override
    protected int designH() {
        return 308;
    }

    @Override
    protected void renderDim(GuiGraphics g) {
        if (!collapsed) {
            renderBackground(g);
        }
    }

    private void recalc() {
        winW = 480;
        winH = 308;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        controlsY = winY + HEADER_H + 4;
        gridX = winX + PAD;
        gridY = controlsY + 22 + 12;
        bottomY = winY + winH - PAD - 20;
        gridH = bottomY - 6 - gridY;
        gridW = winW - 2 * PAD - 8;
        cols = Math.max(1, gridW / CW);
        visibleRows = Math.max(1, (gridH - 8) / CH);
    }

    private int tabAllX() {
        return gridX + 146;
    }

    private int tabFavX() {
        return tabAllX() + 32;
    }

    private int sortBtnX() {
        return tabFavX() + 32;
    }

    private int folderBtnX() {
        return winX + winW - PAD - 64;
    }

    private int gridLeft() {
        return gridX + (gridW - cols * CW) / 2;
    }

    private List<EmotecraftBridge.EmoteRef> displayed() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        ClientPrefs prefs = ClientPrefs.get();
        List<EmotecraftBridge.EmoteRef> out = new ArrayList<>();
        for (EmotecraftBridge.EmoteRef e : emotes) {
            if (!q.isEmpty() && !e.name().toLowerCase(Locale.ROOT).contains(q)
                    && !e.author().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            if (favTab && !prefs.isFavoriteEmote(e.id())) {
                continue;
            }
            out.add(e);
        }
        Comparator<EmotecraftBridge.EmoteRef> cmp = sortMode == 1
                ? Comparator.comparing(EmotecraftBridge.EmoteRef::name, String.CASE_INSENSITIVE_ORDER).reversed()
                : Comparator.comparing(EmotecraftBridge.EmoteRef::name, String.CASE_INSENSITIVE_ORDER);
        out.sort(cmp);
        return out;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        if (collapsed) {

            renderCollapsed(g, mouseX, mouseY);
            return;
        }
        scrollbars.beginFrame();
        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0 ? 0F : Math.min(1F, (now - lastFrameMs) / 120F);
        lastFrameMs = now;

        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, pickMode() ? Component.translatable("wh_npcs.ui.emotecraft.title_pick").getString()
                        : Component.translatable("wh_npcs.ui.emotecraft.title").getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        boolean eyeHover = isOver(mouseX, mouseY, eyeX(), eyeY(), EYE_BTN, EYE_BTN);
        drawEyeButton(g, eyeX(), eyeY(), eyeHover);

        boolean loaded = Compat.emotecraftLoaded();
        drawMini(g, Component.translatable("wh_npcs.ui.emotecraft.tab_all").getString(), tabAllX(), controlsY, 30, mouseX, mouseY,
                !favTab ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean favHover = isOver(mouseX, mouseY, tabFavX(), controlsY, 28, 16);
        VanillaUIHelper.drawButton(g, tabFavX(), controlsY, 28, 16, favHover || favTab);
        drawHeart(g, tabFavX() + 10, controlsY + 5, favTab ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY);
        drawMini(g, sortMode == 1 ? Component.translatable("wh_npcs.ui.emotecraft.sort_za").getString()
                : Component.translatable("wh_npcs.ui.emotecraft.sort_az").getString(), sortBtnX(), controlsY, 36, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawMini(g, Component.translatable("wh_npcs.ui.emotecraft.folder").getString(), folderBtnX(), controlsY, 64, mouseX, mouseY, VanillaUIHelper.TEXT_GRAY);

        g.drawString(font, loaded ? Component.translatable("wh_npcs.ui.emotecraft.status_loaded", emotes.size()).getString()
                        : Component.translatable("wh_npcs.ui.emotecraft.status_missing").getString(),
                gridX, controlsY + 20, VanillaUIHelper.TEXT_DARK_GRAY, false);

        renderGrid(g, mouseX, mouseY, dt);

        boolean can = selected != null;
        drawBtn(g, "▶ " + Component.translatable("wh_npcs.ui.emotecraft.play").getString(), gridX, bottomY, 120, mouseX, mouseY,
                can ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_DARK_GRAY);
        drawBtn(g, "■ " + Component.translatable("wh_npcs.ui.emotecraft.stop").getString(), gridX + 126, bottomY, 70, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        int doneW = pickMode() ? 86 : 60;
        int doneX = winX + winW - PAD - doneW;
        int selX = gridX + 204;
        int selMaxW = Math.max(0, doneX - 6 - selX);
        if (selected != null) {
            String prefix = Component.translatable("wh_npcs.ui.emotecraft.selected_prefix").getString();
            String nm = ellipsize(selected.name(), Math.max(0, selMaxW - font.width(prefix)));
            g.drawString(font, prefix + nm, selX, bottomY + 5, VanillaUIHelper.TEXT_WHITE, false);
        }
        if (pickMode()) {
            drawBtn(g, "✓ " + Component.translatable("wh_npcs.ui.emotecraft.choose").getString(), doneX, bottomY, doneW, mouseX, mouseY,
                    can ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_DARK_GRAY);
        } else {
            drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), doneX, bottomY, doneW, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        }
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        if (collapsed) {
            boolean eyeHover = isOver(mouseX, mouseY, CB_X + 4, CB_Y + (CB_H - EYE_BTN) / 2, EYE_BTN, EYE_BTN);
            int nameX = CB_X + 4 + EYE_BTN + 4;
            int nameMaxW = cbPlayX() - 6 - nameX;
            if (eyeHover) {
                multilineTooltip(g, Component.translatable("wh_npcs.ui.emotecraft.tip_expand").getString(), mouseX, mouseY);
            } else if (selected != null && isOver(mouseX, mouseY, nameX, CB_Y + 6, nameMaxW, 12)
                    && font.width(selected.name()) > nameMaxW) {
                multilineTooltip(g, Component.translatable("wh_npcs.ui.emotecraft.tip_name", selected.name()).getString(), mouseX, mouseY);
            }
            return;
        }
        boolean eyeHover = isOver(mouseX, mouseY, eyeX(), eyeY(), EYE_BTN, EYE_BTN);
        int doneW = pickMode() ? 86 : 60;
        int doneX = winX + winW - PAD - doneW;
        int selX = gridX + 204;
        int selMaxW = Math.max(0, doneX - 6 - selX);

        EmotecraftBridge.EmoteRef hov = tileAt(mouseX, mouseY);
        if (eyeHover) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.emotecraft.tip_collapse").getString(), mouseX, mouseY);
        } else if (selected != null && isOver(mouseX, mouseY, selX, bottomY + 4, selMaxW, 10)
                && font.width(Component.translatable("wh_npcs.ui.emotecraft.selected_prefix").getString() + selected.name()) > selMaxW) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.emotecraft.tip_selected", selected.name()).getString(), mouseX, mouseY);
        } else if (hov != null && !hov.author().isEmpty()) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.emotecraft.tip_emote", hov.name(), hov.author()).getString(), mouseX, mouseY);
        }
    }

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY, float dt) {
        VanillaUIHelper.drawContentPanel(g, gridX, gridY, winW - 2 * PAD, gridH);
        List<EmotecraftBridge.EmoteRef> shown = displayed();
        if (shown.isEmpty()) {
            g.drawCenteredString(font, favTab ? Component.translatable("wh_npcs.ui.emotecraft.empty_fav").getString()
                            : (Compat.emotecraftLoaded() ? Component.translatable("wh_npcs.ui.emotecraft.empty_emotes").getString() : "§8—"),
                    gridX + (winW - 2 * PAD) / 2, gridY + gridH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        ClientPrefs prefs = ClientPrefs.get();
        int totalRows = (shown.size() + cols - 1) / cols;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, totalRows - visibleRows)));
        int left = gridLeft();

        ScaledScreen.enableScissor(g, gridX + 1, gridY + 1, gridX + winW - 2 * PAD - 1, gridY + gridH - 1);
        for (int i = 0; i < shown.size(); i++) {
            int row = i / cols;
            if (row < scroll || row >= scroll + visibleRows) {
                continue;
            }
            int col = i % cols;
            int x = left + col * CW;
            int y = gridY + 4 + (row - scroll) * CH;
            EmotecraftBridge.EmoteRef e = shown.get(i);
            boolean isSel = selected != null && selected.id().equals(e.id());
            boolean hovered = isOver(mouseX, mouseY, x + 2, y, CW - 4, CH - 2);
            boolean fav = prefs.isFavoriteEmote(e.id());

            float p = bounce.getOrDefault(e.id(), 0F);
            p += ((hovered ? 1F : 0F) - p) * dt;
            bounce.put(e.id(), p);

            if (isSel) {
                g.fill(x + 2, y, x + CW - 2, y + CH - 2, VanillaUIHelper.BG_SELECTED);
            } else if (hovered) {
                g.fill(x + 2, y, x + CW - 2, y + CH - 2, VanillaUIHelper.BG_HOVERED);
            }

            int imgX = x + (CW - IMG) / 2;
            int imgY = y + 4;
            drawThumb(g, e, imgX, imgY, p);

            int nameColor = (hovered || isSel) ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE;
            g.drawCenteredString(font, font.plainSubstrByWidth(e.name(), CW - 4),
                    x + CW / 2, imgY + IMG + 3, nameColor);
            if (!e.author().isEmpty()) {
                g.drawCenteredString(font, font.plainSubstrByWidth("§8" + e.author(), CW - 4),
                        x + CW / 2, imgY + IMG + 14, VanillaUIHelper.TEXT_DARK_GRAY);
            }
            if (hovered || fav) {
                boolean hHover = isOver(mouseX, mouseY, imgX + IMG - 9, imgY + 1, 9, 9);
                drawHeart(g, imgX + IMG - 8, imgY + 2, fav ? 0xFFFF5555
                        : (hHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY));
            }
        }
        g.disableScissor();

        VanillaUIHelper.drawScrollbar(g, winX + winW - PAD - 6, gridY + 3, gridH - 6,
                totalRows, visibleRows, scroll, scrollbars, v -> scroll = v);
    }

    private void drawThumb(GuiGraphics g, EmotecraftBridge.EmoteRef e, int imgX, int imgY, float p) {
        net.minecraft.resources.ResourceLocation regIcon = e.iconTexture();
        EmoteThumbnails.Thumb t = regIcon == null ? EmoteThumbnails.get().getReady(e.icon()) : null;
        float s = 1F + 0.14F * p;
        float cx = imgX + IMG / 2F;
        float cy = imgY + IMG / 2F;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(s, s, 1F);
        g.pose().translate(-cx, -cy, 0);
        if (regIcon != null) {
            g.blit(regIcon, imgX, imgY, IMG, IMG, 0F, 0F, 256, 256, 256, 256);
        } else if (t != null) {
            g.blit(t.rl(), imgX, imgY, IMG, IMG, 0F, 0F, t.w(), t.h(), t.w(), t.h());
        } else {
            VanillaUIHelper.drawContentPanel(g, imgX, imgY, IMG, IMG);
            String ph = e.icon().isEmpty()
                    ? (e.name().isEmpty() ? "?" : e.name().substring(0, 1).toUpperCase(Locale.ROOT))
                    : "…";
            g.drawCenteredString(font, ph, imgX + IMG / 2, imgY + IMG / 2 - 4, VanillaUIHelper.TEXT_DARK_GRAY);
        }
        g.pose().popPose();
    }

    @Nullable
    private EmotecraftBridge.EmoteRef tileAt(double mx, double my) {
        List<EmotecraftBridge.EmoteRef> shown = displayed();
        int left = gridLeft();
        for (int i = 0; i < shown.size(); i++) {
            int row = i / cols;
            if (row < scroll || row >= scroll + visibleRows) {
                continue;
            }
            int col = i % cols;
            int x = left + col * CW;
            int y = gridY + 4 + (row - scroll) * CH;
            if (isOver(mx, my, x + 2, y, CW - 4, CH - 2)) {
                return shown.get(i);
            }
        }
        return null;
    }

    @Override
    protected boolean mouseClickedScaled(double mx, double my, int button) {
        recalc();
        if (button != 0) {
            return superMouseClicked(mx, my, button);
        }
        if (collapsed) {
            if (isOver(mx, my, CB_X + 4, CB_Y + (CB_H - EYE_BTN) / 2, EYE_BTN, EYE_BTN)) {
                setCollapsed(false);
                return true;
            }
            if (selected != null && isOver(mx, my, cbPlayX(), CB_Y + 3, 20, 18)) {
                play(selected);
                return true;
            }
            if (isOver(mx, my, cbStopX(), CB_Y + 3, 20, 18)) {
                if (npc != null) {
                    Compat.emotecraft().stopOn(npc);
                }
                return true;
            }
            return superMouseClicked(mx, my, button);
        }
        if (isOver(mx, my, eyeX(), eyeY(), EYE_BTN, EYE_BTN)) {
            setCollapsed(true);
            return true;
        }
        if (scrollbars.click(mx, my)) {
            return true;
        }
        if (isOver(mx, my, tabAllX(), controlsY, 30, 16)) {
            favTab = false;
            scroll = 0;
            return true;
        }
        if (isOver(mx, my, tabFavX(), controlsY, 28, 16)) {
            favTab = true;
            scroll = 0;
            return true;
        }
        if (isOver(mx, my, sortBtnX(), controlsY, 36, 16)) {
            sortMode = (sortMode + 1) % 2;
            return true;
        }
        if (isOver(mx, my, folderBtnX(), controlsY, 64, 16)) {
            openEmotesFolder();
            return true;
        }
        if (selected != null && isOver(mx, my, gridX, bottomY, 120, 18)) {
            play(selected);
            return true;
        }
        if (isOver(mx, my, gridX + 126, bottomY, 70, 18)) {
            if (npc != null) {
                Compat.emotecraft().stopOn(npc);
            }
            return true;
        }
        int doneW = pickMode() ? 86 : 60;
        if (isOver(mx, my, winX + winW - PAD - doneW, bottomY, doneW, 18)) {
            if (pickMode()) {
                if (selected != null && onPick != null) {
                    onPick.accept(selected);
                    onClose();
                }
            } else {
                saveIdleEmote();
                onClose();
            }
            return true;
        }
        List<EmotecraftBridge.EmoteRef> shown = displayed();
        int left = gridLeft();
        for (int i = 0; i < shown.size(); i++) {
            int row = i / cols;
            if (row < scroll || row >= scroll + visibleRows) {
                continue;
            }
            int col = i % cols;
            int x = left + col * CW;
            int y = gridY + 4 + (row - scroll) * CH;
            EmotecraftBridge.EmoteRef e = shown.get(i);
            int imgX = x + (CW - IMG) / 2;
            int imgY = y + 4;
            if (isOver(mx, my, imgX + IMG - 9, imgY + 1, 9, 9)) {
                ClientPrefs.get().toggleFavoriteEmote(e.id());
                return true;
            }
            if (isOver(mx, my, x + 2, y, CW - 4, CH - 2)) {
                long now = System.currentTimeMillis();
                boolean dbl = e.id().equals(lastClickId) && now - lastClickMs < 250;
                if (dbl && pickMode() && onPick != null) {
                    onPick.accept(e);
                    onClose();
                    return true;
                }
                if (dbl && !pickMode()) {
                    play(e);
                } else if (!pickMode() && selected != null && selected.id().equals(e.id())) {
                    selected = null;
                } else {
                    selected = e;
                }
                lastClickId = e.id();
                lastClickMs = now;
                return true;
            }
        }
        return superMouseClicked(mx, my, button);
    }

    private void play(EmotecraftBridge.EmoteRef e) {
        selected = e;
        if (npc != null) {
            Compat.emotecraft().playOn(npc, e.id(), e.name(), e.author());
        }
    }

    private void saveIdleEmote() {
        if (selected != null) {
            profileJson.addProperty("idle_emote", selected.id());
            profileJson.addProperty("idle_emote_name", selected.name());
            profileJson.addProperty("idle_emote_author", selected.author());
        } else {
            profileJson.remove("idle_emote");
            profileJson.remove("idle_emote_name");
            profileJson.remove("idle_emote_author");
        }
    }

    private int eyeX() {
        return winX + winW - PAD - EYE_BTN;
    }

    private int eyeY() {
        return winY + (HEADER_H - EYE_BTN) / 2;
    }

    private static void drawEyeButton(GuiGraphics g, int x, int y, boolean hover) {
        VanillaUIHelper.drawButton(g, x, y, EYE_BTN, EYE_BTN, hover);
        drawEye(g, x + (EYE_BTN - 10) / 2, y + (EYE_BTN - 6) / 2,
                hover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
    }

    private int cbPlayX() {
        return CB_X + CB_W - 44;
    }

    private int cbStopX() {
        return CB_X + CB_W - 22;
    }

    private void setCollapsed(boolean c) {
        collapsed = c;
        if (searchBox != null) {
            searchBox.visible = !c;
            searchBox.setFocused(false);
        }
    }

    private void renderCollapsed(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawWindow(g, CB_X, CB_Y, CB_W, CB_H);
        boolean eyeHover = isOver(mouseX, mouseY, CB_X + 4, CB_Y + (CB_H - EYE_BTN) / 2, EYE_BTN, EYE_BTN);
        drawEyeButton(g, CB_X + 4, CB_Y + (CB_H - EYE_BTN) / 2, eyeHover);
        int nameX = CB_X + 4 + EYE_BTN + 4;
        int nameMaxW = cbPlayX() - 6 - nameX;
        String nm = selected != null ? ellipsize(selected.name(), nameMaxW) : null;
        g.drawString(font, nm != null ? "§f" + nm : Component.translatable("wh_npcs.ui.emotecraft.no_selection").getString(), nameX, CB_Y + 8,
                VanillaUIHelper.TEXT_WHITE, false);
        drawBtn(g, "▶", cbPlayX(), CB_Y + 3, 20, mouseX, mouseY,
                selected != null ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_DARK_GRAY);
        drawBtn(g, "■", cbStopX(), CB_Y + 3, 20, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
    }

    private String ellipsize(String s, int maxW) {
        if (font.width(s) <= maxW) {
            return s;
        }
        return font.plainSubstrByWidth(s, Math.max(0, maxW - font.width("..."))) + "...";
    }

    private static void drawEye(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 2, y, x + 7, y + 1, c);
        g.fill(x + 1, y + 1, x + 8, y + 2, c);
        g.fill(x, y + 2, x + 9, y + 3, c);
        g.fill(x + 1, y + 3, x + 8, y + 4, c);
        g.fill(x + 2, y + 4, x + 7, y + 5, c);
        g.fill(x + 3, y + 1, x + 6, y + 4, 0xFF20242C);
    }

    @Override
    protected boolean mouseDraggedScaled(double mx, double my, int button, double dragX, double dragY) {
        if (!collapsed && scrollbars.drag(my)) {
            return true;
        }
        return superMouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mx, double my, int button) {
        scrollbars.release();
        return superMouseReleased(mx, my, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mx, double my, double delta) {
        recalc();
        if (collapsed) {
            return superMouseScrolled(mx, my, delta);
        }
        if (isOver(mx, my, gridX, gridY, winW - 2 * PAD, gridH)) {
            scroll = Math.max(0, scroll - (int) Math.signum(delta));
            return true;
        }
        return superMouseScrolled(mx, my, delta);
    }

    private void openEmotesFolder() {
        java.nio.file.Path dir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("emotes");
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException ignored) {

        }
        net.minecraft.Util.getPlatform().openFile(dir.toFile());
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        EmoteThumbnails.get().clear();
        super.removed();
    }

    private void drawMini(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 16);
        VanillaUIHelper.drawButton(g, x, y, w, 16, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 4, hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        VanillaUIHelper.drawSmallButton(g, font, label, x, y, w, isOver(mouseX, mouseY, x, y, w, 18), color);
    }

    private static void drawHeart(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 3, y + 1, c);
        g.fill(x + 4, y, x + 6, y + 1, c);
        g.fill(x, y + 1, x + 7, y + 3, c);
        g.fill(x + 1, y + 3, x + 6, y + 4, c);
        g.fill(x + 2, y + 4, x + 5, y + 5, c);
        g.fill(x + 3, y + 5, x + 4, y + 6, c);
    }

    private void multilineTooltip(GuiGraphics g, String text, int x, int y) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, x, y);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
