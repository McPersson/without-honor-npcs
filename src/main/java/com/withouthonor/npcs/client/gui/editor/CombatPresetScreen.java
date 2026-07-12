package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CombatPresetScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 16;
    private static final String[] AGG_CATS =
            {"monsters", "animals", "villagers", "npcs", "players", "factions"};
    private static final String[] AGG_LABEL_KEYS =
            {"wh_npcs.ui.combat_preset.agg.monsters", "wh_npcs.ui.combat_preset.agg.animals",
                    "wh_npcs.ui.combat_preset.agg.villagers", "wh_npcs.ui.combat_preset.agg.npcs",
                    "wh_npcs.ui.combat_preset.agg.players", "wh_npcs.ui.combat_preset.agg.factions"};

    private final Screen parent;
    private final JsonObject profileJson;

    @Nullable
    private EditBox intervalBox;
    @Nullable
    private EditBox rangeBox;
    @Nullable
    private EditBox speedBox;
    @Nullable
    private EditBox leapBox;

    private int winX, winY, winW, winH;
    private int listTop, descTop, optTop, bottomY;
    @Nullable
    private String hoverTooltip;
    @Nullable
    private String intervalTipKey, rangeTipKey, speedTipKey, leapTipKey;
    @Nullable
    private String[] descCache;
    private int descCacheIdx = -1;

    public CombatPresetScreen(@Nullable Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.combat_preset.title"));
        this.parent = parent;
        this.profileJson = profileJson;
    }

    @Override
    protected int designW() {
        return 360;
    }

    @Override
    protected int designH() {
        return 360;
    }

    /** Резерв под описание пресета — под самое длинное (Стрелок, 4 строки), чтобы блок опций
     *  не «прыгал» при переключении пресетов (стабильная сетка). */
    private static final int DESC_RESERVE = 4 * 10 + 8;

    private void recalc() {
        winW = 360;
        winH = 360;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        listTop = winY + HEADER_H + 8;
        descTop = listTop + NpcEditorScreen.PRESET_IDS.length * ROW_H + 8;
        optTop = descTop + DESC_RESERVE;
        bottomY = winY + winH - PAD - 20;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        this.intervalBox = null;
        this.rangeBox = null;
        this.speedBox = null;
        this.leapBox = null;
        this.intervalTipKey = null;
        this.rangeTipKey = null;
        this.speedTipKey = null;
        this.leapTipKey = null;

        int by = optTop + 14 + 3 * 16 + 16 + 4;
        String p = preset();
        if ("bow".equals(p)) {
            intervalBox = numBox(winX + PAD + 78, by, 40, "ranged_interval_s", 1.25F, 0.3F, 10.0F);
            intervalTipKey = "wh_npcs.ui.combat_preset.tip.bow_interval";
            rangeBox = numBox(winX + PAD + 196, by, 40, "ranged_range", 16.0F, 4.0F, 32.0F);
            rangeTipKey = "wh_npcs.ui.combat_preset.tip.bow_range";
        } else if ("potion".equals(p)) {
            intervalBox = numBox(winX + PAD + 78, by, 40, "potion_interval_s", 3.0F, 0.5F, 10.0F);
            intervalTipKey = "wh_npcs.ui.combat_preset.tip.potion_interval";
            rangeBox = numBox(winX + PAD + 196, by, 40, "potion_range", 10.0F, 4.0F, 32.0F);
            rangeTipKey = "wh_npcs.ui.combat_preset.tip.potion_range";
        } else if ("melee".equals(p)) {
            speedBox = numBox(winX + PAD + 110, by, 40, "melee_chase_speed", 1.2F, 0.5F, 2.0F);
            speedTipKey = "wh_npcs.ui.combat_preset.tip.melee_speed";
            if (leapEnabled()) {
                leapBox = numBox(winX + PAD + 240, by, 40, "leap_strength", 0.4F, 0.1F, 1.0F);
                leapTipKey = "wh_npcs.ui.combat_preset.tip.leap_strength";
            }
        } else if ("shield".equals(p)) {
            intervalBox = numBox(winX + PAD + 96, by, 40, "shield_hold_s", 1.5F, 0.3F, 5.0F);
            intervalTipKey = "wh_npcs.ui.combat_preset.tip.shield_hold";
            rangeBox = numBox(winX + PAD + 214, by, 40, "shield_cooldown_s", 2.0F, 0.5F, 10.0F);
            rangeTipKey = "wh_npcs.ui.combat_preset.tip.shield_cooldown";
        }
    }

    private String[] descLines(int idx) {
        if (descCache == null || descCacheIdx != idx) {
            descCache = Component.translatable(NpcEditorScreen.PRESET_DESC_KEYS[idx]).getString().split("\n");
            descCacheIdx = idx;
        }
        return descCache;
    }

    @Nullable
    private String boxTip(@Nullable EditBox box, @Nullable String key, int mouseX, int mouseY) {
        if (box != null && key != null
                && isOver(mouseX, mouseY, box.getX(), box.getY(), box.getWidth(), box.getHeight())) {
            return Component.translatable(key).getString();
        }
        return null;
    }

    private boolean leapEnabled() {
        return profileJson.has("leap_at_target") && profileJson.get("leap_at_target").getAsBoolean();
    }

    private EditBox numBox(int x, int y, int w, String key, float def, float min, float max) {
        EditBox box = addRenderableWidget(new SelectableEditBox(font, x, y, w, 16, Component.literal(key)));
        box.setMaxLength(6);
        float value = profileJson.has(key) ? profileJson.get(key).getAsFloat() : def;
        box.setValue(value == (int) value ? String.valueOf((int) value) : String.valueOf(value));
        box.setResponder(v -> {
            try {
                float parsed = Math.max(min, Math.min(max, Float.parseFloat(v.trim().replace(',', '.'))));
                profileJson.addProperty(key, parsed);
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    private String preset() {
        return profileJson.has("combat_preset") ? profileJson.get("combat_preset").getAsString() : "passive";
    }

    private String aggStr() {
        return profileJson.has("aggressor_targets")
                ? profileJson.get("aggressor_targets").getAsString() : "";
    }

    private int presetIdx() {
        String p = preset();
        for (int i = 0; i < NpcEditorScreen.PRESET_IDS.length; i++) {
            if (NpcEditorScreen.PRESET_IDS[i].equals(p)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        int idx = presetIdx();
        for (int i = 0; i < NpcEditorScreen.PRESET_IDS.length; i++) {
            int y = listTop + i * ROW_H;
            boolean isCur = i == idx;
            boolean hovered = isOver(mouseX, mouseY, winX + 6, y, winW - 12, ROW_H);
            if (isCur || hovered) {
                g.fill(winX + 6, y, winX + winW - 6, y + ROW_H, isCur ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            g.drawString(font, (isCur ? "§e" : "§b")
                    + Component.translatable(NpcEditorScreen.PRESET_NAME_KEYS[i]).getString(),
                    winX + 12, y + 4, VanillaUIHelper.TEXT_WHITE, false);
        }

        g.fill(winX + 6, descTop - 5, winX + winW - 6, descTop - 4, 0xFF373737);
        String[] desc = descLines(idx);
        for (int line = 0; line < desc.length; line++) {
            g.drawString(font, "§7" + desc[line], winX + PAD, descTop + line * 10, VanillaUIHelper.TEXT_WHITE, false);
        }

        // Разделитель между фиксированной полосой описания и блоком опций (визуальный ритм сетки).
        g.fill(winX + 6, optTop - 6, winX + winW - 6, optTop - 5, 0xFF373737);
        String tooltip = renderOptions(g, NpcEditorScreen.PRESET_IDS[idx], mouseX, mouseY);
        if (tooltip == null) {
            tooltip = boxTip(intervalBox, intervalTipKey, mouseX, mouseY);
        }
        if (tooltip == null) {
            tooltip = boxTip(rangeBox, rangeTipKey, mouseX, mouseY);
        }
        if (tooltip == null) {
            tooltip = boxTip(speedBox, speedTipKey, mouseX, mouseY);
        }
        if (tooltip == null) {
            tooltip = boxTip(leapBox, leapTipKey, mouseX, mouseY);
        }

        // Провокация/дружественный огонь — общий блок для всех боевых пресетов, в отдельном экране.
        // При мирном пресете кнопка не прячется, а серится (клик тоже заблокирован — симметрично).
        boolean provokeActive = !"passive".equals(NpcEditorScreen.PRESET_IDS[idx]);
        boolean provokeHover = isOver(mouseX, mouseY, winX + PAD, bottomY, 110, 18);
        VanillaUIHelper.drawSmallButton(g, font,
                Component.translatable("wh_npcs.ui.combat_preset.provoke_btn").getString(),
                winX + PAD, bottomY, 110, provokeActive && provokeHover,
                provokeActive ? VanillaUIHelper.TEXT_AQUA : VanillaUIHelper.TEXT_DARK_GRAY);
        if (provokeHover) {
            tooltip = Component.translatable(provokeActive
                    ? "wh_npcs.ui.combat_preset.provoke_btn_tip"
                    : "wh_npcs.ui.combat_preset.provoke_btn_passive_tip").getString();
        }
        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        hoverTooltip = tooltip;
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    @Nullable
    private String renderOptions(GuiGraphics g, String preset, int mouseX, int mouseY) {
        int x = winX + PAD;
        if ("passive".equals(preset)) {
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.passive_note").getString(),
                    x, optTop + 4, VanillaUIHelper.TEXT_WHITE, false);
            return null;
        }
        String tip = null;

        g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.whom").getString(), x, optTop, VanillaUIHelper.TEXT_GRAY, false);
        java.util.Set<String> cats =
                com.withouthonor.npcs.common.profile.CompanionProfile.parseAggressorTargets(aggStr());
        for (int i = 0; i < AGG_CATS.length; i++) {
            int cx = x + (i < 3 ? 0 : 150);
            int cy = optTop + 14 + (i < 3 ? i : i - 3) * 16;
            boolean ch = isOver(mouseX, mouseY, cx, cy, 12, 12);
            VanillaUIHelper.drawButton(g, cx, cy, 12, 12, ch);
            if (cats.contains(AGG_CATS[i])) {
                VanillaUIHelper.drawCheck(g, cx + 1, cy + 2, VanillaUIHelper.TEXT_GREEN);
            }
            g.drawString(font, Component.translatable(AGG_LABEL_KEYS[i]).getString(), cx + 16, cy + 2, VanillaUIHelper.TEXT_GRAY, false);
            if (ch || isOver(mouseX, mouseY, cx + 16, cy, 130, 12)) {
                if ("factions".equals(AGG_CATS[i])) {
                    tip = Component.translatable("wh_npcs.ui.combat_preset.tip.factions").getString();
                } else if ("players".equals(AGG_CATS[i])) {
                    tip = Component.translatable("wh_npcs.ui.combat_preset.tip.players").getString();
                }
            }
        }

        int creeperY = optTop + 14 + 3 * 16;
        if (cats.contains("monsters")) {
            boolean spare = profileJson.has("guard_spare_creepers")
                    && profileJson.get("guard_spare_creepers").getAsBoolean();
            boolean h = isOver(mouseX, mouseY, x, creeperY, 12, 12);
            VanillaUIHelper.drawButton(g, x, creeperY, 12, 12, h);
            if (spare) {
                VanillaUIHelper.drawCheck(g, x + 1, creeperY + 2, VanillaUIHelper.TEXT_GREEN);
            }
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.spare_creepers").getString(), x + 16, creeperY + 2, VanillaUIHelper.TEXT_GRAY, false);
            if (h || isOver(mouseX, mouseY, x + 16, creeperY, 140, 12)) {
                tip = Component.translatable("wh_npcs.ui.combat_preset.tip.spare_creepers").getString();
            }
        }

        int by = creeperY + 16 + 4;
        if ("bow".equals(preset)) {
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.shot_s").getString(), x, by + 4, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.range_short").getString(), x + 126, by + 4, VanillaUIHelper.TEXT_GRAY, false);
            int ny = by + 24;
            for (String line : Component.translatable("wh_npcs.ui.combat_preset.arrows_note").getString().split("\n")) {
                g.drawString(font, line, x, ny, VanillaUIHelper.TEXT_WHITE, false);
                ny += 10;
            }
        } else if ("potion".equals(preset)) {
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.throw_s").getString(), x, by + 4, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.range_short").getString(), x + 126, by + 4, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.potions_note").getString(),
                    x, by + 24, VanillaUIHelper.TEXT_WHITE, false);
        } else if ("melee".equals(preset)) {
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.chase_speed").getString(), x, by + 4, VanillaUIHelper.TEXT_GRAY, false);
            if (leapEnabled()) {
                g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.leap").getString(), x + 190, by + 4, VanillaUIHelper.TEXT_GRAY, false);
            } else {
                g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.leap_note").getString(),
                        x, by + 24, VanillaUIHelper.TEXT_WHITE, false);
            }
        } else if ("shield".equals(preset)) {
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.shield_hold").getString(), x, by + 4, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.shield_cooldown").getString(), x + 144, by + 4, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.combat_preset.shield_note").getString(),
                    x, by + 24, VanillaUIHelper.TEXT_WHITE, false);
        }
        return tip;
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0) {
            for (int i = 0; i < NpcEditorScreen.PRESET_IDS.length; i++) {
                int y = listTop + i * ROW_H;
                if (isOver(mouseX, mouseY, winX + 6, y, winW - 12, ROW_H)) {
                    profileJson.addProperty("combat_preset", NpcEditorScreen.PRESET_IDS[i]);
                    init(minecraft, width, height);
                    return true;
                }
            }
            String preset = preset();
            if (!"passive".equals(preset)) {

                for (int i = 0; i < AGG_CATS.length; i++) {
                    int cx = winX + PAD + (i < 3 ? 0 : 150);
                    int cy = optTop + 14 + (i < 3 ? i : i - 3) * 16;
                    if (isOver(mouseX, mouseY, cx, cy, 12, 12)) {
                        java.util.Set<String> cats =
                                com.withouthonor.npcs.common.profile.CompanionProfile.parseAggressorTargets(aggStr());
                        if (!cats.add(AGG_CATS[i])) {
                            cats.remove(AGG_CATS[i]);
                        }
                        profileJson.addProperty("aggressor_targets", String.join(",", cats));
                        init(minecraft, width, height);
                        return true;
                    }
                }

                java.util.Set<String> cats =
                        com.withouthonor.npcs.common.profile.CompanionProfile.parseAggressorTargets(aggStr());
                int creeperY = optTop + 14 + 3 * 16;
                if (cats.contains("monsters") && isOver(mouseX, mouseY, winX + PAD, creeperY, 12, 12)) {
                    if (profileJson.has("guard_spare_creepers")
                            && profileJson.get("guard_spare_creepers").getAsBoolean()) {
                        profileJson.remove("guard_spare_creepers");
                    } else {
                        profileJson.addProperty("guard_spare_creepers", true);
                    }
                    return true;
                }
            }
            // Кнопка «Провокация…» рисуется всегда; при мирном пресете клик глотаем (кнопка серая).
            if (isOver(mouseX, mouseY, winX + PAD, bottomY, 110, 18)) {
                if (!"passive".equals(preset) && minecraft != null) {
                    minecraft.setScreen(new ProvocationScreen(this, profileJson));
                }
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
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void multilineTooltip(GuiGraphics g, String text, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        queueTooltip(lines);
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        VanillaUIHelper.drawSmallButton(g, font, label, x, y, w, isOver(mouseX, mouseY, x, y, w, 18), color);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
