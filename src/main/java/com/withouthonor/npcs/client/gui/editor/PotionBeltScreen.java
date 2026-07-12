package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PotionBeltScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int SEC_H = 40;
    private static final int SLOTS = 3;
    private static final int WIN_W = 340;
    private static final int WIN_H = 300;

    private static final String[] CAT_KEY = {"potion_self", "potion_enemy", "potion_ally"};
    private static final String[] CAT_OFF = {"potion_self_off", "potion_enemy_off", "potion_ally_off"};
    private static final String[] CAT_LABEL_KEYS = {
            "wh_npcs.ui.potion_belt.cat_self", "wh_npcs.ui.potion_belt.cat_enemy", "wh_npcs.ui.potion_belt.cat_ally"};
    private static final int[] CAT_BG = {0x2655FF55, 0x26FF5555, 0x265599FF};
    private static final int[] CAT_COLOR = {VanillaUIHelper.TEXT_GREEN, VanillaUIHelper.TEXT_RED, VanillaUIHelper.TEXT_AQUA};

    private final Screen parent;
    private final JsonObject profileJson;
    private final ItemStack[][] belt = new ItemStack[3][SLOTS];
    private final boolean[] enabled = {true, true, true};

    private final boolean[] seq = {false, false, false};
    private boolean selfCombat;
    private ItemStack combatBuff = ItemStack.EMPTY;
    private ItemStack carried = ItemStack.EMPTY;

    private static final String[] CAT_TIP_KEYS = {
            "wh_npcs.ui.potion_belt.tip_self",
            "wh_npcs.ui.potion_belt.tip_enemy",
            "wh_npcs.ui.potion_belt.tip_ally",
    };

    private int winX, winY, winW, winH;
    private int sectionsTop, invY, bottomY;

    /**
     * Правое выравнивание кластера ряда: [?][☑ an][☑ im Kampf/der Reihe nach]. Возвращает x кнопок
     * {?, «вкл», комбо}. Комбо-метка прижата к правому краю секции — адаптивно под длину перевода (DE/CJK).
     */
    private String ellipsize(String s, int maxW) {
        if (font.width(s) <= maxW) {
            return s;
        }
        if (maxW <= 0) {
            return "";
        }
        return font.plainSubstrByWidth(s, Math.max(0, maxW - font.width("…"))) + "…";
    }

    /**
     * Колонки правого кластера [?][☑ an][☑ im Kampf/der Reihe nach] — ОДИНАКОВЫЕ для всех рядов
     * (чекбоксы не прыгают по X между строками). Комбо-колонка отступает от правого края на самую
     * длинную из меток («im Kampf»/«der Reihe nach»), так что и она влезает, и колонки выровнены.
     */
    private int[] clusterX() {
        int right = winX + winW - PAD - 4;
        int comboW = Math.max(
                font.width(Component.translatable("wh_npcs.ui.potion_belt.in_combat").getString()),
                font.width(Component.translatable("wh_npcs.ui.potion_belt.cycle").getString()));
        int comboX = right - comboW - 15;
        int onW = font.width(Component.translatable("wh_npcs.ui.potion_belt.on").getString());
        int enX = comboX - 16 - onW - 15;
        int qX = enX - 10 - 11;
        return new int[]{qX, enX, comboX};
    }
    private ItemStack hoverTip = ItemStack.EMPTY;
    @Nullable
    private String hoverCatTip;

    public PotionBeltScreen(@Nullable Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.potion_belt.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        for (int c = 0; c < 3; c++) {
            for (int i = 0; i < SLOTS; i++) {
                belt[c][i] = ItemStack.EMPTY;
            }
            if (profileJson.has(CAT_KEY[c])) {
                JsonArray arr = profileJson.getAsJsonArray(CAT_KEY[c]);
                for (int i = 0; i < SLOTS && i < arr.size(); i++) {
                    belt[c][i] = fromSpec(arr.get(i).getAsJsonObject());
                }
            }
            enabled[c] = !profileJson.has(CAT_OFF[c]);
        }
        selfCombat = profileJson.has("potion_self_combat");
        seq[1] = profileJson.has("potion_enemy_seq");
        seq[2] = profileJson.has("potion_ally_seq");
        if (profileJson.has("potion_combat_buff")) {
            JsonArray a = profileJson.getAsJsonArray("potion_combat_buff");
            if (!a.isEmpty()) {
                combatBuff = fromSpec(a.get(0).getAsJsonObject());
            }
        }
    }

    private static ItemStack fromSpec(JsonObject spec) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(spec.get("item").getAsString()));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, spec.has("count") ? spec.get("count").getAsInt() : 1);
        if (spec.has("nbt")) {
            try {
                stack.setTag(net.minecraft.nbt.TagParser.parseTag(spec.get("nbt").getAsString()));
            } catch (Exception ignored) {
            }
        }
        return stack;
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
        sectionsTop = winY + HEADER_H + 34;
        invY = sectionsTop + 3 * SEC_H + 14;
        bottomY = winY + winH - PAD - 20;
    }

    private int slotX(int i) {
        return winX + PAD + 4 + i * 22;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.potion_belt.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        g.drawString(font, Component.translatable("wh_npcs.ui.potion_belt.note1").getString(),
                winX + PAD, winY + HEADER_H + 6, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.potion_belt.note2").getString(),
                winX + PAD, winY + HEADER_H + 18, VanillaUIHelper.TEXT_GRAY, false);

        hoverTip = ItemStack.EMPTY;
        hoverCatTip = null;
        for (int c = 0; c < 3; c++) {
            int secY = sectionsTop + c * SEC_H;

            g.fill(winX + PAD, secY - 3, winX + winW - PAD, secY + SEC_H - 8, CAT_BG[c]);

            int[] cx = clusterX();
            int qX = cx[0], enX = cx[1], comboX = cx[2];
            // Правый кластер — по вертикальному центру цветовой полосы (secY-3 .. secY+SEC_H-8), не у верха.
            int cy = secY - 3 + (SEC_H - 5 - 12) / 2; // центр 12px-чекбокса в полосе высотой SEC_H-5
            // Подпись категории не заезжает на правый кластер — режем «…» до qX.
            g.drawString(font, ellipsize(Component.translatable(CAT_LABEL_KEYS[c]).getString(),
                            qX - (winX + PAD + 4) - 6), winX + PAD + 4, secY,
                    enabled[c] ? CAT_COLOR[c] : VanillaUIHelper.TEXT_DARK_GRAY, false);
            boolean qHover = isOver(mouseX, mouseY, qX, cy, 11, 11);
            VanillaUIHelper.drawButton(g, qX, cy, 11, 11, qHover);
            g.drawCenteredString(font, "?", qX + 5, cy + 2,
                    qHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            if (qHover) {
                hoverCatTip = Component.translatable(CAT_TIP_KEYS[c]).getString();
            }

            boolean enHover = isOver(mouseX, mouseY, enX, cy, 12, 12);
            VanillaUIHelper.drawButton(g, enX, cy, 12, 12, enHover);
            if (enabled[c]) {
                VanillaUIHelper.drawCheck(g, enX + 1, cy + 2, VanillaUIHelper.TEXT_GREEN);
            }
            g.drawString(font, Component.translatable("wh_npcs.ui.potion_belt.on").getString(), enX + 15, cy + 2, VanillaUIHelper.TEXT_GRAY, false);

            boolean comboHover = isOver(mouseX, mouseY, comboX, cy, 12, 12);
            VanillaUIHelper.drawButton(g, comboX, cy, 12, 12, comboHover);
            if (c == 0 ? selfCombat : seq[c]) {
                VanillaUIHelper.drawCheck(g, comboX + 1, cy + 2, VanillaUIHelper.TEXT_GREEN);
            }
            g.drawString(font, Component.translatable(c == 0 ? "wh_npcs.ui.potion_belt.in_combat"
                            : "wh_npcs.ui.potion_belt.cycle").getString(),
                    comboX + 15, cy + 2, VanillaUIHelper.TEXT_GRAY, false);

            for (int i = 0; i < SLOTS; i++) {
                int x = slotX(i);
                boolean hov = isOver(mouseX, mouseY, x, secY + 14, 18, 18);
                VanillaUIHelper.drawItemSlot(g, x, secY + 14, hov);
                if (!belt[c][i].isEmpty()) {
                    g.renderItem(belt[c][i], x + 1, secY + 15);
                    g.renderItemDecorations(font, belt[c][i], x + 1, secY + 15);
                    if (hov && carried.isEmpty()) {
                        hoverTip = belt[c][i];
                    }
                }
            }

            if (c == 0) {
                int bx = buffSlotX();
                boolean bHov = isOver(mouseX, mouseY, bx, secY + 14, 18, 18);
                VanillaUIHelper.drawItemSlot(g, bx, secY + 14, bHov);
                drawGoldFrame(g, bx, secY + 14);
                if (!combatBuff.isEmpty()) {
                    g.renderItem(combatBuff, bx + 1, secY + 15);
                    g.renderItemDecorations(font, combatBuff, bx + 1, secY + 15);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.potion_belt.buff").getString(), bx + 22, secY + 18, VanillaUIHelper.TEXT_GOLD, false);
                if (bHov) {
                    hoverCatTip = Component.translatable("wh_npcs.ui.potion_belt.buff_tip").getString();
                    if (carried.isEmpty() && !combatBuff.isEmpty()) {
                        hoverTip = combatBuff;
                    }
                }
            }
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.potion_belt.inventory").getString(),
                winX + PAD, invY - 11, VanillaUIHelper.TEXT_GRAY, false);
        if (minecraft != null && minecraft.player != null) {
            for (int i = 0; i < 36; i++) {
                int x = winX + PAD + (i % 9) * 18;
                int sy = invY + (i / 9) * 18;
                boolean hov = isOver(mouseX, mouseY, x, sy, 18, 18);
                VanillaUIHelper.drawItemSlot(g, x, sy, hov);
                ItemStack stack = minecraft.player.getInventory().items.get(i);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, x + 1, sy + 1);
                    g.renderItemDecorations(font, stack, x + 1, sy + 1);
                    if (hov && carried.isEmpty()) {
                        hoverTip = stack;
                    }
                }
            }
        }

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!hoverTip.isEmpty()) {
            g.renderTooltip(font, hoverTip, mouseX, mouseY);
        } else if (hoverCatTip != null) {
            multilineTooltip(g, hoverCatTip, mouseX, mouseY);
        }
        if (!carried.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderItem(carried, mouseX - 8, mouseY - 8);
            g.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
            g.pose().popPose();
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        for (int c = 0; c < 3; c++) {
            int secY = sectionsTop + c * SEC_H;

            int[] cx = clusterX();
            int cy = secY - 3 + (SEC_H - 5 - 12) / 2;
            if (button == 0 && isOver(mouseX, mouseY, cx[1], cy, 12, 12)) {
                enabled[c] = !enabled[c];
                writeBack();
                return true;
            }

            if (button == 0 && isOver(mouseX, mouseY, cx[2], cy, 12, 12)) {
                if (c == 0) {
                    selfCombat = !selfCombat;
                } else {
                    seq[c] = !seq[c];
                }
                writeBack();
                return true;
            }

            for (int i = 0; i < SLOTS; i++) {
                if (isOver(mouseX, mouseY, slotX(i), secY + 14, 18, 18)) {
                    if (button == 1) {
                        belt[c][i] = carried.isEmpty() ? ItemStack.EMPTY : carried.copy();
                    } else if (button == 0) {
                        ItemStack tmp = belt[c][i];
                        belt[c][i] = carried;
                        carried = tmp;
                    }
                    writeBack();
                    return true;
                }
            }

            if (c == 0 && isOver(mouseX, mouseY, buffSlotX(), secY + 14, 18, 18)) {
                if (button == 1) {
                    combatBuff = carried.isEmpty() ? ItemStack.EMPTY : carried.copy();
                } else if (button == 0) {
                    ItemStack tmp = combatBuff;
                    combatBuff = carried;
                    carried = tmp;
                }
                writeBack();
                return true;
            }
        }

        if (button == 0 && minecraft != null && minecraft.player != null) {
            for (int i = 0; i < 36; i++) {
                int x = winX + PAD + (i % 9) * 18;
                int sy = invY + (i / 9) * 18;
                if (isOver(mouseX, mouseY, x, sy, 18, 18)) {
                    ItemStack stack = minecraft.player.getInventory().items.get(i);
                    if (!carried.isEmpty()) {

                        if (stack.isEmpty() && minecraft.gameMode != null
                                && minecraft.player.getAbilities().instabuild) {
                            minecraft.player.getInventory().setItem(i, carried.copy());
                            int menuSlot = i < 9 ? i + 36 : i;
                            minecraft.gameMode.handleCreativeModeItemAdd(
                                    minecraft.player.getInventory().getItem(i), menuSlot);
                            carried = ItemStack.EMPTY;
                        }
                    } else {
                        carried = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
                    }
                    return true;
                }
            }
        }
        if (button == 0 && isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
            onClose();
            return true;
        }
        if (!carried.isEmpty()) {
            carried = ItemStack.EMPTY;
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void writeBack() {
        for (int c = 0; c < 3; c++) {
            JsonArray arr = new JsonArray();
            for (ItemStack stack : belt[c]) {
                if (stack.isEmpty()) {
                    continue;
                }
                JsonObject spec = new JsonObject();
                var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                spec.addProperty("item", key != null ? key.toString() : "minecraft:potion");
                spec.addProperty("count", stack.getCount());
                if (stack.getTag() != null) {
                    spec.addProperty("nbt", stack.getTag().toString());
                }
                arr.add(spec);
            }
            if (arr.isEmpty()) {
                profileJson.remove(CAT_KEY[c]);
            } else {
                profileJson.add(CAT_KEY[c], arr);
            }
            if (enabled[c]) {
                profileJson.remove(CAT_OFF[c]);
            } else {
                profileJson.addProperty(CAT_OFF[c], true);
            }
        }
        if (selfCombat) {
            profileJson.addProperty("potion_self_combat", true);
        } else {
            profileJson.remove("potion_self_combat");
        }
        if (seq[1]) {
            profileJson.addProperty("potion_enemy_seq", true);
        } else {
            profileJson.remove("potion_enemy_seq");
        }
        if (seq[2]) {
            profileJson.addProperty("potion_ally_seq", true);
        } else {
            profileJson.remove("potion_ally_seq");
        }
        if (combatBuff.isEmpty()) {
            profileJson.remove("potion_combat_buff");
        } else {
            JsonObject spec = new JsonObject();
            var key = ForgeRegistries.ITEMS.getKey(combatBuff.getItem());
            spec.addProperty("item", key != null ? key.toString() : "minecraft:potion");
            spec.addProperty("count", combatBuff.getCount());
            if (combatBuff.getTag() != null) {
                spec.addProperty("nbt", combatBuff.getTag().toString());
            }
            JsonArray arr = new JsonArray();
            arr.add(spec);
            profileJson.add("potion_combat_buff", arr);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
    }

    private int buffSlotX() {
        return slotX(SLOTS - 1) + 18 + 14;
    }

    private static void drawGoldFrame(GuiGraphics g, int x, int y) {
        int c = VanillaUIHelper.TEXT_GOLD;
        g.fill(x - 1, y - 1, x + 19, y, c);
        g.fill(x - 1, y + 18, x + 19, y + 19, c);
        g.fill(x - 1, y, x, y + 18, c);
        g.fill(x + 18, y, x + 19, y + 18, c);
    }

    private void multilineTooltip(GuiGraphics g, String text, int mouseX, int mouseY) {
        java.util.List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
