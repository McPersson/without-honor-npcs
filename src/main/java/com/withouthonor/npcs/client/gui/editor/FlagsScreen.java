package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.cache.ClientSkinCache;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.network.FlagPackets;
import com.withouthonor.npcs.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FlagsScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 14;
    private static final int WIN_W = 470;
    private static final int WIN_H = 320;
    private static final int LEFT_W = 140;

    private final Screen parent;

    @Nullable
    private List<FlagPackets.PlayerRef> players;
    @Nullable
    private List<FlagPackets.FlagRef> flags;
    @Nullable
    private String selectedUuid;
    private String selectedName = "";

    private int playerScroll, flagScroll;
    private int playerSortMode;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private EditBox searchBox;
    private EditBox playerSearchBox;

    @Nullable
    private String editingFlag;
    @Nullable
    private EditBox descBox;
    private EditBox addBox;

    private int winX, winY, winW, winH;
    private int labelsY, searchRowY, listY, listH;
    private int rightX, rightW;
    private int bottomY;
    @Nullable
    private String hoverTooltip;

    public FlagsScreen(Screen parent) {
        super(Component.translatable("wh_npcs.ui.flags.title"));
        this.parent = parent;
        NetworkHandler.sendToServer(new FlagPackets.RequestPlayers());
    }

    public static void acceptPlayers(List<FlagPackets.PlayerRef> list) {
        if (Minecraft.getInstance().screen instanceof FlagsScreen s) {
            s.players = list;
        }
    }

    public static void acceptFlags(String uuid, List<FlagPackets.FlagRef> list) {
        if (Minecraft.getInstance().screen instanceof FlagsScreen s
                && uuid.equals(s.selectedUuid)) {
            s.flags = list;
        }
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        editingFlag = null;
        descBox = null;
        String oldPs = playerSearchBox != null ? playerSearchBox.getValue() : "";
        playerSearchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, searchRowY, LEFT_W - 24, 14,
                Component.translatable("wh_npcs.ui.flags.player_search")));
        playerSearchBox.setMaxLength(32);
        playerSearchBox.setValue(oldPs);
        playerSearchBox.setHint(Component.translatable("wh_npcs.ui.flags.player_search"));
        playerSearchBox.setResponder(v -> playerScroll = 0);

        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, rightX, searchRowY, rightW - 70, 14,
                Component.translatable("wh_npcs.ui.flags.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.flags.search_hint"));
        searchBox.setResponder(v -> flagScroll = 0);

        String oldAdd = addBox != null ? addBox.getValue() : "";
        addBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, bottomY, 188, 16,
                Component.translatable("wh_npcs.ui.flags.add_hint")));
        addBox.setMaxLength(64);
        addBox.setValue(oldAdd);
        addBox.setHint(Component.translatable("wh_npcs.ui.flags.add_hint"));
    }

    private void doAddFlag() {
        if (selectedUuid == null || addBox == null) {
            return;
        }
        String flag = addBox.getValue().trim();
        if (!flag.isEmpty()) {
            NetworkHandler.sendToServer(new FlagPackets.AddFlag(selectedUuid, flag));
            addBox.setValue("");
        }
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
        labelsY = winY + HEADER_H + 4;
        searchRowY = winY + HEADER_H + 16;
        listY = winY + HEADER_H + 36;
        listH = bottomY - 8 - listY;
        rightX = winX + PAD + LEFT_W + 8;
        rightW = winX + winW - PAD - rightX;
    }

    private List<FlagPackets.PlayerRef> displayedPlayers() {
        List<FlagPackets.PlayerRef> out = new ArrayList<>();
        if (players == null) {
            return out;
        }
        String q = playerSearchBox != null ? playerSearchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        com.withouthonor.npcs.client.ClientPrefs prefs = com.withouthonor.npcs.client.ClientPrefs.get();
        List<FlagPackets.PlayerRef> pinned = new ArrayList<>();
        List<FlagPackets.PlayerRef> rest = new ArrayList<>();
        for (FlagPackets.PlayerRef p : players) {
            if (!q.isEmpty() && !p.name().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            (prefs.isPinnedFlagPlayer(p.uuid()) ? pinned : rest).add(p);
        }
        java.util.Comparator<FlagPackets.PlayerRef> cmp = switch (playerSortMode) {
            case 1 -> java.util.Comparator.comparing(FlagPackets.PlayerRef::name, String.CASE_INSENSITIVE_ORDER).reversed();
            case 2 -> java.util.Comparator.comparingInt(FlagPackets.PlayerRef::count).reversed()
                    .thenComparing(FlagPackets.PlayerRef::name, String.CASE_INSENSITIVE_ORDER);
            default -> java.util.Comparator.comparing(FlagPackets.PlayerRef::name, String.CASE_INSENSITIVE_ORDER);
        };
        pinned.sort(cmp);
        rest.sort(cmp);
        out.addAll(pinned);
        out.addAll(rest);
        return out;
    }

    private List<FlagPackets.FlagRef> displayedFlags() {
        List<FlagPackets.FlagRef> out = new ArrayList<>();
        if (flags == null) {
            return out;
        }
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        for (FlagPackets.FlagRef f : flags) {
            if (q.isEmpty() || f.name().toLowerCase(Locale.ROOT).contains(q)
                    || f.desc().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(f);
            }
        }
        return out;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        hoverTooltip = null;
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.flags.title").getString(), winX + PAD, winY + 7,
                VanillaUIHelper.TEXT_YELLOW, false);

        renderPlayers(g, mouseX, mouseY);
        renderFlags(g, mouseX, mouseY);

        if (addBox != null) {
            addBox.visible = selectedUuid != null;
        }
        if (selectedUuid != null) {
            drawMini(g, Component.translatable("wh_npcs.ui.flags.add").getString(),
                    winX + PAD + 192, bottomY, 70, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        }

        drawMini(g, Component.translatable("wh_npcs.ui.flags.refresh").getString(),
                rightX + rightW - 64, searchRowY - 1, 64, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawMini(g, Component.translatable("wh_npcs.ui.common.done").getString(),
                winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    private int sortBtnX() {
        return winX + PAD + LEFT_W - 22;
    }

    private void renderPlayers(GuiGraphics g, int mouseX, int mouseY) {
        int px = winX + PAD;
        g.drawString(font, Component.translatable("wh_npcs.ui.flags.players").getString(), px, labelsY,
                VanillaUIHelper.TEXT_GRAY, false);
        String sortLabel = switch (playerSortMode) {
            case 1 -> Component.translatable("wh_npcs.ui.flags.sort_za").getString();
            case 2 -> Component.translatable("wh_npcs.ui.flags.sort_count").getString();
            default -> Component.translatable("wh_npcs.ui.flags.sort_az").getString();
        };
        drawMini(g, sortLabel, sortBtnX(), searchRowY - 1, 22, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        if (isOver(mouseX, mouseY, sortBtnX(), searchRowY - 1, 22, 16)) {
            hoverTooltip = Component.translatable("wh_npcs.ui.flags.sort_tip").getString();
        }
        VanillaUIHelper.drawContentPanel(g, px, listY, LEFT_W, listH);
        if (players == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.flags.loading").getString(),
                    px + LEFT_W / 2, listY + listH / 2 - 4, VanillaUIHelper.TEXT_STATUS);
            return;
        }
        List<FlagPackets.PlayerRef> shown = displayedPlayers();
        if (shown.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.flags.no_players").getString(),
                    px + LEFT_W / 2, listY + listH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        com.withouthonor.npcs.client.ClientPrefs prefs = com.withouthonor.npcs.client.ClientPrefs.get();
        int visible = (listH - 8) / ROW_H;
        playerScroll = Math.max(0, Math.min(playerScroll, Math.max(0, shown.size() - visible)));
        int y = listY + 4;
        for (int i = playerScroll; i < Math.min(shown.size(), playerScroll + visible); i++) {
            FlagPackets.PlayerRef p = shown.get(i);
            boolean sel = p.uuid().equals(selectedUuid);
            boolean pinnedP = prefs.isPinnedFlagPlayer(p.uuid());
            boolean hovered = isOver(mouseX, mouseY, px + 2, y, LEFT_W - 4, ROW_H);
            if (sel || hovered) {
                g.fill(px + 2, y, px + LEFT_W - 2, y + ROW_H, sel ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            drawPlayerHead(g, p.name(), px + 4, y + 3);
            String cnt = String.valueOf(p.count());
            boolean showPin = hovered || pinnedP;
            int rightReserve = (showPin ? 12 : 0) + font.width(cnt) + 8;
            String label = font.plainSubstrByWidth(p.name(), LEFT_W - 18 - rightReserve);
            g.drawString(font, label, px + 16, y + 3,
                    sel ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, cnt, px + LEFT_W - 6 - font.width(cnt), y + 3, VanillaUIHelper.TEXT_DARK_GRAY, false);
            if (showPin) {
                boolean ph = isOver(mouseX, mouseY, pinX(px), y + 2, 10, 10);
                drawPin(g, pinX(px), y + 3, pinnedP ? VanillaUIHelper.TEXT_GOLD
                        : (ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, px + LEFT_W - 6, listY + 3, listH - 6,
                shown.size(), visible, playerScroll, scrollbars, v -> playerScroll = v);
    }

    private int pinX(int px) {
        return px + LEFT_W - 14 - font.width("99");
    }

    private static void drawPin(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 7, y + 4, c);
        g.fill(x + 3, y + 4, x + 5, y + 8, c);
    }

    private void renderFlags(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, selectedUuid == null
                        ? Component.translatable("wh_npcs.ui.flags.pick_player").getString()
                        : Component.translatable("wh_npcs.ui.flags.flags_of", selectedName).getString(),
                rightX, labelsY, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawContentPanel(g, rightX, listY, rightW, listH);
        if (selectedUuid == null) {
            return;
        }
        if (flags == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.flags.loading").getString(),
                    rightX + rightW / 2, listY + listH / 2 - 4, VanillaUIHelper.TEXT_STATUS);
            return;
        }
        List<FlagPackets.FlagRef> shown = displayedFlags();
        if (shown.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.flags.no_flags").getString(),
                    rightX + rightW / 2, listY + listH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        int visible = (listH - 8) / ROW_H;
        flagScroll = Math.max(0, Math.min(flagScroll, Math.max(0, shown.size() - visible)));
        int y = listY + 4;
        boolean canEdit = canManage();
        for (int i = flagScroll; i < Math.min(shown.size(), flagScroll + visible); i++) {
            FlagPackets.FlagRef f = shown.get(i);
            boolean hovered = isOver(mouseX, mouseY, rightX + 2, y, rightW - 4, ROW_H);
            if (hovered) {
                g.fill(rightX + 2, y, rightX + rightW - 2, y + ROW_H, VanillaUIHelper.BG_HOVERED);
            }
            if (editingFlag != null && editingFlag.equals(f.name())) {
                if (descBox != null) {
                    descBox.setY(y + 1);
                }
                y += ROW_H;
                continue;
            }
            int nameW = Math.min(140, rightW / 2);
            g.drawString(font, font.plainSubstrByWidth(f.name(), nameW), rightX + 5, y + 3,
                    VanillaUIHelper.TEXT_AQUA, false);
            int descX = rightX + 10 + nameW;
            int descLimit = rightX + rightW - (hovered && canEdit ? 30 : 6) - descX;
            String desc = f.desc().isEmpty()
                    ? Component.translatable("wh_npcs.ui.flags.no_desc").getString()
                    : f.desc();
            g.drawString(font, font.plainSubstrByWidth(desc, Math.max(10, descLimit)), descX, y + 3,
                    f.desc().isEmpty() ? VanillaUIHelper.TEXT_DARK_GRAY : VanillaUIHelper.TEXT_GRAY, false);
            if (hovered && canEdit) {
                boolean rh = isOver(mouseX, mouseY, renX(), y + 2, 10, 10);
                VanillaUIHelper.drawRenameIcon(g, font, renX(), y + 3, rh ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY);
                boolean dh = isOver(mouseX, mouseY, delX(), y + 2, 10, 10);
                g.drawString(font, "✕", delX(), y + 3, dh ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
            }
            if (hovered) {
                String src = f.source().isEmpty()
                        ? Component.translatable("wh_npcs.ui.flags.source_unknown").getString()
                        : f.source();
                String when = f.time() > 0
                        ? new java.text.SimpleDateFormat("dd.MM.yy HH:mm").format(new java.util.Date(f.time()))
                        : "—";
                hoverTooltip = Component.translatable("wh_npcs.ui.flags.provenance", src, when).getString();
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, rightX + rightW - 6, listY + 3, listH - 6,
                shown.size(), visible, flagScroll, scrollbars, v -> flagScroll = v);
    }

    private int renX() {
        return rightX + rightW - 30;
    }

    private int delX() {
        return rightX + rightW - 14;
    }

    private boolean canManage() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private void startDescEdit(String flag, String curDesc, int rowY) {
        editingFlag = flag;
        descBox = addRenderableWidget(new SelectableEditBox(font, rightX + 5, rowY + 1, rightW - 40, 12,
                Component.translatable("wh_npcs.ui.flags.desc_hint")));
        descBox.setMaxLength(240);
        descBox.setValue(curDesc);
        descBox.setHint(Component.translatable("wh_npcs.ui.flags.desc_hint"));
        setFocused(descBox);
    }

    private void commitDescEdit() {
        if (editingFlag != null && descBox != null) {
            String desc = descBox.getValue().trim();
            NetworkHandler.sendToServer(new FlagPackets.SetDescription(editingFlag, desc));
            if (flags != null) {
                for (int i = 0; i < flags.size(); i++) {
                    if (flags.get(i).name().equals(editingFlag)) {
                        FlagPackets.FlagRef o = flags.get(i);
                        flags.set(i, new FlagPackets.FlagRef(o.name(), desc, o.source(), o.time()));
                    }
                }
            }
        }
        cancelDescEdit();
    }

    private void cancelDescEdit() {
        if (descBox != null) {
            removeWidget(descBox);
        }
        descBox = null;
        editingFlag = null;
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

    private void drawMini(GuiGraphics g, String label, int x, int y, int w,
                          int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 16);
        VanillaUIHelper.drawButton(g, x, y, w, 16, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 4, hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private static void drawPlayerHead(GuiGraphics g, String name, int x, int y) {
        ClientSkinCache.Skin skin = name == null || name.isEmpty() ? null : ClientSkinCache.getInstance().get(name);
        if (skin != null) {
            g.blit(skin.location(), x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(skin.location(), x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + 8, y + 8, 0xFF6E5037);
            g.fill(x + 2, y + 4, x + 3, y + 5, 0xFF2B1F14);
            g.fill(x + 5, y + 4, x + 6, y + 5, 0xFF2B1F14);
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return superMouseClicked(mouseX, mouseY, button);
        }
        recalc();
        if (editingFlag != null) {
            if (descBox != null && isOver(mouseX, mouseY,
                    descBox.getX(), descBox.getY(), descBox.getWidth(), descBox.getHeight())) {
                return superMouseClicked(mouseX, mouseY, button);
            }
            commitDescEdit();
            return true;
        }
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }
        if (isOver(mouseX, mouseY, rightX + rightW - 64, searchRowY - 1, 64, 16)) {
            NetworkHandler.sendToServer(new FlagPackets.RequestPlayers());
            if (selectedUuid != null) {
                NetworkHandler.sendToServer(new FlagPackets.RequestFlags(selectedUuid));
            }
            return true;
        }
        if (isOver(mouseX, mouseY, sortBtnX(), searchRowY - 1, 22, 16)) {
            playerSortMode = (playerSortMode + 1) % 3;
            playerScroll = 0;
            return true;
        }
        if (selectedUuid != null && isOver(mouseX, mouseY, winX + PAD + 192, bottomY, 70, 16)) {
            doAddFlag();
            return true;
        }
        if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
            onClose();
            return true;
        }
        if (players != null) {
            int px = winX + PAD;
            List<FlagPackets.PlayerRef> shown = displayedPlayers();
            int visible = (listH - 8) / ROW_H;
            int y = listY + 4;
            for (int i = playerScroll; i < Math.min(shown.size(), playerScroll + visible); i++) {
                FlagPackets.PlayerRef p = shown.get(i);
                if (isOver(mouseX, mouseY, pinX(px), y + 2, 10, 10)) {
                    com.withouthonor.npcs.client.ClientPrefs.get().togglePinnedFlagPlayer(p.uuid());
                    return true;
                }
                if (isOver(mouseX, mouseY, px + 2, y, LEFT_W - 4, ROW_H)) {
                    selectedUuid = p.uuid();
                    selectedName = p.name();
                    flags = null;
                    flagScroll = 0;
                    NetworkHandler.sendToServer(new FlagPackets.RequestFlags(selectedUuid));
                    return true;
                }
                y += ROW_H;
            }
        }
        if (selectedUuid != null && flags != null && canManage()) {
            List<FlagPackets.FlagRef> shown = displayedFlags();
            int visible = (listH - 8) / ROW_H;
            int y = listY + 4;
            for (int i = flagScroll; i < Math.min(shown.size(), flagScroll + visible); i++) {
                FlagPackets.FlagRef f = shown.get(i);
                if (isOver(mouseX, mouseY, renX(), y + 2, 10, 10)) {
                    startDescEdit(f.name(), f.desc(), y);
                    return true;
                }
                if (isOver(mouseX, mouseY, delX(), y + 2, 10, 10)) {
                    NetworkHandler.sendToServer(new FlagPackets.RemoveFlag(selectedUuid, f.name()));
                    return true;
                }
                y += ROW_H;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (editingFlag != null) {
            if (key == 257 || key == 335) {
                commitDescEdit();
                return true;
            }
            if (key == 256) {
                cancelDescEdit();
                return true;
            }
        }
        if ((key == 257 || key == 335) && addBox != null && addBox.isFocused() && selectedUuid != null) {
            doAddFlag();
            return true;
        }
        return super.keyPressed(key, scancode, mods);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, winX + PAD, listY, LEFT_W, listH)) {
            playerScroll -= (int) Math.signum(delta) * 2;
            return true;
        }
        if (isOver(mouseX, mouseY, rightX, listY, rightW, listH)) {
            flagScroll -= (int) Math.signum(delta) * 2;
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
