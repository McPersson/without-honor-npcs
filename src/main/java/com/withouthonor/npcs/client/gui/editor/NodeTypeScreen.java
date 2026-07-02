package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.DialogueNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class NodeTypeScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final String[] TYPES = {"text", "input", "check", "random"};
    private static final String[] LABEL_KEYS = {
            "wh_npcs.ui.node_type.type_text", "wh_npcs.ui.node_type.type_input",
            "wh_npcs.ui.node_type.type_check", "wh_npcs.ui.node_type.type_random"};

    private final Screen parent;
    private final DialogueNode node;

    private EditBox hintBox;
    private EditBox storeBox;
    private EditBox fallbackBox;

    private EditBox chanceBox;
    private EditBox successBox;
    private EditBox failBox;

    private final List<EditBox> rWeight = new ArrayList<>();
    private final List<EditBox> rNext = new ArrayList<>();

    private int winX, winY, winW, winH;
    private int bottomY;

    public NodeTypeScreen(Screen parent, DialogueNode node) {
        super(Component.translatable("wh_npcs.ui.node_type.title"));
        this.parent = parent;
        this.node = node;
    }

    @Override
    protected int designW() {
        return 380;
    }

    @Override
    protected int designH() {
        return 300;
    }

    private void recalc() {
        winW = 380;
        winH = 300;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        bottomY = winY + winH - PAD - 20;
    }

    private int bodyY() {
        return winY + HEADER_H + 32;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        rWeight.clear();
        rNext.clear();
        int fx = winX + PAD;
        int boxX = fx + 150;
        int boxW = winW - PAD - boxX + winX;
        switch (node.getType()) {
            case "input" -> {
                hintBox = add(boxX, bodyY(), boxW, Component.translatable("wh_npcs.ui.node_type.hint_field").getString(), node.getInputHint(), 80, node::setInputHint);
                storeBox = add(boxX, bodyY() + 24, boxW, Component.translatable("wh_npcs.ui.node_type.hint_var").getString(), node.getInputStoreVar(), 32,
                        node::setInputStoreVar);
                fallbackBox = add(boxX, bodyY() + 48, boxW, Component.translatable("wh_npcs.ui.node_type.hint_node_id").getString(), node.getInputFallbackNext(), 64,
                        node::setInputFallbackNext);
            }
            case "check" -> {
                chanceBox = add(boxX, bodyY() + 24, 46, "100", String.valueOf(node.getCheckChance()), 3,
                        v -> node.setCheckChance(parseInt(v, 100)));
                successBox = add(boxX, bodyY() + 48, boxW, Component.translatable("wh_npcs.ui.node_type.hint_node_id").getString(), node.getCheckSuccessNext(), 64,
                        node::setCheckSuccessNext);
                failBox = add(boxX, bodyY() + 72, boxW, Component.translatable("wh_npcs.ui.node_type.hint_node_id").getString(), node.getCheckFailNext(), 64,
                        node::setCheckFailNext);
            }
            case "random" -> {
                for (int i = 0; i < node.getRandomOptions().size(); i++) {
                    DialogueNode.RandomOption opt = node.getRandomOptions().get(i);
                    int y = bodyY() + 42 + i * 22;
                    EditBox w = add(fx, y, 40, Component.translatable("wh_npcs.ui.node_type.weight").getString(), String.valueOf(opt.weight()), 4, v -> writeBackRandom());
                    EditBox n = add(fx + 48, y, winW - 2 * PAD - 72, Component.translatable("wh_npcs.ui.node_type.hint_node_id").getString(), opt.next(), 64, v -> writeBackRandom());
                    rWeight.add(w);
                    rNext.add(n);
                }
            }
            default -> {
            }
        }
    }

    private EditBox add(int x, int y, int w, String hint, String value, int max,
                        java.util.function.Consumer<String> onChange) {
        EditBox box = addRenderableWidget(new SelectableEditBox(font, x, y, w, 16, Component.literal(hint)));
        box.setMaxLength(max);
        box.setValue(value);
        box.setHint(Component.literal(hint));
        box.setResponder(v -> onChange.accept(v.trim()));
        return box;
    }

    private static int parseInt(String v, int def) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void writeBackRandom() {
        node.getRandomOptions().clear();
        for (int i = 0; i < rWeight.size(); i++) {
            node.getRandomOptions().add(new DialogueNode.RandomOption(
                    Math.max(1, parseInt(rWeight.get(i).getValue(), 1)), rNext.get(i).getValue().trim()));
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.node_type.title").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        for (int i = 0; i < TYPES.length; i++) {
            int x = winX + PAD + i * 88;
            boolean cur = node.getType().equals(TYPES[i]);
            boolean hover = isOver(mouseX, mouseY, x, winY + HEADER_H + 6, 86, 18);
            VanillaUIHelper.drawButton(g, x, winY + HEADER_H + 6, 86, 18, hover || cur);
            g.drawCenteredString(font, Component.translatable(LABEL_KEYS[i]).getString(), x + 43, winY + HEADER_H + 11,
                    cur ? VanillaUIHelper.TEXT_YELLOW : (hover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE));
        }

        int fx = winX + PAD;
        switch (node.getType()) {
            case "text" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.text_desc1").getString(), fx, bodyY(),
                        VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.text_desc2").getString(), fx, bodyY() + 16,
                        VanillaUIHelper.TEXT_WHITE, false);
            }
            case "input" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_hint_label").getString(), fx, bodyY() + 4, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_store_label").getString(), fx, bodyY() + 28, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_fallback_label").getString(), fx, bodyY() + 52, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_desc1").getString(),
                        fx, bodyY() + 76, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_desc2").getString(),
                        fx, bodyY() + 88, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_desc3").getString(),
                        fx, bodyY() + 100, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_desc4").getString(),
                        fx, bodyY() + 112, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_desc5").getString(),
                        fx, bodyY() + 124, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.input_desc6").getString(),
                        fx, bodyY() + 136, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "check" -> {
                boolean condHover = isOver(mouseX, mouseY, fx, bodyY(), 200, 18);
                VanillaUIHelper.drawButton(g, fx, bodyY(), 200, 18, condHover);
                g.drawCenteredString(font, Component.translatable("wh_npcs.ui.node_type.conditions", node.getCheckConditions().size()).getString(),
                        fx + 100, bodyY() + 5, condHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.chance_label").getString(), fx, bodyY() + 28, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.success_label").getString(), fx, bodyY() + 52, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.fail_label").getString(), fx, bodyY() + 76, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.check_desc1").getString(),
                        fx, bodyY() + 100, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.check_desc2").getString(),
                        fx, bodyY() + 112, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "random" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.random_desc1").getString(),
                        fx, bodyY(), VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.random_desc2").getString(),
                        fx, bodyY() + 12, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.weight").getString(), fx, bodyY() + 30, VanillaUIHelper.TEXT_DARK_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.node_type.hint_node_id").getString(), fx + 48, bodyY() + 30, VanillaUIHelper.TEXT_DARK_GRAY, false);
                for (int i = 0; i < rWeight.size(); i++) {
                    int y = bodyY() + 42 + i * 22;
                    drawBtn(g, "✕", winX + winW - PAD - 18, y, 18, mouseX, mouseY, VanillaUIHelper.TEXT_RED);
                }
                if (node.getRandomOptions().size() < 8) {
                    drawBtn(g, Component.translatable("wh_npcs.ui.node_type.add_option").getString(), fx, bodyY() + 42 + rWeight.size() * 22, 90, mouseX, mouseY,
                            VanillaUIHelper.TEXT_GREEN);
                }
            }
            default -> {
            }
        }

        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button == 0) {

            for (int i = 0; i < TYPES.length; i++) {
                int x = winX + PAD + i * 88;
                if (isOver(mouseX, mouseY, x, winY + HEADER_H + 6, 86, 18)) {
                    if (!node.getType().equals(TYPES[i])) {
                        node.setType(TYPES[i]);
                        if (TYPES[i].equals("random") && node.getRandomOptions().isEmpty()) {
                            node.getRandomOptions().add(new DialogueNode.RandomOption(1, ""));
                        }
                        init(minecraft, width, height);
                    }
                    return true;
                }
            }
            int fx = winX + PAD;
            if ("check".equals(node.getType()) && isOver(mouseX, mouseY, fx, bodyY(), 200, 18)) {
                ConditionsEditorScreen.openForConditions(this, node.getCheckConditions());
                return true;
            }
            if ("random".equals(node.getType())) {
                for (int i = 0; i < rWeight.size(); i++) {
                    int y = bodyY() + 42 + i * 22;
                    if (isOver(mouseX, mouseY, winX + winW - PAD - 18, y, 18, 18)) {
                        writeBackRandom();
                        node.getRandomOptions().remove(i);
                        init(minecraft, width, height);
                        return true;
                    }
                }
                if (node.getRandomOptions().size() < 8
                        && isOver(mouseX, mouseY, fx, bodyY() + 42 + rWeight.size() * 22, 90, 18)) {
                    writeBackRandom();
                    node.getRandomOptions().add(new DialogueNode.RandomOption(1, ""));
                    init(minecraft, width, height);
                    return true;
                }
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5, hovered ? VanillaUIHelper.TEXT_YELLOW : color);
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
