package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.NumberScrub;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.compat.Compat;
import com.withouthonor.npcs.compat.SpellInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Экран лоадаута мага (Iron's Spells). Пикер спеллов с иконками и цветом школы, категория на
 * каждый спелл (attack/defense/movement/support → 4 списка WizardAttackGoal.setSpells), тумблеры
 * «отступать»/«заклинания с оружия», скрабы силы и интервала. Спеллы из спеллбука в Curios-слоте
 * "spellbook" NPC показываются read-only строками с пометкой «из книги» (книга в слоте = активны,
 * сервер добавляет их в пул атаки сам — см. IronsSpellsBridgeImpl.buildMageGoal).
 * Читает/пишет profileJson.magic. ISS-типы не линкует — данные берёт через мост (SpellInfo).
 */
public class MagicLoadoutScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 18;
    private static final int MAX_ROWS = 8;
    private static final int WIN_W = 360;
    private static final int WIN_H = 320;

    // «support» разведён на лечение/бафф: WizardSupportGoal берёт из них по HP союзника.
    private static final String[] CATS = {"attack", "defense", "movement", "support_heal", "support_buff"};
    private static final String[] CAT_KEYS = {"wh_npcs.ui.magic.cat_attack", "wh_npcs.ui.magic.cat_defense",
            "wh_npcs.ui.magic.cat_movement", "wh_npcs.ui.magic.cat_heal", "wh_npcs.ui.magic.cat_buff"};

    private final Screen parent;
    private final JsonObject profileJson;
    /** id сущности NPC в мире (для чтения спеллбука из Curios-слота), -1 если недоступна. */
    private final int npcEntityId;

    private final List<SpellInfo> all = new ArrayList<>();
    private final Map<String, String> loadout = new LinkedHashMap<>(); // id -> категория
    // Спеллы из книги в Curios-слоте "spellbook" — read-only: активны, пока книга в слоте.
    private final java.util.Set<String> bookIds = new java.util.LinkedHashSet<>();
    private final Map<String, Integer> weights = new LinkedHashMap<>(); // id -> вес 1..3; храним только != 1
    private boolean flee;
    private boolean fromWeapon;
    private boolean supportAllies;
    private float qMin = 0.3F, qMax = 0.7F, iMin = 1.5F, iMax = 4.0F;

    private EditBox searchBox;
    @Nullable
    private EditBox qMinBox, qMaxBox, iMinBox, iMaxBox;
    @Nullable
    private List<SpellInfo> displayedCache;
    private int scroll;
    private int tab; // 0 — все, 1 — оригинал (ISS), 2 — аддоны
    private final ScrollDrag scrollbars = new ScrollDrag();
    // числовые поля с драг-скрабом, кнопками −/+ и ПКМ-сбросом — стандарт NumberScrub
    private final List<NumberScrub> scrubs = new ArrayList<>();
    private int winX, winY, winW, winH, listTop, setTop, bottomY;
    @Nullable
    private String hoverTooltip;

    /** Поддержка союзников работает только при боевом пресете «Маг» (связка целей ISS в этой ветке). */
    private final boolean isMagePreset;

    public MagicLoadoutScreen(Screen parent, JsonObject profileJson, int npcEntityId) {
        super(Component.translatable("wh_npcs.ui.magic.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        this.npcEntityId = npcEntityId;
        this.isMagePreset = profileJson.has("combat_preset")
                && "mage".equals(profileJson.get("combat_preset").getAsString());
        loadState();
    }

    @Override
    protected int designW() {
        return WIN_W;
    }

    @Override
    protected int designH() {
        return WIN_H;
    }

    private void loadState() {
        JsonObject m = profileJson.has("magic") ? profileJson.getAsJsonObject("magic") : new JsonObject();
        loadout.clear();
        weights.clear();
        if (m.has("spells")) {
            for (JsonElement e : m.getAsJsonArray("spells")) {
                JsonObject s = e.getAsJsonObject();
                String cat = s.has("category") ? s.get("category").getAsString() : "attack";
                if ("support".equals(cat)) {
                    cat = "support_heal"; // легаси-категория до разделения лечение/бафф
                }
                String id = s.get("id").getAsString();
                loadout.put(id, cat);
                // Вес 1..3 (кламп как в CompanionProfile.MagicSpell.fromJson); дефолт 1 не храним
                int w = s.has("weight") ? Math.max(1, Math.min(3, s.get("weight").getAsInt())) : 1;
                if (w != 1) {
                    weights.put(id, w);
                }
            }
        }
        flee = m.has("flee") && m.get("flee").getAsBoolean();
        fromWeapon = m.has("spells_from_weapon") && m.get("spells_from_weapon").getAsBoolean();
        supportAllies = profileJson.has("support_allies") && profileJson.get("support_allies").getAsBoolean();
        qMin = m.has("min_quality") ? m.get("min_quality").getAsFloat() : 0.3F;
        qMax = m.has("max_quality") ? m.get("max_quality").getAsFloat() : 0.7F;
        iMin = m.has("min_interval") ? m.get("min_interval").getAsFloat() : 1.5F;
        iMax = m.has("max_interval") ? m.get("max_interval").getAsFloat() : 4.0F;
    }

    private void recalc() {
        winW = WIN_W;
        winH = WIN_H;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        listTop = winY + HEADER_H + 22;
        setTop = listTop + MAX_ROWS * ROW_H + 12;
        bottomY = winY + winH - PAD - 20;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        if (all.isEmpty()) {
            all.addAll(Compat.ironsSpells().listSpells());
        }
        // Книжные спеллы из Curios-слота "spellbook": стеки Curios синкаются трекаемым клиентам
        // (тот же канал, что кормит CuriosNpcLayer), поэтому читаем клиентскую сущность напрямую.
        bookIds.clear();
        if (npcEntityId >= 0 && minecraft != null && minecraft.level != null
                && minecraft.level.getEntity(npcEntityId)
                        instanceof net.minecraft.world.entity.LivingEntity le) {
            bookIds.addAll(Compat.ironsSpells().readEquippedSpellbookSpellIds(le));
        }
        displayedCache = null;
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, winY + HEADER_H + 2,
                winW - PAD * 2 - 150, 16, Component.translatable("wh_npcs.ui.magic.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.magic.search_hint"));
        searchBox.setResponder(v -> {
            scroll = 0;
            displayedCache = null;
        });

        // Поля раздвинуты — по бокам каждого кнопки −/+ скраба; дефолты для ПКМ-сброса из loadState.
        int qy = setTop + 44;
        qMinBox = addRenderableWidget(numBox(winX + PAD + 48, qy, 28, qMin, d -> qMin = (float) d, 0.0F, 1.0F));
        qMaxBox = addRenderableWidget(numBox(winX + PAD + 116, qy, 28, qMax, d -> qMax = (float) d, 0.0F, 1.0F));
        iMinBox = addRenderableWidget(numBox(winX + PAD + 232, qy, 28, iMin, d -> iMin = (float) d, 0.2F, 20.0F));
        iMaxBox = addRenderableWidget(numBox(winX + PAD + 300, qy, 28, iMax, d -> iMax = (float) d, 0.2F, 20.0F));
        scrubs.clear();
        scrubs.add(new NumberScrub(qMinBox, 0.0F, 1.0F, 0.1F, 0.01F, "0.3", false, () -> setFocused(null)));
        scrubs.add(new NumberScrub(qMaxBox, 0.0F, 1.0F, 0.1F, 0.01F, "0.7", false, () -> setFocused(null)));
        scrubs.add(new NumberScrub(iMinBox, 0.2F, 20.0F, 0.5F, 0.05F, "1.5", false, () -> setFocused(null)));
        scrubs.add(new NumberScrub(iMaxBox, 0.2F, 20.0F, 0.5F, 0.05F, "4", false, () -> setFocused(null)));
    }

    private EditBox numBox(int x, int y, int w, float val, java.util.function.DoubleConsumer setter,
                           float min, float max) {
        EditBox b = new SelectableEditBox(font, x, y, w, 14, Component.empty());
        b.setMaxLength(5);
        b.setValue(fmt(val));
        b.setResponder(v -> {
            try {
                setter.accept(Math.max(min, Math.min(max, Float.parseFloat(v.trim().replace(',', '.')))));
            } catch (NumberFormatException ignored) {
            }
        });
        return b;
    }

    private static String fmt(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }

    /** Вне фокуса подставляем уже зажатое значение поля (визуальная нормализация ввода). */
    private void syncBox(@Nullable EditBox box, float val) {
        if (box != null && !box.isFocused() && !box.getValue().equals(fmt(val))) {
            box.setValue(fmt(val));
        }
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private List<SpellInfo> displayed() {
        if (displayedCache != null) {
            return displayedCache;
        }
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        List<SpellInfo> sel = new ArrayList<>();
        List<SpellInfo> rest = new ArrayList<>();
        for (SpellInfo e : all) {
            boolean original = e.id().startsWith("irons_spellbooks:");
            if (tab == 1 && !original) {
                continue;
            }
            if (tab == 2 && original) {
                continue;
            }
            if (!q.isEmpty() && !e.name().toLowerCase(Locale.ROOT).contains(q)
                    && !e.id().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            // Выбранные и книжные — вверх, внутри группы алфавит из all
            (loadout.containsKey(e.id()) || bookIds.contains(e.id()) ? sel : rest).add(e);
        }
        sel.addAll(rest);
        displayedCache = sel;
        return sel;
    }

    private int catColor(String cat) {
        return switch (cat) {
            case "defense" -> VanillaUIHelper.TEXT_AQUA;
            case "movement" -> VanillaUIHelper.TEXT_GREEN;
            case "support_heal", "support" -> VanillaUIHelper.TEXT_YELLOW;
            case "support_buff" -> VanillaUIHelper.TEXT_GOLD;
            default -> VanillaUIHelper.TEXT_RED;
        };
    }

    private String catLabel(String cat) {
        for (int i = 0; i < CATS.length; i++) {
            if (CATS[i].equals(cat)) {
                return Component.translatable(CAT_KEYS[i]).getString();
            }
        }
        return Component.translatable(CAT_KEYS[0]).getString();
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        hoverTooltip = null;
        // Пока поле не в фокусе — показываем уже зажатое значение (5 → 1, буквы → последнее валидное).
        syncBox(qMinBox, qMin);
        syncBox(qMaxBox, qMax);
        syncBox(iMinBox, iMin);
        syncBox(iMaxBox, iMax);
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.magic.title").getString()
                + "  §7(" + loadout.size() + ")", winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        // Весь лоадаут работает только с боевым пресетом «Маг» — предупреждаем прямо в шапке.
        if (!isMagePreset) {
            String warn = "§6⚠ " + tr("wh_npcs.ui.magic.need_mage_banner");
            g.drawString(font, warn, winX + winW - PAD - font.width(warn), winY + 7,
                    VanillaUIHelper.TEXT_GOLD, false);
        }

        String[] tabKeys = {"wh_npcs.ui.magic.tab_all", "wh_npcs.ui.magic.tab_orig", "wh_npcs.ui.magic.tab_addon"};
        for (int i = 0; i < 3; i++) {
            int tx = winX + winW - PAD - 142 + i * 48;
            boolean hov = isOver(mouseX, mouseY, tx, winY + HEADER_H + 2, 46, 16);
            VanillaUIHelper.drawButton(g, tx, winY + HEADER_H + 2, 46, 16, hov || tab == i);
            g.drawCenteredString(font, tr(tabKeys[i]), tx + 23, winY + HEADER_H + 6,
                    tab == i ? VanillaUIHelper.TEXT_YELLOW : (hov ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA));
        }

        VanillaUIHelper.drawContentPanel(g, winX + PAD, listTop, winW - PAD * 2, MAX_ROWS * ROW_H + 6);
        List<SpellInfo> list = displayed();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - MAX_ROWS)));
        // «×N» — вес спелла справа от категории; кнопка категории ужата с 78 до 62, чтобы влезть в строку.
        int wBtnW = 18;
        int wBtnX = winX + winW - PAD - 16 - wBtnW;
        int catBtnW = 62;
        int catBtnX = wBtnX - 2 - catBtnW;
        int nameW = catBtnX - (winX + PAD + 44) - 4;
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            SpellInfo e = list.get(i);
            int y = listTop + 2 + (i - scroll) * ROW_H;
            boolean on = loadout.containsKey(e.id());
            // Книжная строка (read-only): спелл задан спеллбуком в Curios-слоте и не выбран вручную.
            boolean book = !on && bookIds.contains(e.id());
            boolean hov = isOver(mouseX, mouseY, winX + PAD + 2, y - 1, winW - PAD * 2 - 8, ROW_H);
            // Правый край подсветки/свечения не доходит до скроллбара — симметричный зазор с обеих сторон.
            int rowRight = winX + winW - PAD - 13;
            if (on || book || hov) {
                g.fill(winX + PAD + 2, y - 1, rowRight, y - 1 + ROW_H,
                        on || book ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
                SpellPickerScreen.schoolGlow(g, winX + PAD + 4, y - 1, rowRight, y - 1 + ROW_H, e.color());
            }
            // Полоска цвета школы слева.
            g.fill(winX + PAD + 2, y - 1, winX + PAD + 4, y - 1 + ROW_H, e.color());
            VanillaUIHelper.drawButton(g, winX + PAD + 6, y + 2, 12, 12, false);
            if (on) {
                VanillaUIHelper.drawCheck(g, winX + PAD + 7, y + 3, VanillaUIHelper.TEXT_GREEN);
            } else if (book) {
                // Светло-пурпурная галочка (§d): активен, но управляется книгой, а не кликом.
                VanillaUIHelper.drawCheck(g, winX + PAD + 7, y + 3, 0xFFFF55FF);
            }
            g.blit(e.icon(), winX + PAD + 22, y, 0.0F, 0.0F, 16, 16, 16, 16);
            g.drawString(font, font.plainSubstrByWidth(e.name(), nameW), winX + PAD + 44, y + 4,
                    on ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (book) {
                // Вместо кнопок категории/веса — пометка «из книги»; тултип объясняет read-only.
                String badge = "§d" + tr("wh_npcs.ui.magic.book_badge");
                g.drawString(font, badge, wBtnX + wBtnW - font.width(badge), y + 4,
                        VanillaUIHelper.TEXT_WHITE, false);
                if (hov) {
                    hoverTooltip = tr("wh_npcs.ui.magic.book_row_tip");
                }
            }
            if (on) {
                String cat = loadout.get(e.id());
                boolean chov = isOver(mouseX, mouseY, catBtnX, y + 1, catBtnW, 14);
                VanillaUIHelper.drawButton(g, catBtnX, y + 1, catBtnW, 14, chov);
                g.drawCenteredString(font, catLabel(cat), catBtnX + catBtnW / 2, y + 4, catColor(cat));
                if (chov) {
                    hoverTooltip = tr("wh_npcs.ui.magic.cat_tip");
                }
                // Вес «×N»: серый при дефолте ×1, жёлтый при усилении
                int wgt = weights.getOrDefault(e.id(), 1);
                boolean whov = isOver(mouseX, mouseY, wBtnX, y + 1, wBtnW, 14);
                VanillaUIHelper.drawButton(g, wBtnX, y + 1, wBtnW, 14, whov);
                g.drawCenteredString(font, "×" + wgt, wBtnX + wBtnW / 2, y + 4,
                        wgt == 1 ? VanillaUIHelper.TEXT_GRAY : VanillaUIHelper.TEXT_YELLOW);
                if (whov) {
                    hoverTooltip = tr("wh_npcs.ui.magic.weight_tip");
                }
            }
        }
        VanillaUIHelper.drawScrollbar(g, winX + winW - PAD - 8, listTop + 2, MAX_ROWS * ROW_H - 4,
                list.size(), MAX_ROWS, scroll, scrollbars, v -> scroll = v);

        // --- Настройки ---
        drawCheckbox(g, winX + PAD, setTop, flee, "wh_npcs.ui.magic.flee", mouseX, mouseY);
        drawCheckbox(g, winX + PAD + 170, setTop, fromWeapon, "wh_npcs.ui.magic.from_weapon", mouseX, mouseY);
        drawCheckbox(g, winX + PAD, setTop + 18, supportAllies, "wh_npcs.ui.magic.support_allies", mouseX, mouseY);

        int qy = setTop + 44;
        g.drawString(font, Component.translatable("wh_npcs.ui.magic.power").getString(),
                winX + PAD, qy + 3, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, "–", winX + PAD + 93, qy + 3, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.magic.interval").getString(),
                winX + PAD + 166, qy + 3, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, "–", winX + PAD + 278, qy + 3, VanillaUIHelper.TEXT_GRAY, false);
        for (NumberScrub s : scrubs) {
            s.render(g, font, mouseX, mouseY);
        }

        // Нижние кнопки — через общий VanillaUIHelper.drawSmallButton (единый вид с «Готово»).
        // «Из книги» убрана: книжные спеллы из Curios-слота подхватываются сами (строки выше).
        // «Очистить»: при пустом лоадауте задизейблена — серый текст, hover не подсвечивается.
        boolean clearHover = isOver(mouseX, mouseY, winX + PAD, bottomY, 84, 18) && !loadout.isEmpty();
        VanillaUIHelper.drawSmallButton(g, font, Component.translatable("wh_npcs.ui.magic.clear").getString(),
                winX + PAD, bottomY, 84, clearHover,
                loadout.isEmpty() ? VanillaUIHelper.TEXT_DARK_GRAY : VanillaUIHelper.TEXT_RED);
        drawBtn(g, Component.translatable("wh_npcs.ui.magic.attrs").getString(),
                winX + PAD + 92, bottomY, 76, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(),
                winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);

        if (hoverTooltip == null) {
            if (isOver(mouseX, mouseY, winX + PAD, setTop, 130, 12)) {
                hoverTooltip = tr("wh_npcs.ui.magic.flee_tip");
            } else if (isOver(mouseX, mouseY, winX + PAD + 170, setTop, 150, 12)) {
                hoverTooltip = tr("wh_npcs.ui.magic.from_weapon_tip");
            } else if (isOver(mouseX, mouseY, winX + PAD, setTop + 18, 170, 12)) {
                hoverTooltip = tr("wh_npcs.ui.magic.support_allies_tip");
            } else if (isOver(mouseX, mouseY, winX + PAD, qy, 156, 12)) {
                hoverTooltip = tr("wh_npcs.ui.magic.power_tip");
            } else if (isOver(mouseX, mouseY, winX + PAD + 166, qy, 174, 12)) {
                hoverTooltip = tr("wh_npcs.ui.magic.interval_tip");
            }
        }
    }

    private void drawCheckbox(GuiGraphics g, int x, int y, boolean on, String key, int mouseX, int mouseY) {
        boolean h = isOver(mouseX, mouseY, x, y, 12, 12);
        VanillaUIHelper.drawButton(g, x, y, 12, 12, h);
        if (on) {
            VanillaUIHelper.drawCheck(g, x + 1, y + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, Component.translatable(key).getString(), x + 16, y + 2,
                VanillaUIHelper.TEXT_GRAY, false);
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
        // ЛКМ по −/+ и ПКМ-сброс; ЛКМ по самому полю лишь «взводит» скраб (false) — фокус идёт дальше
        for (NumberScrub s : scrubs) {
            if (s.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        // «×N» — вес спелла: ЛКМ цикл 1→2→3→1, ПКМ — сброс в 1 (поэтому проверка ДО отсечения ПКМ)
        if (button == 0 || button == 1) {
            int wBtnW = 18;
            int wBtnX = winX + winW - PAD - 16 - wBtnW;
            List<SpellInfo> wlist = displayed();
            for (int i = scroll; i < Math.min(wlist.size(), scroll + MAX_ROWS); i++) {
                SpellInfo e = wlist.get(i);
                int y = listTop + 2 + (i - scroll) * ROW_H;
                if (loadout.containsKey(e.id()) && isOver(mouseX, mouseY, wBtnX, y + 1, wBtnW, 14)) {
                    int wgt = button == 1 ? 1 : weights.getOrDefault(e.id(), 1) % 3 + 1;
                    if (wgt == 1) {
                        weights.remove(e.id()); // дефолт не храним
                    } else {
                        weights.put(e.id(), wgt);
                    }
                    return true;
                }
            }
        }
        if (button != 0) {
            return superMouseClicked(mouseX, mouseY, button);
        }
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }
        for (int i = 0; i < 3; i++) {
            int tx = winX + winW - PAD - 142 + i * 48;
            if (isOver(mouseX, mouseY, tx, winY + HEADER_H + 2, 46, 16)) {
                tab = i;
                scroll = 0;
                displayedCache = null;
                return true;
            }
        }
        List<SpellInfo> list = displayed();
        // Координаты те же, что в renderContent: категория 62 + «×N» 18 справа
        int catBtnW = 62;
        int catBtnX = winX + winW - PAD - 16 - 18 - 2 - catBtnW;
        for (int i = scroll; i < Math.min(list.size(), scroll + MAX_ROWS); i++) {
            SpellInfo e = list.get(i);
            int y = listTop + 2 + (i - scroll) * ROW_H;
            if (loadout.containsKey(e.id()) && isOver(mouseX, mouseY, catBtnX, y + 1, catBtnW, 14)) {
                cycleCategory(e.id());
                return true;
            }
            if (isOver(mouseX, mouseY, winX + PAD + 2, y - 1, winW - PAD * 2 - 8, ROW_H)) {
                setFocused(null);
                // Книжная строка read-only: клик глотаем, управляется книгой в Curios-слоте.
                if (!loadout.containsKey(e.id()) && bookIds.contains(e.id())) {
                    return true;
                }
                toggle(e.id());
                return true;
            }
        }
        if (isOver(mouseX, mouseY, winX + PAD, setTop, 130, 12)) {
            flee = !flee;
            return true;
        }
        if (isOver(mouseX, mouseY, winX + PAD + 170, setTop, 150, 12)) {
            fromWeapon = !fromWeapon;
            return true;
        }
        if (isOver(mouseX, mouseY, winX + PAD, setTop + 18, 170, 12)) {
            supportAllies = !supportAllies;
            return true;
        }
        if (!loadout.isEmpty() && isOver(mouseX, mouseY, winX + PAD, bottomY, 84, 18)) {
            loadout.clear();
            weights.clear();
            displayedCache = null;
            return true;
        }
        if (isOver(mouseX, mouseY, winX + PAD + 92, bottomY, 76, 18)) {
            // Экран атрибутов пишет в profileJson.magic.attrs; наш onClose ключ не затирает.
            if (minecraft != null) {
                minecraft.setScreen(new MagicAttrsScreen(this, profileJson));
            }
            return true;
        }
        if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
            onClose();
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void toggle(String id) {
        if (loadout.remove(id) == null) {
            loadout.put(id, "attack");
        } else {
            weights.remove(id); // снятый спелл не тянет старый вес при повторном добавлении
        }
        displayedCache = null;
    }

    private void cycleCategory(String id) {
        String cur = loadout.getOrDefault(id, "attack");
        int i = 0;
        for (; i < CATS.length; i++) {
            if (CATS[i].equals(cur)) {
                break;
            }
        }
        loadout.put(id, CATS[(i + 1) % CATS.length]);
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
        for (NumberScrub s : scrubs) {
            if (s.mouseDragged(mouseX, mouseY, button)) {
                return true;
            }
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        for (NumberScrub s : scrubs) {
            s.mouseReleased();
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        VanillaUIHelper.drawSmallButton(g, font, label, x, y, w, isOver(mouseX, mouseY, x, y, w, 18), color);
    }

    @Override
    public void onClose() {
        JsonObject m = profileJson.has("magic") ? profileJson.getAsJsonObject("magic") : new JsonObject();
        JsonArray arr = new JsonArray();
        for (Map.Entry<String, String> e : loadout.entrySet()) {
            JsonObject s = new JsonObject();
            s.addProperty("id", e.getKey());
            s.addProperty("category", e.getValue());
            int wgt = weights.getOrDefault(e.getKey(), 1);
            if (wgt != 1) {
                s.addProperty("weight", wgt); // дефолтный вес 1 в JSON не пишем
            }
            arr.add(s);
        }
        m.add("spells", arr);
        m.addProperty("flee", flee);
        m.addProperty("spells_from_weapon", fromWeapon);
        // Гарантируем корректный порядок границ (мин ≤ макс), даже если ввели наоборот.
        m.addProperty("min_quality", Math.min(qMin, qMax));
        m.addProperty("max_quality", Math.max(qMin, qMax));
        m.addProperty("min_interval", Math.min(iMin, iMax));
        m.addProperty("max_interval", Math.max(iMin, iMax));
        profileJson.add("magic", m);
        profileJson.addProperty("support_allies", supportAllies); // поведение (верхний уровень), не в magic
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
