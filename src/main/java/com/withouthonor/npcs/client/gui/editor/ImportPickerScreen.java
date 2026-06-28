package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.client.cache.ClientSkinCache;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.ProfileSharePackets;
import com.withouthonor.npcs.network.ProfileSharePackets.FileEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ImportPickerScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 16;
    private static final int WIN_W = 360;
    private static final int WIN_H = 308;

    private final Screen parent;
    private final int entityId;

    private final boolean spawnMode;
    private List<FileEntry> files;
    private final ScrollDrag scrollbars = new ScrollDrag();

    private EditBox searchBox;
    private boolean favTab;
    private boolean clientSource;
    private List<FileEntry> clientFiles = new ArrayList<>();

    private int sortMode;
    private int scroll;
    @Nullable
    private String confirmImport;
    @Nullable
    private String confirmDelete;
    @Nullable
    private String renamingFile;
    @Nullable
    private EditBox renameBox;

    private int winX, winY, winW, winH;
    private int controlsY, listY, listH;
    private int bottomY;
    @Nullable
    private String hoverTooltip;

    public ImportPickerScreen(Screen parent, int entityId, List<FileEntry> files) {
        super(Component.translatable("wh_npcs.ui.import.title"));
        this.parent = parent;
        this.entityId = entityId;
        this.files = files;
        this.spawnMode = false;
    }

    public ImportPickerScreen(List<FileEntry> files) {
        super(Component.translatable("wh_npcs.ui.import.title_spawn"));
        this.parent = null;
        this.entityId = -1;
        this.files = files;
        this.spawnMode = true;
    }

    public void acceptList(List<FileEntry> updated) {
        this.files = updated;
        this.confirmDelete = null;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, winY + HEADER_H + 4, 150, 16,
                Component.translatable("wh_npcs.ui.import.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.import.search_hint"));
        searchBox.setResponder(v -> scroll = 0);
        refreshClient();
        renamingFile = null;
        renameBox = null;
    }

    private int renX(int listX, int listW) {
        return delX(listX, listW) - 14;
    }

    private void startRename(String name, int rowY) {
        renamingFile = name;
        renameBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD + 18, rowY + 2, 150, 12,
                Component.translatable("wh_npcs.ui.import.rename_hint")));
        renameBox.setMaxLength(48);
        renameBox.setValue(name);
        renameBox.setHint(Component.translatable("wh_npcs.ui.import.rename_hint"));
        setFocused(renameBox);
    }

    private void commitRename() {
        if (renamingFile != null && renameBox != null) {
            String nw = renameBox.getValue().trim();
            if (!nw.isEmpty() && !nw.equals(renamingFile)) {
                if (clientSource) {
                    com.withouthonor.npcs.client.ClientLocalFiles.renameProfile(renamingFile, nw);
                    refreshClient();
                } else {
                    NetworkHandler.sendToServer(new ProfileSharePackets.Rename(renamingFile, nw));
                }
            }
        }
        cancelRename();
    }

    private void cancelRename() {
        if (renameBox != null) {
            removeWidget(renameBox);
        }
        renameBox = null;
        renamingFile = null;
    }

    private void refreshClient() {
        String me = minecraft != null && minecraft.player != null
                ? minecraft.player.getGameProfile().getName() : "";
        clientFiles = com.withouthonor.npcs.client.ClientLocalFiles.listEntries(false, me);
    }

    private List<FileEntry> source() {
        return clientSource ? clientFiles : files;
    }

    private boolean clientTabs() {
        return true;
    }

    private int srcServerX() {
        return winX + PAD;
    }

    private int srcClientX() {
        return srcServerX() + 52;
    }

    private int browseBtnX() {
        return srcClientX() + 56;
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
        controlsY = winY + HEADER_H + 4;
        listY = controlsY + 22 + 20;
        listH = bottomY - 8 - listY;
    }

    private int tabAllX() {
        return winX + PAD + 156;
    }

    private int tabFavX() {
        return tabAllX() + 36;
    }

    private int sortBtnX() {
        return tabFavX() + 34;
    }

    private int folderBtnX() {
        return winX + winW - PAD - 64;
    }

    private List<FileEntry> displayed() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        ClientPrefs prefs = ClientPrefs.get();
        List<FileEntry> pinned = new ArrayList<>();
        List<FileEntry> rest = new ArrayList<>();
        for (FileEntry f : source()) {
            if (!q.isEmpty() && !f.name().toLowerCase(Locale.ROOT).contains(q)
                    && !f.author().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            if (favTab && !prefs.isFavoriteProfile(f.name())) {
                continue;
            }
            (prefs.isPinnedProfile(f.name()) ? pinned : rest).add(f);
        }
        Comparator<FileEntry> cmp = switch (sortMode) {
            case 1 -> Comparator.comparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER).reversed();
            case 2 -> Comparator.comparingLong(FileEntry::mtime).reversed();
            default -> Comparator.comparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER);
        };
        pinned.sort(cmp);
        rest.sort(cmp);
        pinned.addAll(rest);
        return pinned;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        hoverTooltip = null;
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, (spawnMode ? Component.translatable("wh_npcs.ui.import.title_spawn")
                        : Component.translatable("wh_npcs.ui.import.title")).getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        drawMini(g, Component.translatable("wh_npcs.ui.import.tab_all").getString(), tabAllX(), controlsY, 32, mouseX, mouseY,
                !favTab ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean favHover = isOver(mouseX, mouseY, tabFavX(), controlsY, 30, 16);
        VanillaUIHelper.drawButton(g, tabFavX(), controlsY, 30, 16, favHover || favTab);
        drawHeart(g, tabFavX() + 11, controlsY + 5, favTab ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY);
        String sortLabel = switch (sortMode) {
            case 1 -> Component.translatable("wh_npcs.ui.import.sort_za").getString();
            case 2 -> Component.translatable("wh_npcs.ui.import.sort_new").getString();
            default -> Component.translatable("wh_npcs.ui.import.sort_az").getString();
        };
        drawMini(g, sortLabel, sortBtnX(), controlsY, 36, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        if (isLocalWorld() || clientSource) {
            drawMini(g, Component.translatable("wh_npcs.ui.import.folder").getString() + " ↗", folderBtnX(), controlsY, 64, mouseX, mouseY, VanillaUIHelper.TEXT_GRAY);
        }

        if (clientTabs()) {
            int row = controlsY + 21;
            drawMini(g, Component.translatable("wh_npcs.ui.import.src_server").getString(), srcServerX(), row, 48, mouseX, mouseY,
                    !clientSource ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            drawMini(g, Component.translatable("wh_npcs.ui.import.src_client").getString(), srcClientX(), row, 48, mouseX, mouseY,
                    clientSource ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            if (clientSource) {
                drawMini(g, Component.translatable("wh_npcs.ui.import.browse").getString(), browseBtnX(), row, 64, mouseX, mouseY,
                        VanillaUIHelper.TEXT_GREEN);
            }
        } else {
            g.drawString(font, Component.translatable("wh_npcs.ui.import.world_folder").getString(), winX + PAD, controlsY + 20,
                    VanillaUIHelper.TEXT_DARK_GRAY, false);
        }

        renderList(g, mouseX, mouseY);

        drawBtn(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), winX + winW - PAD - 60, bottomY, 60, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);

        if (confirmImport == null && confirmDelete == null && isLocalWorld()
                && isOver(mouseX, mouseY, folderBtnX(), controlsY, 64, 16)) {
            hoverTooltip = Component.translatable("wh_npcs.ui.import.folder_tip").getString();
        }
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
        if (confirmImport != null) {
            if (spawnMode) {
                renderConfirm(g, mouseX, mouseY, Component.translatable("wh_npcs.ui.import.confirm_spawn").getString(), confirmImport,
                        Component.translatable("wh_npcs.ui.import.spawn").getString(), VanillaUIHelper.TEXT_GREEN, Component.translatable("wh_npcs.ui.import.confirm_spawn_sub").getString());
            } else {
                renderConfirm(g, mouseX, mouseY, Component.translatable("wh_npcs.ui.import.confirm_import").getString(), confirmImport,
                        Component.translatable("wh_npcs.ui.import.import").getString(), VanillaUIHelper.TEXT_GREEN, Component.translatable("wh_npcs.ui.import.confirm_irreversible").getString());
            }
        } else if (confirmDelete != null) {
            renderConfirm(g, mouseX, mouseY, Component.translatable("wh_npcs.ui.import.confirm_delete").getString(), confirmDelete,
                    Component.translatable("wh_npcs.ui.common.delete").getString(), VanillaUIHelper.TEXT_RED, Component.translatable("wh_npcs.ui.import.confirm_irreversible").getString());
        }
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int listX = winX + PAD;
        int listW = winW - 2 * PAD;
        VanillaUIHelper.drawContentPanel(g, listX, listY, listW, listH);
        List<FileEntry> shown = displayed();
        if (shown.isEmpty()) {
            g.drawCenteredString(font, (favTab ? Component.translatable("wh_npcs.ui.import.empty_fav")
                            : Component.translatable("wh_npcs.ui.import.empty")).getString(),
                    winX + winW / 2, listY + listH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        ClientPrefs prefs = ClientPrefs.get();
        boolean canDel = clientSource || canDelete();
        int visible = (listH - 8) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, shown.size() - visible)));
        int y = listY + 4;
        for (int i = scroll; i < Math.min(shown.size(), scroll + visible); i++) {
            FileEntry f = shown.get(i);
            boolean pinned = prefs.isPinnedProfile(f.name());
            boolean fav = prefs.isFavoriteProfile(f.name());
            boolean hovered = isOver(mouseX, mouseY, listX + 2, y, listW - 4, ROW_H);
            if (hovered) {
                g.fill(listX + 2, y, listX + listW - 2, y + ROW_H, VanillaUIHelper.BG_HOVERED);
            }
            drawAuthorHead(g, f.author(), listX + 5, y + 4);
            int nameX = listX + 18;
            if (pinned) {
                drawPin(g, nameX, y + 4, VanillaUIHelper.TEXT_GOLD);
                nameX += 10;
            }
            if (renamingFile != null && renamingFile.equals(f.name())) {
                if (renameBox != null) {
                    renameBox.setY(y + 2);
                }
                y += ROW_H;
                continue;
            }
            boolean showDelete = hovered && canDel;
            int rightLimit = showDelete
                    ? renX(listX, listW) - 6
                    : heartX(listX, listW) - 8 - font.width(Component.translatable("wh_npcs.ui.import.size_kb", f.sizeKb()).getString());
            g.drawString(font, font.plainSubstrByWidth(f.name(), rightLimit - nameX), nameX, y + 4,
                    hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (showDelete) {
                boolean renHover = isOver(mouseX, mouseY, renX(listX, listW), y + 3, 10, 10);
                drawPencil(g, renX(listX, listW), y + 3, renHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY);
                boolean delHover = isOver(mouseX, mouseY, delX(listX, listW), y + 3, 10, 10);
                g.drawString(font, "✕", delX(listX, listW), y + 4,
                        delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
            } else {
                String size = Component.translatable("wh_npcs.ui.import.size_kb", f.sizeKb()).getString();
                g.drawString(font, size, pinX(listX, listW) - 6 - font.width(size), y + 4,
                        VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            if (hovered || pinned) {
                drawPin(g, pinX(listX, listW), y + 4, pinned ? VanillaUIHelper.TEXT_GOLD
                        : (isOver(mouseX, mouseY, pinX(listX, listW), y + 3, 10, 10)
                        ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
            }
            if (hovered || fav) {
                drawHeart(g, heartX(listX, listW), y + 5, fav ? 0xFFFF5555
                        : (isOver(mouseX, mouseY, heartX(listX, listW), y + 3, 10, 10)
                        ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
            }

            if (!f.author().isEmpty() && confirmImport == null && confirmDelete == null
                    && isOver(mouseX, mouseY, listX + 5, y + 4, 8, 8)) {
                hoverTooltip = Component.translatable("wh_npcs.ui.import.author_tip", f.author()).getString();
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + listW - 6, listY + 3, listH - 6,
                shown.size(), visible, scroll, scrollbars, v -> scroll = v);
    }

    private int heartX(int listX, int listW) {
        return listX + listW - 16;
    }

    private int pinX(int listX, int listW) {
        return listX + listW - 30;
    }

    private int delX(int listX, int listW) {
        return listX + listW - 44;
    }

    private void renderConfirm(GuiGraphics g, int mouseX, int mouseY, String title, String value,
                               String okLabel, int okColor, String subtitle) {
        int w = 260;
        int h = 96;
        int x = (width - w) / 2;
        int cy = (height - h) / 2;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, width, height, 0xA0000000);
        VanillaUIHelper.drawWindow(g, x, cy, w, h);
        g.drawCenteredString(font, title, x + w / 2, cy + 14, VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, "§b" + font.plainSubstrByWidth(value, w - 30), x + w / 2, cy + 30, VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, subtitle, x + w / 2, cy + 48, VanillaUIHelper.TEXT_WHITE);
        drawBtn(g, okLabel, x + w / 2 - 110, cy + h - 28, 104, mouseX, mouseY, okColor);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), x + w / 2 + 6, cy + h - 28, 104, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        g.pose().popPose();
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button != 0) {
            return superMouseClicked(mouseX, mouseY, button);
        }

        if (renamingFile != null) {
            if (renameBox != null && isOver(mouseX, mouseY,
                    renameBox.getX(), renameBox.getY(), renameBox.getWidth(), renameBox.getHeight())) {
                return superMouseClicked(mouseX, mouseY, button);
            }
            commitRename();
            return true;
        }

        if (confirmImport != null || confirmDelete != null) {
            int w = 260;
            int h = 96;
            int x = (width - w) / 2;
            int cy = (height - h) / 2;
            if (isOver(mouseX, mouseY, x + w / 2 - 110, cy + h - 28, 104, 18)) {
                if (confirmImport != null) {
                    if (spawnMode && clientSource) {
                        try {
                            byte[] b = com.withouthonor.npcs.client.ClientLocalFiles.read(false, confirmImport);
                            NetworkHandler.sendToServer(new ProfileSharePackets.SpawnFromClient(b));
                        } catch (Exception ignored) {
                        }
                        confirmImport = null;
                    } else if (spawnMode) {
                        NetworkHandler.sendToServer(new ProfileSharePackets.SpawnFromFile(confirmImport));
                        confirmImport = null;
                    } else if (clientSource) {
                        try {
                            byte[] b = com.withouthonor.npcs.client.ClientLocalFiles.read(false, confirmImport);
                            NetworkHandler.sendToServer(new com.withouthonor.npcs.network.ClientImportPacket(
                                    com.withouthonor.npcs.network.ClientImportPacket.KIND_PROFILE, entityId, b));
                        } catch (Exception ignored) {
                        }
                        onClose();
                    } else {
                        NetworkHandler.sendToServer(new ProfileSharePackets.Import(entityId, confirmImport));
                        onClose();
                    }
                } else {
                    if (clientSource) {
                        com.withouthonor.npcs.client.ClientLocalFiles.delete(false, confirmDelete);
                        refreshClient();
                    } else {
                        NetworkHandler.sendToServer(new ProfileSharePackets.Delete(confirmDelete));
                    }
                    confirmDelete = null;
                }
                return true;
            }
            if (isOver(mouseX, mouseY, x + w / 2 + 6, cy + h - 28, 104, 18)) {
                confirmImport = null;
                confirmDelete = null;
                return true;
            }
            return true;
        }
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }

        if (isOver(mouseX, mouseY, tabAllX(), controlsY, 32, 16)) {
            favTab = false;
            scroll = 0;
            return true;
        }
        if (isOver(mouseX, mouseY, tabFavX(), controlsY, 30, 16)) {
            favTab = true;
            scroll = 0;
            return true;
        }
        if (isOver(mouseX, mouseY, sortBtnX(), controlsY, 36, 16)) {
            sortMode = (sortMode + 1) % 3;
            return true;
        }
        if ((isLocalWorld() || clientSource) && isOver(mouseX, mouseY, folderBtnX(), controlsY, 64, 16)) {
            if (clientSource) {
                com.withouthonor.npcs.client.ClientLocalFiles.openFolder(false);
            } else {
                openExportsFolder();
            }
            return true;
        }
        if (clientTabs()) {
            int row = controlsY + 21;
            if (isOver(mouseX, mouseY, srcServerX(), row, 48, 16)) {
                clientSource = false;
                scroll = 0;
                return true;
            }
            if (isOver(mouseX, mouseY, srcClientX(), row, 48, 16)) {
                clientSource = true;
                scroll = 0;
                refreshClient();
                return true;
            }
            if (clientSource && isOver(mouseX, mouseY, browseBtnX(), row, 64, 16)) {
                if (spawnMode) {
                    com.withouthonor.npcs.client.ClientLocalFiles.browseJson(bytes ->
                            NetworkHandler.sendToServer(new ProfileSharePackets.SpawnFromClient(bytes)));
                } else {
                    com.withouthonor.npcs.client.ClientLocalFiles.browseJson(bytes ->
                            NetworkHandler.sendToServer(new com.withouthonor.npcs.network.ClientImportPacket(
                                    com.withouthonor.npcs.network.ClientImportPacket.KIND_PROFILE, entityId, bytes)));
                }
                return true;
            }
        }

        int listX = winX + PAD;
        int listW = winW - 2 * PAD;
        List<FileEntry> shown = displayed();
        boolean canDel = clientSource || canDelete();
        int visible = (listH - 8) / ROW_H;
        int y = listY + 4;
        for (int i = scroll; i < Math.min(shown.size(), scroll + visible); i++) {
            FileEntry f = shown.get(i);
            boolean hovered = isOver(mouseX, mouseY, listX + 2, y, listW - 4, ROW_H);
            if (hovered) {
                if (isOver(mouseX, mouseY, heartX(listX, listW), y + 3, 10, 10)) {
                    ClientPrefs.get().toggleFavoriteProfile(f.name());
                    return true;
                }
                if (isOver(mouseX, mouseY, pinX(listX, listW), y + 3, 10, 10)) {
                    ClientPrefs.get().togglePinnedProfile(f.name());
                    return true;
                }
                if (canDel && isOver(mouseX, mouseY, renX(listX, listW), y + 3, 10, 10)) {
                    startRename(f.name(), y);
                    return true;
                }
                if (canDel && isOver(mouseX, mouseY, delX(listX, listW), y + 3, 10, 10)) {
                    confirmDelete = f.name();
                    return true;
                }
                confirmImport = f.name();
                return true;
            }
            y += ROW_H;
        }
        if (isOver(mouseX, mouseY, winX + winW - PAD - 60, bottomY, 60, 18)) {
            onClose();
            return true;
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
        if (isOver(mouseX, mouseY, winX + PAD, listY, winW - 2 * PAD, listH)) {
            scroll -= (int) Math.signum(delta) * 2;
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (renamingFile != null) {
            if (key == 257 || key == 335) {
                commitRename();
                return true;
            }
            if (key == 256) {
                cancelRename();
                return true;
            }
            return super.keyPressed(key, scancode, mods);
        }
        if (key == 256 && (confirmImport != null || confirmDelete != null)) {
            confirmImport = null;
            confirmDelete = null;
            return true;
        }
        return super.keyPressed(key, scancode, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private boolean isLocalWorld() {
        return minecraft != null && minecraft.hasSingleplayerServer();
    }

    private boolean canDelete() {
        return minecraft != null && minecraft.player != null
                && (minecraft.player.hasPermissions(3) || minecraft.hasSingleplayerServer());
    }

    private void openExportsFolder() {
        if (minecraft == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        java.nio.file.Path dir = minecraft.getSingleplayerServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("wh_npcs").resolve("exports");
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException ignored) {

        }
        net.minecraft.Util.getPlatform().openFile(dir.toFile());
    }

    private static void drawPencil(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 6, y, x + 9, y + 2, color);
        g.fill(x + 4, y + 2, x + 7, y + 4, color);
        g.fill(x + 2, y + 4, x + 5, y + 6, color);
        g.fill(x, y + 6, x + 3, y + 9, color);
    }

    private static void drawAuthorHead(GuiGraphics g, String author, int x, int y) {
        ClientSkinCache.Skin skin = author.isEmpty() ? null : ClientSkinCache.getInstance().get(author);
        if (skin != null) {
            g.blit(skin.location(), x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(skin.location(), x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + 8, y + 8, 0xFF6E5037);
            g.fill(x + 2, y + 4, x + 3, y + 5, 0xFF2B1F14);
            g.fill(x + 5, y + 4, x + 6, y + 5, 0xFF2B1F14);
        }
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

    private static void drawPin(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 7, y + 4, c);
        g.fill(x + 3, y + 4, x + 5, y + 8, c);
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
