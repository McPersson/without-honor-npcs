package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.client.cache.ClientImageCache;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.DialogueNode;
import com.withouthonor.npcs.common.storage.ImageStore;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.RequestImageListPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NodeImagesScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 24;
    private static final int FILE_ROW_H = 12;
    private static final int WIN_W = 440;
    private static final int WIN_H = 312;

    private final Screen parent;
    private final DialogueNode node;

    @Nullable
    private List<ImageStore.ImageInfo> serverFiles;
    private int fileScroll;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private final List<EditBox> captionBoxes = new ArrayList<>();
    private EditBox searchBox;
    private boolean favTab;

    private int sortMode;

    private int winX, winY, winW, winH;
    private int rowsY;
    private int controlsY, filesY, filesH;
    private int bottomY;
    @Nullable
    private String hoverTooltip;
    @Nullable
    private String previewName;
    @Nullable
    private String renamingFile;
    @Nullable
    private EditBox renameBox;

    public NodeImagesScreen(Screen parent, DialogueNode node) {
        super(Component.translatable("wh_npcs.ui.node_images.title"));
        this.parent = parent;
        this.node = node;
        NetworkHandler.sendToServer(new RequestImageListPacket());
    }

    public static void acceptServerList(List<ImageStore.ImageInfo> images) {
        if (Minecraft.getInstance().screen instanceof NodeImagesScreen screen) {
            screen.serverFiles = images;
        }
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        captionBoxes.clear();
        renamingFile = null;
        renameBox = null;
        String oldSearch = searchBox != null ? searchBox.getValue() : "";
        for (int i = 0; i < node.getImages().size(); i++) {
            DialogueNode.ImageRef ref = node.getImages().get(i);
            EditBox box = addRenderableWidget(new SelectableEditBox(font,
                    winX + PAD + 150, rowsY + i * ROW_H, winW - PAD * 2 - 150 - 26, 16,
                    Component.translatable("wh_npcs.ui.node_images.caption")));
            box.setMaxLength(80);
            box.setValue(ref.caption());
            box.setHint(Component.translatable("wh_npcs.ui.node_images.caption_hint"));
            final int idx = i;
            box.setResponder(value -> {
                if (idx < node.getImages().size()) {
                    node.getImages().set(idx, new DialogueNode.ImageRef(
                            node.getImages().get(idx).file(), value));
                }
            });
            captionBoxes.add(box);
        }
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, controlsY, 150, 16,
                Component.translatable("wh_npcs.ui.node_images.search")));
        searchBox.setMaxLength(64);
        searchBox.setValue(oldSearch);
        searchBox.setHint(Component.translatable("wh_npcs.ui.node_images.search_hint"));
        searchBox.setResponder(v -> fileScroll = 0);
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
        rowsY = winY + HEADER_H + 18;
        controlsY = rowsY + DialogueNode.MAX_IMAGES * ROW_H + 6;
        filesY = controlsY + 22;
        filesH = bottomY - 8 - filesY;
    }

    private List<ImageStore.ImageInfo> displayed() {
        List<ImageStore.ImageInfo> result = new ArrayList<>();
        if (serverFiles == null) {
            return result;
        }
        String query = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        ClientPrefs prefs = ClientPrefs.get();
        List<ImageStore.ImageInfo> pinned = new ArrayList<>();
        List<ImageStore.ImageInfo> rest = new ArrayList<>();
        for (ImageStore.ImageInfo info : serverFiles) {
            if (!query.isEmpty() && !info.name().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            if (favTab && !prefs.isFavoriteImage(info.name())) {
                continue;
            }
            (prefs.isPinnedImage(info.name()) ? pinned : rest).add(info);
        }
        Comparator<ImageStore.ImageInfo> comparator = switch (sortMode) {
            case 1 -> Comparator.comparing(ImageStore.ImageInfo::name, String.CASE_INSENSITIVE_ORDER).reversed();
            case 2 -> Comparator.comparingLong(ImageStore.ImageInfo::mtime).reversed();
            default -> Comparator.comparing(ImageStore.ImageInfo::name, String.CASE_INSENSITIVE_ORDER);
        };
        pinned.sort(comparator);
        rest.sort(comparator);
        result.addAll(pinned);
        result.addAll(rest);
        return result;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        hoverTooltip = null;
        previewName = null;
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.node_images.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        String limit = node.getImages().size() + " / " + DialogueNode.MAX_IMAGES;
        g.drawString(font, limit, winX + winW - PAD - font.width(limit), winY + 7,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        g.drawString(font, Component.translatable("wh_npcs.ui.node_images.attachments").getString(), winX + PAD, winY + HEADER_H + 6,
                VanillaUIHelper.TEXT_GRAY, false);
        if (node.getImages().isEmpty()) {
            g.drawString(font, Component.translatable("wh_npcs.ui.node_images.empty_attachments").getString(), winX + PAD, rowsY + 4, VanillaUIHelper.TEXT_WHITE, false);
        }
        for (int i = 0; i < node.getImages().size(); i++) {
            int y = rowsY + i * ROW_H;
            g.drawString(font, font.plainSubstrByWidth(node.getImages().get(i).file(), 144),
                    winX + PAD, y + 4, VanillaUIHelper.TEXT_AQUA, false);
            if (isOver(mouseX, mouseY, winX + PAD, y, 144, 16)) {
                previewName = node.getImages().get(i).file();
            }
            boolean removeHover = isOver(mouseX, mouseY, winX + winW - PAD - 18, y, 18, 16);
            VanillaUIHelper.drawButton(g, winX + winW - PAD - 18, y, 18, 16, removeHover);
            g.drawCenteredString(font, "×", winX + winW - PAD - 9, y + 4,
                    removeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_RED);
        }

        drawMini(g, Component.translatable("wh_npcs.ui.node_images.tab_all").getString(), tabAllX(), controlsY, 36, mouseX, mouseY,
                !favTab ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean favHover = isOver(mouseX, mouseY, tabFavX(), controlsY, 30, 16);
        VanillaUIHelper.drawButton(g, tabFavX(), controlsY, 30, 16, favHover || favTab);
        drawHeart(g, tabFavX() + 11, controlsY + 5, favTab ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY);
        String sortLabel = switch (sortMode) {
            case 1 -> Component.translatable("wh_npcs.ui.node_images.sort_za").getString();
            case 2 -> Component.translatable("wh_npcs.ui.node_images.sort_new").getString();
            default -> Component.translatable("wh_npcs.ui.node_images.sort_az").getString();
        };
        drawMini(g, sortLabel, sortBtnX(), controlsY, 36, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        if (isOver(mouseX, mouseY, sortBtnX(), controlsY, 36, 16)) {
            hoverTooltip = Component.translatable("wh_npcs.ui.node_images.sort_tooltip").getString();
        }
        if (isLocalWorld()) {
            drawMini(g, Component.translatable("wh_npcs.ui.node_images.folder").getString(), folderBtnX(), controlsY, 64, mouseX, mouseY, VanillaUIHelper.TEXT_GRAY);
            if (isOver(mouseX, mouseY, folderBtnX(), controlsY, 64, 16)) {
                hoverTooltip = Component.translatable("wh_npcs.ui.node_images.folder_tooltip").getString();
            }
        }

        renderFileList(g, mouseX, mouseY);

        drawMini(g, Component.translatable("wh_npcs.ui.portrait.upload").getString(), winX + PAD, bottomY, 120, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawMini(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawPreview(g, mouseX, mouseY);
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    private void drawPreview(GuiGraphics g, int mouseX, int mouseY) {
        if (previewName == null) {
            return;
        }
        ClientImageCache.Entry e = ClientImageCache.getInstance().get(previewName);
        if (e.state() != ClientImageCache.State.READY || e.location() == null
                || e.width() <= 0 || e.height() <= 0) {
            return;
        }
        int max = 132;
        float ar = (float) e.width() / e.height();
        int w = max;
        int h = Math.round(w / ar);
        if (h > max) {
            h = max;
            w = Math.round(h * ar);
        }
        int pad = 3;
        int bx = mouseX + 14;
        int by = mouseY - h - 8;
        if (bx + w + pad * 2 > width) {
            bx = mouseX - w - pad * 2 - 10;
        }
        if (bx < 0) {
            bx = 0;
        }
        if (by < 0) {
            by = mouseY + 14;
        }
        if (by + h + pad * 2 > height) {
            by = height - h - pad * 2;
        }
        VanillaUIHelper.drawWindow(g, bx, by, w + pad * 2, h + pad * 2);
        g.blit(e.location(), bx + pad, by + pad, w, h, 0f, 0f,
                e.width(), e.height(), e.width(), e.height());
    }

    private int tabAllX() {
        return winX + PAD + 156;
    }

    private int tabFavX() {
        return tabAllX() + 40;
    }

    private int sortBtnX() {
        return tabFavX() + 34;
    }

    private int folderBtnX() {
        return winX + winW - PAD - 64;
    }

    private boolean isLocalWorld() {
        return minecraft != null && minecraft.hasSingleplayerServer();
    }

    private void pickAndUpload() {
        com.withouthonor.npcs.client.ClientLocalFiles.browsePng(bytes -> {
            if (bytes.length > ImageStore.MAX_BYTES) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("wh_npcs.msg.image.too_big"), false);
                }
            } else {
                com.withouthonor.npcs.client.ClientLocalFiles.uploadImage(bytes, false);
            }
        });
    }

    private void openImagesFolder() {
        if (minecraft == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        java.nio.file.Path dir = minecraft.getSingleplayerServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("wh_npcs").resolve("images");
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException ignored) {

        }
        net.minecraft.Util.getPlatform().openFile(dir.toFile());
    }

    private void renderFileList(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, winX + PAD, filesY, winW - PAD * 2, filesH);
        if (serverFiles == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.node_images.loading").getString(), winX + winW / 2, filesY + filesH / 2 - 4,
                    VanillaUIHelper.TEXT_STATUS);
            return;
        }
        List<ImageStore.ImageInfo> files = displayed();
        if (files.isEmpty()) {
            g.drawCenteredString(font, favTab
                            ? Component.translatable("wh_npcs.ui.node_images.empty_favorites").getString()
                            : Component.translatable("wh_npcs.ui.node_images.empty_search").getString(),
                    winX + winW / 2, filesY + filesH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        ClientPrefs prefs = ClientPrefs.get();
        int visible = (filesH - 8) / FILE_ROW_H;
        fileScroll = Math.max(0, Math.min(fileScroll, Math.max(0, files.size() - visible)));
        int y = filesY + 4;
        for (int i = fileScroll; i < Math.min(files.size(), fileScroll + visible); i++) {
            ImageStore.ImageInfo info = files.get(i);
            boolean pinned = prefs.isPinnedImage(info.name());
            boolean favorite = prefs.isFavoriteImage(info.name());
            boolean hovered = isOver(mouseX, mouseY, winX + PAD + 2, y, winW - PAD * 2 - 4, FILE_ROW_H);
            if (hovered) {
                g.fill(winX + PAD + 2, y, winX + winW - PAD - 2, y + FILE_ROW_H, VanillaUIHelper.BG_HOVERED);
                previewName = info.name();
            }
            if (renamingFile != null && renamingFile.equals(info.name())) {
                if (renameBox != null) {
                    renameBox.setY(y);
                }
                y += FILE_ROW_H;
                continue;
            }
            int nameX = winX + PAD + 6;
            if (pinned) {
                drawPin(g, nameX, y + 2, VanillaUIHelper.TEXT_GOLD);
                nameX += 11;
            }
            g.drawString(font, font.plainSubstrByWidth(info.name(), winW - PAD * 2 - 110 - (pinned ? 11 : 0)),
                    nameX, y + 2, hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            boolean showDel = hovered && canDeleteImages();
            if (showDel) {
                boolean rh = isOver(mouseX, mouseY, renImgX(), y + 1, 10, 10);
                drawPencil(g, renImgX(), y + 1, rh ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY);
                boolean dh = isOver(mouseX, mouseY, delImgX(), y + 1, 10, 10);
                g.drawString(font, "✕", delImgX(), y + 2, dh ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
            } else {
                String size = Component.translatable("wh_npcs.ui.node_images.size_kb", info.sizeKb()).getString();
                g.drawString(font, size, heartX() - 8 - font.width(size), y + 2,
                        VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            if (hovered || favorite || pinned) {
                boolean heartHover = isOver(mouseX, mouseY, heartX(), y + 1, 10, 10);
                drawHeart(g, heartX(), y + 2, favorite ? 0xFFFF5555
                        : (heartHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
                boolean pinHover = isOver(mouseX, mouseY, pinX(), y + 1, 10, 10);
                drawPin(g, pinX(), y + 2, pinned ? VanillaUIHelper.TEXT_GOLD
                        : (pinHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
            }
            y += FILE_ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, winX + winW - PAD - 6, filesY + 3, filesH - 6,
                files.size(), visible, fileScroll, scrollbars, v -> fileScroll = v);
    }

    private boolean canDeleteImages() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private int delImgX() {
        return winX + winW - PAD - 50;
    }

    private int renImgX() {
        return winX + winW - PAD - 64;
    }

    private void startRename(String name, int rowY) {
        renamingFile = name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        renameBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD + 6, rowY, 150, FILE_ROW_H,
                Component.translatable("wh_npcs.ui.node_images.rename_hint")));
        renameBox.setMaxLength(48);
        renameBox.setValue(base);
        renameBox.setHint(Component.translatable("wh_npcs.ui.node_images.rename_hint"));
        setFocused(renameBox);
    }

    private void commitRename() {
        if (renamingFile != null && renameBox != null) {
            String nw = renameBox.getValue().trim();
            int dot = renamingFile.lastIndexOf('.');
            String oldBase = dot > 0 ? renamingFile.substring(0, dot) : renamingFile;
            if (!nw.isEmpty() && !nw.equals(oldBase)) {
                NetworkHandler.sendToServer(
                        new com.withouthonor.npcs.network.RenameImagePacket(renamingFile, nw, false));
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

    private static void drawPencil(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 6, y, x + 9, y + 2, color);
        g.fill(x + 4, y + 2, x + 7, y + 4, color);
        g.fill(x + 2, y + 4, x + 5, y + 6, color);
        g.fill(x, y + 6, x + 3, y + 9, color);
    }

    private int heartX() {
        return winX + winW - PAD - 34;
    }

    private int pinX() {
        return winX + winW - PAD - 20;
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

    private void drawMini(GuiGraphics g, String label, int x, int y, int w,
                          int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 16);
        VanillaUIHelper.drawButton(g, x, y, w, 16, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 4,
                hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            recalc();
            if (renamingFile != null) {
                if (renameBox != null && isOver(mouseX, mouseY,
                        renameBox.getX(), renameBox.getY(), renameBox.getWidth(), renameBox.getHeight())) {
                    return superMouseClicked(mouseX, mouseY, button);
                }
                commitRename();
                return true;
            }
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }
            for (int i = 0; i < node.getImages().size(); i++) {
                if (isOver(mouseX, mouseY, winX + winW - PAD - 18, rowsY + i * ROW_H, 18, 16)) {
                    node.getImages().remove(i);
                    init(minecraft, width, height);
                    return true;
                }
            }
            if (isOver(mouseX, mouseY, tabAllX(), controlsY, 36, 16)) {
                favTab = false;
                fileScroll = 0;
                return true;
            }
            if (isOver(mouseX, mouseY, tabFavX(), controlsY, 30, 16)) {
                favTab = true;
                fileScroll = 0;
                return true;
            }
            if (isOver(mouseX, mouseY, sortBtnX(), controlsY, 36, 16)) {
                sortMode = (sortMode + 1) % 3;
                return true;
            }
            if (isLocalWorld() && isOver(mouseX, mouseY, folderBtnX(), controlsY, 64, 16)) {
                openImagesFolder();
                return true;
            }
            if (serverFiles != null) {
                List<ImageStore.ImageInfo> files = displayed();
                int visible = (filesH - 8) / FILE_ROW_H;
                int y = filesY + 4;
                for (int i = fileScroll; i < Math.min(files.size(), fileScroll + visible); i++) {
                    ImageStore.ImageInfo info = files.get(i);
                    if (canDeleteImages() && isOver(mouseX, mouseY, renImgX(), y + 1, 10, 10)) {
                        startRename(info.name(), y);
                        return true;
                    }
                    if (canDeleteImages() && isOver(mouseX, mouseY, delImgX(), y + 1, 10, 10)) {
                        NetworkHandler.sendToServer(
                                new com.withouthonor.npcs.network.DeleteImagePacket(info.name(), false));
                        return true;
                    }
                    if (isOver(mouseX, mouseY, heartX(), y + 1, 10, 10)) {
                        ClientPrefs.get().toggleFavoriteImage(info.name());
                        return true;
                    }
                    if (isOver(mouseX, mouseY, pinX(), y + 1, 10, 10)) {
                        ClientPrefs.get().togglePinnedImage(info.name());
                        return true;
                    }
                    if (isOver(mouseX, mouseY, winX + PAD + 2, y, winW - PAD * 2 - 4, FILE_ROW_H)) {
                        boolean exists = node.getImages().stream().anyMatch(r -> r.file().equals(info.name()));
                        if (!exists && node.getImages().size() < DialogueNode.MAX_IMAGES) {
                            node.getImages().add(new DialogueNode.ImageRef(info.name(), ""));
                            init(minecraft, width, height);
                        }
                        return true;
                    }
                    y += FILE_ROW_H;
                }
            }
            if (isOver(mouseX, mouseY, winX + PAD, bottomY, 120, 16)) {
                pickAndUpload();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, winX + PAD, filesY, winW - PAD * 2, filesH)) {
            fileScroll -= (int) Math.signum(delta) * 2;
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
        }
        return super.keyPressed(key, scancode, mods);
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
