package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiConsumer;

public class VoicePickerScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 12;

    private static final String[][] CATEGORIES = {
            {"Ambient", "ambient"}, {"Block", "block"}, {"Entity", "entity"}, {"Item", "item"},
            {"Music", "music"}, {"Particle", "particle"}, {"UI", "ui"}, {"Weather", "weather"}};
    private static final String OTHER = "\0other";

    private final Screen parent;
    private final BiConsumer<String, Float> onPicked;

    private final List<ResourceLocation> all = new ArrayList<>();
    private final List<String> namespaces = new ArrayList<>();
    private final List<ResourceLocation> filtered = new ArrayList<>();
    @Nullable
    private ResourceLocation selected;
    @Nullable
    private String namespaceFilter;
    @Nullable
    private String categoryFilter;
    private boolean favOnly;
    private boolean sortDesc;
    private int scroll;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private EditBox searchBox;
    private EditBox pitchBox;
    private final float initialPitch;
    @Nullable
    private net.minecraft.client.resources.sounds.SimpleSoundInstance currentPreview;

    private int winX, winY, winW, winH;
    private int listX, listW, listY, listH;
    private int bottomY;
    @Nullable
    private String hoverTooltip;

    public VoicePickerScreen(Screen parent, @Nullable String current, float pitch,
                             BiConsumer<String, Float> onPicked) {
        super(Component.translatable("wh_npcs.ui.voice.title"));
        this.parent = parent;
        this.initialPitch = pitch <= 0 ? 1.0F : pitch;
        this.onPicked = onPicked;
        TreeSet<String> ns = new TreeSet<>();
        java.util.Set<ResourceLocation> seen = new java.util.HashSet<>();
        for (ResourceLocation key : ForgeRegistries.SOUND_EVENTS.getKeys()) {
            if (seen.add(key)) {
                all.add(key);
                ns.add(key.getNamespace());
            }
        }

        var sm = net.minecraft.client.Minecraft.getInstance().getSoundManager();
        if (sm != null) {
            for (ResourceLocation key : sm.getAvailableSounds()) {
                if (seen.add(key)) {
                    all.add(key);
                    ns.add(key.getNamespace());
                }
            }
        }
        all.sort(Comparator.comparing(ResourceLocation::toString));
        if (ns.remove("minecraft")) {
            namespaces.add("minecraft");
        }
        namespaces.addAll(ns);
        if (current != null && !current.isBlank()) {
            selected = ResourceLocation.tryParse(current);
        }
        rebuild();
    }

    private static String categoryOf(ResourceLocation key) {
        String path = key.getPath();
        int cut = path.length();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '.' || c == '_') {
                cut = i;
                break;
            }
        }
        return path.substring(0, cut);
    }

    private boolean matchesCategory(ResourceLocation key) {
        if (categoryFilter == null) {
            return true;
        }
        String cat = categoryOf(key);
        if (categoryFilter.equals(OTHER)) {
            for (String[] c : CATEGORIES) {
                if (c[1].equals(cat)) {
                    return false;
                }
            }
            return true;
        }
        return categoryFilter.equals(cat);
    }

    private void rebuild() {
        ClientPrefs prefs = ClientPrefs.get();
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        filtered.clear();
        for (ResourceLocation key : all) {
            if (namespaceFilter != null && !key.getNamespace().equals(namespaceFilter)) {
                continue;
            }
            if (!matchesCategory(key)) {
                continue;
            }
            if (favOnly && !prefs.isFavoriteVoice(key.toString())) {
                continue;
            }
            if (!q.isEmpty() && !key.toString().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            filtered.add(key);
        }
        Comparator<ResourceLocation> byId = Comparator.comparing(ResourceLocation::toString);
        Comparator<ResourceLocation> cmp = sortDesc ? byId.reversed() : byId;
        filtered.sort((a, b) -> {
            boolean pa = prefs.isPinnedVoice(a.toString());
            boolean pb = prefs.isPinnedVoice(b.toString());
            return pa != pb ? (pa ? -1 : 1) : cmp.compare(a, b);
        });
        scroll = 0;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        String oldSearch = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, listX, winY + HEADER_H + 4, listW, 16,
                Component.translatable("wh_npcs.ui.voice.search")));
        searchBox.setMaxLength(64);
        searchBox.setValue(oldSearch);
        searchBox.setHint(Component.translatable("wh_npcs.ui.voice.search_hint"));
        searchBox.setResponder(v -> rebuild());
        String oldPitch = pitchBox != null ? pitchBox.getValue()
                : (initialPitch == (int) initialPitch ? String.valueOf((int) initialPitch) : String.valueOf(initialPitch));
        pitchBox = addRenderableWidget(new SelectableEditBox(font, listX + listW - 36, winY + HEADER_H + 24, 34, 16,
                Component.translatable("wh_npcs.ui.voice.pitch")));
        pitchBox.setMaxLength(4);
        pitchBox.setValue(oldPitch);
        pitchBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.voice.pitch_tip")));
        setFocused(searchBox);
    }

    @Override
    protected int designW() {
        return 470;
    }

    @Override
    protected int designH() {
        return 312;
    }

    private void recalc() {
        winW = 470;
        winH = 312;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        bottomY = winY + winH - PAD - 20;
        listX = winX + PAD;
        listW = winW - 2 * PAD;
        listY = winY + HEADER_H + 44;
        listH = bottomY - 16 - listY;
    }

    private int toolbarY() {
        return winY + HEADER_H + 24;
    }

    private int nsBtnW() {
        return 112;
    }

    private int catBtnX() {
        return listX + nsBtnW() + 4;
    }

    private int favBtnX() {
        return catBtnX() + 104 + 4;
    }

    private int sortBtnX() {
        return favBtnX() + 17 + 4;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.voice.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        String count = filtered.size() + " / " + all.size();
        g.drawString(font, count, winX + winW - PAD - font.width(count), winY + 7,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        int ty = toolbarY();
        toolButton(g, listX, nsBtnW(), Component.translatable("wh_npcs.ui.voice.set", nsLabel()).getString(),
                namespaceFilter == null
                ? VanillaUIHelper.TEXT_GRAY : VanillaUIHelper.TEXT_AQUA, mouseX, mouseY);
        toolButton(g, catBtnX(), 104, Component.translatable("wh_npcs.ui.voice.type", catLabel()).getString(),
                categoryFilter == null
                ? VanillaUIHelper.TEXT_GRAY : VanillaUIHelper.TEXT_AQUA, mouseX, mouseY);
        boolean favHover = isOver(mouseX, mouseY, favBtnX(), ty, 17, 16);
        VanillaUIHelper.drawButton(g, favBtnX(), ty, 17, 16, favHover || favOnly);
        drawHeart(g, favBtnX() + 5, ty + 5, favOnly ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY);
        boolean sortHover = isOver(mouseX, mouseY, sortBtnX(), ty, 34, 16);
        VanillaUIHelper.drawButton(g, sortBtnX(), ty, 34, 16, sortHover);
        g.drawCenteredString(font, (sortDesc ? Component.translatable("wh_npcs.ui.voice.sort_desc")
                : Component.translatable("wh_npcs.ui.voice.sort_asc")).getString(), sortBtnX() + 17, ty + 4, VanillaUIHelper.TEXT_AQUA);
        g.drawString(font, Component.translatable("wh_npcs.ui.voice.pitch").getString(), listX + listW - 60, ty + 4, VanillaUIHelper.TEXT_DARK_GRAY, false);
        String tooltip = null;
        if (isOver(mouseX, mouseY, listX, ty, nsBtnW(), 16)) {
            tooltip = Component.translatable("wh_npcs.ui.voice.set_tip").getString();
        } else if (isOver(mouseX, mouseY, catBtnX(), ty, 104, 16)) {
            tooltip = Component.translatable("wh_npcs.ui.voice.type_tip").getString();
        } else if (favHover) {
            tooltip = Component.translatable("wh_npcs.ui.voice.fav_tip").getString();
        } else if (sortHover) {
            tooltip = Component.translatable("wh_npcs.ui.voice.sort_tip").getString();
        }

        renderList(g, mouseX, mouseY);

        drawBtn(g, Component.translatable("wh_npcs.ui.voice.stop").getString(), listX, bottomY, 90, mouseX, mouseY,
                currentPreview != null ? 0xFFFF6B6B : VanillaUIHelper.TEXT_DARK_GRAY);
        drawBtn(g, Component.translatable("wh_npcs.ui.voice.select").getString(), winX + winW - PAD - 232, bottomY, 70, mouseX, mouseY,
                selected != null ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_DARK_GRAY);
        drawBtn(g, Component.translatable("wh_npcs.ui.voice.no_voice").getString(), winX + winW - PAD - 156, bottomY, 90, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), winX + winW - PAD - 60, bottomY, 60, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);

        hoverTooltip = tooltip;
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, listX, listY, listW, listH);
        ClientPrefs prefs = ClientPrefs.get();
        int visibleRows = (listH - 8) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - visibleRows)));
        if (filtered.isEmpty()) {
            g.drawString(font, (favOnly ? Component.translatable("wh_npcs.ui.voice.empty_fav")
                    : Component.translatable("wh_npcs.ui.voice.empty")).getString(),
                    listX + 5, listY + 5, VanillaUIHelper.TEXT_WHITE, false);
        }
        int heartX = listX + listW - 16;
        int pinX = listX + listW - 30;
        int playX = listX + listW - 44;
        int labelX = listX + 5;
        int y = listY + 4;
        for (int i = scroll; i < Math.min(filtered.size(), scroll + visibleRows); i++) {
            ResourceLocation key = filtered.get(i);
            String id = key.toString();
            boolean isSelected = key.equals(selected);
            boolean hovered = isOver(mouseX, mouseY, listX + 2, y, listW - 4, ROW_H);
            boolean pinned = prefs.isPinnedVoice(id);
            boolean fav = prefs.isFavoriteVoice(id);
            if (isSelected || hovered) {
                g.fill(listX + 2, y, listX + listW - 2, y + ROW_H,
                        isSelected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }

            int nsW = font.width(key.getNamespace());
            String shownPath = font.plainSubstrByWidth(key.getPath(), (playX - 4) - labelX - nsW - 6);
            g.drawString(font, shownPath, labelX, y + 2,
                    isSelected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, "§8" + key.getNamespace(), labelX + font.width(shownPath) + 6, y + 2,
                    VanillaUIHelper.TEXT_WHITE, false);

            if (hovered) {
                drawPlay(g, playX, y + 3, isOver(mouseX, mouseY, playX, y + 1, 10, 9)
                        ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            }
            if (pinned || hovered) {
                drawPin(g, pinX, y + 2, pinned ? VanillaUIHelper.TEXT_GOLD
                        : (isOver(mouseX, mouseY, pinX, y + 1, 10, 9) ? VanillaUIHelper.TEXT_YELLOW
                        : VanillaUIHelper.TEXT_DARK_GRAY));
            }
            if (fav || hovered) {
                drawHeart(g, heartX, y + 3, fav ? 0xFFFF5555
                        : (isOver(mouseX, mouseY, heartX, y + 1, 10, 9) ? VanillaUIHelper.TEXT_YELLOW
                        : VanillaUIHelper.TEXT_DARK_GRAY));
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + listW - 6, listY + 3, listH - 6,
                filtered.size(), visibleRows, scroll, scrollbars, v -> scroll = v);
    }

    private void toolButton(GuiGraphics g, int x, int w, String label, int color, int mouseX, int mouseY) {
        boolean hover = isOver(mouseX, mouseY, x, toolbarY(), w, 16);
        VanillaUIHelper.drawButton(g, x, toolbarY(), w, 16, hover);
        g.drawString(font, font.plainSubstrByWidth(label, w - 8), x + 5, toolbarY() + 4,
                hover ? VanillaUIHelper.TEXT_YELLOW : color, false);
    }

    private String nsLabel() {
        if (namespaceFilter == null) {
            return Component.translatable("wh_npcs.ui.voice.all").getString();
        }
        return namespaceFilter.equals("minecraft")
                ? Component.translatable("wh_npcs.ui.voice.vanilla").getString() : namespaceFilter;
    }

    private String catLabel() {
        if (categoryFilter == null) {
            return Component.translatable("wh_npcs.ui.voice.all").getString();
        }
        if (categoryFilter.equals(OTHER)) {
            return Component.translatable("wh_npcs.ui.voice.other").getString();
        }
        for (String[] c : CATEGORIES) {
            if (c[1].equals(categoryFilter)) {
                return c[0];
            }
        }
        return categoryFilter;
    }

    private void openNamespacePicker() {
        if (minecraft == null) {
            return;
        }
        Map<String, Integer> counts = new HashMap<>();
        for (ResourceLocation key : all) {
            counts.merge(key.getNamespace(), 1, Integer::sum);
        }
        minecraft.setScreen(new NamespacePickerScreen(this, namespaces, counts, all.size(),
                namespaceFilter, ns -> {
            namespaceFilter = ns;
            rebuild();
        }));
    }

    private void cycleCategory() {
        List<String> opts = new ArrayList<>();
        opts.add(null);
        for (String[] c : CATEGORIES) {
            opts.add(c[1]);
        }
        opts.add(OTHER);
        categoryFilter = opts.get((opts.indexOf(categoryFilter) + 1) % opts.size());
        rebuild();
    }

    private float parsePitch() {
        try {
            return Math.max(0.5F, Math.min(2.0F, Float.parseFloat(pitchBox.getValue().trim().replace(',', '.'))));
        } catch (NumberFormatException e) {
            return 1.0F;
        }
    }

    private void playPreview(ResourceLocation id) {

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (sound == null) {
            sound = SoundEvent.createVariableRangeEvent(id);
        }
        if (minecraft != null) {
            stopPreview();
            currentPreview = SimpleSoundInstance.forUI(sound, parsePitch(), 0.9F);
            minecraft.getSoundManager().play(currentPreview);
        }
    }

    private void stopPreview() {
        if (currentPreview != null && minecraft != null) {
            minecraft.getSoundManager().stop(currentPreview);
            currentPreview = null;
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0) {
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }
            int ty = toolbarY();
            if (isOver(mouseX, mouseY, listX, ty, nsBtnW(), 16)) {
                openNamespacePicker();
                return true;
            }
            if (isOver(mouseX, mouseY, catBtnX(), ty, 104, 16)) {
                cycleCategory();
                return true;
            }
            if (isOver(mouseX, mouseY, favBtnX(), ty, 17, 16)) {
                favOnly = !favOnly;
                rebuild();
                return true;
            }
            if (isOver(mouseX, mouseY, sortBtnX(), ty, 34, 16)) {
                sortDesc = !sortDesc;
                rebuild();
                return true;
            }
            int visibleRows = (listH - 8) / ROW_H;
            int heartX = listX + listW - 16;
            int pinX = listX + listW - 30;
            int playX = listX + listW - 44;
            int y = listY + 4;
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visibleRows); i++) {
                if (isOver(mouseX, mouseY, listX + 2, y, listW - 4, ROW_H)) {
                    ResourceLocation key = filtered.get(i);
                    if (isOver(mouseX, mouseY, playX, y + 1, 10, 9)) {
                        playPreview(key);
                    } else if (isOver(mouseX, mouseY, heartX, y + 1, 10, 9)) {
                        ClientPrefs.get().toggleFavoriteVoice(key.toString());
                        rebuild();
                    } else if (isOver(mouseX, mouseY, pinX, y + 1, 10, 9)) {
                        ClientPrefs.get().togglePinnedVoice(key.toString());
                        rebuild();
                    } else {
                        selected = key;
                        playPreview(key);
                    }
                    return true;
                }
                y += ROW_H;
            }
            if (isOver(mouseX, mouseY, listX, bottomY, 90, 18)) {
                stopPreview();
                return true;
            }
            if (selected != null && isOver(mouseX, mouseY, winX + winW - PAD - 232, bottomY, 70, 18)) {
                onPicked.accept(selected.toString(), parsePitch());
                onClose();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 156, bottomY, 90, 18)) {
                onPicked.accept(null, parsePitch());
                onClose();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 60, bottomY, 60, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
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
        stopPreview();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
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

    private static void drawPlay(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 1, y + 7, c);
        g.fill(x + 1, y + 1, x + 3, y + 6, c);
        g.fill(x + 3, y + 2, x + 5, y + 5, c);
        g.fill(x + 5, y + 3, x + 6, y + 4, c);
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
