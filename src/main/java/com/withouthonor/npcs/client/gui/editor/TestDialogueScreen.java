package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.DialogueChoice;
import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import com.withouthonor.npcs.common.dialogue.DialogueNode;
import com.withouthonor.npcs.common.dialogue.action.Actions;
import com.withouthonor.npcs.common.dialogue.action.DialogueAction;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TestDialogueScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int SIM_W = 170;
    private static final int CHOICE_H = 20;
    private static final int LINE_H = 11;
    private static final int WIN_W = 540;
    private static final int WIN_H = 300;

    private final DialogueEditorScreen parent;
    private final DialogueGraph graph;

    private String nodeId;
    private int pageIndex;
    private int textScroll;

    private final Map<DialogueCondition, Boolean> overrides = new IdentityHashMap<>();
    private final List<String> log = new ArrayList<>();
    private boolean closed;

    private int winX, winY, winW, winH;
    private int simX, simY, simH;
    private int textX, textY, textW, textH;
    private int choicesY;
    private int bottomY;
    @Nullable
    private Style hoverAnnStyle;

    public TestDialogueScreen(DialogueEditorScreen parent, DialogueGraph graph) {
        super(Component.translatable("wh_npcs.ui.test_dialogue.title"));
        this.parent = parent;
        this.graph = graph;
        this.nodeId = graph.getStart();
        log.add(Component.translatable("wh_npcs.ui.test_dialogue.log_start", nodeId).getString());
    }

    @Nullable
    private DialogueNode node() {
        return graph.getNode(nodeId);
    }

    private boolean passes(DialogueChoice choice) {
        for (DialogueCondition condition : choice.getConditions()) {
            if (!overrides.getOrDefault(condition, true)) {
                return false;
            }
        }
        return true;
    }

    private String replaceVars(String text) {
        String player = minecraft != null && minecraft.player != null
                ? minecraft.player.getGameProfile().getName() : Component.translatable("wh_npcs.ui.test_dialogue.player_fallback").getString();
        return text.replace("{player}", player).replace("{npc}", "NPC");
    }

    private String applyPlaceholders(String text) {
        return com.withouthonor.npcs.common.dialogue.DialogueRuntime.stripAnnotations(replaceVars(text));
    }

    private String pageRawText() {
        DialogueNode node = node();
        if (node == null || node.getPages().isEmpty()) {
            return "";
        }
        List<String> pages = node.getPages();
        int pi = Math.max(0, Math.min(pageIndex, pages.size() - 1));
        String t = replaceVars(pages.get(pi));
        if (node.isRandomPage() && pages.size() > 1) {
            t = Component.translatable("wh_npcs.ui.test_dialogue.random_page", (pi + 1), pages.size()).getString() + "§r\n" + t;
        }
        return t;
    }

    private Component buildPageComponent(String page) {
        MutableComponent root = Component.empty();
        StringBuilder seg = new StringBuilder();
        String ann = null;
        for (int i = 0; i < page.length(); i++) {
            char c = page.charAt(i);
            if (c == '§' && i + 1 < page.length()) {
                char n = page.charAt(i + 1);
                if (n == '{') {
                    int close = page.indexOf('}', i + 2);
                    if (close >= 0) {
                        flushSegment(root, seg, ann);
                        ann = page.substring(i + 2, close);
                        i = close;
                        continue;
                    }
                } else if (n == '}') {
                    flushSegment(root, seg, ann);
                    ann = null;
                    i++;
                    continue;
                }
            }
            seg.append(c);
        }
        flushSegment(root, seg, ann);
        return root;
    }

    private void flushSegment(MutableComponent root, StringBuilder seg, @Nullable String ann) {
        if (seg.length() == 0) {
            return;
        }
        MutableComponent part = Component.literal(seg.toString());
        if (ann != null) {
            var term = com.withouthonor.npcs.client.ClientGlossary.byId(ann);
            Component hover = term != null
                    ? Component.literal("§e" + (term.getTitle().isBlank() ? ann : term.getTitle())
                            + (term.getBody().isBlank() ? "" : "\n§7" + term.getBody()))
                    : Component.translatable("wh_npcs.ui.test_dialogue.term_not_found", ann);
            part = part.withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        }
        root.append(part);
        seg.setLength(0);
    }

    @Nullable
    private Style annotationStyleAt(int mouseX, int mouseY) {
        if (mouseX < textX + 7 || mouseX >= textX + textW - 6 || mouseY < textY + 6) {
            return null;
        }
        List<FormattedCharSequence> lines = font.split(buildPageComponent(pageRawText()), textW - 14);
        int visibleLines = (textH - 24) / LINE_H;
        int rel = (mouseY - (textY + 6)) / LINE_H;
        int idx = textScroll + rel;
        if (rel < 0 || rel > visibleLines || idx < 0 || idx >= lines.size()) {
            return null;
        }
        Style st = font.getSplitter().componentStyleAtWidth(lines.get(idx), mouseX - (textX + 7));
        return st != null && st.getHoverEvent() != null ? st : null;
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
        simX = winX + PAD;
        simY = winY + HEADER_H + 6;
        simH = bottomY - 6 - simY;
        textX = simX + SIM_W + PAD;
        textY = simY;
        textW = winX + winW - PAD - textX;
        DialogueNode node = node();
        int rows = closed ? 1
                : (node != null ? Math.max(1, visibleChoices(node).size()) : 1);
        choicesY = bottomY - 6 - rows * CHOICE_H;
        textH = choicesY - 8 - textY;
    }

    private record TestChoice(DialogueChoice choice, boolean ok, boolean hiddenForPlayer) {
    }

    private List<TestChoice> visibleChoices(DialogueNode node) {
        List<TestChoice> list = new ArrayList<>();
        for (DialogueChoice choice : node.getChoices()) {
            boolean ok = passes(choice);
            boolean hidden = !ok && choice.getLockedHint() == null;
            list.add(new TestChoice(choice, ok, hidden));
        }
        return list;
    }

    private void clickChoice(TestChoice tc) {
        if (!tc.ok()) {
            return;
        }
        for (DialogueAction action : tc.choice().getActions()) {
            log.add("§b▸ §7" + describeAction(action));
        }
        if (tc.choice().getNext() == null) {
            log.add(Component.translatable("wh_npcs.ui.test_dialogue.log_closed").getString());
            closed = true;
        } else if (graph.getNode(tc.choice().getNext()) == null) {
            log.add(Component.translatable("wh_npcs.ui.test_dialogue.log_missing_node", tc.choice().getNext()).getString());
        } else {
            nodeId = tc.choice().getNext();
            pageIndex = 0;
            textScroll = 0;
            log.add(Component.translatable("wh_npcs.ui.test_dialogue.log_to_node", nodeId).getString());
        }
    }

    private static String describeAction(DialogueAction action) {
        if (action instanceof Actions.GiveItem give) {
            return Component.translatable("wh_npcs.ui.test_dialogue.act_give", give.items().size()).getString();
        }
        if (action instanceof Actions.TakeItem take) {
            return Component.translatable("wh_npcs.ui.test_dialogue.act_take", take.slots().size()).getString();
        }
        if (action instanceof Actions.SetFlag flag) {
            return Component.translatable(flag.value() ? "wh_npcs.ui.test_dialogue.act_set_flag" : "wh_npcs.ui.test_dialogue.act_unset_flag", flag.flag()).getString();
        }
        if (action instanceof Actions.RunCommand command) {
            return Component.translatable("wh_npcs.ui.test_dialogue.act_command", command.command()).getString();
        }
        if (action instanceof Actions.Sound sound) {
            return Component.translatable("wh_npcs.ui.test_dialogue.act_sound", sound.soundId().getPath()).getString();
        }
        if (action instanceof Actions.Title) {
            return Component.translatable("wh_npcs.ui.test_dialogue.act_title").getString();
        }
        return action.type();
    }

    private void restart() {
        nodeId = graph.getStart();
        pageIndex = 0;
        textScroll = 0;
        closed = false;
        log.clear();
        log.add(Component.translatable("wh_npcs.ui.test_dialogue.log_start", nodeId).getString());
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);

        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.test_dialogue.header", graph.getId()).getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_GOLD, false);
        String nodeLabel = Component.translatable("wh_npcs.ui.test_dialogue.node_label", nodeId).getString();
        g.drawString(font, nodeLabel, winX + winW - PAD - font.width(nodeLabel), winY + 7,
                VanillaUIHelper.TEXT_GRAY, false);

        renderSimulator(g, mouseX, mouseY);
        renderDialogue(g, mouseX, mouseY);

        drawSmall(g, Component.translatable("wh_npcs.ui.test_dialogue.restart").getString(), winX + PAD, bottomY, 84, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawSmall(g, Component.translatable("wh_npcs.ui.test_dialogue.to_editor").getString(), winX + winW - PAD - 90, bottomY, 90, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);

        hoverAnnStyle = annotationStyleAt(mouseX, mouseY);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {

        if (hoverAnnStyle != null) {
            g.renderComponentHoverEffect(font, hoverAnnStyle, mouseX, mouseY);
        }
    }

    private void renderSimulator(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, simX, simY, SIM_W, simH);
        g.drawString(font, Component.translatable("wh_npcs.ui.test_dialogue.sim_conditions").getString(), simX + 5, simY + 5, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawSeparator(g, simX + 2, simY + 16, SIM_W - 4);

        DialogueNode node = node();
        int y = simY + 21;
        boolean any = false;
        if (node != null) {
            for (DialogueChoice choice : node.getChoices()) {
                for (DialogueCondition condition : choice.getConditions()) {
                    any = true;
                    boolean pass = overrides.getOrDefault(condition, true);
                    boolean hovered = isOver(mouseX, mouseY, simX + 4, y, SIM_W - 8, 11);
                    if (hovered) {
                        g.fill(simX + 4, y, simX + SIM_W - 4, y + 11, VanillaUIHelper.BG_HOVERED);
                    }
                    g.drawString(font, pass ? "§a✔" : "§c✘", simX + 5, y + 1, VanillaUIHelper.TEXT_WHITE, false);
                    g.drawString(font, font.plainSubstrByWidth(
                                    ConditionsEditorScreen.summary(condition), SIM_W - 26),
                            simX + 17, y + 1, pass ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                    y += 12;
                }
            }
        }
        if (!any) {
            g.drawString(font, Component.translatable("wh_npcs.ui.test_dialogue.no_conditions").getString(), simX + 5, y + 1, VanillaUIHelper.TEXT_WHITE, false);
            y += 12;
        }

        int logTop = y + 6;
        VanillaUIHelper.drawSeparator(g, simX + 2, logTop - 3, SIM_W - 4);
        g.drawString(font, Component.translatable("wh_npcs.ui.test_dialogue.log").getString(), simX + 5, logTop + 1, VanillaUIHelper.TEXT_GRAY, false);
        int maxLines = (simY + simH - logTop - 14) / 10;
        int from = Math.max(0, log.size() - maxLines);
        int ly = logTop + 12;
        for (int i = from; i < log.size(); i++) {
            g.drawString(font, font.plainSubstrByWidth(log.get(i), SIM_W - 12), simX + 5, ly, VanillaUIHelper.TEXT_WHITE, false);
            ly += 10;
        }
    }

    private void renderDialogue(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, textX, textY, textW, textH);
        DialogueNode node = node();
        if (node == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.test_dialogue.node_not_found", nodeId).getString(),
                    textX + textW / 2, textY + 20, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        List<String> pages = node.getPages();
        pageIndex = Math.max(0, Math.min(pageIndex, pages.size() - 1));
        List<FormattedCharSequence> lines = font.split(buildPageComponent(pageRawText()), textW - 14);
        int visibleLines = (textH - 24) / LINE_H;
        textScroll = Math.max(0, Math.min(textScroll, Math.max(0, lines.size() - visibleLines)));
        ScaledScreen.enableScissor(g, textX + 2, textY + 2, textX + textW - 2, textY + textH - 14);
        int y = textY + 6;
        for (int i = textScroll; i < Math.min(lines.size(), textScroll + visibleLines + 1); i++) {
            g.drawString(font, lines.get(i), textX + 7, y, VanillaUIHelper.TEXT_WHITE, false);
            y += LINE_H;
        }
        g.disableScissor();

        if (pages.size() > 1) {
            String pager = Component.translatable("wh_npcs.ui.test_dialogue.pager", (pageIndex + 1), pages.size()).getString();
            g.drawString(font, pager, textX + 6, textY + textH - 11, VanillaUIHelper.TEXT_STATUS, false);
        }

        if (closed) {
            g.fill(textX, choicesY, textX + textW, choicesY + CHOICE_H - 2, VanillaUIHelper.BG_HEADER);
            VanillaUIHelper.drawInsetFrame(g, textX, choicesY, textW, CHOICE_H - 2);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.test_dialogue.closed_note").getString(),
                    textX + textW / 2, choicesY + 5, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        List<TestChoice> choices = visibleChoices(node);
        int cy = choicesY;
        for (TestChoice tc : choices) {
            boolean hovered = tc.ok() && isOver(mouseX, mouseY, textX, cy, textW, CHOICE_H - 2);
            if (tc.ok()) {
                VanillaUIHelper.drawButton(g, textX, cy, textW, CHOICE_H - 2, hovered);
                g.drawString(font, font.plainSubstrByWidth(applyPlaceholders(tc.choice().getText()), textW - 16),
                        textX + 8, cy + 5, hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            } else {
                g.fill(textX, cy, textX + textW, cy + CHOICE_H - 2, VanillaUIHelper.BG_HOVERED);
                VanillaUIHelper.drawInsetFrame(g, textX, cy, textW, CHOICE_H - 2);
                String marker = Component.translatable(tc.hiddenForPlayer() ? "wh_npcs.ui.test_dialogue.marker_hidden" : "wh_npcs.ui.test_dialogue.marker_locked").getString();
                g.drawString(font, font.plainSubstrByWidth(marker + "§8" + tc.choice().getText(), textW - 16),
                        textX + 8, cy + 5, VanillaUIHelper.TEXT_WHITE, false);
            }
            cy += CHOICE_H;
        }
    }

    private void drawSmall(GuiGraphics g, String label, int x, int y, int w,
                           int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            recalc();
            if (isOver(mouseX, mouseY, winX + PAD, bottomY, 84, 18)) {
                restart();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 90, bottomY, 90, 18)) {
                onClose();
                return true;
            }
            DialogueNode node = node();
            if (node != null) {

                int y = simY + 21;
                for (DialogueChoice choice : node.getChoices()) {
                    for (DialogueCondition condition : choice.getConditions()) {
                        if (isOver(mouseX, mouseY, simX + 4, y, SIM_W - 8, 11)) {
                            overrides.merge(condition, false, (a, b) -> !a);
                            return true;
                        }
                        y += 12;
                    }
                }

                if (isOver(mouseX, mouseY, textX, textY, textW, textH)
                        && pageIndex < node.getPages().size() - 1) {
                    pageIndex++;
                    textScroll = 0;
                    return true;
                }

                if (!closed) {
                    List<TestChoice> choices = visibleChoices(node);
                    int cy = choicesY;
                    for (TestChoice tc : choices) {
                        if (isOver(mouseX, mouseY, textX, cy, textW, CHOICE_H - 2)) {
                            clickChoice(tc);
                            return true;
                        }
                        cy += CHOICE_H;
                    }
                }
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        textScroll -= (int) Math.signum(delta);
        return true;
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
