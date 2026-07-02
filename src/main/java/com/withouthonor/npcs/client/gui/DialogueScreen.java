package com.withouthonor.npcs.client.gui;

import com.withouthonor.npcs.client.audio.ClientNpcAudio;
import com.withouthonor.npcs.network.DialogueChoicePacket;
import com.withouthonor.npcs.network.DialogueInputPacket;
import com.withouthonor.npcs.network.DialogueNodeData;
import com.withouthonor.npcs.network.FollowControlPacket;
import com.withouthonor.npcs.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

public class DialogueScreen extends ScaledScreen {

    private static final int PAD = 10;
    private static final int HEADER_H = 24;
    private static final int PORTRAIT_W = 110;
    private static final int CHOICE_BTN_H = 24;
    private static final int CHOICE_STRIDE = 28;

    private static final int VISIBLE_ROWS = 4;
    private static final int LINE_H = 11;
    private static final int PAGER_H = 18;
    private static final net.minecraft.resources.ResourceLocation EMOTE_ATLAS =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wh_npcs", "textures/entity/emotes.png");

    private DialogueNodeData data;
    private int pageIndex;
    private int textScroll;
    private int choiceScroll;
    private final ScrollDrag scrollbars = new ScrollDrag();
    private boolean serverClosed;
    @Nullable
    private List<FormattedCharSequence> cachedLines;

    private static final long MUSIC_IN_MS = 320L;
    private static final long MUSIC_OUT_MS = 260L;
    @Nullable
    private String shownMusicId;
    private String shownMusicLabel = "";
    private boolean musicShowing;
    private long musicAnimMs;
    @Nullable
    private String musicTooltip;
    @Nullable
    private String followTooltip;
    @Nullable
    private String choiceHintTooltip;
    @Nullable
    private EditBox inputBox;

    private int winX, winY, winW, winH;
    private int textX, textY, textW, textH;
    private int choicesX, choicesY, choicesW;
    private int pagerY;
    private boolean hasPager;
    private boolean hasSecondChar;
    private int secondX;

    @Nullable
    private LivingEntity secondGhost;
    private String secondGhostSkin = "";

    public DialogueScreen(DialogueNodeData data) {
        super(Component.literal(data.npcName()));
        this.data = data;
        prefetchImages();
    }

    public void updateNode(DialogueNodeData data) {
        this.data = data;
        this.pageIndex = 0;
        this.textScroll = 0;
        this.choiceScroll = 0;
        this.cachedLines = null;
        prefetchImages();
        rebuildInputWidget();
    }

    private void rebuildInputWidget() {
        if (inputBox != null) {
            removeWidget(inputBox);
            inputBox = null;
        }
        if (data.inputMode()) {
            recalc();
            inputBox = new EditBox(font, choicesX, choicesY + 1, choicesW - 60, 18, Component.empty());
            inputBox.setMaxLength(256);
            if (!data.inputHint().isEmpty()) {
                inputBox.setHint(Component.literal(data.inputHint()));
            }
            addRenderableWidget(inputBox);
            setFocused(inputBox);
        }
    }

    private void submitInput() {
        if (inputBox == null) {
            return;
        }
        NetworkHandler.sendToServer(new DialogueInputPacket(data.dialogueId(), data.nodeId(), inputBox.getValue()));
        inputBox.setValue("");
    }

    private void prefetchImages() {
        data.images().forEach(img ->
                com.withouthonor.npcs.client.cache.ClientImageCache.getInstance().get(img.file()));
        if (data.npcPortraitShow() && !data.npcPortrait().isEmpty()) {
            com.withouthonor.npcs.client.cache.ClientImageCache.getInstance().get(data.npcPortrait());
        }
        if (data.secondCharPortraitShow() && !data.secondCharPortrait().isEmpty()) {
            com.withouthonor.npcs.client.cache.ClientImageCache.getInstance().get(data.secondCharPortrait());
        }
    }

    private int imageLinksH() {
        return data.images().isEmpty() ? 0 : data.images().size() * 12 + 5;
    }

    private int imageLinkY(int index) {
        return textY + textH - imageLinksH() + 3 + index * 12;
    }

    public void closeFromServer() {
        serverClosed = true;
        onClose();
    }

    @Override
    protected void init() {
        cachedLines = null;
        rebuildInputWidget();
    }

    @Override
    protected int designW() {
        return 540;
    }

    @Override
    protected int designH() {

        int choicesAreaH = VISIBLE_ROWS * CHOICE_STRIDE - (CHOICE_STRIDE - CHOICE_BTN_H);
        return HEADER_H + PAD + 112 + 6 + PAGER_H + 10 + choicesAreaH + PAD;
    }

    private void recalc() {
        hasPager = data.pages().size() > 1;

        int choicesAreaH = VISIBLE_ROWS * CHOICE_STRIDE - (CHOICE_STRIDE - CHOICE_BTN_H);

        hasSecondChar = !data.secondCharSkin().isEmpty()
                || (data.secondCharPortraitShow() && !data.secondCharPortrait().isEmpty());

        winW = hasSecondChar ? 540 : 460;
        textH = 112;
        winH = HEADER_H + PAD + textH + 6 + PAGER_H + 10 + choicesAreaH + PAD;
        int maxH = height - 20;
        if (winH > maxH) {
            textH = Math.max(64, textH - (winH - maxH));
            winH = HEADER_H + PAD + textH + 6 + PAGER_H + 10 + choicesAreaH + PAD;
        }
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;

        textX = winX + PAD + PORTRAIT_W + PAD;
        textY = winY + HEADER_H + PAD;

        secondX = winX + winW - PAD - PORTRAIT_W;
        int rightEdge = hasSecondChar ? secondX - PAD : winX + winW - PAD;
        textW = rightEdge - textX;
        pagerY = textY + textH + 6;
        choicesX = textX;
        choicesW = textW;
        choicesY = pagerY + PAGER_H + 10;
    }

    private List<FormattedCharSequence> lines() {
        if (cachedLines == null) {
            String page = data.pages().isEmpty() ? ""
                    : data.pages().get(Math.min(pageIndex, data.pages().size() - 1));
            cachedLines = font.split(buildPageComponent(page), textW - 16);
        }
        return cachedLines;
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
            DialogueNodeData.Annotation a = annotationById(ann);
            Component hover;
            if (a != null) {
                MutableComponent h = Component.empty()
                        .append(Component.literal(a.title().isBlank() ? ann : a.title())
                                .withStyle(net.minecraft.ChatFormatting.YELLOW));
                if (!a.body().isBlank()) {
                    h.append(Component.literal("\n§r" + a.body()));
                }
                hover = h;
            } else {
                hover = Component.translatable("wh_npcs.ui.dialogue.term_not_found", ann);
            }
            part = part.withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        }
        root.append(part);
        seg.setLength(0);
    }

    @Nullable
    private DialogueNodeData.Annotation annotationById(String id) {
        for (DialogueNodeData.Annotation a : data.annotations()) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }

    @Nullable
    private Style annotationStyleAt(int mouseX, int mouseY) {
        if (data.annotations().isEmpty()
                || mouseX < textX + 8 || mouseX >= textX + textW - 6 || mouseY < textY + 8) {
            return null;
        }
        List<FormattedCharSequence> lines = lines();
        int visibleLines = (textH - 16 - imageLinksH()) / LINE_H;
        int rel = (mouseY - (textY + 8)) / LINE_H;
        int idx = textScroll + rel;
        if (rel < 0 || rel > visibleLines || idx < 0 || idx >= lines.size()) {
            return null;
        }
        Style st = font.getSplitter().componentStyleAtWidth(lines.get(idx), mouseX - (textX + 8));
        return st != null && st.getHoverEvent() != null ? st : null;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();

        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);

        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, data.npcName(), winX + PAD, winY + 8, nameColor(), false);
        if (!data.npcTitle().isEmpty()) {
            g.drawString(font, data.npcTitle(), winX + PAD + font.width(data.npcName()) + 10, winY + 8,
                    VanillaUIHelper.TEXT_GRAY, false);
        }
        renderReputation(g, mouseX, mouseY);
        followTooltip = null;
        renderFollowButtons(g, mouseX, mouseY);

        renderPortrait(g, mouseX, mouseY);
        renderEmoteIcon(g);
        renderSecondChar(g, mouseX, mouseY);
        renderText(g, mouseX, mouseY);
        renderPager(g, mouseX, mouseY);
        choiceHintTooltip = null;
        renderChoices(g, mouseX, mouseY);
        musicTooltip = null;
        renderMusicIndicator(g, mouseX, mouseY);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {

        if (data.inputMode() && isLastPage() && inputBox != null && inputBox.visible
                && inputBox.getValue().isEmpty() && !data.inputHint().isEmpty()) {
            g.drawString(font, data.inputHint(), choicesX + 4, choicesY + 6, 0xFF808080, false);
        }

        Style annStyle = annotationStyleAt(mouseX, mouseY);
        if (annStyle != null) {
            g.renderComponentHoverEffect(font, annStyle, mouseX, mouseY);
        }
        String tip = followTooltip != null ? followTooltip : musicTooltip;
        if (tip != null) {
            java.util.List<Component> lines = new java.util.ArrayList<>();
            for (String line : tip.split("\n")) {
                lines.add(Component.literal(line));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }

        if (choiceHintTooltip != null && annStyle == null) {
            java.util.List<Component> lines = new java.util.ArrayList<>();
            for (String line : choiceHintTooltip.split("\n")) {
                lines.add(Component.literal(line));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void renderFollowButtons(GuiGraphics g, int mouseX, int mouseY) {
        if (!data.followingViewer()) {
            return;
        }
        int by = winY + 4;
        int homeX = winX + winW - PAD - 16;
        int stopX = homeX - 4 - 16;
        boolean stopHover = isOver(mouseX, mouseY, stopX, by, 16, 16);
        boolean homeHover = isOver(mouseX, mouseY, homeX, by, 16, 16);
        VanillaUIHelper.drawButton(g, stopX, by, 16, 16, stopHover);
        g.fill(stopX + 4, by + 4, stopX + 12, by + 12, stopHover ? VanillaUIHelper.TEXT_YELLOW : 0xFFFF6B6B);
        VanillaUIHelper.drawButton(g, homeX, by, 16, 16, homeHover);
        drawHomeIcon(g, homeX + 3, by + 4, homeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (stopHover) {
            followTooltip = Component.translatable("wh_npcs.ui.dialogue.tip_stay").getString();
        } else if (homeHover) {
            followTooltip = Component.translatable("wh_npcs.ui.dialogue.tip_goodbye").getString();
        }
    }

    private static void drawHomeIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 4, y, x + 6, y + 1, c);
        g.fill(x + 3, y + 1, x + 7, y + 2, c);
        g.fill(x + 2, y + 2, x + 8, y + 3, c);
        g.fill(x + 1, y + 3, x + 9, y + 4, c);
        g.fill(x + 2, y + 4, x + 8, y + 9, c);
    }

    private void renderPortrait(GuiGraphics g, int mouseX, int mouseY) {
        int px = winX + PAD;
        int py = textY;
        int ph = winY + winH - PAD - py;
        VanillaUIHelper.drawContentPanel(g, px, py, PORTRAIT_W, ph);

        if (data.npcPortraitShow() && !data.npcPortrait().isEmpty()) {
            var entry = com.withouthonor.npcs.client.cache.ClientImageCache.getInstance().get(data.npcPortrait());
            if (entry.state() == com.withouthonor.npcs.client.cache.ClientImageCache.State.READY
                    && entry.location() != null && entry.width() > 0 && entry.height() > 0) {

                int availH = ph - 4;
                float ar = (float) entry.width() / entry.height();
                int dh = availH;
                int dw = Math.round(dh * ar);
                int ix = px + (PORTRAIT_W - dw) / 2;
                int iy = py + (ph - dh) / 2;
                ScaledScreen.enableScissor(g, px + 2, py + 2, px + PORTRAIT_W - 2, py + ph - 2);
                g.blit(entry.location(), ix, iy, dw, dh, 0f, 0f,
                        entry.width(), entry.height(), entry.width(), entry.height());
                g.disableScissor();
                return;
            }

        }
        LivingEntity npc = findNpc();
        if (npc != null) {
            int centerX = px + PORTRAIT_W / 2;
            int feetY = py + ph - 12;
            int scale = (int) (ph / 2.6F);
            ScaledScreen.enableScissor(g, px + 2, py + 2, px + PORTRAIT_W - 2, py + ph - 2);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, centerX, feetY, scale,
                    centerX - mouseX, py + ph / 3F - mouseY, npc);
            g.disableScissor();
        }
    }

    private void renderSecondChar(GuiGraphics g, int mouseX, int mouseY) {
        if (!hasSecondChar) {
            return;
        }
        int px = secondX;
        int py = textY;
        int ph = winY + winH - PAD - py;
        VanillaUIHelper.drawContentPanel(g, px, py, PORTRAIT_W, ph);
        boolean drewPhoto = false;

        if (data.secondCharPortraitShow() && !data.secondCharPortrait().isEmpty()) {
            var entry = com.withouthonor.npcs.client.cache.ClientImageCache.getInstance()
                    .get(data.secondCharPortrait());
            if (entry.state() == com.withouthonor.npcs.client.cache.ClientImageCache.State.READY
                    && entry.location() != null && entry.width() > 0 && entry.height() > 0) {
                int availH = ph - 4;
                float ar = (float) entry.width() / entry.height();
                int dh = availH;
                int dw = Math.round(dh * ar);
                int ix = px + (PORTRAIT_W - dw) / 2;
                int iy = py + (ph - dh) / 2;
                ScaledScreen.enableScissor(g, px + 2, py + 2, px + PORTRAIT_W - 2, py + ph - 2);
                g.blit(entry.location(), ix, iy, dw, dh, 0f, 0f,
                        entry.width(), entry.height(), entry.width(), entry.height());
                g.disableScissor();
                drewPhoto = true;
            }
        }
        if (!drewPhoto) {
            LivingEntity ghost = secondCharEntity();
            if (ghost != null) {
                int centerX = px + PORTRAIT_W / 2;
                int feetY = py + ph - 12;
                int scale = (int) (ph / 2.6F);
                ScaledScreen.enableScissor(g, px + 2, py + 2, px + PORTRAIT_W - 2, py + ph - 2);
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, centerX, feetY, scale,
                        centerX - mouseX, py + ph / 3F - mouseY, ghost);
                g.disableScissor();
            }
        }
        if (data.secondCharNameShow() && !data.secondCharName().isEmpty()) {
            g.drawCenteredString(font, font.plainSubstrByWidth(data.secondCharName(), PORTRAIT_W - 4),
                    px + PORTRAIT_W / 2, py + 4, VanillaUIHelper.TEXT_YELLOW);
        }
    }

    @Nullable
    private LivingEntity secondCharEntity() {
        if (minecraft == null || minecraft.level == null || data.secondCharSkin().isEmpty()) {
            return null;
        }
        if (secondGhost == null) {
            secondGhost = com.withouthonor.npcs.common.registry.ModEntities.COMPANION.get()
                    .create(minecraft.level);
        }
        if (secondGhost instanceof com.withouthonor.npcs.common.entity.CompanionEntity ce
                && !data.secondCharSkin().equals(secondGhostSkin)) {
            ce.setSkinName(data.secondCharSkin());
            secondGhostSkin = data.secondCharSkin();
        }
        return secondGhost;
    }

    private void renderEmoteIcon(GuiGraphics g) {
        com.withouthonor.npcs.common.dialogue.EmoteIcon icon =
                com.withouthonor.npcs.client.ClientEmotes.get(data.npcEntityId());
        if (icon == null) {
            return;
        }
        int frames = com.withouthonor.npcs.common.dialogue.EmoteIcon.COUNT;
        int size = 24;
        int x = winX + PAD + PORTRAIT_W / 2 - size / 2;
        int y = textY + 5;
        g.blit(EMOTE_ATLAS, x, y, size, size, icon.atlasIndex() * 16f, 0f, 16, 16, frames * 16, 16);
    }

    @Nullable
    private LivingEntity findNpc() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(data.npcEntityId());
        return entity instanceof LivingEntity living ? living : null;
    }

    private void renderText(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, textX, textY, textW, textH);
        List<FormattedCharSequence> lines = lines();
        int visibleLines = (textH - 16 - imageLinksH()) / LINE_H;
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        textScroll = Math.max(0, Math.min(textScroll, maxScroll));

        boolean hasAnn = !data.annotations().isEmpty();
        ScaledScreen.enableScissor(g, textX + 2, textY + 2, textX + textW - 2, textY + textH - 2);
        int y = textY + 8;
        for (int i = textScroll; i < Math.min(lines.size(), textScroll + visibleLines + 1); i++) {
            FormattedCharSequence line = lines.get(i);
            g.drawString(font, line, textX + 8, y, VanillaUIHelper.TEXT_WHITE, false);

            if (hasAnn) {
                for (int px = 0; px < textW - 16; px++) {
                    Style st = font.getSplitter().componentStyleAtWidth(line, px);
                    if (st != null && st.getHoverEvent() != null) {
                        g.fill(textX + 8 + px, y + LINE_H - 1, textX + 9 + px, y + LINE_H, 0xFF35C8C8);
                    }
                }
            }
            y += LINE_H;
        }
        g.disableScissor();

        if (maxScroll > 0) {
            float progress = (float) textScroll / maxScroll;
            int barH = Math.max(10, (textH - 4) * visibleLines / lines.size());
            int barY = textY + 2 + (int) ((textH - 4 - barH) * progress);
            g.fill(textX + textW - 5, textY + 2, textX + textW - 2, textY + textH - 2, VanillaUIHelper.BG_INNER);
            g.fill(textX + textW - 5, barY, textX + textW - 2, barY + barH, VanillaUIHelper.BORDER_LIGHT);

            scrollbars.arm(textX + textW - 5, textY + 2, textH - 4, lines.size(), visibleLines, v -> textScroll = v);
        }
        renderImageLinks(g, mouseX, mouseY);
    }

    private void renderImageLinks(GuiGraphics g, int mouseX, int mouseY) {
        if (data.images().isEmpty()) {
            return;
        }
        VanillaUIHelper.drawSeparator(g, textX + 6, textY + textH - imageLinksH() - 1, textW - 12);
        for (int i = 0; i < data.images().size(); i++) {
            DialogueNodeData.ImageData img = data.images().get(i);
            var entry = com.withouthonor.npcs.client.cache.ClientImageCache.getInstance().get(img.file());
            int y = imageLinkY(i);
            boolean ready = entry.state() == com.withouthonor.npcs.client.cache.ClientImageCache.State.READY;
            boolean hovered = ready && isOver(mouseX, mouseY, textX + 6, y - 1, textW - 16, 11);
            int color = switch (entry.state()) {
                case READY -> hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA;
                case LOADING -> VanillaUIHelper.TEXT_GRAY;
                case FAILED -> 0xFF884444;
            };
            drawPhotoIcon(g, textX + 8, y, color);
            String caption = img.caption().isBlank() ? Component.translatable("wh_npcs.ui.dialogue.image_default").getString() : img.caption();
            String label = switch (entry.state()) {
                case READY -> hovered ? "§n" + caption : caption;
                case LOADING -> caption + " " + Component.translatable("wh_npcs.ui.dialogue.image_loading").getString();
                case FAILED -> caption + " " + Component.translatable("wh_npcs.ui.dialogue.image_unavailable").getString();
            };
            g.drawString(font, font.plainSubstrByWidth(label, textW - 36), textX + 22, y, color, false);
        }
    }

    private static void drawPhotoIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 10, y + 1, c);
        g.fill(x, y + 7, x + 10, y + 8, c);
        g.fill(x, y, x + 1, y + 8, c);
        g.fill(x + 9, y, x + 10, y + 8, c);
        g.fill(x + 2, y + 2, x + 3, y + 3, c);
        g.fill(x + 5, y + 3, x + 6, y + 4, c);
        g.fill(x + 4, y + 4, x + 7, y + 5, c);
        g.fill(x + 3, y + 5, x + 8, y + 7, c);
    }

    private void renderMusicIndicator(GuiGraphics g, int mouseX, int mouseY) {
        String cur = ClientNpcAudio.currentMusicId();
        long now = System.currentTimeMillis();
        if (cur != null) {
            if (!cur.equals(shownMusicId) || !musicShowing) {
                shownMusicId = cur;
                shownMusicLabel = ClientNpcAudio.currentMusicLabel();
                musicShowing = true;
                musicAnimMs = now;
            }
        } else if (shownMusicId != null && musicShowing) {
            musicShowing = false;
            musicAnimMs = now;
        }
        if (shownMusicId == null) {
            return;
        }

        float alpha;
        float offsetY;
        if (musicShowing) {
            float t = easeOut(clamp01((now - musicAnimMs) / (float) MUSIC_IN_MS));
            alpha = t;
            offsetY = (1f - t) * 10f;
        } else {
            float e = easeIn(clamp01((now - musicAnimMs) / (float) MUSIC_OUT_MS));
            if (e >= 1f) {
                shownMusicId = null;
                return;
            }
            alpha = 1f - e;
            offsetY = -e * 18f;
        }

        int px = winX + PAD;
        int ph = winY + winH - PAD - textY;
        int stripX = px + 2;
        int stripW = PORTRAIT_W - 4;
        int y = textY + 2 + Math.round(offsetY);
        int iconW = 9;
        int iconX = stripX + stripW - iconW - 2;
        boolean muted = ClientNpcAudio.isMusicMuted();
        boolean iconHover = isOver(mouseX, mouseY, iconX - 1, y, iconW + 2, 12);
        int bgA = (int) (0xCC * alpha) & 0xFF;
        int fgA = (int) (0xFF * alpha) & 0xFF;
        ScaledScreen.enableScissor(g, px + 2, textY, px + PORTRAIT_W - 2, textY + ph - 2);
        g.fill(stripX, y, stripX + stripW, y + 12, (bgA << 24) | 0x101010);
        if (fgA >= 16) {
            int aqua = (fgA << 24) | (VanillaUIHelper.TEXT_AQUA & 0xFFFFFF);
            drawNote(g, stripX + 3, y + 2, muted ? ((fgA << 24) | 0x00808080) : aqua);
            String strip = Component.translatable("wh_npcs.ui.dialogue.music_now").getString();
            g.drawString(font, font.plainSubstrByWidth(strip, stripW - iconW - 19),
                    stripX + 13, y + 2, muted ? ((fgA << 24) | 0x00808080) : aqua, false);
            int speaker = iconHover ? ((fgA << 24) | (VanillaUIHelper.TEXT_YELLOW & 0xFFFFFF))
                    : (muted ? ((fgA << 24) | 0x00808080) : aqua);
            drawSpeaker(g, iconX, y + 2, speaker, (fgA << 24) | 0x00FF5555, muted);
        }
        g.disableScissor();

        if (isOver(mouseX, mouseY, stripX, y, stripW, 12)) {
            musicTooltip = Component.translatable("wh_npcs.ui.dialogue.music_playing", musicLabel()).getString() + "\n"
                    + Component.translatable(muted ? "wh_npcs.ui.dialogue.music_muted" : "wh_npcs.ui.dialogue.music_click_mute").getString();
        }
    }

    private static void drawSpeaker(GuiGraphics g, int x, int y, int c, int redC, boolean muted) {
        g.fill(x, y + 2, x + 2, y + 6, c);
        g.fill(x + 2, y + 1, x + 4, y + 7, c);
        if (!muted) {
            g.fill(x + 5, y + 3, x + 6, y + 5, c);
            g.fill(x + 7, y + 1, x + 8, y + 7, c);
        } else {
            g.fill(x + 4, y + 6, x + 5, y + 7, redC);
            g.fill(x + 5, y + 5, x + 6, y + 6, redC);
            g.fill(x + 6, y + 4, x + 7, y + 5, redC);
            g.fill(x + 7, y + 3, x + 8, y + 4, redC);
            g.fill(x + 8, y + 2, x + 9, y + 3, redC);
        }
    }

    private static float clamp01(float t) {
        return Math.max(0f, Math.min(1f, t));
    }

    private static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float easeIn(float t) {
        return t * t * t;
    }

    private String musicLabel() {
        if (shownMusicLabel != null && !shownMusicLabel.isEmpty()) {
            return shownMusicLabel;
        }
        return discName(shownMusicId);
    }

    private static String discName(String discId) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(discId);
        net.minecraft.world.item.Item item = id != null
                ? net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id) : null;
        return item != null ? new net.minecraft.world.item.ItemStack(item).getHoverName().getString() : discId;
    }

    private static void drawNote(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 4, y, x + 5, y + 6, c);
        g.fill(x + 5, y, x + 7, y + 2, c);
        g.fill(x + 1, y + 5, x + 5, y + 8, c);
    }

    private void renderPager(GuiGraphics g, int mouseX, int mouseY) {
        if (!hasPager) {
            return;
        }
        boolean prevHover = isOver(mouseX, mouseY, textX, pagerY, PAGER_H, PAGER_H) && pageIndex > 0;
        boolean nextHover = isOver(mouseX, mouseY, textX + textW - PAGER_H, pagerY, PAGER_H, PAGER_H)
                && pageIndex < data.pages().size() - 1;
        VanillaUIHelper.drawButton(g, textX, pagerY, PAGER_H, PAGER_H, prevHover);
        VanillaUIHelper.drawButton(g, textX + textW - PAGER_H, pagerY, PAGER_H, PAGER_H, nextHover);
        g.drawCenteredString(font, "<", textX + PAGER_H / 2 + 1, pagerY + 5,
                pageIndex > 0 ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY);
        g.drawCenteredString(font, ">", textX + textW - PAGER_H / 2 - 1, pagerY + 5,
                pageIndex < data.pages().size() - 1 ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY);
        g.drawCenteredString(font, (pageIndex + 1) + " / " + data.pages().size(),
                textX + textW / 2, pagerY + 5, VanillaUIHelper.TEXT_STATUS);
    }

    private void renderChoices(GuiGraphics g, int mouseX, int mouseY) {
        if (!isLastPage()) {
            if (inputBox != null) {
                inputBox.visible = false;
            }
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.dialogue.flip_more").getString(), choicesX + choicesW / 2,
                    choicesY + 8, VanillaUIHelper.TEXT_STATUS);
            return;
        }
        if (data.inputMode()) {
            if (inputBox != null) {
                inputBox.visible = true;
                inputBox.setPosition(choicesX, choicesY + 1);
                inputBox.setWidth(choicesW - 60);
            }
            boolean okHover = isOver(mouseX, mouseY, choicesX + choicesW - 56, choicesY, 56, 20);
            VanillaUIHelper.drawButton(g, choicesX + choicesW - 56, choicesY, 56, 20, okHover);
            g.drawCenteredString(font, "OK", choicesX + choicesW - 28, choicesY + 6,
                    okHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
            g.drawString(font, Component.translatable("wh_npcs.ui.dialogue.input_hint").getString(), choicesX, choicesY + 24,
                    VanillaUIHelper.TEXT_WHITE, false);
            return;
        }
        if (inputBox != null) {
            inputBox.visible = false;
        }
        List<DialogueNodeData.ChoiceData> choices = data.choices();
        int visible = Math.min(VISIBLE_ROWS, choices.size());
        choiceScroll = Math.max(0, Math.min(choiceScroll, choices.size() - visible));

        DialogueNodeData.ChoiceData hoveredLocked = null;
        for (int row = 0; row < visible; row++) {
            DialogueNodeData.ChoiceData choice = choices.get(choiceScroll + row);
            int y = choicesY + row * CHOICE_STRIDE;
            boolean hovered = isOver(mouseX, mouseY, choicesX, y, choicesW, CHOICE_BTN_H);
            if (choice.locked()) {
                g.fill(choicesX, y, choicesX + choicesW, y + CHOICE_BTN_H, VanillaUIHelper.BG_HOVERED);
                VanillaUIHelper.drawInsetFrame(g, choicesX, y, choicesW, CHOICE_BTN_H);
                drawLockIcon(g, choicesX + 8, y + 8);
                g.drawString(font, choice.text(), choicesX + 22, y + 8, VanillaUIHelper.TEXT_DARK_GRAY, false);
                if (hovered && !choice.hint().isBlank()) {
                    hoveredLocked = choice;
                }
            } else {
                VanillaUIHelper.drawButton(g, choicesX, y, choicesW, CHOICE_BTN_H, hovered);
                g.drawString(font, choice.text(), choicesX + 10, y + 8,
                        hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            }
        }
        if (choices.size() > visible) {
            g.drawCenteredString(font, "▼", choicesX + choicesW / 2,
                    choicesY + visible * CHOICE_STRIDE - 2, VanillaUIHelper.TEXT_STATUS);
        }
        choiceHintTooltip = hoveredLocked != null ? hoveredLocked.hint() : null;
    }

    private static void drawLockIcon(GuiGraphics g, int x, int y) {
        int c = VanillaUIHelper.TEXT_DARK_GRAY;
        g.fill(x + 1, y, x + 5, y + 1, c);
        g.fill(x, y, x + 1, y + 3, c);
        g.fill(x + 5, y, x + 6, y + 3, c);
        g.fill(x - 1, y + 3, x + 7, y + 8, c);
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

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            recalc();
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }

            if (data.followingViewer()) {
                int by = winY + 4;
                int homeX = winX + winW - PAD - 16;
                int stopX = homeX - 4 - 16;
                if (isOver(mouseX, mouseY, stopX, by, 16, 16)) {
                    NetworkHandler.sendToServer(new FollowControlPacket(data.npcEntityId(), FollowControlPacket.STOP));
                    return true;
                }
                if (isOver(mouseX, mouseY, homeX, by, 16, 16)) {
                    NetworkHandler.sendToServer(new FollowControlPacket(data.npcEntityId(), FollowControlPacket.GOODBYE));
                    onClose();
                    return true;
                }
            }

            if (shownMusicId != null && musicShowing) {
                int stripX = winX + PAD + 2;
                int stripW = PORTRAIT_W - 4;
                int iconX = stripX + stripW - 9 - 2;
                if (isOver(mouseX, mouseY, iconX - 1, textY + 2, 11, 12)) {
                    ClientNpcAudio.toggleMusicMute();
                    return true;
                }
            }

            for (int i = 0; i < data.images().size(); i++) {
                if (isOver(mouseX, mouseY, textX + 6, imageLinkY(i) - 1, textW - 16, 11)) {
                    DialogueNodeData.ImageData img = data.images().get(i);
                    var entry = com.withouthonor.npcs.client.cache.ClientImageCache.getInstance().get(img.file());
                    if (entry.state() == com.withouthonor.npcs.client.cache.ClientImageCache.State.READY
                            && entry.location() != null && minecraft != null) {
                        minecraft.setScreen(new ImageViewScreen(this, entry.location(),
                                entry.width(), entry.height(), img.caption()));
                    }
                    return true;
                }
            }
            if (hasPager) {
                if (isOver(mouseX, mouseY, textX, pagerY, PAGER_H, PAGER_H)) {
                    turnPage(-1);
                    return true;
                }
                if (isOver(mouseX, mouseY, textX + textW - PAGER_H, pagerY, PAGER_H, PAGER_H)) {
                    turnPage(1);
                    return true;
                }
            }
            if (isLastPage() && data.inputMode()) {
                if (isOver(mouseX, mouseY, choicesX + choicesW - 56, choicesY, 56, 20)) {
                    submitInput();
                    return true;
                }
            }
            if (isLastPage() && !data.inputMode()) {
                List<DialogueNodeData.ChoiceData> choices = data.choices();
                int visible = Math.min(VISIBLE_ROWS, choices.size());
                for (int row = 0; row < visible; row++) {
                    int y = choicesY + row * CHOICE_STRIDE;
                    if (isOver(mouseX, mouseY, choicesX, y, choicesW, CHOICE_BTN_H)) {
                        DialogueNodeData.ChoiceData choice = choices.get(choiceScroll + row);
                        if (!choice.locked()) {
                            NetworkHandler.sendToServer(new DialogueChoicePacket(
                                    data.dialogueId(), data.nodeId(), choice.index()));
                        }
                        return true;
                    }
                }
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, choicesX, choicesY, choicesW, VISIBLE_ROWS * CHOICE_STRIDE)
                && isLastPage()) {
            choiceScroll -= (int) Math.signum(delta);
        } else {
            textScroll -= (int) Math.signum(delta);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER && isLastPage() && data.inputMode()) {
            submitInput();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) && !isLastPage()) {
            turnPage(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void turnPage(int delta) {
        int newIndex = Math.max(0, Math.min(data.pages().size() - 1, pageIndex + delta));
        if (newIndex != pageIndex) {
            pageIndex = newIndex;
            textScroll = 0;
            cachedLines = null;
        }
    }

    private boolean isLastPage() {
        return pageIndex >= data.pages().size() - 1;
    }

    private void renderReputation(GuiGraphics g, int mouseX, int mouseY) {
        DialogueNodeData.ReputationData rep = data.reputation();
        if (rep == null) {
            return;
        }
        int w = 50;
        int h = 7;
        int x = winX + winW - PAD - w - (data.followingViewer() ? 40 : 0);
        int y = winY + 9;

        int nameEnd = winX + PAD + font.width(data.npcName())
                + (data.npcTitle().isEmpty() ? 0 : font.width(data.npcTitle()) + 10);
        int tierW = font.width(rep.tierName());
        if (x - 6 - tierW > nameEnd + 8) {
            g.drawString(font, rep.tierName(), x - 6 - tierW, y, rep.tierColor(), false);
        }
        g.fill(x, y, x + w, y + h, 0xFF1A1A1A);
        int fill = (int) ((w - 2) * Math.max(0.0F, Math.min(1.0F, rep.progress())));
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, rep.factionColor());
        }
        VanillaUIHelper.drawInsetFrame(g, x, y, w, h);
        if (isOver(mouseX, mouseY, x - 6 - tierW, y - 2, w + 6 + tierW, h + 4)) {
            java.util.List<Component> lines = new java.util.ArrayList<>();
            lines.add(Component.literal("§e" + rep.factionName()));
            lines.add(Component.translatable("wh_npcs.ui.dialogue.rep_value", rep.value(), rep.tierName()));
            lines.add(Component.translatable("wh_npcs.ui.dialogue.rep_bar"));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private int nameColor() {
        net.minecraft.ChatFormatting color = net.minecraft.ChatFormatting.getByName(data.npcNameColor());
        return color != null && color.isColor() && color.getColor() != null
                ? 0xFF000000 | color.getColor()
                : VanillaUIHelper.TEXT_YELLOW;
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        cachedLines = null;
        super.resize(minecraft, width, height);
    }

    @Override
    public void onClose() {
        if (!serverClosed) {
            NetworkHandler.sendToServer(new DialogueChoicePacket(data.dialogueId(), data.nodeId(), -1));
        }
        ClientNpcAudio.fadeAll();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
