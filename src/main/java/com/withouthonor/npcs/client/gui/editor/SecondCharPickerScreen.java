package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.DialogueNode;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.ProfileSharePackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SecondCharPickerScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 12;

    private final Screen parent;
    private final DialogueNode node;

    private List<ProfileSharePackets.FileEntry> files;
    private int scroll;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private EditBox searchBox;

    private int winX, winY, winW, winH;
    private int listY, listH, controlsY, bottomY;

    public SecondCharPickerScreen(Screen parent, DialogueNode node) {
        super(Component.translatable("wh_npcs.ui.second_char.title"));
        this.parent = parent;
        this.node = node;
        NetworkHandler.sendToServer(new ProfileSharePackets.RequestList());
    }

    public void acceptList(List<ProfileSharePackets.FileEntry> list) {
        this.files = list;
    }

    private String preset() {
        return node.getSecondCharPreset();
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, winY + HEADER_H + 6,
                winW - PAD * 2, 16, Component.translatable("wh_npcs.ui.second_char.search")));
        searchBox.setMaxLength(64);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.second_char.search_hint"));
        searchBox.setResponder(v -> scroll = 0);
    }

    @Override
    protected int designW() {
        return 360;
    }

    @Override
    protected int designH() {
        return 280;
    }

    private void recalc() {
        winW = 360;
        winH = 280;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        bottomY = winY + winH - PAD - 20;
        controlsY = bottomY - 24;
        listY = winY + HEADER_H + 28;
        listH = controlsY - 6 - listY;
    }

    private List<ProfileSharePackets.FileEntry> displayed() {
        List<ProfileSharePackets.FileEntry> out = new ArrayList<>();
        if (files == null) {
            return out;
        }
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        for (ProfileSharePackets.FileEntry e : files) {
            if (q.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(e);
            }
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.second_char.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        String cur = preset().isEmpty() ? Component.translatable("wh_npcs.ui.second_char.none").getString() : "§b" + preset();
        g.drawString(font, cur, winX + winW - PAD - font.width(cur.replaceAll("§.", "")) - 4, winY + 7,
                VanillaUIHelper.TEXT_WHITE, false);

        VanillaUIHelper.drawContentPanel(g, winX + PAD, listY, winW - PAD * 2, listH);
        if (files == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.second_char.loading").getString(), winX + winW / 2, listY + listH / 2 - 4,
                    VanillaUIHelper.TEXT_STATUS);
        } else {
            List<ProfileSharePackets.FileEntry> list = displayed();
            if (list.isEmpty()) {
                g.drawCenteredString(font, Component.translatable("wh_npcs.ui.second_char.empty").getString(), winX + winW / 2,
                        listY + listH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            } else {
                int visible = (listH - 8) / ROW_H;
                scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - visible)));
                int y = listY + 4;
                for (int i = scroll; i < Math.min(list.size(), scroll + visible); i++) {
                    ProfileSharePackets.FileEntry e = list.get(i);
                    boolean sel = e.name().equals(preset());
                    boolean hov = isOver(mouseX, mouseY, winX + PAD + 2, y, winW - PAD * 2 - 4, ROW_H);
                    if (sel || hov) {
                        g.fill(winX + PAD + 2, y, winX + winW - PAD - 2, y + ROW_H,
                                sel ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
                    }
                    g.drawString(font, font.plainSubstrByWidth(e.name(), winW - PAD * 2 - 80), winX + PAD + 6, y + 2,
                            sel ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
                    if (!e.author().isEmpty()) {
                        String a = "§8" + e.author();
                        g.drawString(font, a, winX + winW - PAD - 10 - font.width(e.author()), y + 2,
                                VanillaUIHelper.TEXT_WHITE, false);
                    }
                    y += ROW_H;
                }
                VanillaUIHelper.drawScrollbar(g, winX + winW - PAD - 6, listY + 3, listH - 6,
                        list.size(), visible, scroll, scrollbars, v -> scroll = v);
            }
        }

        boolean portraitOn = node.isSecondCharPortraitShow() && !node.getSecondCharPortrait().isEmpty();
        boolean portHover = isOver(mouseX, mouseY, winX + PAD, controlsY, 110, 16);
        VanillaUIHelper.drawButton(g, winX + PAD, controlsY, 110, 16, portHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.second_char.portrait").getString(), winX + PAD + 55, controlsY + 4,
                portraitOn ? VanillaUIHelper.TEXT_GREEN
                        : (portHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA));
        int nameX = winX + PAD + 124;
        boolean nameHover = isOver(mouseX, mouseY, nameX, controlsY, 12, 12);
        VanillaUIHelper.drawButton(g, nameX, controlsY, 12, 12, nameHover);
        if (node.isSecondCharNameShow()) {
            VanillaUIHelper.drawCheck(g, nameX + 1, controlsY + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, Component.translatable("wh_npcs.ui.second_char.name").getString(), nameX + 16, controlsY + 2, VanillaUIHelper.TEXT_GRAY, false);

        boolean clearHover = isOver(mouseX, mouseY, winX + PAD, bottomY, 110, 18) && !preset().isEmpty();
        VanillaUIHelper.drawButton(g, winX + PAD, bottomY, 110, 18, clearHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.second_char.clear_model").getString(), winX + PAD + 55, bottomY + 5,
                preset().isEmpty() ? VanillaUIHelper.TEXT_DARK_GRAY
                        : (clearHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_RED));
        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            recalc();
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }
            if (!preset().isEmpty() && isOver(mouseX, mouseY, winX + PAD, bottomY, 90, 18)) {
                node.setSecondCharPreset("");
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }

            if (isOver(mouseX, mouseY, winX + PAD, controlsY, 110, 16) && minecraft != null) {
                minecraft.setScreen(new PortraitPickerScreen(this, new PortraitTarget() {
                    @Override
                    public String image() {
                        return node.getSecondCharPortrait();
                    }

                    @Override
                    public void setImage(String n) {
                        node.setSecondCharPortrait(n);
                    }

                    @Override
                    public boolean show() {
                        return node.isSecondCharPortraitShow();
                    }

                    @Override
                    public void setShow(boolean s) {
                        node.setSecondCharPortraitShow(s);
                    }
                }, Component.translatable("wh_npcs.ui.second_char.portrait_title").getString()));
                return true;
            }

            if (isOver(mouseX, mouseY, winX + winW - PAD - 90, controlsY, 12, 12)) {
                node.setSecondCharNameShow(!node.isSecondCharNameShow());
                return true;
            }
            if (files != null) {
                List<ProfileSharePackets.FileEntry> list = displayed();
                int visible = (listH - 8) / ROW_H;
                int y = listY + 4;
                for (int i = scroll; i < Math.min(list.size(), scroll + visible); i++) {
                    if (isOver(mouseX, mouseY, winX + PAD + 2, y, winW - PAD * 2 - 4, ROW_H)) {
                        node.setSecondCharPreset(list.get(i).name());
                        return true;
                    }
                    y += ROW_H;
                }
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, winX + PAD, listY, winW - PAD * 2, listH)) {
            scroll -= (int) Math.signum(delta) * 2;
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

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    public void onClose() {
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
