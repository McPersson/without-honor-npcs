package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.action.Actions;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;
import com.withouthonor.npcs.common.trade.TradeOffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OffersEditorScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int PAGE_SIZE = 18;
    private static final int PAGES = 2;
    private static final int ROW_H = 22;

    private static class Draft {
        final ResourceLocation[] item = new ResourceLocation[3];
        final CompoundTag[] nbt = new CompoundTag[3];
        final int[] count = {1, 1, 1};
        int maxUses;
        boolean shared;
        int xp;
        final List<DialogueCondition> conditions = new ArrayList<>();

        boolean valid() {
            return item[0] != null && item[2] != null;
        }

        boolean empty() {
            return item[0] == null && item[1] == null && item[2] == null;
        }

        void clear() {
            for (int i = 0; i < 3; i++) {
                item[i] = null;
                nbt[i] = null;
                count[i] = 1;
            }
            maxUses = 0;
            shared = false;
            xp = 0;
            conditions.clear();
        }
    }

    private final Screen parent;
    private final JsonObject profileJson;
    private final Draft[] drafts = new Draft[PAGE_SIZE * PAGES];

    private int page;
    private int selected;
    private String draftRestock = "0";

    private ItemStack carried = ItemStack.EMPTY;

    private int carriedInvSlot = -1;

    private boolean confirmRestock;

    private EditBox maxUsesBox;
    private EditBox xpBox;
    private EditBox restockBox;

    @Nullable
    private String hoverTooltip;

    private int winX, winY, winW, winH;
    private int bottomY;

    public OffersEditorScreen(Screen parent, JsonObject profileJson) {
        super(Component.translatable("wh_npcs.ui.offers.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        for (int i = 0; i < drafts.length; i++) {
            drafts[i] = new Draft();
        }
        int index = 0;
        if (profileJson.has("offers")) {
            for (JsonElement e : profileJson.getAsJsonArray("offers")) {
                if (index >= drafts.length) {
                    break;
                }
                try {
                    fillDraft(drafts[index++], TradeOffer.fromJson(e.getAsJsonObject()));
                } catch (Exception ex) {
                    WHCompanions.LOGGER.warn("Bad offer in profile json, skipped", ex);
                }
            }
        }
        if (profileJson.has("restock_minutes")) {
            draftRestock = String.valueOf(profileJson.get("restock_minutes").getAsInt());
        }
    }

    private static void fillDraft(Draft draft, TradeOffer offer) {
        draft.item[0] = offer.costA().itemId();
        draft.nbt[0] = offer.costA().nbt();
        draft.count[0] = offer.costA().count();
        if (offer.costB() != null) {
            draft.item[1] = offer.costB().itemId();
            draft.nbt[1] = offer.costB().nbt();
            draft.count[1] = offer.costB().count();
        }
        draft.item[2] = offer.result().itemId();
        draft.nbt[2] = offer.result().nbt();
        draft.count[2] = offer.result().count();
        draft.maxUses = offer.maxUses();
        draft.shared = offer.sharedLimit();
        draft.xp = offer.playerXp();
        draft.conditions.addAll(offer.conditions());
    }

    private static final int WIN_W = 520;
    private static final int WIN_H = 330;

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
        bottomY = winY + winH - PAD - 20;
    }

    private int cellX(int cell) {
        return winX + 14 + (cell / 6) * 166;
    }

    private int cellY(int cell) {
        return winY + 32 + (cell % 6) * ROW_H;
    }

    private int slotX(int cell, int slot) {
        return cellX(cell) + 24 + slot * 34;
    }

    private int invX() {
        return winX + 14;
    }

    private int invY() {
        return winY + 188;
    }

    private int panelX() {
        return winX + 200;
    }

    private int panelY() {
        return winY + 178;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();

        Draft draft = drafts[selected];
        maxUsesBox = addRenderableWidget(new SelectableEditBox(font, panelX() + 50, panelY() + 14, 40, 16,
                Component.translatable("wh_npcs.ui.offers.limit")));
        maxUsesBox.setMaxLength(4);
        maxUsesBox.setValue(String.valueOf(draft.maxUses));
        maxUsesBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.offers.limit.tip")));
        maxUsesBox.setResponder(v -> {
            try {
                draft.maxUses = Math.max(0, Integer.parseInt(v.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        xpBox = addRenderableWidget(new SelectableEditBox(font, panelX() + 50, panelY() + 36, 40, 16,
                Component.translatable("wh_npcs.ui.offers.xp")));
        xpBox.setMaxLength(4);
        xpBox.setValue(String.valueOf(draft.xp));
        xpBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.offers.xp.tip")));
        xpBox.setResponder(v -> {
            try {
                draft.xp = Math.max(0, Integer.parseInt(v.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        restockBox = addRenderableWidget(new SelectableEditBox(font, panelX() + 196, panelY() + 58, 44, 16,
                Component.translatable("wh_npcs.ui.offers.restock")));
        restockBox.setMaxLength(5);
        restockBox.setValue(draftRestock);
        restockBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.offers.restock.tip")));
        restockBox.setResponder(v -> draftRestock = v);
    }

    private void writeBack() {
        JsonArray array = new JsonArray();
        for (Draft draft : drafts) {
            if (!draft.valid()) {
                continue;
            }
            array.add(new TradeOffer(
                    new Actions.ItemSpec(draft.item[0], Math.max(1, draft.count[0]), draft.nbt[0]),
                    draft.item[1] != null
                            ? new Actions.ItemSpec(draft.item[1], Math.max(1, draft.count[1]), draft.nbt[1])
                            : null,
                    new Actions.ItemSpec(draft.item[2], Math.max(1, draft.count[2]), draft.nbt[2]),
                    draft.maxUses, draft.shared, draft.xp, List.copyOf(draft.conditions)).toJson());
        }
        if (array.isEmpty()) {
            profileJson.remove("offers");
        } else {
            profileJson.add("offers", array);
        }
        int restock = 0;
        try {
            restock = Math.max(0, Integer.parseInt(draftRestock.trim()));
        } catch (NumberFormatException ignored) {
        }
        if (restock > 0) {
            profileJson.addProperty("restock_minutes", restock);
        } else {
            profileJson.remove("restock_minutes");
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.offers.header").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        renderPageNav(g, mouseX, mouseY);

        hoverTooltip = null;

        for (int cell = 0; cell < PAGE_SIZE; cell++) {
            int index = page * PAGE_SIZE + cell;
            Draft draft = drafts[index];
            int y = cellY(cell);
            boolean rowHover = isOver(mouseX, mouseY, cellX(cell), y - 2, 156, ROW_H);
            if (index == selected || rowHover) {
                g.fill(cellX(cell) - 2, y - 2, cellX(cell) + 154, y + ROW_H - 2,
                        index == selected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            String number = (index + 1) + ".";
            g.drawString(font, number, cellX(cell) + 18 - font.width(number), y + 5,
                    index == selected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY, false);
            g.drawString(font, "+", slotX(cell, 0) + 23, y + 5, VanillaUIHelper.TEXT_DARK_GRAY, false);
            g.drawString(font, "=", slotX(cell, 1) + 23, y + 5, VanillaUIHelper.TEXT_AQUA, false);
            for (int s = 0; s < 3; s++) {
                boolean slotHover = isOver(mouseX, mouseY, slotX(cell, s), y, 18, 18);
                VanillaUIHelper.drawItemSlot(g, slotX(cell, s), y, slotHover);
                if (draft.item[s] != null) {
                    ItemStack stack = ghostStack(draft, s);
                    g.renderItem(stack, slotX(cell, s) + 1, y + 1);
                    g.renderItemDecorations(font, stack, slotX(cell, s) + 1, y + 1);
                    if (slotHover && carried.isEmpty()) {
                        g.renderTooltip(font, stack, mouseX, mouseY);
                    }
                }
            }

            if (!draft.empty() && !draft.valid()) {
                g.drawString(font, "!", cellX(cell) + 130, y + 5, 0xFFFF5555, false);
                if (isOver(mouseX, mouseY, cellX(cell) + 128, y + 3, 9, 12)) {
                    hoverTooltip = Component.translatable("wh_npcs.ui.offers.invalid.tip").getString();
                }
            } else if (!draft.conditions.isEmpty() || draft.maxUses > 0 || draft.xp > 0) {
                g.drawString(font, "•", cellX(cell) + 130, y + 5, VanillaUIHelper.TEXT_GOLD, false);
                if (isOver(mouseX, mouseY, cellX(cell) + 128, y + 3, 9, 12)) {
                    hoverTooltip = Component.translatable("wh_npcs.ui.offers.has_settings.tip").getString();
                }
            }
        }

        VanillaUIHelper.drawSeparator(g, winX + PAD, winY + 168, winW - PAD * 2);
        g.drawString(font, Component.translatable("wh_npcs.ui.offers.inventory").getString(), invX(), invY() - 10, VanillaUIHelper.TEXT_GRAY, false);
        if (minecraft != null && minecraft.player != null) {
            for (int i = 0; i < 36; i++) {
                int x = invX() + (i % 9) * 18;
                int sy = invY() + (i / 9) * 18;
                boolean hovered = isOver(mouseX, mouseY, x, sy, 18, 18);
                VanillaUIHelper.drawItemSlot(g, x, sy, hovered);
                ItemStack stack = minecraft.player.getInventory().items.get(i);

                boolean liftedFromHere = i == carriedInvSlot && !carried.isEmpty();
                if (!stack.isEmpty() && !liftedFromHere) {
                    g.renderItem(stack, x + 1, sy + 1);
                    g.renderItemDecorations(font, stack, x + 1, sy + 1);
                    if (hovered && carried.isEmpty()) {
                        g.renderTooltip(font, stack, mouseX, mouseY);
                    }
                }
            }
        }

        Draft sel = drafts[selected];
        g.drawString(font, Component.translatable("wh_npcs.ui.offers.offer_n", selected + 1).getString(), panelX(), panelY() + 2,
                VanillaUIHelper.TEXT_YELLOW, false);
        boolean clearHover = isOver(mouseX, mouseY, panelX() + 174, panelY(), 66, 14);
        if (!sel.empty()) {
            int clearColor = clearHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_DARK_GRAY;
            g.drawString(font, "✕", panelX() + 174, panelY() + 2, clearColor, false);
            g.drawString(font, Component.translatable("wh_npcs.ui.offers.clear").getString(), panelX() + 186, panelY() + 2, clearColor, false);
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.offers.limit_label").getString(), panelX(), panelY() + 18, VanillaUIHelper.TEXT_GRAY, false);

        boolean modeHover = isOver(mouseX, mouseY, panelX() + 94, panelY() + 13, 84, 18);
        VanillaUIHelper.drawButton(g, panelX() + 94, panelY() + 13, 84, 18, modeHover);
        g.drawCenteredString(font, Component.translatable(sel.shared
                        ? "wh_npcs.ui.offers.limit.all" : "wh_npcs.ui.offers.limit.player").getString(), panelX() + 136, panelY() + 18,
                modeHover ? VanillaUIHelper.TEXT_YELLOW
                        : (sel.shared ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_AQUA));
        if (modeHover) {
            hoverTooltip = Component.translatable("wh_npcs.ui.offers.limit.mode.tip", Math.max(1, sel.maxUses)).getString();
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.offers.xp_label").getString(), panelX(), panelY() + 40, VanillaUIHelper.TEXT_GRAY, false);
        int n = sel.conditions.size();

        boolean condHover = isOver(mouseX, mouseY, panelX() + 94, panelY() + 35, 84, 18);
        VanillaUIHelper.drawButton(g, panelX() + 94, panelY() + 35, 84, 18, condHover);
        g.drawCenteredString(font, n > 0
                        ? Component.translatable("wh_npcs.ui.offers.cond_count", n).getString()
                        : Component.translatable("wh_npcs.ui.offers.cond").getString(), panelX() + 136, panelY() + 40,
                condHover ? VanillaUIHelper.TEXT_YELLOW
                        : (n > 0 ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_WHITE));
        if (condHover) {
            hoverTooltip = Component.translatable("wh_npcs.ui.offers.cond.tip").getString();
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.offers.restock_label").getString(), panelX(), panelY() + 62,
                VanillaUIHelper.TEXT_GRAY, false);
        boolean restockHover = isOver(mouseX, mouseY, panelX(), panelY() + 80, 110, 18);
        VanillaUIHelper.drawButton(g, panelX(), panelY() + 80, 110, 18, restockHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.offers.restock_now").getString(), panelX() + 55, panelY() + 85,
                restockHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (restockHover) {
            hoverTooltip = Component.translatable("wh_npcs.ui.offers.restock_now.tip").getString();
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.offers.hint.count").getString(),
                winX + PAD, bottomY - 7, VanillaUIHelper.TEXT_WHITE, false);
        g.drawString(font, Component.translatable("wh_npcs.ui.offers.hint.slot").getString(),
                winX + PAD, bottomY + 5, VanillaUIHelper.TEXT_WHITE, false);
        drawSmall(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }

        if (!carried.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderItem(carried, mouseX - 8, mouseY - 8);
            g.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
            g.pose().popPose();
        }
        if (confirmRestock) {
            renderConfirmRestock(g, mouseX, mouseY);
        }
    }

    private void renderConfirmRestock(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        int w = 240;
        int h = 78;
        int x = winX + (winW - w) / 2;
        int y = winY + (winH - h) / 2;
        VanillaUIHelper.drawWindow(g, x, y, w, h);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.offers.confirm_restock.title").getString(), x + w / 2, y + 10, VanillaUIHelper.TEXT_YELLOW);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.offers.confirm_restock.body").getString(), x + w / 2, y + 26, VanillaUIHelper.TEXT_WHITE);
        boolean okHover = isOver(mouseX, mouseY, x + w / 2 - 86, y + h - 28, 80, 18);
        boolean cancelHover = isOver(mouseX, mouseY, x + w / 2 + 6, y + h - 28, 80, 18);
        VanillaUIHelper.drawButton(g, x + w / 2 - 86, y + h - 28, 80, 18, okHover);
        VanillaUIHelper.drawButton(g, x + w / 2 + 6, y + h - 28, 80, 18, cancelHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.offers.confirm_restock.ok").getString(), x + w / 2 - 46, y + h - 23,
                okHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.cancel").getString(), x + w / 2 + 46, y + h - 23,
                cancelHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        g.pose().popPose();
    }

    private void renderPageNav(GuiGraphics g, int mouseX, int mouseY) {
        int x = winX + winW - PAD - 92;
        boolean prevHover = isOver(mouseX, mouseY, x, winY + 3, 16, 16);
        boolean nextHover = isOver(mouseX, mouseY, x + 76, winY + 3, 16, 16);
        VanillaUIHelper.drawButton(g, x, winY + 3, 16, 16, prevHover);
        VanillaUIHelper.drawButton(g, x + 76, winY + 3, 16, 16, nextHover);
        g.drawCenteredString(font, "<", x + 8, winY + 7,
                prevHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, ">", x + 84, winY + 7,
                nextHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.offers.page", page + 1, PAGES).getString(), x + 46, winY + 7,
                VanillaUIHelper.TEXT_GRAY);
    }

    private ItemStack ghostStack(Draft draft, int slot) {
        Item item = ForgeRegistries.ITEMS.getValue(draft.item[slot]);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, Math.max(1, draft.count[slot]));
        if (draft.nbt[slot] != null) {
            stack.setTag(draft.nbt[slot].copy());
        }
        return stack;
    }

    private void multilineTooltip(GuiGraphics g, String text, int x, int y) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, x, y);
    }

    private void drawSmall(GuiGraphics g, String label, int x, int y, int w,
                           int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, font.plainSubstrByWidth(label, w - 6), x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();

        if (confirmRestock) {
            if (button == 0) {
                int w = 240;
                int h = 78;
                int x = winX + (winW - w) / 2;
                int y = winY + (winH - h) / 2;
                if (isOver(mouseX, mouseY, x + w / 2 - 86, y + h - 28, 80, 18)) {
                    try {
                        java.util.UUID id = java.util.UUID.fromString(profileJson.get("id").getAsString());
                        com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                                new com.withouthonor.npcs.network.RestockTradesPacket(id));
                    } catch (Exception ignored) {
                    }
                    confirmRestock = false;
                } else if (isOver(mouseX, mouseY, x + w / 2 + 6, y + h - 28, 80, 18)) {
                    confirmRestock = false;
                }
            }
            return true;
        }

        for (int cell = 0; cell < PAGE_SIZE; cell++) {
            int index = page * PAGE_SIZE + cell;
            Draft draft = drafts[index];
            int y = cellY(cell);
            for (int s = 0; s < 3; s++) {
                if (isOver(mouseX, mouseY, slotX(cell, s), y, 18, 18)) {
                    selectOffer(index);
                    if (button == 1) {
                        if (carried.isEmpty()) {
                            draft.item[s] = null;
                            draft.nbt[s] = null;
                            draft.count[s] = 1;
                        } else {
                            depositOne(draft, s);
                        }
                    } else if (button == 0) {
                        ItemStack old = ghostStack(draft, s);
                        boolean had = draft.item[s] != null;
                        if (!carried.isEmpty()) {
                            putCarried(draft, s, true);
                        } else if (had) {
                            draft.item[s] = null;
                            draft.nbt[s] = null;
                            draft.count[s] = 1;
                        }
                        carried = had ? old : ItemStack.EMPTY;
                        carriedInvSlot = -1;
                    }
                    return true;
                }
            }
            if (button == 0 && isOver(mouseX, mouseY, cellX(cell) - 2, y - 2, 158, ROW_H)) {
                selectOffer(index);
                return true;
            }
        }
        if (button == 0) {

            int navX = winX + winW - PAD - 92;
            if (isOver(mouseX, mouseY, navX, winY + 3, 16, 16)) {
                page = (page + PAGES - 1) % PAGES;
                return true;
            }
            if (isOver(mouseX, mouseY, navX + 76, winY + 3, 16, 16)) {
                page = (page + 1) % PAGES;
                return true;
            }

            if (minecraft != null && minecraft.player != null) {
                for (int i = 0; i < 36; i++) {
                    int x = invX() + (i % 9) * 18;
                    int sy = invY() + (i / 9) * 18;
                    if (isOver(mouseX, mouseY, x, sy, 18, 18)) {
                        var items = minecraft.player.getInventory().items;
                        if (!carried.isEmpty()) {
                            if (carriedInvSlot >= 0 && carriedInvSlot != i) {
                                com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                                        new com.withouthonor.npcs.network.EditorMoveItemPacket(
                                                carriedInvSlot, i));
                                ItemStack moved = items.get(carriedInvSlot);
                                items.set(carriedInvSlot, items.get(i));
                                items.set(i, moved);
                            } else if (carriedInvSlot < 0 && items.get(i).isEmpty()
                                    && minecraft.gameMode != null
                                    && minecraft.player.getAbilities().instabuild) {

                                items.set(i, carried.copy());
                                int menuSlot = i < 9 ? i + 36 : i;
                                minecraft.gameMode.handleCreativeModeItemAdd(items.get(i), menuSlot);
                            }
                            carried = ItemStack.EMPTY;
                            carriedInvSlot = -1;
                        } else {
                            ItemStack stack = items.get(i);
                            carried = stack.copy();
                            carriedInvSlot = stack.isEmpty() ? -1 : i;
                        }
                        return true;
                    }
                }
            }

            Draft sel = drafts[selected];
            if (!sel.empty() && isOver(mouseX, mouseY, panelX() + 174, panelY(), 66, 14)) {
                sel.clear();
                init(minecraft, width, height);
                return true;
            }
            if (isOver(mouseX, mouseY, panelX() + 94, panelY() + 13, 84, 18)) {
                sel.shared = !sel.shared;
                return true;
            }
            if (isOver(mouseX, mouseY, panelX() + 94, panelY() + 35, 84, 18)) {
                ConditionsEditorScreen.openForConditions(this, sel.conditions);
                return true;
            }
            if (isOver(mouseX, mouseY, panelX(), panelY() + 80, 110, 18)) {
                confirmRestock = true;
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }
        }

        if (!carried.isEmpty()) {
            carried = ItemStack.EMPTY;
            carriedInvSlot = -1;
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void putCarried(Draft draft, int slot, boolean consume) {
        draft.item[slot] = ForgeRegistries.ITEMS.getKey(carried.getItem());
        draft.nbt[slot] = carried.getTag() != null ? carried.getTag().copy() : null;
        draft.count[slot] = Math.max(1, Math.min(64, carried.getCount()));
        if (consume) {
            carried = ItemStack.EMPTY;
        }
    }

    private void depositOne(Draft draft, int slot) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(carried.getItem());
        CompoundTag tag = carried.getTag();
        if (draft.item[slot] == null) {
            draft.item[slot] = id;
            draft.nbt[slot] = tag != null ? tag.copy() : null;
            draft.count[slot] = 1;
        } else if (id != null && id.equals(draft.item[slot])
                && java.util.Objects.equals(draft.nbt[slot], tag)) {
            if (draft.count[slot] >= 64) {
                return;
            }
            draft.count[slot]++;
        } else {
            return;
        }
        carried.shrink(1);
        if (carried.isEmpty()) {
            carried = ItemStack.EMPTY;
            carriedInvSlot = -1;
        }
    }

    private void selectOffer(int index) {
        if (index != selected) {
            selected = index;
            init(minecraft, width, height);
        }
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (confirmRestock) {
            return true;
        }
        for (int cell = 0; cell < PAGE_SIZE; cell++) {
            Draft draft = drafts[page * PAGE_SIZE + cell];
            int y = cellY(cell);
            for (int s = 0; s < 3; s++) {
                if (isOver(mouseX, mouseY, slotX(cell, s), y, 18, 18) && draft.item[s] != null) {
                    int step = (hasShiftDown() ? 8 : 1) * (int) Math.signum(delta);
                    draft.count[s] = Math.max(1, Math.min(64, draft.count[s] + step));
                    return true;
                }
            }
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmRestock && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            confirmRestock = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        writeBack();
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
