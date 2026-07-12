package com.withouthonor.npcs.client.gui.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.client.cache.ClientSkinCache;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.registry.ModEntities;
import com.withouthonor.npcs.common.skin.DefaultSkins;
import com.withouthonor.npcs.common.skin.UrlSkinRegistry;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.SkinLibraryPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class SkinLibraryScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int TAB_H = 18;
    private static final int LIST_W = 250;
    private static final int ROW_H = 20;
    private static final String[] TAB_KEYS = {
            "wh_npcs.ui.skin_lib.tab.defaults",
            "wh_npcs.ui.skin_lib.tab.files",
            "wh_npcs.ui.skin_lib.tab.nick",
            "wh_npcs.ui.skin_lib.tab.url",
            "wh_npcs.ui.skin_lib.tab.nearby"
    };

    private static String[] tabLabels() {
        String[] labels = new String[6];
        for (int i = 0; i < TAB_KEYS.length; i++) {
            labels[i] = Component.translatable(TAB_KEYS[i]).getString();
        }
        labels[5] = "♥";
        return labels;
    }

    /** Обрезка по ширине с «…», если не влезает (длинные переводы). */
    private String ellipsize(String s, int maxW) {
        if (font.width(s) <= maxW) {
            return s;
        }
        return font.plainSubstrByWidth(s, Math.max(0, maxW - font.width("…"))) + "…";
    }

    private static final class TabLayout {
        int x, w;
        String label, full;
        boolean clipped;
    }

    /** Раскладка ряда вкладок: не заезжает за панель списка, длинные метки режутся «…» (для CJK/DE). */
    private java.util.List<TabLayout> layoutTabs() {
        java.util.List<TabLayout> out = new java.util.ArrayList<>();
        String[] tabs = tabLabels();
        int availRight = listX + LIST_W;
        // Резервируем место под последнюю вкладку (♥) у правого края, чтобы её не выдавило текстовыми.
        int lastNatural = font.width(tabs[tabs.length - 1]) + 14;
        int tx = winX + 4;
        for (int idx = 0; idx < tabs.length; idx++) {
            String full = tabs[idx];
            boolean last = idx == tabs.length - 1;
            int limitRight = last ? availRight : availRight - lastNatural - 2;
            int natural = font.width(full) + 14;
            int tw = Math.min(natural, Math.max(20, limitRight - tx));
            TabLayout tl = new TabLayout();
            tl.x = tx;
            tl.w = tw;
            tl.full = full;
            tl.clipped = tw < natural;
            tl.label = tl.clipped ? ellipsize(full, tw - 14) : full;
            out.add(tl);
            tx += tw + 2;
        }
        return out;
    }

    private static int lastTab;

    private final Screen parent;
    private final Consumer<String> onPicked;

    private int tab = lastTab;

    @Nullable
    private String selected;
    private int scroll;
    private final ScrollDrag scrollbars = new ScrollDrag();

    @Nullable
    private List<SkinLibraryPackets.FileEntry> files;
    @Nullable
    private List<UrlSkinRegistry.UrlSkin> urls;

    private EditBox nickBox;
    private EditBox urlBox;

    @Nullable
    private String renamingUrl;
    @Nullable
    private EditBox renameBox;

    private static final int CONFIRM_W = 240;
    private static final int CONFIRM_H = 86;
    @Nullable
    private String confirmDeleteValue;
    private boolean confirmDeleteIsFile;
    private String confirmDeleteLabel = "";

    @Nullable
    private CompanionEntity previewNpc;
    private float previewAngle = 200.0F;
    private long lastFrameMs = System.currentTimeMillis();
    private boolean rotDragging;
    private boolean paused;
    private int frozenTick;

    @Nullable
    private String hoverTooltip;

    private int winX, winY, winW, winH;
    private int listX, listY, listH;
    private int prevX, prevY, prevW, prevH;
    private int bottomY;

    public SkinLibraryScreen(Screen parent, @Nullable String current, Consumer<String> onPicked) {
        super(Component.translatable("wh_npcs.ui.skin_lib.title"));
        this.parent = parent;
        this.onPicked = onPicked;
        this.selected = current == null || current.isBlank() ? null : current;
        NetworkHandler.sendToServer(new SkinLibraryPackets.Request());
    }

    public static void acceptLibrary(List<SkinLibraryPackets.FileEntry> files,
                                     List<UrlSkinRegistry.UrlSkin> urls) {
        if (Minecraft.getInstance().screen instanceof SkinLibraryScreen screen) {
            screen.files = files;
            screen.urls = urls;
        }
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        renameBox = null;
        renamingUrl = null;
        if (tab == 2) {
            String old = nickBox != null ? nickBox.getValue() : "";
            nickBox = addRenderableWidget(new SelectableEditBox(font, listX + 4, listY + 16, LIST_W - 96, 16,
                    Component.translatable("wh_npcs.ui.skin_lib.nick")));
            nickBox.setMaxLength(16);
            nickBox.setValue(old);
            nickBox.setHint(Component.translatable("wh_npcs.ui.skin_lib.nick.hint"));
        } else if (tab == 3) {
            String old = urlBox != null ? urlBox.getValue() : "";
            urlBox = addRenderableWidget(new SelectableEditBox(font, listX + 4, listY + 16, LIST_W - 8, 16,
                    Component.translatable("wh_npcs.ui.skin_lib.url")));
            urlBox.setMaxLength(250);
            urlBox.setValue(old);
            urlBox.setHint(Component.translatable("wh_npcs.ui.skin_lib.url.hint"));
        }
    }

    @Override
    protected int designW() {
        return 470;
    }

    @Override
    protected int designH() {
        return 300;
    }

    private void recalc() {
        winW = 470;
        winH = 300;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        bottomY = winY + winH - PAD - 20;
        listX = winX + PAD;
        listY = winY + HEADER_H + TAB_H + 6;
        listH = bottomY - 6 - listY;
        prevX = listX + LIST_W + PAD;
        prevY = winY + HEADER_H + 4;
        prevW = winX + winW - PAD - prevX;
        prevH = bottomY - 6 - prevY;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.skin_lib.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        java.util.List<TabLayout> tls = layoutTabs();
        String tabBarTip = null;
        for (int i = 0; i < tls.size(); i++) {
            TabLayout tl = tls.get(i);
            boolean hovered = isOver(mouseX, mouseY, tl.x, winY + HEADER_H, tl.w, TAB_H);
            VanillaUIHelper.drawTab(g, tl.x, winY + HEADER_H, tl.w, TAB_H, tab == i, hovered);
            g.drawString(font, tl.label, tl.x + 7, winY + HEADER_H + 5,
                    tab == i ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (hovered && tl.clipped) {
                tabBarTip = tl.full;
            }
        }

        VanillaUIHelper.drawContentPanel(g, listX, listY, LIST_W, listH);
        hoverTooltip = switch (tab) {
            case 0 -> renderDefaultsTab(g, mouseX, mouseY);
            case 1 -> renderFilesTab(g, mouseX, mouseY);
            case 2 -> renderNickTab(g, mouseX, mouseY);
            case 3 -> renderUrlTab(g, mouseX, mouseY);
            case 4 -> renderNearbyTab(g, mouseX, mouseY);
            default -> renderFavoritesTab(g, mouseX, mouseY);
        };
        if (tabBarTip != null) {
            hoverTooltip = tabBarTip; // тултип обрезанной вкладки важнее (курсор над вкладкой, не над списком)
        }

        renderPreview(g, mouseX, mouseY);

        drawBtn(g, Component.translatable("wh_npcs.ui.skin_lib.select").getString(), winX + winW - PAD - 232, bottomY, 70, mouseX, mouseY,
                selected != null ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_DARK_GRAY);
        drawBtn(g, Component.translatable("wh_npcs.ui.skin_lib.no_skin").getString(), winX + winW - PAD - 156, bottomY, 76, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), winX + winW - PAD - 74, bottomY, 74, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (confirmDeleteValue != null) {
            renderConfirmDelete(g, mouseX, mouseY);
            return;
        }
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    private int confirmX() {
        return winX + (winW - CONFIRM_W) / 2;
    }

    private int confirmY() {
        return winY + (winH - CONFIRM_H) / 2;
    }

    private void renderConfirmDelete(GuiGraphics g, int mouseX, int mouseY) {

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        int x = confirmX();
        int y = confirmY();
        VanillaUIHelper.drawWindow(g, x, y, CONFIRM_W, CONFIRM_H);
        g.drawCenteredString(font, Component.translatable(confirmDeleteIsFile
                        ? "wh_npcs.ui.skin_lib.confirm_del.file" : "wh_npcs.ui.skin_lib.confirm_del.url").getString(),
                x + CONFIRM_W / 2, y + 10, VanillaUIHelper.TEXT_YELLOW);
        g.drawCenteredString(font, "§b" + font.plainSubstrByWidth(confirmDeleteLabel, CONFIRM_W - 20),
                x + CONFIRM_W / 2, y + 26, VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.confirm_del.irreversible").getString(), x + CONFIRM_W / 2, y + 40, VanillaUIHelper.TEXT_WHITE);
        boolean delHover = isOver(mouseX, mouseY, x + CONFIRM_W / 2 - 86, y + CONFIRM_H - 28, 80, 18);
        boolean cancelHover = isOver(mouseX, mouseY, x + CONFIRM_W / 2 + 6, y + CONFIRM_H - 28, 80, 18);
        VanillaUIHelper.drawButton(g, x + CONFIRM_W / 2 - 86, y + CONFIRM_H - 28, 80, 18, delHover);
        VanillaUIHelper.drawButton(g, x + CONFIRM_W / 2 + 6, y + CONFIRM_H - 28, 80, 18, cancelHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.delete").getString(), x + CONFIRM_W / 2 - 46, y + CONFIRM_H - 23,
                delHover ? VanillaUIHelper.TEXT_YELLOW : 0xFFFF5555);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.cancel").getString(), x + CONFIRM_W / 2 + 46, y + CONFIRM_H - 23,
                cancelHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        g.pose().popPose();
    }

    private boolean canDeleteFiles() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(3);
    }

    private boolean canDeleteUrl(UrlSkinRegistry.UrlSkin url) {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        return minecraft.player.hasPermissions(3)
                || url.addedBy().equalsIgnoreCase(minecraft.player.getGameProfile().getName());
    }

    private boolean skinRow(GuiGraphics g, int mouseX, int mouseY, int y, String spec,
                            String label, @Nullable String rightInfo, boolean withFavorite) {
        boolean isSelected = spec.equals(selected);
        boolean hovered = isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H);
        if (isSelected || hovered) {
            g.fill(listX + 2, y, listX + LIST_W - 2, y + ROW_H,
                    isSelected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
        }
        drawSkinHead(g, spec, listX + 6, y + 2, 16);
        g.drawString(font, font.plainSubstrByWidth(label, LIST_W - 70), listX + 28, y + 6,
                isSelected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
        if (rightInfo != null) {
            g.drawString(font, rightInfo, listX + LIST_W - 24 - font.width(rightInfo), y + 6,
                    VanillaUIHelper.TEXT_DARK_GRAY, false);
        }
        if (withFavorite && (hovered || ClientPrefs.get().isFavoriteSkin(spec))) {
            boolean favorite = ClientPrefs.get().isFavoriteSkin(spec);
            boolean heartHover = isOver(mouseX, mouseY, listX + LIST_W - 18, y + 6, 10, 9);
            drawHeart(g, listX + LIST_W - 18, y + 6, favorite ? 0xFFFF5555
                    : (heartHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
        }
        return hovered;
    }

    @Nullable
    private String renderDefaultsTab(GuiGraphics g, int mouseX, int mouseY) {
        List<DefaultSkins.DefaultSkin> all = DefaultSkins.ALL;
        int visible = (listH - 8) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, all.size() - visible)));
        int y = listY + 4;
        for (int i = scroll; i < Math.min(all.size(), scroll + visible); i++) {
            DefaultSkins.DefaultSkin skin = all.get(i);
            skinRow(g, mouseX, mouseY, y, skin.spec(),
                    skin.displayName(), skin.slim() ? "slim" : "wide", true);
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + LIST_W - 6, listY + 3, listH - 6,
                all.size(), visible, scroll, scrollbars, v -> scroll = v);
        return null;
    }

    private static String filesPathLabel() {
        return Component.translatable("wh_npcs.ui.skin_lib.files.path").getString();
    }

    private boolean isLocalWorld() {
        return minecraft != null && minecraft.hasSingleplayerServer();
    }

    @Nullable
    private String renderFilesTab(GuiGraphics g, int mouseX, int mouseY) {
        String tooltip = null;
        if (isLocalWorld()) {
            String filesPathLabel = filesPathLabel();
            boolean pathHover = isOver(mouseX, mouseY, listX + 4, listY + 3, font.width(filesPathLabel), 10);
            g.drawString(font, font.plainSubstrByWidth(filesPathLabel, LIST_W - 8), listX + 4, listY + 4,
                    pathHover ? VanillaUIHelper.TEXT_AQUA : VanillaUIHelper.TEXT_DARK_GRAY, false);
            if (pathHover) {
                tooltip = Component.translatable("wh_npcs.ui.skin_lib.files.path.tip").getString();
            }
        } else {
            boolean upHover = isOver(mouseX, mouseY, listX + 4, listY + 1, 116, 13);
            VanillaUIHelper.drawButton(g, listX + 4, listY + 1, 116, 13, upHover);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.files.upload").getString(),
                    listX + 4 + 58, listY + 4, upHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            if (upHover) {
                tooltip = Component.translatable("wh_npcs.ui.skin_lib.files.upload.tip").getString();
            }
        }
        if (files == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.loading").getString(), listX + LIST_W / 2, listY + listH / 2, VanillaUIHelper.TEXT_STATUS);
            return tooltip;
        }
        if (files.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.files.empty").getString(), listX + LIST_W / 2, listY + listH / 2, VanillaUIHelper.TEXT_WHITE);
            return tooltip;
        }
        List<SkinLibraryPackets.FileEntry> sorted = sortedFiles();
        int visible = (listH - 18) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, sorted.size() - visible)));
        int y = listY + 16;
        for (int i = scroll; i < Math.min(sorted.size(), scroll + visible); i++) {
            SkinLibraryPackets.FileEntry file = sorted.get(i);
            boolean rowHover = isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H);
            boolean showDelete = rowHover && canDeleteFiles();

            skinRow(g, mouseX, mouseY, y, file.name(), file.name(),
                    showDelete ? null : Component.translatable("wh_npcs.ui.skin_lib.files.size_kb", file.sizeKb()).getString(), true);
            if (showDelete) {
                boolean delHover = isOver(mouseX, mouseY, listX + LIST_W - 34, y + 5, 10, 10);
                g.drawString(font, "✕", listX + LIST_W - 34, y + 6,
                        delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
                if (delHover) {
                    tooltip = Component.translatable("wh_npcs.ui.skin_lib.files.del.tip").getString();
                }
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + LIST_W - 6, listY + 16, listH - 20,
                sorted.size(), visible, scroll, scrollbars, v -> scroll = v);
        return tooltip;
    }

    private void openSkinsFolder() {
        if (minecraft == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        java.nio.file.Path dir = minecraft.getSingleplayerServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("wh_npcs").resolve("skins");
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException ignored) {

        }
        net.minecraft.Util.getPlatform().openFile(dir.toFile());
    }

    private List<SkinLibraryPackets.FileEntry> sortedFiles() {
        List<SkinLibraryPackets.FileEntry> favorite = new ArrayList<>();
        List<SkinLibraryPackets.FileEntry> rest = new ArrayList<>();
        for (SkinLibraryPackets.FileEntry file : files) {
            (ClientPrefs.get().isFavoriteSkin(file.name()) ? favorite : rest).add(file);
        }
        favorite.addAll(rest);
        return favorite;
    }

    @Nullable
    private String renderNickTab(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, Component.translatable("wh_npcs.ui.skin_lib.nick.header").getString(), listX + 4, listY + 5,
                VanillaUIHelper.TEXT_GRAY, false);
        drawBtn(g, Component.translatable("wh_npcs.ui.skin_lib.nick.show").getString(), listX + LIST_W - 88, listY + 15, 84, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawBtn(g, Component.translatable("wh_npcs.ui.skin_lib.nick.mine").getString(), listX + 4, listY + 40, 84, mouseX, mouseY, VanillaUIHelper.TEXT_GOLD);
        g.drawString(font, Component.translatable("wh_npcs.ui.skin_lib.nick.note1").getString(), listX + 4, listY + 66, VanillaUIHelper.TEXT_WHITE, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.skin_lib.nick.note2").getString(), listX + 4, listY + 78, VanillaUIHelper.TEXT_WHITE, false);
        return null;
    }

    @Nullable
    private String renderUrlTab(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, Component.translatable("wh_npcs.ui.skin_lib.url.header").getString(), listX + 4, listY + 5, VanillaUIHelper.TEXT_GRAY, false);
        drawBtn(g, Component.translatable("wh_npcs.ui.skin_lib.url.add").getString(), listX + 4, listY + 36, 124, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        drawBtn(g, Component.translatable("wh_npcs.ui.skin_lib.url.preview").getString(), listX + 132, listY + 36, 58, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        String tooltip = null;
        if (urls == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.loading").getString(), listX + LIST_W / 2, listY + listH / 2, VanillaUIHelper.TEXT_STATUS);
            return tooltip;
        }
        int top = listY + 60;
        int visible = (listH - 64) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, urls.size() - visible)));
        int y = top;
        for (int i = scroll; i < Math.min(urls.size(), scroll + visible); i++) {
            UrlSkinRegistry.UrlSkin url = urls.get(i);
            boolean renaming = url.url().equals(renamingUrl);
            String label = renaming ? "" : (url.name().isEmpty()
                    ? url.url().replaceFirst("https?://", "") : url.name());
            boolean hovered = skinRow(g, mouseX, mouseY, y, url.url(), label, null, !renaming);
            if (renaming && renameBox != null) {
                renameBox.setY(y + 2);
            }

            if (hovered && !renaming && canDeleteUrl(url)) {
                boolean delHover = isOver(mouseX, mouseY, listX + LIST_W - 76, y + 5, 10, 10);
                g.drawString(font, "✕", listX + LIST_W - 76, y + 6,
                        delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
                if (delHover) {
                    tooltip = Component.translatable("wh_npcs.ui.skin_lib.url.del.tip").getString();
                }
            }
            boolean editHover = isOver(mouseX, mouseY, listX + LIST_W - 62, y + 5, 10, 10);
            if (hovered || renaming || editHover) {
                VanillaUIHelper.drawRenameIcon(g, font, listX + LIST_W - 62, y + 6,
                        renaming ? VanillaUIHelper.TEXT_GOLD
                                : (editHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY));
                if (editHover) {
                    tooltip = Component.translatable(renaming ? "wh_npcs.ui.skin_lib.url.rename.editing"
                            : "wh_npcs.ui.skin_lib.url.rename.tip").getString();
                }
            }
            if (!url.addedBy().isEmpty()) {
                drawAuthorHead(g, url.addedBy(), listX + LIST_W - 48, y + 6);
                if (isOver(mouseX, mouseY, listX + LIST_W - 48, y + 6, 8, 8)) {
                    tooltip = Component.translatable("wh_npcs.ui.skin_lib.url.added_by", url.addedBy()).getString();
                }
            }
            if (hovered && !renaming) {
                boolean copyHover = isOver(mouseX, mouseY, listX + LIST_W - 34, y + 5, 10, 10);
                drawCopyIcon(g, listX + LIST_W - 34, y + 6,
                        copyHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY);
                if (copyHover) {
                    tooltip = Component.translatable("wh_npcs.ui.skin_lib.url.copy").getString();
                } else if (tooltip == null) {
                    tooltip = "§7" + url.url();
                }
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + LIST_W - 6, top, listH - 64,
                urls.size(), visible, scroll, scrollbars, v -> scroll = v);
        return tooltip;
    }

    @Nullable
    private String renderNearbyTab(GuiGraphics g, int mouseX, int mouseY) {
        List<CompanionEntity> nearby = nearbyNpcs();
        if (nearby.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.nearby.empty").getString(),
                    listX + LIST_W / 2, listY + listH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return null;
        }
        int visible = (listH - 8) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, nearby.size() - visible)));
        int y = listY + 4;
        for (int i = scroll; i < Math.min(nearby.size(), scroll + visible); i++) {
            CompanionEntity npc = nearby.get(i);
            String name = net.minecraft.ChatFormatting.stripFormatting(npc.getName().getString());
            int dist = minecraft != null && minecraft.player != null
                    ? (int) minecraft.player.distanceTo(npc) : 0;
            skinRow(g, mouseX, mouseY, y, npc.getSkinName(),
                    name != null ? name : Component.translatable("wh_npcs.ui.skin_lib.nearby.npc").getString(),
                    Component.translatable("wh_npcs.ui.skin_lib.nearby.dist", dist).getString(), true);
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + LIST_W - 6, listY + 3, listH - 6,
                nearby.size(), visible, scroll, scrollbars, v -> scroll = v);
        return null;
    }

    private List<CompanionEntity> nearbyNpcs() {
        List<CompanionEntity> result = new ArrayList<>();
        if (minecraft != null && minecraft.level != null) {
            for (var entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof CompanionEntity npc && !npc.getSkinName().isEmpty()) {
                    result.add(npc);
                }
            }
            result.sort(java.util.Comparator.comparingDouble(npc ->
                    minecraft.player != null ? minecraft.player.distanceToSqr(npc) : 0));
        }
        return result;
    }

    @Nullable
    private String renderFavoritesTab(GuiGraphics g, int mouseX, int mouseY) {
        List<String> favorites = favoritesList();
        if (favorites.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.fav.empty1").getString(),
                    listX + LIST_W / 2, listY + listH / 2 - 10, VanillaUIHelper.TEXT_WHITE);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.fav.empty2").getString(),
                    listX + LIST_W / 2, listY + listH / 2 + 2, VanillaUIHelper.TEXT_WHITE);
            return null;
        }
        int visible = (listH - 8) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, favorites.size() - visible)));
        int y = listY + 4;
        for (int i = scroll; i < Math.min(favorites.size(), scroll + visible); i++) {
            String spec = favorites.get(i);
            skinRow(g, mouseX, mouseY, y, spec, displayLabel(spec), null, true);
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + LIST_W - 6, listY + 3, listH - 6,
                favorites.size(), visible, scroll, scrollbars, v -> scroll = v);
        return null;
    }

    private String displayLabel(String spec) {
        DefaultSkins.DefaultSkin defaultSkin = DefaultSkins.bySpec(spec);
        if (defaultSkin != null) {
            return defaultSkin.displayName();
        }
        if (urls != null) {
            for (UrlSkinRegistry.UrlSkin url : urls) {
                if (url.url().equals(spec) && !url.name().isEmpty()) {
                    return url.name();
                }
            }
        }
        return spec.replaceFirst("https?://", "");
    }


    private void startRename(UrlSkinRegistry.UrlSkin url, int rowY) {
        cancelRename();
        renamingUrl = url.url();
        renameBox = addRenderableWidget(new SelectableEditBox(font, listX + 24, rowY + 2, LIST_W - 94, 14,
                Component.translatable("wh_npcs.ui.skin_lib.url.rename.name")));
        renameBox.setMaxLength(48);
        renameBox.setValue(url.name());
        renameBox.setHint(Component.translatable("wh_npcs.ui.skin_lib.url.rename.name.hint"));
        setFocused(renameBox);
    }

    private void commitRename() {
        if (renamingUrl != null && renameBox != null) {
            NetworkHandler.sendToServer(new SkinLibraryPackets.Rename(renamingUrl, renameBox.getValue().trim()));
        }
        cancelRename();
    }

    private void cancelRename() {
        if (renameBox != null) {
            removeWidget(renameBox);
        }
        renameBox = null;
        renamingUrl = null;
    }

    private static void drawCopyIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 6, y + 1, c);
        g.fill(x, y, x + 1, y + 6, c);
        g.fill(x + 2, y + 2, x + 8, y + 3, c);
        g.fill(x + 2, y + 7, x + 8, y + 8, c);
        g.fill(x + 2, y + 2, x + 3, y + 8, c);
        g.fill(x + 7, y + 2, x + 8, y + 8, c);
    }

    private void renderPreview(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, prevX, prevY, prevW, prevH);
        if (selected == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.skin_lib.preview.default").getString(), prevX + prevW / 2, prevY + prevH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        CompanionEntity npc = previewEntity();
        if (npc == null) {
            return;
        }
        npc.setSkinName(selected);
        long now = System.currentTimeMillis();
        if (!rotDragging && !paused) {
            previewAngle = (previewAngle + (now - lastFrameMs) * 0.04F) % 360.0F;
        }
        lastFrameMs = now;
        if (minecraft != null && minecraft.level != null) {
            if (!paused) {
                frozenTick = (int) minecraft.level.getGameTime();
            }
            npc.tickCount = frozenTick;
        }
        float scale = (prevH - 44.0F) / 2.0F;
        ScaledScreen.enableScissor(g, prevX + 2, prevY + 2, prevX + prevW - 2, prevY + prevH - 2);
        renderRotating(g, npc, prevX + prevW / 2, prevY + prevH / 2 + (int) (scale * 0.95F), scale, previewAngle);
        g.disableScissor();
        var entry = ClientSkinCache.getInstance().get(selected.toLowerCase(Locale.ROOT));
        if (entry == null) {
            g.drawString(font, Component.translatable("wh_npcs.ui.skin_lib.preview.loading").getString(), prevX + 6, prevY + prevH - 12, VanillaUIHelper.TEXT_WHITE, false);
        }

        boolean pauseHover = isOver(mouseX, mouseY, pauseBtnX(), pauseBtnY(), 16, 16);
        VanillaUIHelper.drawButton(g, pauseBtnX(), pauseBtnY(), 16, 16, pauseHover);
        int ic = pauseHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY;
        if (paused) {
            g.fill(pauseBtnX() + 6, pauseBtnY() + 4, pauseBtnX() + 7, pauseBtnY() + 12, ic);
            g.fill(pauseBtnX() + 7, pauseBtnY() + 5, pauseBtnX() + 9, pauseBtnY() + 11, ic);
            g.fill(pauseBtnX() + 9, pauseBtnY() + 6, pauseBtnX() + 11, pauseBtnY() + 10, ic);
        } else {
            g.fill(pauseBtnX() + 5, pauseBtnY() + 4, pauseBtnX() + 7, pauseBtnY() + 12, ic);
            g.fill(pauseBtnX() + 9, pauseBtnY() + 4, pauseBtnX() + 11, pauseBtnY() + 12, ic);
        }

        String modelLabel = Component.translatable("wh_npcs.ui.skin_lib.preview.model", switch (modelOverride()) {
            case "slim" -> "Slim";
            case "wide" -> "Wide";
            default -> Component.translatable("wh_npcs.ui.skin_lib.preview.model.auto").getString();
        }).getString();
        boolean modelHover = isOver(mouseX, mouseY, prevX + 4, prevY + prevH - 22, 96, 18);
        VanillaUIHelper.drawButton(g, prevX + 4, prevY + prevH - 22, 96, 18, modelHover);
        g.drawCenteredString(font, modelLabel, prevX + 52, prevY + prevH - 17,
                modelHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
    }

    private int pauseBtnX() {
        return prevX + prevW - 20;
    }

    private int pauseBtnY() {
        return prevY + 4;
    }

    private String modelOverride() {
        if (selected == null) {
            return "";
        }
        String lower = selected.toLowerCase(Locale.ROOT);
        if (lower.endsWith("#slim")) {
            return "slim";
        }
        return lower.endsWith("#wide") ? "wide" : "";
    }

    private void cycleModelOverride() {
        if (selected == null) {
            return;
        }
        String base = selected;
        String lower = base.toLowerCase(Locale.ROOT);
        if (lower.endsWith("#slim") || lower.endsWith("#wide")) {
            base = base.substring(0, base.length() - 5);
        }
        selected = switch (modelOverride()) {
            case "" -> base + "#slim";
            case "slim" -> base + "#wide";
            default -> base;
        };
    }

    @Nullable
    private CompanionEntity previewEntity() {
        if (previewNpc == null && minecraft != null && minecraft.level != null) {
            previewNpc = ModEntities.COMPANION.get().create(minecraft.level);
        }
        return previewNpc;
    }

    private void renderRotating(GuiGraphics g, CompanionEntity entity, int x, int y, float scale, float angleDeg) {
        entity.yBodyRot = angleDeg;
        entity.yBodyRotO = angleDeg;
        entity.yHeadRot = angleDeg;
        entity.yHeadRotO = angleDeg;
        entity.setYRot(angleDeg);
        entity.yRotO = angleDeg;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 100);
        pose.scale(scale, scale, -scale);
        pose.mulPose(new Quaternionf().rotateZ((float) Math.PI));
        pose.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-12)));
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        try {
            dispatcher.render(entity, 0, 0, 0, 0, 1.0F, pose, buffer, 0xF000F0);
        } catch (Exception ignored) {
        }
        buffer.endBatch();
        dispatcher.setRenderShadow(true);
        pose.popPose();
    }

    private void drawSkinHead(GuiGraphics g, String spec, int x, int y, int size) {
        ClientSkinCache.Skin skin = ClientSkinCache.getInstance().get(spec.toLowerCase(Locale.ROOT));
        if (skin != null) {
            g.blit(skin.location(), x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(skin.location(), x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + size, y + size, 0xFF3A3A3A);
            VanillaUIHelper.drawInsetFrame(g, x, y, size, size);
        }
    }

    private void drawAuthorHead(GuiGraphics g, String author, int x, int y) {
        ClientSkinCache.Skin skin = ClientSkinCache.getInstance().get(author.toLowerCase(Locale.ROOT));
        if (skin != null) {
            g.blit(skin.location(), x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(skin.location(), x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + 8, y + 8, 0xFF6E5037);
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

    private void multilineTooltip(GuiGraphics g, String text, int x, int y) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, x, y);
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w,
                         int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, font.plainSubstrByWidth(label, w - 6), x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private long lastRowClickMs;
    @Nullable
    private String lastRowClickSpec;

    private boolean rowClick(String spec) {
        long now = System.currentTimeMillis();
        if (spec.equals(lastRowClickSpec) && now - lastRowClickMs < 350) {
            selected = spec;
            pick();
        } else {
            selected = spec;
        }
        lastRowClickSpec = spec;
        lastRowClickMs = now;
        return true;
    }

    private void pick() {
        onPicked.accept(selected);
        lastTab = tab;
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmDeleteValue != null && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            confirmDeleteValue = null;
            return true;
        }
        if (renameBox != null && renameBox.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                commitRename();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                cancelRename();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {

        if (confirmDeleteValue != null) {
            if (button == 0) {
                recalc();
                if (isOver(mouseX, mouseY, confirmX() + CONFIRM_W / 2 - 86, confirmY() + CONFIRM_H - 28, 80, 18)) {
                    NetworkHandler.sendToServer(new SkinLibraryPackets.Delete(confirmDeleteIsFile, confirmDeleteValue));
                    if (selected != null && stripModel(selected).equalsIgnoreCase(confirmDeleteValue)) {
                        selected = null;
                    }
                    confirmDeleteValue = null;
                } else if (isOver(mouseX, mouseY, confirmX() + CONFIRM_W / 2 + 6, confirmY() + CONFIRM_H - 28, 80, 18)) {
                    confirmDeleteValue = null;
                }
            }
            return true;
        }
        if (button == 0) {
            recalc();
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }

            if (renameBox != null && !renameBox.isMouseOver(mouseX, mouseY)) {
                commitRename();
            }

            java.util.List<TabLayout> tls = layoutTabs();
            for (int i = 0; i < tls.size(); i++) {
                TabLayout tl = tls.get(i);
                if (isOver(mouseX, mouseY, tl.x, winY + HEADER_H, tl.w, TAB_H)) {
                    tab = i;
                    scroll = 0;
                    init(minecraft, width, height);
                    return true;
                }
            }

            if (isOver(mouseX, mouseY, winX + winW - PAD - 232, bottomY, 70, 18) && selected != null) {
                pick();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 156, bottomY, 76, 18)) {
                onPicked.accept(null);
                lastTab = tab;
                onClose();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 74, bottomY, 74, 18)) {
                onClose();
                return true;
            }

            if (selected != null && isOver(mouseX, mouseY, pauseBtnX(), pauseBtnY(), 16, 16)) {
                paused = !paused;
                return true;
            }
            if (selected != null && isOver(mouseX, mouseY, prevX + 4, prevY + prevH - 22, 96, 18)) {
                cycleModelOverride();
                return true;
            }

            if (isOver(mouseX, mouseY, prevX, prevY, prevW, prevH)) {
                rotDragging = true;
                return true;
            }
            if (handleListClick(mouseX, mouseY)) {
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private boolean handleListClick(double mouseX, double mouseY) {
        if (tab == 0) {
            List<DefaultSkins.DefaultSkin> all = DefaultSkins.ALL;
            int visible = (listH - 8) / ROW_H;
            int y = listY + 4;
            for (int i = scroll; i < Math.min(all.size(), scroll + visible); i++) {
                DefaultSkins.DefaultSkin skin = all.get(i);
                if (isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H)) {
                    if (isOver(mouseX, mouseY, listX + LIST_W - 18, y + 6, 10, 9)) {
                        ClientPrefs.get().toggleFavoriteSkin(skin.spec());
                        return true;
                    }
                    return rowClick(skin.spec());
                }
                y += ROW_H;
            }
        } else if (tab == 1) {
            if (isLocalWorld()
                    && isOver(mouseX, mouseY, listX + 4, listY + 3, font.width(filesPathLabel()), 10)) {
                openSkinsFolder();
                return true;
            }
            if (!isLocalWorld() && isOver(mouseX, mouseY, listX + 4, listY + 1, 116, 13)) {
                com.withouthonor.npcs.client.ClientLocalFiles.browsePngNamed((nm, bytes) ->
                        com.withouthonor.npcs.client.ClientLocalFiles.uploadSkin(nm, bytes));
                return true;
            }
            if (files == null) {
                return false;
            }
            List<SkinLibraryPackets.FileEntry> sorted = sortedFiles();
            int visible = (listH - 18) / ROW_H;
            int y = listY + 16;
            for (int i = scroll; i < Math.min(sorted.size(), scroll + visible); i++) {
                if (isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H)) {
                    String spec = sorted.get(i).name();
                    if (isOver(mouseX, mouseY, listX + LIST_W - 18, y + 6, 10, 9)) {
                        ClientPrefs.get().toggleFavoriteSkin(spec);
                        return true;
                    }
                    if (canDeleteFiles() && isOver(mouseX, mouseY, listX + LIST_W - 34, y + 5, 10, 10)) {
                        confirmDeleteValue = spec;
                        confirmDeleteIsFile = true;
                        confirmDeleteLabel = spec;
                        return true;
                    }
                    return rowClick(spec);
                }
                y += ROW_H;
            }
        } else if (tab == 2) {
            if (isOver(mouseX, mouseY, listX + LIST_W - 88, listY + 15, 84, 18)) {
                String nick = nickBox.getValue().trim();
                if (!nick.isEmpty()) {
                    selected = nick;
                }
                return true;
            }
            if (isOver(mouseX, mouseY, listX + 4, listY + 40, 84, 18)) {
                if (minecraft != null && minecraft.player != null) {
                    String self = minecraft.player.getGameProfile().getName();
                    nickBox.setValue(self);
                    selected = self;
                }
                return true;
            }
        } else if (tab == 3) {
            if (isOver(mouseX, mouseY, listX + 4, listY + 36, 124, 18)) {
                String url = urlBox.getValue().trim();
                if (!url.isEmpty()) {
                    NetworkHandler.sendToServer(new SkinLibraryPackets.AddUrl(url));
                }
                return true;
            }
            if (isOver(mouseX, mouseY, listX + 132, listY + 36, 58, 18)) {
                String url = urlBox.getValue().trim();
                if (!url.isEmpty()) {
                    selected = url;
                }
                return true;
            }
            if (urls != null) {
                int top = listY + 60;
                int visible = (listH - 64) / ROW_H;
                int y = top;
                for (int i = scroll; i < Math.min(urls.size(), scroll + visible); i++) {
                    if (isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H)) {
                        UrlSkinRegistry.UrlSkin url = urls.get(i);
                        if (canDeleteUrl(url) && !url.url().equals(renamingUrl)
                                && isOver(mouseX, mouseY, listX + LIST_W - 76, y + 5, 10, 10)) {
                            confirmDeleteValue = url.url();
                            confirmDeleteIsFile = false;
                            confirmDeleteLabel = displayLabel(url.url());
                            return true;
                        }
                        if (isOver(mouseX, mouseY, listX + LIST_W - 62, y + 5, 10, 10)) {
                            startRename(url, y);
                            return true;
                        }
                        if (url.url().equals(renamingUrl)) {
                            return true;
                        }
                        if (isOver(mouseX, mouseY, listX + LIST_W - 18, y + 6, 10, 9)) {
                            ClientPrefs.get().toggleFavoriteSkin(url.url());
                            return true;
                        }
                        if (isOver(mouseX, mouseY, listX + LIST_W - 34, y + 5, 10, 10) && minecraft != null) {
                            minecraft.keyboardHandler.setClipboard(url.url());
                            return true;
                        }
                        return rowClick(url.url());
                    }
                    y += ROW_H;
                }
            }
        } else if (tab == 4) {
            List<CompanionEntity> nearby = nearbyNpcs();
            int visible = (listH - 8) / ROW_H;
            int y = listY + 4;
            for (int i = scroll; i < Math.min(nearby.size(), scroll + visible); i++) {
                if (isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H)) {
                    String spec = nearby.get(i).getSkinName();
                    if (isOver(mouseX, mouseY, listX + LIST_W - 18, y + 6, 10, 9)) {
                        ClientPrefs.get().toggleFavoriteSkin(spec);
                        return true;
                    }
                    return rowClick(spec);
                }
                y += ROW_H;
            }
        } else if (tab == 5) {
            List<String> favorites = favoritesList();
            int visible = (listH - 8) / ROW_H;
            int y = listY + 4;
            for (int i = scroll; i < Math.min(favorites.size(), scroll + visible); i++) {
                if (isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H)) {
                    String spec = favorites.get(i);
                    if (isOver(mouseX, mouseY, listX + LIST_W - 18, y + 6, 10, 9)) {
                        ClientPrefs.get().toggleFavoriteSkin(spec);
                        return true;
                    }
                    return rowClick(spec);
                }
                y += ROW_H;
            }
        }
        return false;
    }

    private static String stripModel(String spec) {
        String lower = spec.toLowerCase(Locale.ROOT);
        return lower.endsWith("#slim") || lower.endsWith("#wide")
                ? spec.substring(0, spec.length() - 5) : spec;
    }

    private List<String> favoritesList() {
        java.util.LinkedHashSet<String> favorites = new java.util.LinkedHashSet<>();
        for (DefaultSkins.DefaultSkin skin : DefaultSkins.ALL) {
            if (ClientPrefs.get().isFavoriteSkin(skin.spec())) {
                favorites.add(skin.spec());
            }
        }
        if (files != null) {
            files.forEach(f -> {
                if (ClientPrefs.get().isFavoriteSkin(f.name())) {
                    favorites.add(f.name());
                }
            });
        }
        if (urls != null) {
            urls.forEach(u -> {
                if (ClientPrefs.get().isFavoriteSkin(u.url())) {
                    favorites.add(u.url());
                }
            });
        }
        favorites.addAll(ClientPrefs.get().favoriteSkinSet());
        return new ArrayList<>(favorites);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        if (rotDragging && button == 0) {
            previewAngle = (previewAngle + (float) dragX * 1.5F) % 360.0F;
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        if (rotDragging && button == 0) {
            rotDragging = false;
            return true;
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (renamingUrl != null || confirmDeleteValue != null) {
            return true;
        }
        if (isOver(mouseX, mouseY, listX, listY, LIST_W, listH)) {
            scroll -= (int) Math.signum(delta);
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        lastTab = tab;
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
