package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.compat.Compat;
import com.withouthonor.npcs.compat.MagicAttrInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Атрибуты магии Iron's Spells: 3 общих + 9 школ × (сопротивление, сила).
 *
 * Все они у ISS — MagicPercentAttribute(имя, 1.0, -100, 100), поэтому показываем проценты
 * с нулём в нейтрали: attr = 1 + pct/100. Пишем в profileJson.magic.attrs только ненулевые.
 * ISS-типы не линкуем — список приходит из моста (MagicAttrInfo).
 */
public class MagicAttrsScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 20;
    private static final int WIN_W = 470;
    private static final int GENERAL_LABEL_W = 220;  // «Сокращение времени чтения заклинаний» — длинное
    private static final int SCHOOL_LABEL_W = 88;
    private static final int VALUE_W = 40;
    private static final int MIN_PCT = -100;
    private static final int MAX_PCT = 100;

    /** Пара атрибутов одной школы. */
    private record SchoolRow(String school, int color, MagicAttrInfo resist, MagicAttrInfo power) {}

    private final Screen parent;
    private final JsonObject profileJson;

    private final List<MagicAttrInfo> general = new ArrayList<>();
    private final List<SchoolRow> schools = new ArrayList<>();
    private final Map<String, Integer> pct = new LinkedHashMap<>();

    /** Активный ползунок: id атрибута, либо null. */
    private String activeId;
    private int winX, winY, winH, generalTop, sepY, schoolTop, bottomY;
    private String hoverTooltip;

    public MagicAttrsScreen(Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.magic_attrs.title"));
        this.parent = parent;
        this.profileJson = profileJson;

        Map<String, MagicAttrInfo> resistBySchool = new LinkedHashMap<>();
        Map<String, MagicAttrInfo> powerBySchool = new LinkedHashMap<>();
        Map<String, Integer> colorBySchool = new LinkedHashMap<>();
        for (MagicAttrInfo a : Compat.ironsSpells().listMagicAttributes()) {
            if (!a.isSchool()) {
                general.add(a);
                continue;
            }
            colorBySchool.putIfAbsent(a.school(), a.color());
            (a.power() ? powerBySchool : resistBySchool).put(a.school(), a);
        }
        for (Map.Entry<String, Integer> e : colorBySchool.entrySet()) {
            MagicAttrInfo r = resistBySchool.get(e.getKey());
            MagicAttrInfo p = powerBySchool.get(e.getKey());
            if (r != null && p != null) {
                schools.add(new SchoolRow(e.getKey(), e.getValue(), r, p));
            }
        }
        loadState();
    }

    private void loadState() {
        JsonObject attrs = profileJson.has("magic") && profileJson.getAsJsonObject("magic").has("attrs")
                ? profileJson.getAsJsonObject("magic").getAsJsonObject("attrs")
                : new JsonObject();
        for (MagicAttrInfo a : allAttrs()) {
            double v = attrs.has(a.id()) ? attrs.get(a.id()).getAsDouble() : 1.0D;
            pct.put(a.id(), Mth.clamp((int) Math.round((v - 1.0D) * 100.0D), MIN_PCT, MAX_PCT));
        }
    }

    private List<MagicAttrInfo> allAttrs() {
        List<MagicAttrInfo> out = new ArrayList<>(general);
        for (SchoolRow s : schools) {
            out.add(s.resist());
            out.add(s.power());
        }
        return out;
    }

    @Override
    protected int designW() {
        return WIN_W;
    }

    @Override
    protected int designH() {
        return winHeight();
    }

    private int winHeight() {
        // шапка + общие + воздух + разделитель + заголовки колонок + школы + нижний ряд
        return HEADER_H + 10 + general.size() * ROW_H + 12 + 28 + schools.size() * ROW_H + 36;
    }

    private void recalc() {
        winH = winHeight();
        winX = (width - WIN_W) / 2;
        winY = (height - winH) / 2;
        generalTop = winY + HEADER_H + 10;
        sepY = generalTop + general.size() * ROW_H + 12;
        schoolTop = sepY + 28;
        bottomY = winY + winH - PAD - 18;
    }

    // --- Геометрия ползунков: одна и та же для рендера и кликов ---

    private int cellX(int col) {
        return col == 0 ? winX + PAD : winX + WIN_W / 2 + 2;
    }

    private int cellW() {
        return WIN_W / 2 - PAD - 4;
    }

    private int trackX(boolean isGeneral, int col) {
        return isGeneral ? winX + PAD + GENERAL_LABEL_W : cellX(col) + SCHOOL_LABEL_W;
    }

    private int trackW(boolean isGeneral) {
        return isGeneral
                ? WIN_W - PAD * 2 - GENERAL_LABEL_W - VALUE_W
                : cellW() - SCHOOL_LABEL_W - (VALUE_W - 8);
    }

    private int rowY(boolean isGeneral, int index) {
        return (isGeneral ? generalTop : schoolTop) + index * ROW_H;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, WIN_W, winH);
        g.fill(winX + 2, winY + 2, winX + WIN_W - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, tr("wh_npcs.ui.magic_attrs.title"), winX + PAD, winY + 7,
                VanillaUIHelper.TEXT_YELLOW, false);

        hoverTooltip = null;

        for (int i = 0; i < general.size(); i++) {
            MagicAttrInfo a = general.get(i);
            int y = rowY(true, i);
            g.drawString(font, fit(a.name(), GENERAL_LABEL_W - 8), winX + PAD, y,
                    VanillaUIHelper.TEXT_GRAY, false);
            slider(g, a, VanillaUIHelper.TEXT_AQUA, trackX(true, 0), trackW(true), y, mouseX, mouseY);
        }

        VanillaUIHelper.drawSeparator(g, winX + PAD, sepY, WIN_W - PAD * 2);
        g.drawString(font, tr("wh_npcs.ui.magic_attrs.col_resist"), cellX(0), sepY + 10,
                VanillaUIHelper.TEXT_DARK_GRAY, false);
        g.drawString(font, tr("wh_npcs.ui.magic_attrs.col_power"), cellX(1), sepY + 10,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        for (int i = 0; i < schools.size(); i++) {
            SchoolRow s = schools.get(i);
            int y = rowY(false, i);
            for (int col = 0; col < 2; col++) {
                MagicAttrInfo a = col == 0 ? s.resist() : s.power();
                g.drawString(font, fit(s.school(), SCHOOL_LABEL_W - 6), cellX(col), y, s.color(), false);
                slider(g, a, s.color(), trackX(false, col), trackW(false), y, mouseX, mouseY);
            }
        }

        g.drawString(font, tr("wh_npcs.ui.magic_attrs.hint"), winX + PAD, bottomY + 5,
                VanillaUIHelper.TEXT_DARK_GRAY, false);
        drawBtn(g, tr("wh_npcs.ui.common.done"), winX + WIN_W - PAD - 70, bottomY, 70, mouseX, mouseY);
    }

    private void slider(GuiGraphics g, MagicAttrInfo a, int accent, int tx, int tw, int y,
                        int mouseX, int mouseY) {
        int v = pct.getOrDefault(a.id(), 0);
        int cy = y + 3;

        g.fill(tx, cy - 1, tx + tw, cy + 2, VanillaUIHelper.BG_INNER);
        g.fill(tx, cy - 1, tx + tw, cy, VanillaUIHelper.BORDER_OUTER);

        int handleX = tx + Math.round((v - MIN_PCT) / (float) (MAX_PCT - MIN_PCT) * tw);
        int zeroX = tx + tw / 2;
        g.fill(zeroX, cy, zeroX + 1, cy + 1, VanillaUIHelper.BORDER_LIGHT);

        int a0 = Math.min(zeroX, handleX);
        int b0 = Math.max(zeroX, handleX);
        if (b0 > a0) {
            g.fill(a0, cy - 1, b0, cy + 2, v >= 0 ? accent : VanillaUIHelper.TEXT_RED);
        }

        boolean hover = isOver(mouseX, mouseY, tx - 4, y - 2, tw + 8, ROW_H - 4);
        VanillaUIHelper.drawButton(g, handleX - 2, cy - 4, 5, 9, hover || a.id().equals(activeId));
        g.drawString(font, (v > 0 ? "+" : "") + v + "%", tx + tw + 5, y,
                v == 0 ? VanillaUIHelper.TEXT_DARK_GRAY : VanillaUIHelper.TEXT_AQUA, false);

        if (hover) {
            hoverTooltip = tipFor(a);
        }
    }

    /**
     * Тултипы разные по смыслу: общая сила — множитель мощи, «Сила» на экране магии — уровень
     * заклинания; сокращение чтения — длительность каста, «Интервал» — пауза между кастами.
     */
    private static String tipFor(MagicAttrInfo a) {
        if (a.isSchool()) {
            String key = a.power() ? "school_power" : "school_resist";
            return Component.translatable("wh_npcs.ui.magic_attrs.tip." + key, a.school()).getString();
        }
        String path = a.id().substring(a.id().indexOf(':') + 1);
        return tr("wh_npcs.ui.magic_attrs.tip." + path);
    }

    /** Обрезает подпись под ширину колонки, чтобы она не наезжала на ползунок. */
    private String fit(String s, int maxW) {
        if (font.width(s) <= maxW) {
            return s;
        }
        return font.plainSubstrByWidth(s, maxW - font.width("…")) + "…";
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            List<Component> lines = new ArrayList<>();
            for (String line : hoverTooltip.split("\n")) {
                lines.add(Component.literal(line));
            }
            queueTooltip(lines);
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0 && isOver(mouseX, mouseY, winX + WIN_W - PAD - 70, bottomY, 70, 18)) {
            onClose();
            return true;
        }
        String hit = attrAt(mouseX, mouseY);
        if (hit != null) {
            if (button == 1) {
                pct.put(hit, 0);
                return true;
            }
            if (button == 0) {
                activeId = hit;
                setFromMouse(hit, mouseX);
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && activeId != null) {
            setFromMouse(activeId, mouseX);
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            activeId = null;
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    /** id атрибута под курсором, либо null. */
    private String attrAt(double mouseX, double mouseY) {
        for (int i = 0; i < general.size(); i++) {
            int y = rowY(true, i);
            if (isOver(mouseX, mouseY, trackX(true, 0) - 4, y - 2, trackW(true) + 8, ROW_H - 4)) {
                return general.get(i).id();
            }
        }
        for (int i = 0; i < schools.size(); i++) {
            int y = rowY(false, i);
            for (int col = 0; col < 2; col++) {
                if (isOver(mouseX, mouseY, trackX(false, col) - 4, y - 2, trackW(false) + 8, ROW_H - 4)) {
                    SchoolRow s = schools.get(i);
                    return (col == 0 ? s.resist() : s.power()).id();
                }
            }
        }
        return null;
    }

    private void setFromMouse(String id, double mouseX) {
        boolean isGeneral = general.stream().anyMatch(a -> a.id().equals(id));
        int col = 0;
        if (!isGeneral) {
            col = schools.stream().anyMatch(s -> s.resist().id().equals(id)) ? 0 : 1;
        }
        int tx = trackX(isGeneral, col);
        int tw = trackW(isGeneral);
        float frac = Mth.clamp((float) (mouseX - tx) / tw, 0F, 1F);
        float raw = MIN_PCT + frac * (MAX_PCT - MIN_PCT);
        int step = hasShiftDown() ? 1 : 5;
        pct.put(id, Mth.clamp(Math.round(raw / step) * step, MIN_PCT, MAX_PCT));
    }

    private void apply() {
        JsonObject magic = profileJson.has("magic")
                ? profileJson.getAsJsonObject("magic")
                : new JsonObject();
        JsonObject attrs = new JsonObject();
        for (Map.Entry<String, Integer> e : pct.entrySet()) {
            if (e.getValue() != 0) {
                attrs.addProperty(e.getKey(), 1.0D + e.getValue() / 100.0D);
            }
        }
        if (attrs.size() > 0) {
            magic.add("attrs", attrs);
            profileJson.add("magic", magic);
        } else {
            magic.remove("attrs");
        }
    }

    @Override
    public void onClose() {
        apply();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
