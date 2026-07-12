package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.entity.CreatureType;
import com.withouthonor.npcs.common.entity.ai.MobGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Подменю «Тип существа» (0.9.5 #4), открывается из боевого редактора. Две независимые оси:
 *  1. Как реагируют мобы (creature_type) — кто атакует/защищает NPC. НОВОЕ, поведение в Фазе 1.
 *  2. Урон от зачарований (mob_type) — ванильный MobType: Кара по нежити, Гроза членистоногих и т.п.
 *     Перенесён сюда из инлайна боевого редактора, локализация уточнена (раньше путали с боем).
 * Пишет прямо в profileJson (creature_type != neutral и mob_type != undefined).
 */
public class CreatureTypeScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int WIN_W = 320;
    private static final int WIN_H = 214;
    private static final int BTN_W = 150;

    private final Screen parent;
    private final JsonObject profileJson;

    private int winX, winY, top, bottomY;
    @Nullable
    private String hoverTooltip;

    public CreatureTypeScreen(@Nullable Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.creature_type.title"));
        this.parent = parent;
        this.profileJson = profileJson;
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
        top = winY + HEADER_H + 14;
        bottomY = winY + WIN_H - PAD - 20;
    }

    private CreatureType creatureType() {
        return CreatureType.byId(profileJson.has("creature_type")
                ? profileJson.get("creature_type").getAsString() : "neutral");
    }

    private int mobTypeIndex() {
        String mt = profileJson.has("mob_type") ? profileJson.get("mob_type").getAsString() : "undefined";
        for (int i = 0; i < NpcEditorScreen.MOBTYPE_IDS.length; i++) {
            if (NpcEditorScreen.MOBTYPE_IDS[i].equals(mt)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        hoverTooltip = null;
        VanillaUIHelper.drawWindow(g, winX, winY, WIN_W, WIN_H);
        g.fill(winX + 2, winY + 2, winX + WIN_W - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, tr("wh_npcs.ui.creature_type.title"), winX + PAD, winY + 7,
                VanillaUIHelper.TEXT_YELLOW, false);
        int x = winX + PAD;
        int btnX = winX + WIN_W - PAD - BTN_W;

        // Ось 1: реакция мобов
        CreatureType ct = creatureType();
        g.drawString(font, tr("wh_npcs.ui.creature_type.aggro_label"), x, top + 5, VanillaUIHelper.TEXT_GRAY, false);
        boolean ctHover = cycleButton(g, btnX, top, tr(ct.nameKey()), mouseX, mouseY);
        if (ctHover) {
            hoverTooltip = tr("wh_npcs.ui.creature_type.aggro_tip");
        }
        if (ct == CreatureType.CUSTOM) {
            // Кастом: галочки групп-атакующих (2×2) вместо описания.
            g.drawString(font, tr("wh_npcs.ui.creature_type.custom_label"), x, top + 26,
                    VanillaUIHelper.TEXT_GRAY, false);
            MobGroup[] sel = MobGroup.SELECTABLE;
            for (int i = 0; i < sel.length; i++) {
                int cx = x + (i % 2) * 150;
                int cy = top + 40 + (i / 2) * 18;
                checkbox(g, cx, cy, hasCustomAttacker(sel[i].id()), tr(sel[i].nameKey()), mouseX, mouseY);
            }
        } else {
            // Описание текущего типа — перенос по ширине окна.
            List<FormattedCharSequence> desc = font.split(Component.translatable(ct.descKey()), WIN_W - PAD * 2);
            int dy = top + 26;
            for (FormattedCharSequence line : desc) {
                g.drawString(font, line, x, dy, VanillaUIHelper.TEXT_GRAY, false);
                dy += 10;
            }
        }

        // «Природная вражда» по типу — только для типов с отношениями (не нейтрал/кастом).
        if (ct != CreatureType.NEUTRAL && ct != CreatureType.CUSTOM) {
            checkbox(g, x, top + 62, naturalHostility(), tr("wh_npcs.ui.creature_type.natural_label"),
                    mouseX, mouseY);
            if (isOver(mouseX, mouseY, x, top + 62, 12, 12)
                    || isOver(mouseX, mouseY, x + 16, top + 62, 200, 12)) {
                hoverTooltip = tr("wh_npcs.ui.creature_type.natural_tip");
            }
        }

        // Разделитель
        VanillaUIHelper.drawSeparator(g, x, top + 84, WIN_W - PAD * 2);

        // Ось 2: урон от зачарований (mob_type)
        g.drawString(font, tr("wh_npcs.ui.creature_type.enchant_label"), x, top + 99, VanillaUIHelper.TEXT_GRAY, false);
        boolean mtHover = cycleButton(g, btnX, top + 94, tr(NpcEditorScreen.MOBTYPE_NAME_KEYS[mobTypeIndex()]),
                mouseX, mouseY);
        if (mtHover) {
            hoverTooltip = tr("wh_npcs.ui.creature_type.enchant_tip");
        }

        drawBtn(g, tr("wh_npcs.ui.common.done"), winX + WIN_W - PAD - 80, bottomY, 80, mouseX, mouseY);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            List<Component> lines = new java.util.ArrayList<>();
            for (String line : hoverTooltip.split("\n")) {
                lines.add(Component.literal(line));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void checkbox(GuiGraphics g, int x, int y, boolean on, String label, int mouseX, int mouseY) {
        boolean h = isOver(mouseX, mouseY, x, y, 12, 12);
        VanillaUIHelper.drawButton(g, x, y, 12, 12, h);
        if (on) {
            VanillaUIHelper.drawCheck(g, x + 1, y + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, label, x + 16, y + 2, VanillaUIHelper.TEXT_GRAY, false);
    }

    private boolean naturalHostility() {
        return !profileJson.has("natural_hostility") || profileJson.get("natural_hostility").getAsBoolean();
    }

    private void toggleNaturalHostility() {
        if (naturalHostility()) {
            profileJson.addProperty("natural_hostility", false);
        } else {
            profileJson.remove("natural_hostility"); // дефолт true не пишем
        }
    }

    private boolean hasCustomAttacker(String groupId) {
        if (!profileJson.has("creature_custom_attackers")) {
            return false;
        }
        for (JsonElement e : profileJson.getAsJsonArray("creature_custom_attackers")) {
            if (e.getAsString().equals(groupId)) {
                return true;
            }
        }
        return false;
    }

    private void toggleCustomAttacker(String groupId) {
        JsonArray next = new JsonArray();
        boolean had = false;
        if (profileJson.has("creature_custom_attackers")) {
            for (JsonElement e : profileJson.getAsJsonArray("creature_custom_attackers")) {
                if (e.getAsString().equals(groupId)) {
                    had = true;
                } else {
                    next.add(e);
                }
            }
        }
        if (!had) {
            next.add(groupId);
        }
        if (next.isEmpty()) {
            profileJson.remove("creature_custom_attackers");
        } else {
            profileJson.add("creature_custom_attackers", next);
        }
    }

    private boolean cycleButton(GuiGraphics g, int x, int y, String label, int mouseX, int mouseY) {
        boolean hover = isOver(mouseX, mouseY, x, y, BTN_W, 18);
        VanillaUIHelper.drawButton(g, x, y, BTN_W, 18, hover);
        g.drawCenteredString(font, label + " ▾", x + BTN_W / 2, y + 5,
                hover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        return hover;
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY) {
        boolean hover = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hover);
        g.drawCenteredString(font, label, x + w / 2, y + 5,
                hover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button != 0) {
            return superMouseClicked(mouseX, mouseY, button);
        }
        int btnX = winX + WIN_W - PAD - BTN_W;
        if (isOver(mouseX, mouseY, btnX, top, BTN_W, 18)) {
            String next = creatureType().next().id();
            if ("neutral".equals(next)) {
                profileJson.remove("creature_type");
            } else {
                profileJson.addProperty("creature_type", next);
            }
            return true;
        }
        // Тумблер «природная вражда» (для типов с отношениями).
        CreatureType ctc = creatureType();
        if (ctc != CreatureType.NEUTRAL && ctc != CreatureType.CUSTOM
                && isOver(mouseX, mouseY, winX + PAD, top + 62, 12, 12)) {
            toggleNaturalHostility();
            return true;
        }
        // Галочки кастом-групп (видны только при creature_type == custom).
        if (creatureType() == CreatureType.CUSTOM) {
            MobGroup[] sel = MobGroup.SELECTABLE;
            for (int i = 0; i < sel.length; i++) {
                int cx = (winX + PAD) + (i % 2) * 150;
                int cy = top + 40 + (i / 2) * 18;
                if (isOver(mouseX, mouseY, cx, cy, 12, 12)) {
                    toggleCustomAttacker(sel[i].id());
                    return true;
                }
            }
        }
        if (isOver(mouseX, mouseY, btnX, top + 94, BTN_W, 18)) {
            String next = NpcEditorScreen.MOBTYPE_IDS[(mobTypeIndex() + 1) % NpcEditorScreen.MOBTYPE_IDS.length];
            if ("undefined".equals(next)) {
                profileJson.remove("mob_type");
            } else {
                profileJson.addProperty("mob_type", next);
            }
            return true;
        }
        if (isOver(mouseX, mouseY, winX + WIN_W - PAD - 80, bottomY, 80, 18)) {
            onClose();
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
