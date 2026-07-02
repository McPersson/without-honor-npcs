package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.action.Actions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class EffectPickerScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 20;
    private static final int BOX_H = 16;
    private static final int WIN_W = 340;
    private static final int WIN_H = 286;
    private static final int MAX_ROWS = 10;
    private static final int DUR_W = 42;
    private static final int AMP_W = 30;

    private record Entry(ResourceLocation id, MobEffect effect, String name, boolean vanilla) {
    }

    private final Screen parent;
    private final boolean applyMode;
    @Nullable
    private final List<ResourceLocation> ids;
    @Nullable
    private final List<Actions.EffectSpec> specs;
    private final Runnable onDone;
    private final List<Entry> all = new ArrayList<>();

    private EditBox searchBox;
    private final EditBox[] durBoxes = new EditBox[MAX_ROWS];
    private final EditBox[] ampBoxes = new EditBox[MAX_ROWS];
    private final ResourceLocation[] boundId = new ResourceLocation[MAX_ROWS];
    private boolean suppressResponder;

    private int tab;
    private int scroll;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private int winX, winY, winW, winH, listTop, bottomY;

    private EffectPickerScreen(Screen parent, boolean applyMode, @Nullable List<ResourceLocation> ids,
                              @Nullable List<Actions.EffectSpec> specs, Runnable onDone) {
        super(Component.translatable("wh_npcs.ui.effect_picker.title"));
        this.parent = parent;
        this.applyMode = applyMode;
        this.ids = ids;
        this.specs = specs;
        this.onDone = onDone;
    }

    public EffectPickerScreen(Screen parent, List<ResourceLocation> ids, Runnable onDone) {
        this(parent, false, ids, null, onDone);
    }

    public static EffectPickerScreen forApply(Screen parent, List<Actions.EffectSpec> specs, Runnable onDone) {
        return new EffectPickerScreen(parent, true, null, specs, onDone);
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
        listTop = winY + HEADER_H + 22;
        bottomY = winY + winH - PAD - 20;
    }

    private int durBoxX() {
        return winX + winW - PAD - 118;
    }

    private int ampBoxX() {
        return winX + winW - PAD - 48;
    }


    private boolean isSelected(ResourceLocation id) {
        return applyMode ? indexOf(id) >= 0 : ids.contains(id);
    }

    private int indexOf(ResourceLocation id) {
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void toggle(ResourceLocation id) {
        if (applyMode) {
            int idx = indexOf(id);
            if (idx >= 0) {
                specs.remove(idx);
            } else {
                specs.add(new Actions.EffectSpec(id, 600, 0));
            }
        } else if (!ids.remove(id)) {
            ids.add(id);
        }
    }

    private int count() {
        return applyMode ? specs.size() : ids.size();
    }

    private void clearAll() {
        if (applyMode) {
            specs.clear();
        } else {
            ids.clear();
        }
    }

    private void setDuration(ResourceLocation id, int seconds) {
        int idx = indexOf(id);
        if (idx >= 0) {
            Actions.EffectSpec s = specs.get(idx);
            int ticks = seconds <= 0 ? 0 : Math.min(1000000, seconds) * 20;
            specs.set(idx, new Actions.EffectSpec(id, ticks, s.amplifier()));
        }
    }

    private void setAmp(ResourceLocation id, int amp) {
        int idx = indexOf(id);
        if (idx >= 0) {
            Actions.EffectSpec s = specs.get(idx);
            specs.set(idx, new Actions.EffectSpec(id, s.durationTicks(), Math.max(0, Math.min(127, amp))));
        }
    }

    private void onDurEdited(int slot) {
        if (suppressResponder || boundId[slot] == null) {
            return;
        }
        String v = durBoxes[slot].getValue().trim();
        if (v.isEmpty()) {
            return;
        }
        try {
            setDuration(boundId[slot], Integer.parseInt(v));
        } catch (NumberFormatException ignored) {
        }
    }

    private void onAmpEdited(int slot) {
        if (suppressResponder || boundId[slot] == null) {
            return;
        }
        String v = ampBoxes[slot].getValue().trim();
        if (v.isEmpty()) {
            return;
        }
        try {
            setAmp(boundId[slot], Integer.parseInt(v) - 1);
        } catch (NumberFormatException ignored) {
        }
    }

    private void buildAll() {
        all.clear();
        for (MobEffect effect : ForgeRegistries.MOB_EFFECTS) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
            if (id == null) {
                continue;
            }
            all.add(new Entry(id, effect, effect.getDisplayName().getString(), id.getNamespace().equals("minecraft")));
        }
        all.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        if (all.isEmpty()) {
            buildAll();
        }
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, winY + HEADER_H + 2,
                winW - PAD * 2 - 140, 16, Component.translatable("wh_npcs.ui.effect_picker.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.effect_picker.search_hint"));
        searchBox.setResponder(v -> scroll = 0);

        if (applyMode) {
            for (int i = 0; i < MAX_ROWS; i++) {
                boundId[i] = null;
                final int slot = i;
                EditBox db = addRenderableWidget(new SelectableEditBox(font, 0, 0, DUR_W, BOX_H, Component.empty()));
                db.setMaxLength(6);
                db.setFilter(s -> s.matches("\\d*"));
                db.visible = false;
                db.setResponder(v -> onDurEdited(slot));
                durBoxes[i] = db;
                EditBox ab = addRenderableWidget(new SelectableEditBox(font, 0, 0, AMP_W, BOX_H, Component.empty()));
                ab.setMaxLength(2);
                ab.setFilter(s -> s.matches("\\d*"));
                ab.visible = false;
                ab.setResponder(v -> onAmpEdited(slot));
                ampBoxes[i] = ab;
            }
        }
    }

    private List<Entry> displayed() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) {
            if (tab == 1 && !e.vanilla()) {
                continue;
            }
            if (tab == 2 && e.vanilla()) {
                continue;
            }
            if (!q.isEmpty() && !e.name().toLowerCase(Locale.ROOT).contains(q)
                    && !e.id().toString().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(e);
        }
        return out;
    }

    private void layoutApplyBoxes(List<Entry> list) {
        for (int slot = 0; slot < MAX_ROWS; slot++) {
            int listIndex = scroll + slot;
            boolean show = listIndex < list.size() && isSelected(list.get(listIndex).id());
            EditBox db = durBoxes[slot];
            EditBox ab = ampBoxes[slot];
            if (!show) {
                db.visible = false;
                ab.visible = false;
                boundId[slot] = null;
                continue;
            }
            ResourceLocation id = list.get(listIndex).id();
            int y = listTop + 2 + slot * ROW_H;
            int boxY = y - 1 + (ROW_H - BOX_H) / 2;
            db.setX(durBoxX());
            db.setY(boxY);
            db.setWidth(DUR_W);
            db.visible = true;
            ab.setX(ampBoxX());
            ab.setY(boxY);
            ab.setWidth(AMP_W);
            ab.visible = true;
            if (!id.equals(boundId[slot])) {
                boundId[slot] = id;
                Actions.EffectSpec s = specs.get(indexOf(id));
                suppressResponder = true;
                db.setValue(String.valueOf(s.durationTicks() / 20));
                ab.setValue(String.valueOf(s.amplifier() + 1));
                suppressResponder = false;
            }
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.effect_picker.title").getString()
                        + "  §7(" + count() + ")",
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        String[] tabKeys = {"wh_npcs.ui.effect_picker.tab_all", "wh_npcs.ui.effect_picker.tab_vanilla",
                "wh_npcs.ui.effect_picker.tab_mods"};
        for (int i = 0; i < 3; i++) {
            int tx = winX + winW - PAD - 132 + i * 44;
            boolean hov = isOver(mouseX, mouseY, tx, winY + HEADER_H + 2, 42, 16);
            VanillaUIHelper.drawButton(g, tx, winY + HEADER_H + 2, 42, 16, hov || tab == i);
            g.drawCenteredString(font, Component.translatable(tabKeys[i]).getString(), tx + 21, winY + HEADER_H + 6,
                    tab == i ? VanillaUIHelper.TEXT_YELLOW : (hov ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA));
        }

        VanillaUIHelper.drawContentPanel(g, winX + PAD, listTop, winW - PAD * 2, MAX_ROWS * ROW_H + 6);
        List<Entry> list = displayed();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - MAX_ROWS)));
        if (applyMode) {
            layoutApplyBoxes(list);
        }
        int nameW = applyMode ? durBoxX() - (winX + PAD + 40) - 6 : winW - PAD * 2 - 44;
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            Entry e = list.get(i);
            int y = listTop + 2 + (i - scroll) * ROW_H;
            boolean on = isSelected(e.id());
            boolean hov = isOver(mouseX, mouseY, winX + PAD + 2, y - 1, winW - PAD * 2 - 8, ROW_H);
            if (on || hov) {
                g.fill(winX + PAD + 2, y - 1, winX + winW - PAD - 6, y - 1 + ROW_H,
                        on ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            VanillaUIHelper.drawButton(g, winX + PAD + 4, y + 3, 12, 12, false);
            if (on) {
                VanillaUIHelper.drawCheck(g, winX + PAD + 5, y + 4, VanillaUIHelper.TEXT_GREEN);
            }
            TextureAtlasSprite sprite = minecraft.getMobEffectTextures().get(e.effect());
            g.blit(winX + PAD + 20, y + 1, 0, 16, 16, sprite);
            g.drawString(font, font.plainSubstrByWidth(e.name(), nameW), winX + PAD + 40, y + 5,
                    on ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (applyMode && on) {
                boolean infinite = specs.get(indexOf(e.id())).durationTicks() <= 0;
                String durUnit = infinite
                        ? Component.translatable("wh_npcs.ui.effect_picker.unit_inf").getString()
                        : Component.translatable("wh_npcs.ui.effect_picker.unit_sec").getString();
                g.drawString(font, durUnit, durBoxX() + DUR_W + 2, y + 5,
                        infinite ? VanillaUIHelper.TEXT_AQUA : VanillaUIHelper.TEXT_DARK_GRAY, false);
                String lvl = Component.translatable("wh_npcs.ui.effect_picker.unit_lvl").getString();
                g.drawString(font, lvl, ampBoxX() - font.width(lvl) - 2, y + 5, VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
        }
        VanillaUIHelper.drawScrollbar(g, winX + winW - PAD - 8, listTop + 2, MAX_ROWS * ROW_H - 4,
                list.size(), MAX_ROWS, scroll, scrollbars, v -> scroll = v);

        boolean clearHover = isOver(mouseX, mouseY, winX + PAD, bottomY, 90, 18) && count() > 0;
        VanillaUIHelper.drawButton(g, winX + PAD, bottomY, 90, 18, clearHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.effect_picker.clear").getString(),
                winX + PAD + 45, bottomY + 5, count() == 0 ? VanillaUIHelper.TEXT_DARK_GRAY
                        : (clearHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_RED));
        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(),
                winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!applyMode) {
            return;
        }
        for (int slot = 0; slot < MAX_ROWS; slot++) {
            if (durBoxes[slot].visible && isOver(mouseX, mouseY,
                    durBoxes[slot].getX(), durBoxes[slot].getY(), DUR_W, BOX_H)) {
                tooltip(g, "wh_npcs.ui.effect_picker.dur_tip", mouseX, mouseY);
                return;
            }
            if (ampBoxes[slot].visible && isOver(mouseX, mouseY,
                    ampBoxes[slot].getX(), ampBoxes[slot].getY(), AMP_W, BOX_H)) {
                tooltip(g, "wh_npcs.ui.effect_picker.amp_tip", mouseX, mouseY);
                return;
            }
        }
    }

    private void tooltip(GuiGraphics g, String key, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        for (String line : Component.translatable(key).getString().split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private boolean overAnyBox(double mx, double my) {
        for (int slot = 0; slot < MAX_ROWS; slot++) {
            if (durBoxes[slot].visible && isOver(mx, my,
                    durBoxes[slot].getX(), durBoxes[slot].getY(), DUR_W, BOX_H)) {
                return true;
            }
            if (ampBoxes[slot].visible && isOver(mx, my,
                    ampBoxes[slot].getX(), ampBoxes[slot].getY(), AMP_W, BOX_H)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button != 0) {
            return superMouseClicked(mouseX, mouseY, button);
        }
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }
        for (int i = 0; i < 3; i++) {
            int tx = winX + winW - PAD - 132 + i * 44;
            if (isOver(mouseX, mouseY, tx, winY + HEADER_H + 2, 42, 16)) {
                tab = i;
                scroll = 0;
                return true;
            }
        }
        if (applyMode && overAnyBox(mouseX, mouseY)) {
            return superMouseClicked(mouseX, mouseY, button);
        }
        List<Entry> list = displayed();
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            int y = listTop + 2 + (i - scroll) * ROW_H;
            if (isOver(mouseX, mouseY, winX + PAD + 2, y - 1, winW - PAD * 2 - 8, ROW_H)) {
                setFocused(null);
                toggle(list.get(i).id());
                return true;
            }
        }
        if (count() > 0 && isOver(mouseX, mouseY, winX + PAD, bottomY, 90, 18)) {
            clearAll();
            return true;
        }
        if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
            onClose();
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, winX + PAD, listTop, winW - PAD * 2, MAX_ROWS * ROW_H)) {
            setFocused(null);
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta),
                    Math.max(0, displayed().size() - MAX_ROWS)));
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

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5, hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    @Override
    public void onClose() {
        onDone.run();
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
