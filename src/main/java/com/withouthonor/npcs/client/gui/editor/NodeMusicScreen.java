package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.DialogueNode;
import com.withouthonor.npcs.network.EditorGiveItemPacket;
import com.withouthonor.npcs.network.EditorMoveItemPacket;
import com.withouthonor.npcs.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class NodeMusicScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int WIN_W = 380;
    private static final int WIN_H = 252;

    private final Screen parent;
    private final DialogueNode node;

    @Nullable
    private ResourceLocation disc;
    private String musicUrl = "";
    private String musicTitle = "";
    private boolean etchedPreview;
    private boolean loop;
    private ItemStack carried = ItemStack.EMPTY;
    private boolean carriedFromSlot;
    private int carriedInvSlot = -1;
    @Nullable
    private SimpleSoundInstance preview;
    @Nullable
    private String warn;

    private int winX, winY, winW, winH;
    private int bottomY;
    @Nullable
    private String hoverTooltip;
    @Nullable
    private ItemStack discTip;

    public NodeMusicScreen(Screen parent, DialogueNode node) {
        super(Component.translatable("wh_npcs.ui.node_music.title"));
        this.parent = parent;
        this.node = node;
        this.disc = node.getMusicDisc().isEmpty() ? null : ResourceLocation.tryParse(node.getMusicDisc());
        this.musicUrl = node.getMusicUrl();
        this.musicTitle = node.getMusicTitle();
        this.loop = node.isMusicLoop();
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
        bottomY = winY + winH - PAD - 20;
    }

    private int slotX() {
        return winX + PAD + 70;
    }

    private int slotY() {
        return winY + HEADER_H + 8;
    }

    private int loopY() {
        return slotY() + 30;
    }

    private int btnRowY() {
        return loopY() + 24;
    }

    private int gridY() {
        return bottomY - 8 - 72;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        hoverTooltip = null;
        discTip = null;
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.node_music.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        g.drawString(font, Component.translatable("wh_npcs.ui.node_music.disc").getString(), winX + PAD, slotY() + 5, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawItemSlot(g, slotX(), slotY(), isOver(mouseX, mouseY, slotX(), slotY(), 18, 18));
        if (disc != null) {
            Item item = ForgeRegistries.ITEMS.getValue(disc);
            if (item != null) {
                g.renderItem(new ItemStack(item), slotX() + 1, slotY() + 1);
                String label = musicUrl.isEmpty()
                        ? new ItemStack(item).getHoverName().getString()
                        : "♪ " + (musicTitle.isEmpty() ? musicUrl : musicTitle);
                g.drawString(font, font.plainSubstrByWidth(label, winW - (slotX() - winX) - 24),
                        slotX() + 24, slotY() + 5,
                        musicUrl.isEmpty() ? VanillaUIHelper.TEXT_AQUA : VanillaUIHelper.TEXT_GOLD, false);
            }
        } else {
            g.drawString(font, Component.translatable("wh_npcs.ui.node_music.no_disc").getString(), slotX() + 24, slotY() + 5, VanillaUIHelper.TEXT_WHITE, false);
        }

        boolean loopHover = isOver(mouseX, mouseY, winX + PAD, loopY(), 208, 18);
        VanillaUIHelper.drawButton(g, winX + PAD, loopY(), 208, 18, loopHover);
        g.drawString(font, loop ? Component.translatable("wh_npcs.ui.node_music.loop_on").getString() : Component.translatable("wh_npcs.ui.node_music.loop_off").getString(),
                winX + PAD + 6, loopY() + 5, VanillaUIHelper.TEXT_WHITE, false);
        if (loopHover) {
            hoverTooltip = Component.translatable("wh_npcs.ui.node_music.tip.loop").getString();
        }

        boolean playing = isPreviewing();
        drawBtn(g, disc == null ? Component.translatable("wh_npcs.ui.node_music.listen_off").getString() : (playing ? Component.translatable("wh_npcs.ui.node_music.stop").getString() : Component.translatable("wh_npcs.ui.node_music.listen").getString()),
                winX + PAD, btnRowY(), 110, mouseX, mouseY,
                disc == null ? VanillaUIHelper.TEXT_DARK_GRAY
                        : (playing ? 0xFFFF6B6B : VanillaUIHelper.TEXT_AQUA));
        drawBtn(g, Component.translatable("wh_npcs.ui.node_music.clear").getString(), winX + PAD + 118, btnRowY(), 90, mouseX, mouseY,
                disc != null ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY);

        VanillaUIHelper.drawSeparator(g, winX + PAD, gridY() - 14, winW - 2 * PAD);
        g.drawString(font, warn != null ? warn : Component.translatable("wh_npcs.ui.node_music.inventory").getString(),
                winX + PAD, gridY() - 11, warn != null ? 0xFFFF6B6B : VanillaUIHelper.TEXT_GRAY, false);
        renderInventory(g, mouseX, mouseY);

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);

        if (carried.isEmpty() && disc != null && isOver(mouseX, mouseY, slotX(), slotY(), 18, 18)) {
            Item item = ForgeRegistries.ITEMS.getValue(disc);
            if (item != null) {
                discTip = new ItemStack(item);
            }
        }
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
        if (discTip != null) {
            g.renderTooltip(font, discTip, mouseX, mouseY);
        }

        if (!carried.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderItem(carried, mouseX - 8, mouseY - 8);
            g.pose().popPose();
        }
    }

    private void renderInventory(GuiGraphics g, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        for (int i = 0; i < 36; i++) {
            int x = winX + PAD + (i % 9) * 18;
            int sy = gridY() + (i / 9) * 18;
            boolean hovered = isOver(mouseX, mouseY, x, sy, 18, 18);
            VanillaUIHelper.drawItemSlot(g, x, sy, hovered);
            ItemStack stack = minecraft.player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 1, sy + 1);
                g.renderItemDecorations(font, stack, x + 1, sy + 1);
                if (!(stack.getItem() instanceof RecordItem)
                        && !com.withouthonor.npcs.compat.Compat.etched().isEtchedDisc(stack)) {
                    g.fill(x + 1, sy + 1, x + 17, sy + 17, 0x80202020);
                }
                if (hovered) {
                    g.renderTooltip(font, stack, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();

        if (isOver(mouseX, mouseY, slotX(), slotY(), 18, 18)) {
            if (button == 1) {
                stopPreview();
                disc = null;
                musicUrl = "";
                musicTitle = "";
                apply();
                return true;
            }
            if (button == 0) {
                if (!carried.isEmpty()) {
                    boolean etchedDisc = com.withouthonor.npcs.compat.Compat.etched().isEtchedDisc(carried);
                    if (carried.getItem() instanceof RecordItem || etchedDisc) {
                        stopPreview();
                        disc = ForgeRegistries.ITEMS.getKey(carried.getItem());
                        if (etchedDisc) {
                            musicUrl = com.withouthonor.npcs.compat.Compat.etched().extractUrl(carried);
                            musicTitle = com.withouthonor.npcs.compat.Compat.etched().extractTitle(carried);
                        } else {
                            musicUrl = "";
                            musicTitle = "";
                        }
                        carried = ItemStack.EMPTY;
                        carriedFromSlot = false;
                        carriedInvSlot = -1;
                        warn = (etchedDisc && musicUrl.isEmpty())
                                ? Component.translatable("wh_npcs.ui.node_music.etched_blank").getString() : null;
                        apply();
                    } else {
                        warn = Component.translatable("wh_npcs.ui.node_music.not_disc").getString();
                    }
                } else if (disc != null) {

                    Item it = ForgeRegistries.ITEMS.getValue(disc);
                    carried = it != null ? new ItemStack(it) : ItemStack.EMPTY;
                    carriedFromSlot = !carried.isEmpty();
                    carriedInvSlot = -1;
                    stopPreview();
                    disc = null;
                musicUrl = "";
                musicTitle = "";
                    warn = null;
                    apply();
                }
                return true;
            }
        }
        if (button == 0) {
            if (isOver(mouseX, mouseY, winX + PAD, loopY(), 208, 18)) {
                loop = !loop;
                apply();
                return true;
            }
            if (disc != null && isOver(mouseX, mouseY, winX + PAD, btnRowY(), 110, 18)) {
                if (isPreviewing()) {
                    stopPreview();
                } else {
                    playPreview();
                }
                return true;
            }
            if (isOver(mouseX, mouseY, winX + PAD + 118, btnRowY(), 90, 18)) {
                stopPreview();
                disc = null;
                musicUrl = "";
                musicTitle = "";
                warn = null;
                apply();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }

            if (minecraft != null && minecraft.player != null) {
                for (int i = 0; i < 36; i++) {
                    int x = winX + PAD + (i % 9) * 18;
                    int sy = gridY() + (i / 9) * 18;
                    if (isOver(mouseX, mouseY, x, sy, 18, 18)) {
                        var items = minecraft.player.getInventory().items;
                        if (!carried.isEmpty()) {
                            if (carriedFromSlot) {

                                NetworkHandler.sendToServer(new EditorGiveItemPacket(carried, i));
                                if (items.get(i).isEmpty()) {
                                    items.set(i, carried.copy());
                                }
                            } else if (carriedInvSlot >= 0 && carriedInvSlot != i) {

                                NetworkHandler.sendToServer(new EditorMoveItemPacket(carriedInvSlot, i));
                                ItemStack moved = items.get(carriedInvSlot);
                                items.set(carriedInvSlot, items.get(i));
                                items.set(i, moved);
                            }
                            carried = ItemStack.EMPTY;
                            carriedFromSlot = false;
                            carriedInvSlot = -1;
                        } else {
                            ItemStack stack = items.get(i);
                            carried = stack.copy();
                            carriedFromSlot = false;
                            carriedInvSlot = stack.isEmpty() ? -1 : i;
                            warn = null;
                        }
                        return true;
                    }
                }
            }

            if (!carried.isEmpty()) {
                carried = ItemStack.EMPTY;
                carriedFromSlot = false;
                carriedInvSlot = -1;
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void apply() {
        node.setMusicDisc(disc != null ? disc.toString() : "");
        node.setMusicUrl(musicUrl);
        node.setMusicTitle(musicTitle);
        node.setMusicLoop(loop);
    }

    private void playPreview() {
        if (disc == null || minecraft == null) {
            return;
        }
        if (!musicUrl.isEmpty()) {
            com.withouthonor.npcs.compat.EtchedClientBridge ec =
                    com.withouthonor.npcs.compat.Compat.etchedClient();
            if (ec != null) {
                stopPreview();
                ec.playOnline(musicUrl, musicTitle, false);
                etchedPreview = true;
            }
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(disc);
        if (item instanceof RecordItem record) {
            stopPreview();
            preview = SimpleSoundInstance.forUI(record.getSound(), 1.0F, 0.7F);
            minecraft.getSoundManager().play(preview);
        }
    }

    private void stopPreview() {
        if (preview != null && minecraft != null) {
            minecraft.getSoundManager().stop(preview);
            preview = null;
        }
        if (etchedPreview) {
            com.withouthonor.npcs.compat.EtchedClientBridge ec =
                    com.withouthonor.npcs.compat.Compat.etchedClient();
            if (ec != null) {
                ec.stopOnline();
            }
            etchedPreview = false;
        }
    }

    private boolean isPreviewing() {
        return preview != null || etchedPreview;
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, font.plainSubstrByWidth(label, w - 6), x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private void multilineTooltip(GuiGraphics g, String text, int x, int y) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, x, y);
    }

    @Override
    public void onClose() {
        stopPreview();
        apply();
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
