package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonParser;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.DialogueChoice;
import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import com.withouthonor.npcs.common.dialogue.DialogueNode;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.SaveDialoguePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DialogueEditorScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int LEFT_W = 112;
    private static final int NODE_ROW_H = 13;
    private static final int CHOICE_ROW_H = 24;
    private static final int VISIBLE_CHOICES = 3;

    private final DialogueGraph graph;
    @Nullable
    private final Screen parent;

    private String selectedNodeId;
    private int pageIndex;
    private int nodeScroll;
    private int choiceScroll;
    private final ScrollDrag scrollbars = new ScrollDrag();

    private com.withouthonor.npcs.client.gui.RichTextEditor pageBox;
    private boolean glossaryRequested;
    private final List<EditBox> choiceBoxes = new ArrayList<>();
    private final List<EditBox> targetBoxes = new ArrayList<>();
    private String statusText = "";
    private long statusUntil;

    private boolean confirmDelete;

    private int sortMode;

    private int draggingChoice = -1;

    private record Issue(String text, @Nullable String nodeId) {
    }

    private final java.util.Deque<String> undoStack = new java.util.ArrayDeque<>();
    private final java.util.Deque<String> redoStack = new java.util.ArrayDeque<>();
    private String lastPageValue = "";
    private long lastSnapshotMs;

    @Nullable
    private List<Issue> validationIssues;

    private int winX, winY, winW, winH;
    private int leftX, leftY, leftH;
    private int rightX, rightW;
    private int toolbarY, pageNavY, pageBoxY, pageBoxH, choicesLabelY, choiceRowsY;
    private int bottomY;

    private DialogueEditorScreen(DialogueGraph graph, @Nullable Screen parent) {
        super(Component.translatable("wh_npcs.ui.dialogue_edit.title"));
        this.graph = graph;
        this.parent = parent;
        this.selectedNodeId = graph.getNodes().containsKey(graph.getStart())
                ? graph.getStart()
                : graph.getNodes().keySet().stream().findFirst().orElse(null);
        if (selectedNodeId == null) {
            DialogueNode node = new DialogueNode();
            node.getPages().add("");
            graph.getNodes().put(graph.getStart(), node);
            selectedNodeId = graph.getStart();
        }
    }

    public static void openFromJson(String json) {
        Minecraft minecraft = Minecraft.getInstance();
        DialogueGraph graph = DialogueGraph.fromJson(JsonParser.parseString(json).getAsJsonObject());
        Screen parent = minecraft.screen instanceof NpcEditorScreen hub ? hub : null;
        minecraft.setScreen(new DialogueEditorScreen(graph, parent));
    }

    public static void openNew(String id, @Nullable Screen parent) {
        DialogueGraph graph = new DialogueGraph(id, "start");
        DialogueNode node = new DialogueNode();
        node.getPages().add("");
        graph.getNodes().put("start", node);
        Minecraft.getInstance().setScreen(new DialogueEditorScreen(graph, parent));
    }

    private DialogueNode node() {
        return graph.getNode(selectedNodeId);
    }

    private List<String> displayNodeIds() {
        List<String> ids = new ArrayList<>();
        for (String pinned : graph.getPinnedNodes()) {
            if (graph.getNodes().containsKey(pinned)) {
                ids.add(pinned);
            }
        }
        List<String> rest = new ArrayList<>();
        for (String id : graph.getNodes().keySet()) {
            if (!graph.getPinnedNodes().contains(id)) {
                rest.add(id);
            }
        }
        switch (sortMode) {
            case 1 -> java.util.Collections.reverse(rest);
            case 2 -> rest.sort(String.CASE_INSENSITIVE_ORDER);
            default -> {
            }
        }
        ids.addAll(rest);
        return ids;
    }

    private int nodeListTop() {
        return leftY + 18;
    }

    private int nodeListVisibleRows() {
        return (leftH - 28 - 16) / NODE_ROW_H;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        choiceBoxes.clear();
        targetBoxes.clear();

        DialogueNode node = node();
        if (node.getPages().isEmpty()) {
            node.getPages().add("");
        }
        pageIndex = Math.min(pageIndex, node.getPages().size() - 1);

        pageBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.RichTextEditor(
                font, rightX, pageBoxY, rightW, pageBoxH).withAnnotations());
        pageBox.setValue(node.getPages().get(pageIndex));
        if (!glossaryRequested) {
            glossaryRequested = true;
            com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                    new com.withouthonor.npcs.network.GlossaryPackets.Request());
        }
        undoStack.clear();
        redoStack.clear();
        lastPageValue = pageBox.getValue();

        List<DialogueChoice> choices = node.getChoices();
        choiceScroll = Math.max(0, Math.min(choiceScroll, Math.max(0, choices.size() - VISIBLE_CHOICES)));
        int visible = Math.min(VISIBLE_CHOICES, choices.size());
        for (int row = 0; row < visible; row++) {
            DialogueChoice choice = choices.get(choiceScroll + row);
            int y = choiceRowsY + row * CHOICE_ROW_H;
            EditBox box = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font, rightX, y,
                    rightW - 252, 18, Component.translatable("wh_npcs.ui.dialogue_edit.choice_text")));
            box.setMaxLength(120);
            box.setValue(EditorCodes.toEditor(choice.getText()));
            choiceBoxes.add(box);

            EditBox target = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font, rightX + rightW - 198, y,
                    150, 18, Component.translatable("wh_npcs.ui.dialogue_edit.target_node")));
            target.setMaxLength(64);
            target.setValue(choice.getNext() != null ? choice.getNext() : "");
            target.setHint(Component.translatable("wh_npcs.ui.dialogue_edit.target_close"));
            target.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("wh_npcs.ui.dialogue_edit.target_tip")));
            targetBoxes.add(target);
        }
    }

    private static final int WIN_W = 520;
    private static final int WIN_H = 304;

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

        leftX = winX + PAD;
        leftY = winY + HEADER_H + 4;
        bottomY = winY + winH - PAD - 20;
        leftH = bottomY - 6 - leftY;

        rightX = winX + PAD + LEFT_W + PAD;
        rightW = winX + winW - PAD - rightX - 8;

        toolbarY = leftY;
        pageNavY = toolbarY + 22;
        pageBoxY = pageNavY + 22;
        choicesLabelY = bottomY - 8 - VISIBLE_CHOICES * CHOICE_ROW_H - 24;
        choiceRowsY = choicesLabelY + 24;
        pageBoxH = choicesLabelY - 8 - pageBoxY;
    }

    private void collectCurrent() {
        DialogueNode node = node();
        if (node == null) {
            return;
        }
        if (pageBox != null && pageIndex < node.getPages().size()) {
            node.getPages().set(pageIndex, pageBox.getValue());
        }
        List<DialogueChoice> choices = node.getChoices();
        for (int row = 0; row < choiceBoxes.size(); row++) {
            int idx = choiceScroll + row;
            if (idx < choices.size()) {
                choices.get(idx).setText(EditorCodes.fromEditor(choiceBoxes.get(row).getValue()));
                String target = targetBoxes.get(row).getValue().trim();
                choices.get(idx).setNext(target.isEmpty() ? null : target);
            }
        }
    }

    private void flashStatus(String text) {
        statusText = text;
        statusUntil = System.currentTimeMillis() + 1800;
    }

    private void selectNode(String id) {
        collectCurrent();
        selectedNodeId = id;
        pageIndex = 0;
        choiceScroll = 0;
        init(minecraft, width, height);
    }

    private void addNode() {
        collectCurrent();
        int n = 1;
        while (graph.getNodes().containsKey("node_" + n)) {
            n++;
        }
        String id = "node_" + n;
        DialogueNode node = new DialogueNode();
        node.getPages().add("");
        graph.getNodes().put(id, node);
        selectNode(id);
    }

    private void performDeleteNode() {
        if (selectedNodeId.equals(graph.getStart())) {
            return;
        }
        graph.getNodes().remove(selectedNodeId);
        selectedNodeId = graph.getStart();
        pageIndex = 0;
        choiceScroll = 0;
        init(minecraft, width, height);
    }

    private void makeStart() {
        graph.setStart(selectedNodeId);
    }

    private void changePage(int delta) {
        collectCurrent();
        pageIndex = Math.max(0, Math.min(node().getPages().size() - 1, pageIndex + delta));
        init(minecraft, width, height);
    }

    private void addPage() {
        collectCurrent();
        node().getPages().add(pageIndex + 1, "");
        pageIndex++;
        init(minecraft, width, height);
    }

    private void removePage() {
        DialogueNode node = node();
        if (node.getPages().size() <= 1) {
            return;
        }
        collectCurrent();
        node.getPages().remove(pageIndex);
        pageIndex = Math.max(0, pageIndex - 1);
        init(minecraft, width, height);
    }

    private void addChoice() {
        collectCurrent();
        node().getChoices().add(new DialogueChoice(Component.translatable("wh_npcs.ui.dialogue_edit.new_choice").getString(), null));
        choiceScroll = Math.max(0, node().getChoices().size() - VISIBLE_CHOICES);
        init(minecraft, width, height);
    }

    private void removeChoice(int index) {
        collectCurrent();
        node().getChoices().remove(index);
        init(minecraft, width, height);
    }

    private void save() {
        collectCurrent();
        NetworkHandler.sendToServer(new SaveDialoguePacket(
                graph.toJson().toString().getBytes(StandardCharsets.UTF_8)));
        flashStatus(Component.translatable("wh_npcs.ui.dialogue_edit.status_sent").getString());

        if (parent instanceof NpcEditorScreen hub) {
            hub.onDialogueSaved(graph.getId(), graph.getNodes().size());
        }
    }

    private void validate() {
        collectCurrent();
        List<Issue> issues = new ArrayList<>();

        for (var entry : graph.getNodes().entrySet()) {
            String nodeId = entry.getKey();
            DialogueNode node = entry.getValue();
            boolean allBlank = node.getPages().stream().allMatch(String::isBlank);
            if (allBlank) {
                issues.add(new Issue(Component.translatable("wh_npcs.ui.dialogue_edit.issue_empty_pages", nodeId).getString(), nodeId));
            }
            for (int i = 0; i < node.getChoices().size(); i++) {
                DialogueChoice choice = node.getChoices().get(i);
                if (choice.getText().isBlank()) {
                    issues.add(new Issue(Component.translatable("wh_npcs.ui.dialogue_edit.issue_choice_no_text", nodeId, (i + 1)).getString(), nodeId));
                }
                if (choice.getNext() != null && !graph.getNodes().containsKey(choice.getNext())) {
                    issues.add(new Issue(Component.translatable("wh_npcs.ui.dialogue_edit.issue_choice_bad_target", nodeId, (i + 1), choice.getNext()).getString(), nodeId));
                }
            }
            if (node.getChoices().isEmpty()) {
                issues.add(new Issue(Component.translatable("wh_npcs.ui.dialogue_edit.issue_no_choices", nodeId).getString(), nodeId));
            }
        }

        java.util.Set<String> reachable = new java.util.HashSet<>();
        java.util.Deque<String> queue = new java.util.ArrayDeque<>();
        queue.add(graph.getStart());
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (!reachable.add(id)) {
                continue;
            }
            DialogueNode node = graph.getNode(id);
            if (node != null) {
                for (DialogueChoice choice : node.getChoices()) {
                    if (choice.getNext() != null) {
                        queue.add(choice.getNext());
                    }
                }
            }
        }
        for (String id : graph.getNodes().keySet()) {
            if (!reachable.contains(id)) {
                issues.add(new Issue(Component.translatable("wh_npcs.ui.dialogue_edit.issue_unreachable", id).getString(), id));
            }
        }
        validationIssues = issues;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);

        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.dialogue_edit.header", graph.getId()).getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        String nodeLabel = Component.translatable("wh_npcs.ui.dialogue_edit.node_label", selectedNodeId).getString();
        g.drawString(font, nodeLabel, winX + winW - PAD - font.width(nodeLabel), winY + 7,
                VanillaUIHelper.TEXT_GRAY, false);

        renderNodeList(g, mouseX, mouseY);
        renderToolbar(g, mouseX, mouseY);
        renderPageNav(g, mouseX, mouseY);
        renderChoices(g, mouseX, mouseY);
        trackUndoSnapshots();

        drawSmall(g, Component.translatable("wh_npcs.ui.dialogue_edit.validate").getString(), winX + PAD, bottomY, 76, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawSmall(g, Component.translatable("wh_npcs.ui.dialogue_edit.test").getString(), winX + PAD + 82, bottomY, 62, mouseX, mouseY, VanillaUIHelper.TEXT_GOLD);
        drawSmall(g, Component.translatable("wh_npcs.ui.common.save").getString(), saveBtnX(), bottomY, 90, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        drawSmall(g, Component.translatable("wh_npcs.ui.common.back").getString(), saveBtnX() + 96, bottomY, 70, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (confirmDelete) {
            renderConfirmDelete(g, mouseX, mouseY);
        }
        if (validationIssues != null) {
            renderValidation(g, mouseX, mouseY);
        }
    }

    private void renderValidation(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, VanillaUIHelper.BG_DARK);
        int shown = Math.min(validationIssues.size(), 11);
        int bw = 380;
        int bh = 58 + shown * 12;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;
        VanillaUIHelper.drawWindow(g, bx, by, bw, bh);
        g.fill(bx + 2, by + 2, bx + bw - 2, by + 18, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.dialogue_edit.validation_title").getString(), bx + 8, by + 6, VanillaUIHelper.TEXT_YELLOW, false);
        if (validationIssues.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.dialogue_edit.no_issues").getString(), bx + bw / 2, by + 28, VanillaUIHelper.TEXT_WHITE);
        } else {
            int y = by + 24;
            boolean anyJump = false;
            for (int i = 0; i < shown; i++) {
                Issue issue = validationIssues.get(i);
                boolean clickable = issue.nodeId() != null && graph.getNodes().containsKey(issue.nodeId());
                boolean hovered = clickable && isOver(mouseX, mouseY, bx + 6, y - 1, bw - 12, 12);
                if (hovered) {
                    g.fill(bx + 6, y - 1, bx + bw - 6, y + 11, VanillaUIHelper.BG_HOVERED);
                    anyJump = true;
                }
                g.drawString(font, font.plainSubstrByWidth(issue.text(), bw - 16),
                        bx + 8, y, VanillaUIHelper.TEXT_WHITE, false);
                y += 12;
            }
            if (validationIssues.size() > shown) {
                g.drawString(font, Component.translatable("wh_npcs.ui.dialogue_edit.and_more", (validationIssues.size() - shown)).getString(), bx + 8, y, VanillaUIHelper.TEXT_WHITE, false);
            }
            if (anyJump) {
                multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.click_to_node").getString(), mouseX, mouseY);
            }
        }
        boolean closeHover = isOver(mouseX, mouseY, bx + bw - 86, by + bh - 26, 78, 18);
        VanillaUIHelper.drawButton(g, bx + bw - 86, by + bh - 26, 78, 18, closeHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.common.close").getString(), bx + bw - 47, by + bh - 21,
                closeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        g.pose().popPose();
    }

    private void renderConfirmDelete(GuiGraphics g, int mouseX, int mouseY) {

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, VanillaUIHelper.BG_DARK);
        int bw = 240;
        int bh = 72;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;
        VanillaUIHelper.drawWindow(g, bx, by, bw, bh);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.dialogue_edit.delete_node_q", selectedNodeId).getString(),
                bx + bw / 2, by + 14, VanillaUIHelper.TEXT_WHITE);
        drawSmall(g, Component.translatable("wh_npcs.ui.common.delete").getString(), confirmYesX(), confirmBtnY(), 90, mouseX, mouseY, VanillaUIHelper.TEXT_RED);
        drawSmall(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), confirmYesX() + 96, confirmBtnY(), 90, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        g.pose().popPose();
    }

    private int confirmYesX() {
        return (width - 186) / 2;
    }

    private int confirmBtnY() {
        return (height - 72) / 2 + 40;
    }

    private int saveBtnX() {
        return winX + winW - PAD - 166;
    }

    private void renderNodeList(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, leftX, leftY, LEFT_W, leftH);

        g.drawString(font, Component.translatable("wh_npcs.ui.dialogue_edit.nodes").getString(), leftX + 5, leftY + 5, VanillaUIHelper.TEXT_GRAY, false);
        boolean sortHover = isOver(mouseX, mouseY, sortIconX(), leftY + 3, 14, 12);
        String sortIcon = switch (sortMode) {
            case 1 -> "↓";
            case 2 -> Component.translatable("wh_npcs.ui.dialogue_edit.sort_icon_alpha").getString();
            default -> "↑";
        };
        g.drawString(font, sortIcon, sortIconX() + 4, leftY + 4,
                sortHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY, false);
        if (sortHover) {
            String sortLabel = switch (sortMode) {
                case 1 -> Component.translatable("wh_npcs.ui.dialogue_edit.sort_newest").getString();
                case 2 -> Component.translatable("wh_npcs.ui.dialogue_edit.sort_alpha").getString();
                default -> Component.translatable("wh_npcs.ui.dialogue_edit.sort_oldest").getString();
            };
            multilineTooltip(g, sortLabel + "\n" + Component.translatable("wh_npcs.ui.dialogue_edit.sort_next").getString(), mouseX, mouseY);
        }
        VanillaUIHelper.drawSeparator(g, leftX + 2, leftY + 15, LEFT_W - 4);

        List<String> ids = displayNodeIds();
        int visibleRows = nodeListVisibleRows();
        nodeScroll = Math.max(0, Math.min(nodeScroll, Math.max(0, ids.size() - visibleRows)));
        int y = nodeListTop();
        for (int i = nodeScroll; i < Math.min(ids.size(), nodeScroll + visibleRows); i++) {
            String id = ids.get(i);
            boolean selected = id.equals(selectedNodeId);
            boolean pinned = graph.getPinnedNodes().contains(id);
            boolean hovered = isOver(mouseX, mouseY, leftX + 2, y, LEFT_W - 4, NODE_ROW_H);
            if (selected || hovered) {
                g.fill(leftX + 2, y, leftX + LEFT_W - 2, y + NODE_ROW_H,
                        selected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            String label = (id.equals(graph.getStart()) ? "▶ " : "  ") + id;
            g.drawString(font, font.plainSubstrByWidth(label, LEFT_W - 36), leftX + 4, y + 2,
                    selected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (pinned || hovered) {
                boolean pinHover = isOver(mouseX, mouseY, pinIconX(), y + 1, 12, NODE_ROW_H - 2);
                drawPinIcon(g, pinIconX(), y + 3, pinned, pinHover);
                if (pinHover) {
                    multilineTooltip(g, Component.translatable(pinned ? "wh_npcs.ui.dialogue_edit.unpin" : "wh_npcs.ui.dialogue_edit.pin").getString(), mouseX, mouseY);
                }
            }
            if (hovered) {
                boolean iconHover = isOver(mouseX, mouseY, copyIconX(), y + 1, 12, NODE_ROW_H - 2);
                drawCopyIcon(g, copyIconX(), y + 2, iconHover);
                if (iconHover) {
                    g.renderTooltip(font, Component.translatable("wh_npcs.ui.dialogue_edit.copy_id"), mouseX, mouseY);
                }
            }
            y += NODE_ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, leftX + LEFT_W - 6, nodeListTop(), leftH - 28 - 16,
                ids.size(), visibleRows, nodeScroll, scrollbars, v -> nodeScroll = v);
        drawSmall(g, Component.translatable("wh_npcs.ui.dialogue_edit.add_node").getString(), leftX + 2, leftY + leftH - 20, LEFT_W - 4, mouseX, mouseY,
                VanillaUIHelper.TEXT_GREEN);
    }

    private int copyIconX() {
        return leftX + LEFT_W - 16;
    }

    private int pinIconX() {
        return leftX + LEFT_W - 30;
    }

    private int sortIconX() {
        return leftX + LEFT_W - 20;
    }

    private static void drawPinIcon(GuiGraphics g, int x, int y, boolean pinned, boolean hovered) {
        int c = pinned ? VanillaUIHelper.TEXT_GOLD
                : (hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY);
        g.fill(x + 1, y, x + 7, y + 4, c);
        g.fill(x + 3, y + 4, x + 5, y + 8, c);
    }

    private static void drawCopyIcon(GuiGraphics g, int x, int y, boolean hovered) {
        int c = hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY;

        g.fill(x, y, x + 7, y + 1, c);
        g.fill(x, y, x + 1, y + 7, c);

        g.fill(x + 3, y + 3, x + 10, y + 4, c);
        g.fill(x + 3, y + 9, x + 10, y + 10, c);
        g.fill(x + 3, y + 3, x + 4, y + 10, c);
        g.fill(x + 9, y + 3, x + 10, y + 10, c);
    }

    private void renderToolbar(GuiGraphics g, int mouseX, int mouseY) {
        boolean isStart = selectedNodeId.equals(graph.getStart());

        boolean delHover = !isStart && isOver(mouseX, mouseY, rightX, toolbarY, 18, 18);
        VanillaUIHelper.drawButton(g, rightX, toolbarY, 18, 18, delHover);
        drawTrash(g, rightX + 4, toolbarY + 4, isStart ? VanillaUIHelper.TEXT_DARK_GRAY
                : (delHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_RED));
        int startX = rightX + 22;
        drawSmall(g, Component.translatable(isStart ? "wh_npcs.ui.dialogue_edit.is_start" : "wh_npcs.ui.dialogue_edit.make_start").getString(), startX, toolbarY, 120, mouseX, mouseY,
                isStart ? VanillaUIHelper.TEXT_DARK_GRAY : VanillaUIHelper.TEXT_WHITE);
        int photoX = startX + 124;
        int n = node().getImages().size();
        drawSmall(g, n > 0 ? Component.translatable("wh_npcs.ui.dialogue_edit.photo_n", n).getString() : Component.translatable("wh_npcs.ui.dialogue_edit.photo").getString(), photoX, toolbarY, 56, mouseX, mouseY,
                n > 0 ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_WHITE);
        int musicX = photoX + 60;
        boolean hasMusic = !node().getMusicDisc().isEmpty();
        drawSmall(g, Component.translatable(hasMusic ? "wh_npcs.ui.dialogue_edit.music_on" : "wh_npcs.ui.dialogue_edit.music").getString(), musicX, toolbarY, 66, mouseX, mouseY,
                hasMusic ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_WHITE);

        int scX = musicX + 68;
        boolean hasSecond = !node().getSecondCharPreset().isEmpty();
        drawSmall(g, Component.translatable(hasSecond ? "wh_npcs.ui.dialogue_edit.second_char_on" : "wh_npcs.ui.dialogue_edit.second_char").getString(), scX, toolbarY, 22, mouseX, mouseY,
                hasSecond ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_WHITE);
        int glX = winX + winW - PAD - 86;
        drawSmall(g, Component.translatable("wh_npcs.ui.dialogue_edit.glossary").getString(), glX, toolbarY, 86, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);

        if (isOver(mouseX, mouseY, rightX, toolbarY, 18, 18)) {
            multilineTooltip(g, Component.translatable(isStart
                    ? "wh_npcs.ui.dialogue_edit.tip_delete_start"
                    : "wh_npcs.ui.dialogue_edit.tip_delete").getString(), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, photoX, toolbarY, 56, 18)) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.tip_photo").getString(), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, musicX, toolbarY, 66, 18)) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.tip_music").getString(), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, scX, toolbarY, 22, 18)) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.tip_second_char").getString(), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, glX, toolbarY, 86, 18)) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.tip_glossary").getString(), mouseX, mouseY);
        }
    }

    private static void drawTrash(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 3, y, x + 7, y + 1, c);
        g.fill(x, y + 1, x + 10, y + 3, c);
        g.fill(x + 1, y + 3, x + 9, y + 11, c);
    }

    private void renderPageNav(GuiGraphics g, int mouseX, int mouseY) {
        DialogueNode node = node();
        String label = Component.translatable("wh_npcs.ui.dialogue_edit.page_n", (pageIndex + 1), node.getPages().size()).getString();
        g.drawString(font, label, rightX, pageNavY + 5, VanillaUIHelper.TEXT_GRAY, false);

        String mode = Component.translatable(node.isRandomPage() ? "wh_npcs.ui.dialogue_edit.page_random" : "wh_npcs.ui.dialogue_edit.page_all").getString();
        drawSmall(g, mode, modeBtnX(), pageNavY, 124, mouseX, mouseY,
                node.isRandomPage() ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_WHITE);
        if (isOver(mouseX, mouseY, modeBtnX(), pageNavY, 124, 18)) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.tip_page_mode").getString(), mouseX, mouseY);
        }

        boolean special = !"text".equals(node.getType());
        drawSmall(g, nodeTypeLabel(node.getType()), rightX + 224, pageNavY, 64, mouseX, mouseY,
                special ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_DARK_GRAY);
        if (isOver(mouseX, mouseY, rightX + 224, pageNavY, 64, 18)) {
            multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.tip_node_type").getString(), mouseX, mouseY);
        }

        drawSmall(g, "<", rightX + rightW - 84, pageNavY, 18, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        drawSmall(g, ">", rightX + rightW - 62, pageNavY, 18, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        drawSmall(g, "+", rightX + rightW - 40, pageNavY, 18, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
        drawSmall(g, "-", rightX + rightW - 18, pageNavY, 18, mouseX, mouseY, VanillaUIHelper.TEXT_RED);
    }

    private int modeBtnX() {
        return rightX + 96;
    }

    private static String nodeTypeLabel(String type) {
        return switch (type) {
            case "input" -> Component.translatable("wh_npcs.ui.node_type.type_input").getString();
            case "check" -> Component.translatable("wh_npcs.ui.node_type.type_check").getString();
            case "random" -> Component.translatable("wh_npcs.ui.node_type.type_random").getString();
            default -> Component.translatable("wh_npcs.ui.node_type.type_text").getString();
        };
    }

    private void trackUndoSnapshots() {
        if (pageBox == null) {
            return;
        }
        String value = pageBox.getValue();
        if (!value.equals(lastPageValue) && System.currentTimeMillis() - lastSnapshotMs > 600) {
            pushUndoSnapshot();
            lastPageValue = value;
        }
    }

    private void pushUndoSnapshot() {
        undoStack.push(lastPageValue);
        if (undoStack.size() > 50) {
            undoStack.removeLast();
        }
        redoStack.clear();
        lastSnapshotMs = System.currentTimeMillis();
    }

    private void undo() {
        if (pageBox != null && !undoStack.isEmpty()) {
            redoStack.push(pageBox.getValue());
            String prev = undoStack.pop();
            pageBox.setValue(prev);
            lastPageValue = prev;
        }
    }

    private void redo() {
        if (pageBox != null && !redoStack.isEmpty()) {
            undoStack.push(pageBox.getValue());
            String next = redoStack.pop();
            pageBox.setValue(next);
            lastPageValue = next;
        }
    }

    private void renderChoices(GuiGraphics g, int mouseX, int mouseY) {
        List<DialogueChoice> choices = node().getChoices();
        g.drawString(font, Component.translatable("wh_npcs.ui.dialogue_edit.choices_n", choices.size()).getString(), rightX, choicesLabelY + 5,
                VanillaUIHelper.TEXT_GRAY, false);
        drawSmall(g, Component.translatable("wh_npcs.ui.dialogue_edit.add_choice").getString(), rightX + rightW - 66, choicesLabelY, 66, mouseX, mouseY,
                VanillaUIHelper.TEXT_GREEN);

        for (EditBox target : targetBoxes) {
            String value = target.getValue().trim();
            target.setTextColor(value.isEmpty() || graph.getNodes().containsKey(value)
                    ? 0x55FFFF : 0xFF5555);
        }

        int visible = Math.min(VISIBLE_CHOICES, choices.size());
        for (int row = 0; row < visible; row++) {
            int idx = choiceScroll + row;
            DialogueChoice choice = choices.get(idx);
            int y = choiceRowsY + row * CHOICE_ROW_H;
            if (idx == draggingChoice) {
                g.fill(rightX - 3, y - 2, rightX + rightW + 3, y + 20, VanillaUIHelper.BG_SELECTED);
            }

            int n = choice.getConditions().size();
            boolean condHover = isOver(mouseX, mouseY, condBtnX(), y, 18, 18);
            VanillaUIHelper.drawButton(g, condBtnX(), y, 18, 18, condHover);
            drawFunnelIcon(g, condBtnX() + (n > 0 ? 2 : 4), y + 4,
                    condHover ? VanillaUIHelper.TEXT_YELLOW
                            : (n > 0 ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_GRAY));
            if (n > 0) {
                drawCountBadge(g, condBtnX(), y, n);
            }
            if (condHover) {
                String suffix = n > 0 ? " (" + n + ")" : "";
                multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.tip_conditions", suffix).getString(), mouseX, mouseY);
            }
            int actionCount = choice.getActions().size();
            boolean actHover = isOver(mouseX, mouseY, actBtnX(), y, 18, 18);
            VanillaUIHelper.drawButton(g, actBtnX(), y, 18, 18, actHover);
            drawBoltIcon(g, actBtnX() + (actionCount > 0 ? 2 : 5), y + 4,
                    actHover ? VanillaUIHelper.TEXT_YELLOW
                            : (actionCount > 0 ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_GRAY));
            if (actionCount > 0) {
                drawCountBadge(g, actBtnX(), y, actionCount);
            }
            if (actHover) {
                String suffix = actionCount > 0 ? " (" + actionCount + ")" : "";
                multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.tip_actions", suffix).getString(), mouseX, mouseY);
            }

            boolean gripHover = isOver(mouseX, mouseY, gripX(), y, 14, 18) || idx == draggingChoice;
            drawGripIcon(g, gripX() + 3, y + 5, gripHover);
            if (gripHover && draggingChoice < 0) {
                multilineTooltip(g, Component.translatable("wh_npcs.ui.dialogue_edit.drag_reorder").getString(), mouseX, mouseY);
            }
            drawSmall(g, "×", rightX + rightW - 22, y, 18, mouseX, mouseY, VanillaUIHelper.TEXT_RED);
        }

        for (int row = 0; row < choiceBoxes.size(); row++) {
            EditBox box = choiceBoxes.get(row);
            box.setTooltip(font.width(box.getValue()) > box.getWidth() - 10
                    ? net.minecraft.client.gui.components.Tooltip.create(Component.literal(box.getValue()))
                    : null);
        }

        if (draggingChoice >= 0 && draggingChoice < choices.size()) {
            String text = choices.get(draggingChoice).getText();
            g.pose().pushPose();
            g.pose().translate(mouseX + 10, mouseY - 10, 350);
            g.pose().scale(1.15F, 1.15F, 1.0F);
            int w = Math.min(160, font.width(text) + 26);
            VanillaUIHelper.drawButton(g, 0, 0, w, 18, true);
            drawGripIcon(g, 4, 5, true);
            g.drawString(font, font.plainSubstrByWidth(text, w - 22), 16, 5,
                    VanillaUIHelper.TEXT_YELLOW, false);
            g.pose().popPose();
        }
        VanillaUIHelper.drawScrollbar(g, rightX + rightW + 4, choiceRowsY,
                VISIBLE_CHOICES * CHOICE_ROW_H - 6, choices.size(), visible, choiceScroll,
                scrollbars, v -> choiceScroll = v);
        if (System.currentTimeMillis() < statusUntil) {
            g.drawString(font, font.plainSubstrByWidth(statusText, saveBtnX() - winX - PAD - 160),
                    winX + PAD + 152, bottomY + 5, VanillaUIHelper.TEXT_GREEN, false);
        }
    }

    private int gripX() {
        return rightX + rightW - 44;
    }

    private int condBtnX() {
        return rightX + rightW - 246;
    }

    private int actBtnX() {
        return rightX + rightW - 224;
    }

    private void drawCountBadge(GuiGraphics g, int btnX, int btnY, int count) {
        g.pose().pushPose();
        g.pose().translate(btnX + 12.5F, btnY + 8.5F, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(font, String.valueOf(Math.min(count, 9)), 0, 0, VanillaUIHelper.TEXT_GOLD, false);
        g.pose().popPose();
    }

    private static void drawFunnelIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 9, y + 2, c);
        g.fill(x + 1, y + 2, x + 8, y + 3, c);
        g.fill(x + 2, y + 3, x + 7, y + 4, c);
        g.fill(x + 3, y + 4, x + 6, y + 5, c);
        g.fill(x + 4, y + 5, x + 5, y + 10, c);
    }

    private static void drawBoltIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 3, y, x + 7, y + 1, c);
        g.fill(x + 2, y + 1, x + 6, y + 2, c);
        g.fill(x + 1, y + 2, x + 5, y + 3, c);
        g.fill(x, y + 3, x + 7, y + 4, c);
        g.fill(x + 2, y + 4, x + 6, y + 5, c);
        g.fill(x + 1, y + 5, x + 4, y + 6, c);
        g.fill(x, y + 6, x + 3, y + 7, c);
        g.fill(x, y + 7, x + 2, y + 8, c);
        g.fill(x, y + 8, x + 1, y + 9, c);
    }

    private static void drawGripIcon(GuiGraphics g, int x, int y, boolean hovered) {
        int c = hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY;
        for (int i = 0; i < 3; i++) {
            g.fill(x, y + i * 3, x + 8, y + i * 3 + 1, c);
        }
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
        VanillaUIHelper.drawSmallButton(g, font, label, x, y, w, isOver(mouseX, mouseY, x, y, w, 18), color);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (validationIssues != null) {
            if (button == 0) {
                int shown = Math.min(validationIssues.size(), 11);
                int bw = 380;
                int bh = 58 + shown * 12;
                int bx = (width - bw) / 2;
                int by = (height - bh) / 2;

                for (int i = 0; i < shown; i++) {
                    Issue issue = validationIssues.get(i);
                    if (issue.nodeId() != null && graph.getNodes().containsKey(issue.nodeId())
                            && isOver(mouseX, mouseY, bx + 6, by + 23 + i * 12, bw - 12, 12)) {
                        validationIssues = null;
                        selectNode(issue.nodeId());
                        return true;
                    }
                }
                if (isOver(mouseX, mouseY, bx + bw - 86, by + bh - 26, 78, 18)
                        || !isOver(mouseX, mouseY, bx, by, bw, bh)) {
                    validationIssues = null;
                }
            }
            return true;
        }
        if (confirmDelete) {
            if (button == 0) {
                if (isOver(mouseX, mouseY, confirmYesX(), confirmBtnY(), 90, 18)) {
                    confirmDelete = false;
                    performDeleteNode();
                } else if (isOver(mouseX, mouseY, confirmYesX() + 96, confirmBtnY(), 90, 18)) {
                    confirmDelete = false;
                }

            }
            return true;
        }
        if (button == 0) {
            recalc();
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }

            if (getChildAt(mouseX, mouseY).isEmpty()) {
                if (pageBox != null && pageBox.isFocused()) {
                    pageBox.deselect();
                }
                setFocused(null);
            }

            if (isOver(mouseX, mouseY, sortIconX(), leftY + 3, 14, 12)) {
                sortMode = (sortMode + 1) % 3;
                return true;
            }

            List<String> ids = displayNodeIds();
            int visibleRows = nodeListVisibleRows();
            int y = nodeListTop();
            for (int i = nodeScroll; i < Math.min(ids.size(), nodeScroll + visibleRows); i++) {
                if (isOver(mouseX, mouseY, leftX + 2, y, LEFT_W - 4, NODE_ROW_H)) {
                    String id = ids.get(i);
                    if (isOver(mouseX, mouseY, copyIconX(), y + 1, 12, NODE_ROW_H - 2) && minecraft != null) {
                        minecraft.keyboardHandler.setClipboard(id);
                        flashStatus(Component.translatable("wh_npcs.ui.dialogue_edit.id_copied", id).getString());
                    } else if (isOver(mouseX, mouseY, pinIconX(), y + 1, 12, NODE_ROW_H - 2)) {
                        if (!graph.getPinnedNodes().remove(id)) {
                            graph.getPinnedNodes().add(id);
                        }
                    } else {
                        selectNode(id);
                    }
                    return true;
                }
                y += NODE_ROW_H;
            }
            if (isOver(mouseX, mouseY, leftX + 2, leftY + leftH - 20, LEFT_W - 4, 18)) {
                addNode();
                return true;
            }

            if (isOver(mouseX, mouseY, rightX, toolbarY, 18, 18)) {
                if (!selectedNodeId.equals(graph.getStart())) {
                    confirmDelete = true;
                }
                return true;
            }
            int startX = rightX + 22;
            if (isOver(mouseX, mouseY, startX, toolbarY, 120, 18)) {
                makeStart();
                return true;
            }
            int photoX = startX + 124;
            if (isOver(mouseX, mouseY, photoX, toolbarY, 56, 18)) {
                collectCurrent();
                if (minecraft != null) {
                    minecraft.setScreen(new NodeImagesScreen(this, node()));
                }
                return true;
            }
            if (isOver(mouseX, mouseY, photoX + 60, toolbarY, 66, 18)) {
                collectCurrent();
                if (minecraft != null) {
                    minecraft.setScreen(new NodeMusicScreen(this, node()));
                }
                return true;
            }

            if (isOver(mouseX, mouseY, photoX + 60 + 68, toolbarY, 22, 18)) {
                collectCurrent();
                if (minecraft != null) {
                    minecraft.setScreen(new SecondCharPickerScreen(this, node()));
                }
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 86, toolbarY, 86, 18)) {
                collectCurrent();
                GlossaryScreen.open(this);
                return true;
            }

            if (isOver(mouseX, mouseY, modeBtnX(), pageNavY, 124, 18)) {
                node().setRandomPage(!node().isRandomPage());
                return true;
            }

            if (isOver(mouseX, mouseY, rightX + 224, pageNavY, 64, 18)) {
                collectCurrent();
                if (minecraft != null) {
                    minecraft.setScreen(new NodeTypeScreen(this, node()));
                }
                return true;
            }

            if (isOver(mouseX, mouseY, rightX + rightW - 84, pageNavY, 18, 18)) {
                changePage(-1);
                return true;
            }
            if (isOver(mouseX, mouseY, rightX + rightW - 62, pageNavY, 18, 18)) {
                changePage(1);
                return true;
            }
            if (isOver(mouseX, mouseY, rightX + rightW - 40, pageNavY, 18, 18)) {
                addPage();
                return true;
            }
            if (isOver(mouseX, mouseY, rightX + rightW - 18, pageNavY, 18, 18)) {
                removePage();
                return true;
            }

            if (isOver(mouseX, mouseY, rightX + rightW - 66, choicesLabelY, 66, 18)) {
                addChoice();
                return true;
            }
            List<DialogueChoice> choices = node().getChoices();
            int visible = Math.min(VISIBLE_CHOICES, choices.size());
            for (int row = 0; row < visible; row++) {
                int cy = choiceRowsY + row * CHOICE_ROW_H;
                if (isOver(mouseX, mouseY, condBtnX(), cy, 18, 18)) {
                    collectCurrent();
                    ConditionsEditorScreen.open(this, choices.get(choiceScroll + row));
                    return true;
                }
                if (isOver(mouseX, mouseY, actBtnX(), cy, 18, 18)) {
                    collectCurrent();
                    ActionsEditorScreen.open(this, choices.get(choiceScroll + row));
                    return true;
                }
                if (isOver(mouseX, mouseY, gripX(), cy, 14, 18)) {
                    collectCurrent();
                    draggingChoice = choiceScroll + row;
                    return true;
                }
                if (isOver(mouseX, mouseY, rightX + rightW - 22, cy, 18, 18)) {
                    removeChoice(choiceScroll + row);
                    return true;
                }
            }

            if (isOver(mouseX, mouseY, winX + PAD, bottomY, 76, 18)) {
                validate();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + PAD + 82, bottomY, 62, 18)) {
                collectCurrent();
                if (minecraft != null) {
                    minecraft.setScreen(new TestDialogueScreen(this, graph));
                }
                return true;
            }
            if (isOver(mouseX, mouseY, saveBtnX(), bottomY, 90, 18)) {
                save();
                return true;
            }
            if (isOver(mouseX, mouseY, saveBtnX() + 96, bottomY, 70, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        if (draggingChoice >= 0 && button == 0) {
            List<DialogueChoice> choices = node().getChoices();
            int row = (int) Math.floor((mouseY - choiceRowsY) / CHOICE_ROW_H);
            int target = Math.max(0, Math.min(choices.size() - 1,
                    choiceScroll + Math.max(0, Math.min(VISIBLE_CHOICES - 1, row))));
            if (target != draggingChoice) {
                collectCurrent();
                DialogueChoice moved = choices.remove(draggingChoice);
                choices.add(target, moved);
                draggingChoice = target;
                init(minecraft, width, height);
            }
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        if (draggingChoice >= 0 && button == 0) {
            draggingChoice = -1;
            return true;
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (validationIssues != null) {
                validationIssues = null;
                return true;
            }
            if (confirmDelete) {
                confirmDelete = false;
                return true;
            }
        }

        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE
                && getFocused() == null
                && !confirmDelete && validationIssues == null
                && !selectedNodeId.equals(graph.getStart())) {
            confirmDelete = true;
            return true;
        }

        if (hasControlDown() && getFocused() == pageBox) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_Z) {
                if (hasShiftDown()) {
                    redo();
                } else {
                    undo();
                }
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_Y) {
                redo();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        if (confirmDelete) {
            return true;
        }
        recalc();
        if (isOver(mouseX, mouseY, leftX, leftY, LEFT_W, leftH)) {
            nodeScroll -= (int) Math.signum(delta);
            return true;
        }
        if (isOver(mouseX, mouseY, rightX, choiceRowsY, rightW, VISIBLE_CHOICES * CHOICE_ROW_H)) {
            collectCurrent();
            choiceScroll = Math.max(0, Math.min(choiceScroll - (int) Math.signum(delta),
                    Math.max(0, node().getChoices().size() - VISIBLE_CHOICES)));
            init(minecraft, width, height);
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        collectCurrent();
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
