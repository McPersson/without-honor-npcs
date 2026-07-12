package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.compat.CuriosBridge;
import com.withouthonor.npcs.network.CuriosPackets;
import com.withouthonor.npcs.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CuriosEditorScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 20;
    private static final int WIN_W = 360;
    private static final int WIN_H = 300;

    private static final Map<String, String> LABEL_KEYS = Map.ofEntries(
            Map.entry("head", "wh_npcs.ui.curios.slot.head"), Map.entry("necklace", "wh_npcs.ui.curios.slot.necklace"),
            Map.entry("back", "wh_npcs.ui.curios.slot.back"), Map.entry("body", "wh_npcs.ui.curios.slot.body"),
            Map.entry("bracelet", "wh_npcs.ui.curios.slot.bracelet"), Map.entry("hands", "wh_npcs.ui.curios.slot.hands"),
            Map.entry("ring", "wh_npcs.ui.curios.slot.ring"), Map.entry("belt", "wh_npcs.ui.curios.slot.belt"),
            Map.entry("charm", "wh_npcs.ui.curios.slot.charm"), Map.entry("curio", "wh_npcs.ui.curios.slot.curio"),
            Map.entry("feet", "wh_npcs.ui.curios.slot.feet"),
            // Слот Iron's Spells (регистрируется через Curios) — иначе показывался сырой id
            Map.entry("spellbook", "wh_npcs.ui.curios.slot.spellbook"));

    private final Screen parent;
    private final int npcEntityId;
    private final List<Entry> entries = new ArrayList<>();
    private boolean loaded;
    private ItemStack carried = ItemStack.EMPTY;

    private int winX, winY, winW, winH;
    private int listTop, invY, bottomY;
    private ItemStack hoverTip = ItemStack.EMPTY;

    private static final class Entry {
        final String type;
        final int index;
        ItemStack stack;
        int screenX, screenY;

        Entry(String type, int index, ItemStack stack) {
            this.type = type;
            this.index = index;
            this.stack = stack;
        }
    }

    public CuriosEditorScreen(Screen parent, int npcEntityId) {
        super(Component.translatable("wh_npcs.ui.curios.title"));
        this.parent = parent;
        this.npcEntityId = npcEntityId;
        NetworkHandler.sendToServer(new CuriosPackets.Request(npcEntityId));
    }

    public static void accept(int entityId, List<CuriosBridge.CurioSlotEntry> list) {
        if (Minecraft.getInstance().screen instanceof CuriosEditorScreen s && s.npcEntityId == entityId) {
            s.entries.clear();
            for (CuriosBridge.CurioSlotEntry e : list) {
                s.entries.add(new Entry(e.slotType(), e.index(), e.stack()));
            }
            s.loaded = true;
        }
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
        listTop = winY + HEADER_H + 8;
        bottomY = winY + winH - PAD - 20;
        invY = bottomY - 4 * 18 - 6;
    }

    private List<List<Entry>> groups() {
        List<List<Entry>> groups = new ArrayList<>();
        String prev = null;
        for (Entry e : entries) {
            if (!e.type.equals(prev)) {
                groups.add(new ArrayList<>());
                prev = e.type;
            }
            groups.get(groups.size() - 1).add(e);
        }
        return groups;
    }

    private void layout() {
        List<List<Entry>> groups = groups();
        int leftCount = (groups.size() + 1) / 2;
        int colW = (winW - PAD * 2) / 2;
        for (int gi = 0; gi < groups.size(); gi++) {
            int col = gi < leftCount ? 0 : 1;
            int row = gi < leftCount ? gi : gi - leftCount;
            int colX = winX + PAD + col * colW;
            int y = listTop + row * ROW_H;
            List<Entry> grp = groups.get(gi);
            for (int s = 0; s < grp.size(); s++) {
                Entry e = grp.get(s);
                e.screenX = colX + 72 + s * 20;
                e.screenY = y;
            }
        }
    }

    @Nullable
    private LivingEntity npc() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getEntity(npcEntityId) instanceof LivingEntity le ? le : null;
    }

    private boolean canPlace(Entry e, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        LivingEntity npc = npc();
        return npc == null || com.withouthonor.npcs.compat.Compat.curios()
                .isValidForSlot(npc, e.type, e.index, stack);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        layout();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.curios.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        hoverTip = ItemStack.EMPTY;
        if (!loaded) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.curios.loading").getString(), winX + winW / 2, listTop + 10, VanillaUIHelper.TEXT_STATUS);
        } else if (entries.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.curios.no_slots").getString(), winX + winW / 2, listTop + 10, VanillaUIHelper.TEXT_WHITE);
        } else {
            String prev = null;
            for (Entry e : entries) {
                if (!e.type.equals(prev)) {
                    String label = LABEL_KEYS.containsKey(e.type)
                            ? Component.translatable(LABEL_KEYS.get(e.type)).getString() : e.type;
                    g.drawString(font, font.plainSubstrByWidth(label, 66), e.screenX - 68, e.screenY + 5,
                            VanillaUIHelper.TEXT_GRAY, false);
                    prev = e.type;
                }
                boolean hov = isOver(mouseX, mouseY, e.screenX, e.screenY, 18, 18);
                VanillaUIHelper.drawItemSlot(g, e.screenX, e.screenY, hov);
                if (!e.stack.isEmpty()) {
                    g.renderItem(e.stack, e.screenX + 1, e.screenY + 1);
                    g.renderItemDecorations(font, e.stack, e.screenX + 1, e.screenY + 1);
                    if (hov && carried.isEmpty()) {
                        hoverTip = e.stack;
                    }
                }
            }
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.curios.inventory").getString(), winX + PAD, invY - 11, VanillaUIHelper.TEXT_GRAY, false);
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
        layout();

        for (Entry e : entries) {
            if (isOver(mouseX, mouseY, e.screenX, e.screenY, 18, 18)) {
                if (button == 1) {
                    if (canPlace(e, carried)) {
                        e.stack = carried.isEmpty() ? ItemStack.EMPTY : carried.copy();
                    }
                } else if (button == 0) {
                    if (carried.isEmpty() || canPlace(e, carried)) {
                        ItemStack tmp = e.stack;
                        e.stack = carried;
                        carried = tmp;
                    }
                }
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
        if (!loaded) {
            return;
        }
        List<CuriosBridge.CurioSlotEntry> out = new ArrayList<>(entries.size());
        for (Entry e : entries) {
            out.add(new CuriosBridge.CurioSlotEntry(e.type, e.index, e.stack));
        }
        NetworkHandler.sendToServer(new CuriosPackets.Save(npcEntityId, out));
    }

    @Override
    public void onClose() {
        writeBack();
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

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
