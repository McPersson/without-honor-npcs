package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.cache.ClientImageCache;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
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
import java.util.List;
import java.util.Locale;

public class PortraitPickerScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int FILE_ROW_H = 12;
    private static final int LIST_W = 196;

    private final Screen parent;
    private final PortraitTarget target;
    private final String titleText;

    @Nullable
    private List<ImageStore.ImageInfo> serverFiles;
    private int fileScroll;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private EditBox searchBox;

    private int winX, winY, winW, winH;
    private int searchY, filesY, filesH;
    private int previewX, previewY, previewW, previewH;
    private int toggleY, clearY, bottomY;

    public PortraitPickerScreen(Screen parent, PortraitTarget target, String titleText) {
        super(Component.literal(titleText));
        this.parent = parent;
        this.target = target;
        this.titleText = titleText;
        NetworkHandler.sendToServer(new RequestImageListPacket(true));
    }

    public PortraitPickerScreen(Screen parent, JsonObject json) {
        this(parent, new PortraitTarget() {
            @Override
            public String image() {
                return json.has("portrait_image") ? json.get("portrait_image").getAsString() : "";
            }

            @Override
            public void setImage(String name) {
                if (name == null || name.isEmpty()) {
                    json.remove("portrait_image");
                } else {
                    json.addProperty("portrait_image", name);
                }
            }

            @Override
            public boolean show() {
                return json.has("portrait_show") && json.get("portrait_show").getAsBoolean();
            }

            @Override
            public void setShow(boolean show) {
                if (show) {
                    json.addProperty("portrait_show", true);
                } else {
                    json.remove("portrait_show");
                }
            }
        }, Component.translatable("wh_npcs.ui.portrait.title").getString());
    }

    private static String display(String name) {
        return name.startsWith(ImageStore.AVATAR_PREFIX)
                ? name.substring(ImageStore.AVATAR_PREFIX.length()) : name;
    }

    public static void acceptServerList(List<ImageStore.ImageInfo> images) {
        if (Minecraft.getInstance().screen instanceof PortraitPickerScreen screen) {
            screen.serverFiles = images;
        }
    }

    private String portrait() {
        return target.image();
    }

    private boolean show() {
        return target.show();
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, searchY, LIST_W, 16,
                Component.translatable("wh_npcs.ui.portrait.search")));
        searchBox.setMaxLength(64);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.portrait.search_hint"));
        searchBox.setResponder(v -> fileScroll = 0);
    }

    @Override
    protected int designW() {
        return 440;
    }

    @Override
    protected int designH() {
        return 300;
    }

    private void recalc() {
        winW = 440;
        winH = 300;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        bottomY = winY + winH - PAD - 20;
        searchY = winY + HEADER_H + 8;
        filesY = searchY + 20;
        filesH = bottomY - 8 - filesY;

        previewX = winX + PAD + LIST_W + 12;
        previewY = searchY;
        clearY = bottomY - 22;

        previewH = (clearY - previewY) - 54;
        previewW = Math.round(previewH * 9f / 16f);
        int colW = winX + winW - PAD - previewX;
        if (previewW > colW) {
            previewW = colW;
            previewH = Math.round(previewW * 16f / 9f);
        }

        int previewBottom = previewY + previewH;
        toggleY = previewBottom + (clearY - previewBottom - 22) / 2;
    }

    private List<ImageStore.ImageInfo> displayed() {
        List<ImageStore.ImageInfo> result = new ArrayList<>();
        if (serverFiles == null) {
            return result;
        }
        String query = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        for (ImageStore.ImageInfo info : serverFiles) {
            if (query.isEmpty() || info.name().toLowerCase(Locale.ROOT).contains(query)) {
                result.add(info);
            }
        }
        result.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return result;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, titleText, winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        renderFileList(g, mouseX, mouseY);
        renderPreview(g);

        boolean toggleHover = isOver(mouseX, mouseY, previewX, toggleY, 12, 12);
        VanillaUIHelper.drawButton(g, previewX, toggleY, 12, 12, toggleHover);
        if (show()) {
            VanillaUIHelper.drawCheck(g, previewX + 1, toggleY + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.portrait.show_photo").getString(), previewX + 16, toggleY + 2, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.portrait.instead_3d").getString(), previewX + 16, toggleY + 12, VanillaUIHelper.TEXT_WHITE, false);

        boolean clearHover = isOver(mouseX, mouseY, previewX, clearY, 90, 16) && !portrait().isEmpty();
        VanillaUIHelper.drawButton(g, previewX, clearY, 90, 16, clearHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.portrait.clear_photo").getString(), previewX + 45, clearY + 4,
                portrait().isEmpty() ? VanillaUIHelper.TEXT_DARK_GRAY
                        : (clearHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_RED));

        if (isLocalWorld()) {
            boolean folderHover = isOver(mouseX, mouseY, winX + PAD, bottomY, 90, 18);
            VanillaUIHelper.drawButton(g, winX + PAD, bottomY, 90, 18, folderHover);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.portrait.folder").getString(), winX + PAD + 45, bottomY + 5,
                    folderHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY);
        }

        boolean upHover = isOver(mouseX, mouseY, uploadBtnX(), bottomY, 120, 18);
        VanillaUIHelper.drawButton(g, uploadBtnX(), bottomY, 120, 18, upHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.portrait.upload").getString(), uploadBtnX() + 60, bottomY + 5,
                upHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY);
    }

    private boolean isLocalWorld() {
        return minecraft != null && minecraft.hasSingleplayerServer();
    }

    private boolean canDeleteImages() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private int delPicX() {
        return winX + PAD + LIST_W - 16;
    }

    private int uploadBtnX() {
        return winX + PAD + (isLocalWorld() ? 96 : 0);
    }

    private void pickAndUpload() {
        com.withouthonor.npcs.client.ClientLocalFiles.browsePng(bytes -> {
            if (bytes.length > ImageStore.MAX_BYTES) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("wh_npcs.msg.image.too_big"), false);
                }
            } else {
                com.withouthonor.npcs.client.ClientLocalFiles.uploadImage(bytes, true);
            }
        });
    }

    private void openAvatarsFolder() {
        if (minecraft == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        java.nio.file.Path dir = minecraft.getSingleplayerServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("wh_npcs").resolve("images").resolve("avatars");
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException ignored) {

        }
        net.minecraft.Util.getPlatform().openFile(dir.toFile());
    }

    private void renderFileList(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, winX + PAD, filesY, LIST_W, filesH);
        if (serverFiles == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.portrait.loading").getString(), winX + PAD + LIST_W / 2, filesY + filesH / 2 - 4,
                    VanillaUIHelper.TEXT_STATUS);
            return;
        }
        List<ImageStore.ImageInfo> files = displayed();
        if (files.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.portrait.no_png").getString(), winX + PAD + LIST_W / 2,
                    filesY + filesH / 2 - 8, VanillaUIHelper.TEXT_WHITE);
            if (isLocalWorld()) {
                g.drawCenteredString(font, Component.translatable("wh_npcs.ui.portrait.no_png_hint").getString(), winX + PAD + LIST_W / 2,
                        filesY + filesH / 2 + 4, VanillaUIHelper.TEXT_WHITE);
            }
            return;
        }
        String cur = portrait();
        int visible = (filesH - 8) / FILE_ROW_H;
        fileScroll = Math.max(0, Math.min(fileScroll, Math.max(0, files.size() - visible)));
        int y = filesY + 4;
        for (int i = fileScroll; i < Math.min(files.size(), fileScroll + visible); i++) {
            ImageStore.ImageInfo info = files.get(i);
            boolean selected = info.name().equals(cur);
            boolean hovered = isOver(mouseX, mouseY, winX + PAD + 2, y, LIST_W - 4, FILE_ROW_H);
            if (selected || hovered) {
                g.fill(winX + PAD + 2, y, winX + PAD + LIST_W - 2, y + FILE_ROW_H,
                        selected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            g.drawString(font, font.plainSubstrByWidth(display(info.name()), LIST_W - 50), winX + PAD + 6, y + 2,
                    selected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (hovered && canDeleteImages()) {
                boolean dh = isOver(mouseX, mouseY, delPicX(), y + 1, 10, 10);
                g.drawString(font, "✕", delPicX(), y + 2, dh ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
            } else {
                String size = Component.translatable("wh_npcs.ui.portrait.kb", info.sizeKb()).getString();
                g.drawString(font, size, winX + PAD + LIST_W - 10 - font.width(size), y + 2,
                        VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            y += FILE_ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, winX + PAD + LIST_W - 6, filesY + 3, filesH - 6,
                files.size(), visible, fileScroll, scrollbars, v -> fileScroll = v);
    }

    private void renderPreview(GuiGraphics g) {
        VanillaUIHelper.drawContentPanel(g, previewX, previewY, previewW, previewH);
        String cur = portrait();
        if (cur.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.portrait.none").getString(), previewX + previewW / 2, previewY + previewH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        ClientImageCache.Entry entry = ClientImageCache.getInstance().get(cur);
        if (entry.state() == ClientImageCache.State.READY && entry.location() != null
                && entry.width() > 0 && entry.height() > 0) {
            int availW = previewW - 4;
            int availH = previewH - 4;
            float ar = (float) entry.width() / entry.height();
            int dw = availW;
            int dh = Math.round(dw / ar);
            if (dh > availH) {
                dh = availH;
                dw = Math.round(dh * ar);
            }
            int ix = previewX + (previewW - dw) / 2;
            int iy = previewY + (previewH - dh) / 2;
            g.blit(entry.location(), ix, iy, dw, dh, 0f, 0f,
                    entry.width(), entry.height(), entry.width(), entry.height());
        } else {
            g.drawCenteredString(font, entry.state() == ClientImageCache.State.FAILED ? Component.translatable("wh_npcs.ui.portrait.not_found").getString() : Component.translatable("wh_npcs.ui.portrait.loading_short").getString(),
                    previewX + previewW / 2, previewY + previewH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            recalc();
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }

            if (isOver(mouseX, mouseY, previewX, toggleY, 12, 12)) {
                target.setShow(!show());
                return true;
            }

            if (!portrait().isEmpty() && isOver(mouseX, mouseY, previewX, clearY, 90, 16)) {
                target.setImage("");
                return true;
            }

            if (serverFiles != null) {
                List<ImageStore.ImageInfo> files = displayed();
                int visible = (filesH - 8) / FILE_ROW_H;
                int y = filesY + 4;
                for (int i = fileScroll; i < Math.min(files.size(), fileScroll + visible); i++) {
                    if (canDeleteImages() && isOver(mouseX, mouseY, delPicX(), y + 1, 10, 10)) {
                        NetworkHandler.sendToServer(
                                new com.withouthonor.npcs.network.DeleteImagePacket(files.get(i).name(), true));
                        return true;
                    }
                    if (isOver(mouseX, mouseY, winX + PAD + 2, y, LIST_W - 4, FILE_ROW_H)) {
                        target.setImage(files.get(i).name());
                        return true;
                    }
                    y += FILE_ROW_H;
                }
            }
            if (isLocalWorld() && isOver(mouseX, mouseY, winX + PAD, bottomY, 90, 18)) {
                openAvatarsFolder();
                return true;
            }
            if (isOver(mouseX, mouseY, uploadBtnX(), bottomY, 120, 18)) {
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
        if (isOver(mouseX, mouseY, winX + PAD, filesY, LIST_W, filesH)) {
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

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
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
