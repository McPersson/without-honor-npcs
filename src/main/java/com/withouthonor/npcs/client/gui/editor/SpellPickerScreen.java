package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.compat.Compat;
import com.withouthonor.npcs.compat.SpellInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Одиночный выбор заклинания Iron's Spells для действия диалога «Кастовать».
 * Список из моста (SpellInfo: id, имя, иконка, цвет школы) — без ISS-типов. Поиск, скролл,
 * клик по строке возвращает id через callback и закрывает экран.
 */
public class SpellPickerScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 18;
    private static final int MAX_ROWS = 11;
    private static final int WIN_W = 320;
    private static final int WIN_H = HEADER_H + 24 + MAX_ROWS * ROW_H + 16;

    private final Screen parent;
    private final String current;
    private final Consumer<String> onPick;

    private final List<SpellInfo> all = new ArrayList<>();
    private List<SpellInfo> displayedCache;
    private SelectableEditBox searchBox;
    private int scroll;
    private boolean scrolledToCurrent; // автоскролл к выбранному — один раз при открытии
    private final ScrollDrag scrollbars = new ScrollDrag();
    private int winX, winY, listTop;

    public SpellPickerScreen(Screen parent, String current, Consumer<String> onPick) {
        super(Component.translatable("wh_npcs.ui.spellpick.title"));
        this.parent = parent;
        this.current = current == null ? "" : current;
        this.onPick = onPick;
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
        winX = (width - WIN_W) / 2;
        winY = (height - WIN_H) / 2;
        listTop = winY + HEADER_H + 22;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        if (all.isEmpty()) {
            all.addAll(Compat.ironsSpells().listSpells());
        }
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, winY + HEADER_H + 2,
                WIN_W - PAD * 2, 16, Component.translatable("wh_npcs.ui.spellpick.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.spellpick.search_hint"));
        searchBox.setResponder(v -> {
            scroll = 0;
            displayedCache = null;
        });
        // Автоскролл к уже выбранному спеллу при первом открытии: при 100+ спеллах
        // выбранная строка иначе остаётся за пределами экрана. Позицию ищем в
        // отфильтрованном списке (учитывает восстановленный текст поиска), выбранную
        // строку центрируем, скролл клампим в границы. Ресайз скролл не сбрасывает.
        if (!scrolledToCurrent) {
            scrolledToCurrent = true;
            if (!current.isEmpty()) {
                List<SpellInfo> list = displayed();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).id().equals(current)) {
                        scroll = Math.max(0, Math.min(i - MAX_ROWS / 2,
                                Math.max(0, list.size() - MAX_ROWS)));
                        break;
                    }
                }
            }
        }
    }

    private List<SpellInfo> displayed() {
        if (displayedCache != null) {
            return displayedCache;
        }
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<SpellInfo> out = new ArrayList<>();
        for (SpellInfo s : all) {
            if (q.isEmpty() || s.name().toLowerCase(Locale.ROOT).contains(q)
                    || s.id().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(s);
            }
        }
        displayedCache = out;
        return out;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, WIN_W, WIN_H);
        g.fill(winX + 2, winY + 2, winX + WIN_W - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.spellpick.title").getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        VanillaUIHelper.drawContentPanel(g, winX + PAD, listTop, WIN_W - PAD * 2, MAX_ROWS * ROW_H + 6);
        List<SpellInfo> list = displayed();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - MAX_ROWS)));
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            SpellInfo e = list.get(i);
            int y = listTop + 2 + (i - scroll) * ROW_H;
            boolean on = e.id().equals(current);
            boolean hov = isOver(mouseX, mouseY, winX + PAD + 2, y - 1, WIN_W - PAD * 2 - 8, ROW_H);
            // Правый край подсветки/свечения не доходит до скроллбара — симметричный зазор.
            int rowRight = winX + WIN_W - PAD - 13;
            if (on || hov) {
                g.fill(winX + PAD + 2, y - 1, rowRight, y - 1 + ROW_H,
                        on ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
                schoolGlow(g, winX + PAD + 4, y - 1, rowRight, y - 1 + ROW_H, e.color());
            }
            g.fill(winX + PAD + 2, y - 1, winX + PAD + 4, y - 1 + ROW_H, e.color()); // полоска школы
            g.blit(e.icon(), winX + PAD + 6, y, 0.0F, 0.0F, 16, 16, 16, 16);
            g.drawString(font, e.name(), winX + PAD + 26, y + 4, VanillaUIHelper.TEXT_WHITE, false);
        }
        VanillaUIHelper.drawScrollbar(g, winX + WIN_W - PAD - 8, listTop + 2, MAX_ROWS * ROW_H - 4,
                list.size(), MAX_ROWS, scroll, scrollbars, s -> scroll = s);
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
        List<SpellInfo> list = displayed();
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            int y = listTop + 2 + (i - scroll) * ROW_H;
            if (isOver(mouseX, mouseY, winX + PAD + 2, y - 1, WIN_W - PAD * 2 - 8, ROW_H)) {
                onPick.accept(list.get(i).id());
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dx, double dy) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta),
                Math.max(0, displayed().size() - MAX_ROWS)));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    /**
     * Лёгкое «магическое» свечение цветом школы: мягкий градиент от полоски школы вправо,
     * с пульсацией альфы по времени. Без реальных партиклов в GUI — только цвет школы ISS.
     */
    static void schoolGlow(GuiGraphics g, int x1, int y1, int x2, int y2, int schoolColor) {
        // Медленная плавная пульсация (период ~5с) + небольшая амплитуда:
        // глаз не цепляется, но при взгляде красиво.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 800.0);
        int peak = 0x10 + Math.round(pulse * 0x1C); // альфа у ЛЕВОГО края (16..44 из 255)
        int rgb = schoolColor & 0x00FFFFFF;
        // Ванильный fillGradient — вертикальный (сверху вниз), поэтому горизонтальный градиент
        // «слева направо» рисуем сами: полоски с линейно убывающей альфой (peak → 0).
        int steps = 24;
        float step = (x2 - x1) / (float) steps;
        for (int i = 0; i < steps; i++) {
            int a = Math.round(peak * (1.0F - (i + 0.5F) / steps)); // 100% слева → 0% справа
            if (a <= 0) {
                continue;
            }
            int sx = x1 + Math.round(i * step);
            int ex = x1 + Math.round((i + 1) * step);
            g.fill(sx, y1, ex, y2, rgb | (a << 24));
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
