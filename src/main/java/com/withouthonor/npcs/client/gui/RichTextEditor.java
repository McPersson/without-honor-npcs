package com.withouthonor.npcs.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

import com.withouthonor.npcs.client.ClientGlossary;
import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.common.glossary.GlossaryTerm;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RichTextEditor extends AbstractWidget {

    private static final int LINE_H = 11;
    private static final int PAD = 4;
    private static final int MAX_RAW = 4096;
    private static final String COLOR_CODES = "0123456789abcdef";

    private record VisChar(char c, int rawIndex, Style style, int width, @Nullable String annotationId) {
    }

    private record Line(int from, int to) {
    }

    private final Font font;

    private boolean singleLine;

    private boolean colorOnly;
    @Nullable
    private Component hint;
    private String raw = "";
    private final List<VisChar> chars = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();

    private int cursor;
    private int anchor = -1;
    private int scroll;
    private int preferredColumnX = -1;
    private long lastClickMs;
    private boolean dragging;
    private boolean paletteOpen;

    private boolean annotationsEnabled;
    private boolean annPickerOpen;
    private int annScroll;
    private static final int ANN_VISIBLE = 8;
    private final ScrollDrag scrollbars = new ScrollDrag();

    public RichTextEditor(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("wh_npcs.ui.rich_text.title"));
        this.font = font;
    }

    public RichTextEditor singleLine() {
        this.singleLine = true;
        return this;
    }

    public RichTextEditor colorOnly() {
        this.colorOnly = true;
        return this;
    }

    public void setHint(Component hint) {
        this.hint = hint;
    }

    public RichTextEditor withAnnotations() {
        this.annotationsEnabled = true;
        return this;
    }

    public void setValue(String value) {
        raw = value == null ? "" : value;
        cursor = 0;
        anchor = -1;
        scroll = 0;
        paletteOpen = false;
        annPickerOpen = false;
        rebuild();
    }

    public String getValue() {
        return raw;
    }

    public int firstColor() {
        for (VisChar vc : chars) {
            if (vc.c() != '\n' && vc.style().getColor() != null) {
                return vc.style().getColor().getValue();
            }
        }
        return -1;
    }

    public void deselect() {
        anchor = -1;
        paletteOpen = false;
        annPickerOpen = false;
    }

    private int innerW() {
        return width - PAD * 2 - 6;
    }

    private void rebuild() {
        chars.clear();
        ChatFormatting color = null;
        boolean bold = false, italic = false, underline = false, strike = false, obf = false;
        String currentAnnotation = null;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '§' && i + 1 < raw.length()) {
                ChatFormatting f = ChatFormatting.getByCode(raw.charAt(i + 1));
                if (f != null) {
                    if (f == ChatFormatting.RESET) {
                        color = null;
                        bold = italic = underline = strike = obf = false;
                    } else if (f.isColor()) {
                        color = f;
                        bold = italic = underline = strike = obf = false;
                    } else {
                        switch (f) {
                            case BOLD -> bold = true;
                            case ITALIC -> italic = true;
                            case UNDERLINE -> underline = true;
                            case STRIKETHROUGH -> strike = true;
                            case OBFUSCATED -> obf = true;
                            default -> {
                            }
                        }
                    }
                    i++;
                    continue;
                }

                char nxt = raw.charAt(i + 1);
                if (nxt == '{') {
                    int close = raw.indexOf('}', i + 2);
                    if (close >= 0) {
                        currentAnnotation = raw.substring(i + 2, close);
                        i = close;
                        continue;
                    }
                } else if (nxt == '}') {
                    currentAnnotation = null;
                    i++;
                    continue;
                }
            }
            if (c == '§') {
                continue;
            }
            Style style = Style.EMPTY
                    .withBold(bold).withItalic(italic).withUnderlined(underline)
                    .withStrikethrough(strike).withObfuscated(obf);
            if (color != null) {
                style = style.withColor(color);
            }
            int w = c == '\n' ? 0 : font.width(Component.literal(String.valueOf(c)).withStyle(style));
            chars.add(new VisChar(c, i, style, w, currentAnnotation));
        }
        rewrap();
    }

    private void rewrap() {
        lines.clear();
        if (singleLine) {
            lines.add(new Line(0, chars.size()));
            return;
        }
        int lineStart = 0;
        int x = 0;
        int lastSpace = -1;
        int i = 0;
        while (i < chars.size()) {
            VisChar vc = chars.get(i);
            if (vc.c() == '\n') {
                lines.add(new Line(lineStart, i));
                lineStart = i + 1;
                x = 0;
                lastSpace = -1;
                i++;
                continue;
            }
            if (x + vc.width() > innerW() && i > lineStart) {
                int brk = lastSpace >= lineStart ? lastSpace + 1 : i;
                lines.add(new Line(lineStart, brk));
                lineStart = brk;
                x = 0;
                for (int j = brk; j <= i && j < chars.size(); j++) {
                    if (chars.get(j).c() != '\n') {
                        x += chars.get(j).width();
                    }
                }
                lastSpace = -1;
                i++;
                continue;
            }
            if (vc.c() == ' ') {
                lastSpace = i;
            }
            x += vc.width();
            i++;
        }
        lines.add(new Line(lineStart, chars.size()));
    }

    private int rawPos(int visIndex) {
        return visIndex < chars.size() ? chars.get(visIndex).rawIndex() : raw.length();
    }

    private int lineOf(int visIndex) {
        for (int l = 0; l < lines.size(); l++) {
            if (visIndex <= lines.get(l).to()) {

                if (visIndex == lines.get(l).to() && l + 1 < lines.size()
                        && lines.get(l + 1).from() == visIndex) {
                    return l;
                }
                return l;
            }
        }
        return lines.size() - 1;
    }

    private int xOf(int visIndex) {
        Line line = lines.get(lineOf(visIndex));
        int x = 0;
        for (int i = line.from(); i < Math.min(visIndex, line.to()); i++) {
            x += chars.get(i).width();
        }
        return x;
    }

    private int visAt(double mouseX, double mouseY) {
        int lineIdx = scroll + (int) Math.floor((mouseY - getY() - PAD) / LINE_H);
        lineIdx = Math.max(0, Math.min(lines.size() - 1, lineIdx));
        Line line = lines.get(lineIdx);
        int targetX = (int) (mouseX - getX() - PAD);
        int x = 0;
        for (int i = line.from(); i < line.to(); i++) {
            int w = chars.get(i).width();
            if (x + w / 2 >= targetX) {
                return i;
            }
            x += w;
        }
        return line.to();
    }

    private boolean hasSelection() {
        return anchor >= 0 && anchor != cursor;
    }

    private int selMin() {
        return Math.min(anchor, cursor);
    }

    private int selMax() {
        return Math.max(anchor, cursor);
    }

    private void replaceRaw(int from, int to, String insert) {
        if (raw.length() - (to - from) + insert.length() > MAX_RAW) {
            return;
        }
        raw = raw.substring(0, from) + insert + raw.substring(to);
        rebuild();
    }

    private void deleteSelection() {
        int min = selMin();
        replaceRaw(rawPos(min), rawPos(selMax()), "");
        cursor = min;
        anchor = -1;
    }

    private void insertText(String text) {
        StringBuilder filtered = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\n' && singleLine) {
                continue;
            }
            if (c == '\n' || c == '§' || SharedConstants.isAllowedChatCharacter(c)) {
                filtered.append(c);
            }
        }
        if (filtered.length() == 0) {
            return;
        }
        if (hasSelection()) {
            deleteSelection();
        }
        int visBefore = countVisible(filtered.toString());
        replaceRaw(rawPos(cursor), rawPos(cursor), filtered.toString());
        cursor += visBefore;
        anchor = -1;
    }

    private static int countVisible(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '§' && i + 1 < s.length() && ChatFormatting.getByCode(s.charAt(i + 1)) != null) {
                i++;
                continue;
            }
            if (s.charAt(i) != '§') {
                n++;
            }
        }
        return n;
    }

    private static final String FORMAT_CODES = "0123456789abcdefklmnor";

    public void applyFormat(ChatFormatting formatting) {
        String code = "§" + formatting.getChar();
        if (hasSelection()) {
            int min = selMin();
            int max = selMax();
            int rawA = rawPos(min);
            int rawB = rawPos(max);
            if (formatting == ChatFormatting.RESET) {
                String inner = stripFormatCodes(raw.substring(rawA, rawB));
                String after = activeStyleCodes(raw.substring(0, rawB));
                raw = raw.substring(0, rawA) + "§r" + inner + "§r" + after + raw.substring(rawB);
            } else {
                raw = raw.substring(0, rawA) + code + raw.substring(rawA, rawB) + "§r" + raw.substring(rawB);
            }
            rebuild();

        } else {
            replaceRaw(rawPos(cursor), rawPos(cursor), code);
        }
    }

    private static String stripFormatCodes(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()
                    && FORMAT_CODES.indexOf(Character.toLowerCase(s.charAt(i + 1))) >= 0) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String activeStyleCodes(String prefix) {
        ChatFormatting color = null;
        boolean bold = false, italic = false, underline = false, strike = false, obf = false;
        for (int i = 0; i < prefix.length(); i++) {
            if (prefix.charAt(i) == '§' && i + 1 < prefix.length()) {
                ChatFormatting f = ChatFormatting.getByCode(prefix.charAt(i + 1));
                if (f != null) {
                    if (f == ChatFormatting.RESET) {
                        color = null;
                        bold = italic = underline = strike = obf = false;
                    } else if (f.isColor()) {
                        color = f;
                        bold = italic = underline = strike = obf = false;
                    } else {
                        switch (f) {
                            case BOLD -> bold = true;
                            case ITALIC -> italic = true;
                            case UNDERLINE -> underline = true;
                            case STRIKETHROUGH -> strike = true;
                            case OBFUSCATED -> obf = true;
                            default -> {
                            }
                        }
                    }
                    i++;
                }
            }
        }
        StringBuilder out = new StringBuilder();
        if (color != null) {
            out.append('§').append(color.getChar());
        }
        if (bold) {
            out.append("§l");
        }
        if (italic) {
            out.append("§o");
        }
        if (underline) {
            out.append("§n");
        }
        if (strike) {
            out.append("§m");
        }
        if (obf) {
            out.append("§k");
        }
        return out.toString();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        scrollbars.beginFrame();
        if (singleLine) {

            int border = isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0;
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, border);
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
        } else {
            VanillaUIHelper.drawContentPanel(g, getX(), getY(), width, height);
        }
        int visibleLines = Math.max(1, (height - PAD * 2) / LINE_H);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - visibleLines)));

        ScaledScreen.enableScissor(g, getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2);
        int selA = hasSelection() ? selMin() : -1;
        int selB = hasSelection() ? selMax() : -1;
        for (int l = scroll; l < Math.min(lines.size(), scroll + visibleLines); l++) {
            Line line = lines.get(l);
            int y = getY() + PAD + (l - scroll) * LINE_H;

            if (selA >= 0 && selA < line.to() && selB > line.from()) {
                int from = Math.max(selA, line.from());
                int to = Math.min(selB, line.to());
                int x1 = getX() + PAD + xOfIn(line, from);
                int x2 = getX() + PAD + xOfIn(line, to);
                if (to == line.to() && selB > line.to()) {
                    x2 += 3;
                }
                g.fill(x1, y - 1, x2, y + LINE_H - 2, 0xFF264F78);
            }

            int x = getX() + PAD;
            int i = line.from();
            while (i < line.to()) {
                Style style = chars.get(i).style();
                StringBuilder run = new StringBuilder();
                int runWidth = 0;
                while (i < line.to() && chars.get(i).style().equals(style)) {
                    run.append(chars.get(i).c());
                    runWidth += chars.get(i).width();
                    i++;
                }
                g.drawString(font, Component.literal(run.toString()).withStyle(style), x, y,
                        VanillaUIHelper.TEXT_WHITE, false);
                x += runWidth;
            }

            int ux = getX() + PAD;
            for (int k = line.from(); k < line.to(); k++) {
                VisChar vc = chars.get(k);
                if (vc.annotationId() != null && vc.c() != '\n') {
                    g.fill(ux, y + LINE_H - 2, ux + vc.width(), y + LINE_H - 1, 0xFF35C8C8);
                }
                ux += vc.width();
            }
        }

        if (chars.isEmpty() && hint != null) {
            g.drawString(font, hint, getX() + PAD, getY() + PAD, 0xFF808080, false);
        }

        if (isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cl = lineOf(cursor);
            if (cl >= scroll && cl < scroll + visibleLines) {
                int cx = getX() + PAD + xOf(cursor);
                int cy = getY() + PAD + (cl - scroll) * LINE_H;
                g.fill(cx, cy - 1, cx + 1, cy + LINE_H - 2, 0xFFE0E0E0);
            }
        }
        g.disableScissor();
        if (!singleLine) {
            VanillaUIHelper.drawScrollbar(g, getX() + width - 5, getY() + 2, height - 4,
                    lines.size(), visibleLines, scroll, scrollbars, v -> scroll = v);
        }

        if (isFocused() && hasSelection()) {
            renderToolbar(g, mouseX, mouseY);
        }
    }

    private int xOfIn(Line line, int visIndex) {
        int x = 0;
        for (int i = line.from(); i < visIndex; i++) {
            x += chars.get(i).width();
        }
        return x;
    }

    private record ToolButton(String label, @Nullable ChatFormatting formatting, String tooltip) {
    }

    private static final List<ToolButton> TOOLS = List.of(
            new ToolButton("wh_npcs.ui.rich_text.btn.bold", ChatFormatting.BOLD, "wh_npcs.ui.rich_text.bold"),
            new ToolButton("wh_npcs.ui.rich_text.btn.italic", ChatFormatting.ITALIC, "wh_npcs.ui.rich_text.italic"),
            new ToolButton("wh_npcs.ui.rich_text.btn.underline", ChatFormatting.UNDERLINE, "wh_npcs.ui.rich_text.underline"),
            new ToolButton("wh_npcs.ui.rich_text.btn.strikethrough", ChatFormatting.STRIKETHROUGH, "wh_npcs.ui.rich_text.strikethrough"),
            new ToolButton("wh_npcs.ui.rich_text.btn.obfuscated", ChatFormatting.OBFUSCATED, "wh_npcs.ui.rich_text.obfuscated"),
            new ToolButton("wh_npcs.ui.rich_text.btn.color", null, "wh_npcs.ui.rich_text.color"),
            new ToolButton("✕", ChatFormatting.RESET, "wh_npcs.ui.rich_text.reset_style"));

    private static final List<ToolButton> COLOR_TOOLS = List.of(
            new ToolButton("wh_npcs.ui.rich_text.btn.color", null, "wh_npcs.ui.rich_text.color"),
            new ToolButton("✕", ChatFormatting.RESET, "wh_npcs.ui.rich_text.reset_color"));

    private List<ToolButton> tools() {
        return colorOnly ? COLOR_TOOLS : TOOLS;
    }

    private static final int TOOL_W = 16;

    private static final int ANN_SLOT = 6;

    private int slotCount() {
        return tools().size() + (annotationsEnabled ? 1 : 0);
    }

    private boolean isAnnSlot(int slot) {
        return annotationsEnabled && slot == ANN_SLOT;
    }

    private ToolButton toolAt(int slot) {
        return tools().get(annotationsEnabled && slot > ANN_SLOT ? slot - 1 : slot);
    }

    private int toolbarX() {
        int x = getX() + PAD + xOf(selMin()) - 2;
        return Math.max(getX(), Math.min(x, getX() + width - slotCount() * (TOOL_W + 2) - 4));
    }

    private int toolbarY() {
        int visibleLines = (height - PAD * 2) / LINE_H;
        int selLine = lineOf(selMin());
        int y = getY() + PAD + (selLine - scroll) * LINE_H - 22;
        if (y < getY() - 2 || selLine < scroll) {
            y = getY() + PAD + (Math.max(selLine, scroll) - scroll) * LINE_H + LINE_H + 2;
        }
        return y;
    }

    private void renderToolbar(GuiGraphics g, int mouseX, int mouseY) {
        int tx = toolbarX();
        int ty = toolbarY();
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        int totalW = slotCount() * (TOOL_W + 2) + 2;
        VanillaUIHelper.drawWindow(g, tx, ty, totalW, 20);
        String tooltip = null;
        for (int i = 0; i < slotCount(); i++) {
            int bx = tx + 2 + i * (TOOL_W + 2);
            boolean hovered = mouseX >= bx && mouseX < bx + TOOL_W && mouseY >= ty + 2 && mouseY < ty + 18;
            VanillaUIHelper.drawButton(g, bx, ty + 2, TOOL_W, 16, hovered);
            if (isAnnSlot(i)) {

                int c = hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE;
                g.fill(bx + 5, ty + 6, bx + 12, ty + 7, c);
                g.fill(bx + 5, ty + 8, bx + 10, ty + 9, c);
                g.fill(bx + 4, ty + 11, bx + 13, ty + 12, 0xFF35C8C8);
                if (hovered) {
                    tooltip = "wh_npcs.ui.rich_text.term_annotation";
                }
                continue;
            }
            ToolButton tool = toolAt(i);
            if (tool.formatting() == null) {
                g.fill(bx + 4, ty + 6, bx + TOOL_W - 4, ty + 14, 0xFFFF5555);
                g.fill(bx + 6, ty + 8, bx + TOOL_W - 6, ty + 12, 0xFF5555FF);
            } else {
                String toolLabel = tool.label().startsWith("wh_npcs.")
                        ? Component.translatable(tool.label()).getString() : tool.label();
                g.drawCenteredString(font, toolLabel, bx + TOOL_W / 2, ty + 6,
                        hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            }
            if (hovered) {
                tooltip = tool.tooltip();
            }
        }
        if (paletteOpen) {
            int py = ty + 22;
            VanillaUIHelper.drawWindow(g, tx, py, 8 * 14 + 6, 2 * 14 + 6);
            for (int i = 0; i < 16; i++) {
                ChatFormatting color = ChatFormatting.getByCode(COLOR_CODES.charAt(i));
                int sx = tx + 3 + (i % 8) * 14;
                int sy = py + 3 + (i / 8) * 14;
                int argb = color != null && color.getColor() != null ? 0xFF000000 | color.getColor() : 0xFFFFFFFF;
                g.fill(sx, sy, sx + 12, sy + 12, argb);
                if (mouseX >= sx && mouseX < sx + 12 && mouseY >= sy && mouseY < sy + 12) {
                    g.fill(sx, sy, sx + 12, sy + 1, 0xFFFFFFFF);
                    g.fill(sx, sy + 11, sx + 12, sy + 12, 0xFFFFFFFF);
                    g.fill(sx, sy, sx + 1, sy + 12, 0xFFFFFFFF);
                    g.fill(sx + 11, sy, sx + 12, sy + 12, 0xFFFFFFFF);
                }
            }
        }
        if (annPickerOpen) {
            List<GlossaryTerm> terms = pickerTerms();
            boolean canRemove = selectionAnnotated();
            annScroll = Math.max(0, Math.min(annScroll, Math.max(0, terms.size() - ANN_VISIBLE)));
            int shown = Math.min(terms.size(), ANN_VISIBLE);
            int rowsN = (canRemove ? 1 : 0) + Math.max(shown, terms.isEmpty() ? 1 : 0);
            int pw = 140;
            int ph = 4 + rowsN * 12;
            int px = tx;
            int py = ty + 22;
            VanillaUIHelper.drawWindow(g, px, py, pw, ph);
            int ry = py + 3;
            ClientPrefs prefs = ClientPrefs.get();
            if (canRemove) {
                boolean h = mouseX >= px + 2 && mouseX < px + pw - 2 && mouseY >= ry && mouseY < ry + 12;
                if (h) {
                    g.fill(px + 2, ry, px + pw - 2, ry + 12, VanillaUIHelper.BG_HOVERED);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.rich_text.remove_annotation").getString(), px + 4, ry + 2, VanillaUIHelper.TEXT_WHITE, false);
                ry += 12;
            }
            if (terms.isEmpty()) {
                g.drawString(font, Component.translatable("wh_npcs.ui.rich_text.no_terms").getString(), px + 4, ry + 2, VanillaUIHelper.TEXT_WHITE, false);
            }
            for (int i = annScroll; i < Math.min(terms.size(), annScroll + ANN_VISIBLE); i++) {
                GlossaryTerm term = terms.get(i);
                boolean h = mouseX >= px + 2 && mouseX < px + pw - 2 && mouseY >= ry && mouseY < ry + 12;
                if (h) {
                    g.fill(px + 2, ry, px + pw - 2, ry + 12, VanillaUIHelper.BG_HOVERED);
                }
                int lx = px + 4;
                if (prefs.isPinnedTerm(term.getId())) {
                    g.drawString(font, "§6•", lx, ry + 2, 0xFFFFFFFF, false);
                    lx += 7;
                }
                String label = term.getTitle().isBlank() ? term.getId() : term.getTitle();
                g.drawString(font, font.plainSubstrByWidth(label, px + pw - 6 - lx), lx, ry + 2,
                        h ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
                ry += 12;
            }
        }
        if (tooltip != null) {
            g.renderTooltip(font, Component.translatable(tooltip), mouseX, mouseY);
        }
        g.pose().popPose();
    }

    public boolean clickToolbar(double mouseX, double mouseY) {
        return toolbarClicked(mouseX, mouseY);
    }

    private boolean toolbarClicked(double mouseX, double mouseY) {
        if (!isFocused() || !hasSelection()) {
            return false;
        }
        int tx = toolbarX();
        int ty = toolbarY();
        for (int i = 0; i < slotCount(); i++) {
            int bx = tx + 2 + i * (TOOL_W + 2);
            if (mouseX >= bx && mouseX < bx + TOOL_W && mouseY >= ty + 2 && mouseY < ty + 18) {
                if (isAnnSlot(i)) {
                    annPickerOpen = !annPickerOpen;
                    paletteOpen = false;
                    return true;
                }
                ToolButton tool = toolAt(i);
                if (tool.formatting() == null) {
                    paletteOpen = !paletteOpen;
                } else if (tool.formatting() == ChatFormatting.RESET) {
                    applyFormat(ChatFormatting.RESET);
                    paletteOpen = false;
                } else {
                    applyFormat(tool.formatting());
                    paletteOpen = false;
                }
                annPickerOpen = false;
                return true;
            }
        }
        if (paletteOpen) {
            int py = ty + 22;
            for (int i = 0; i < 16; i++) {
                int sx = tx + 3 + (i % 8) * 14;
                int sy = py + 3 + (i / 8) * 14;
                if (mouseX >= sx && mouseX < sx + 12 && mouseY >= sy && mouseY < sy + 12) {
                    ChatFormatting color = ChatFormatting.getByCode(COLOR_CODES.charAt(i));
                    if (color != null) {
                        applyFormat(color);
                    }
                    paletteOpen = false;
                    return true;
                }
            }
        }
        if (annPickerOpen) {
            List<GlossaryTerm> terms = pickerTerms();
            boolean canRemove = selectionAnnotated();
            int ry = ty + 22 + 3;
            int pw = 140;
            int px = tx;
            if (canRemove) {
                if (mouseX >= px + 2 && mouseX < px + pw - 2 && mouseY >= ry && mouseY < ry + 12) {
                    removeAnnotation();
                    annPickerOpen = false;
                    return true;
                }
                ry += 12;
            }
            for (int i = annScroll; i < Math.min(terms.size(), annScroll + ANN_VISIBLE); i++) {
                if (mouseX >= px + 2 && mouseX < px + pw - 2 && mouseY >= ry && mouseY < ry + 12) {
                    applyAnnotation(terms.get(i).getId());
                    annPickerOpen = false;
                    return true;
                }
                ry += 12;
            }
        }
        return false;
    }

    private List<GlossaryTerm> pickerTerms() {
        ClientPrefs prefs = ClientPrefs.get();
        List<GlossaryTerm> pinned = new ArrayList<>();
        List<GlossaryTerm> fav = new ArrayList<>();
        List<GlossaryTerm> rest = new ArrayList<>();
        for (GlossaryTerm t : ClientGlossary.all()) {
            if (prefs.isPinnedTerm(t.getId())) {
                pinned.add(t);
            } else if (prefs.isFavoriteTerm(t.getId())) {
                fav.add(t);
            } else {
                rest.add(t);
            }
        }
        Comparator<GlossaryTerm> byTitle = Comparator.comparing(
                t -> t.getTitle().isBlank() ? t.getId() : t.getTitle(), String.CASE_INSENSITIVE_ORDER);
        pinned.sort(byTitle);
        fav.sort(byTitle);
        rest.sort(byTitle);
        List<GlossaryTerm> out = new ArrayList<>(pinned);
        out.addAll(fav);
        out.addAll(rest);
        return out;
    }

    private boolean selectionAnnotated() {
        int idx = hasSelection() ? selMin() : Math.min(cursor, chars.size() - 1);
        return idx >= 0 && idx < chars.size() && chars.get(idx).annotationId() != null;
    }

    private void applyAnnotation(String id) {
        if (!hasSelection()) {
            return;
        }
        int rawA = rawPos(selMin());
        int rawB = rawPos(selMax());
        raw = raw.substring(0, rawA) + "§{" + id + "}" + raw.substring(rawA, rawB) + "§}" + raw.substring(rawB);
        rebuild();
    }

    private void removeAnnotation() {
        int idx = hasSelection() ? selMin() : Math.min(cursor, chars.size() - 1);
        if (idx < 0 || idx >= chars.size()) {
            return;
        }
        String ann = chars.get(idx).annotationId();
        if (ann == null) {
            return;
        }
        int a = idx;
        int b = idx;
        while (a > 0 && ann.equals(chars.get(a - 1).annotationId())) {
            a--;
        }
        while (b + 1 < chars.size() && ann.equals(chars.get(b + 1).annotationId())) {
            b++;
        }
        int spanStart = chars.get(a).rawIndex();
        int spanEnd = chars.get(b).rawIndex() + 1;
        String open = "§{" + ann + "}";

        if (raw.startsWith("§}", spanEnd)) {
            raw = raw.substring(0, spanEnd) + raw.substring(spanEnd + 2);
        }
        int openStart = spanStart - open.length();
        if (openStart >= 0 && raw.startsWith(open, openStart)) {
            raw = raw.substring(0, openStart) + raw.substring(openStart + open.length());
        }
        rebuild();
        cursor = Math.min(cursor, chars.size());
        anchor = -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (toolbarClicked(mouseX, mouseY)) {
            return true;
        }
        if (!(mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height)) {
            return false;
        }
        paletteOpen = false;
        annPickerOpen = false;
        long now = System.currentTimeMillis();
        int pos = visAt(mouseX, mouseY);
        if (now - lastClickMs < 250 && pos == cursor) {
            selectWord(pos);
        } else {
            cursor = pos;
            anchor = pos;
            dragging = true;
        }
        lastClickMs = now;
        preferredColumnX = -1;
        return true;
    }

    private void selectWord(int pos) {
        int a = Math.min(pos, chars.size() - 1);
        if (a < 0) {
            return;
        }
        int b = a;
        while (a > 0 && !Character.isWhitespace(chars.get(a - 1).c())) {
            a--;
        }
        while (b < chars.size() && !Character.isWhitespace(chars.get(b).c())) {
            b++;
        }
        anchor = a;
        cursor = b;
        dragging = false;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (dragging) {
            cursor = visAt(mouseX, mouseY);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        dragging = false;
        if (anchor == cursor) {
            anchor = -1;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollbars.release();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (annPickerOpen) {
            annScroll -= (int) Math.signum(delta);
            return true;
        }
        if (!singleLine && mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
            scroll -= (int) Math.signum(delta);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!isFocused()) {
            return false;
        }
        if (SharedConstants.isAllowedChatCharacter(c)) {
            insertText(String.valueOf(c));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) {
            return false;
        }
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (!singleLine) {
                    insertText("\n");
                }
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursor > 0) {
                    replaceRaw(rawPos(cursor - 1), rawPos(cursor), "");
                    cursor--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursor < chars.size()) {
                    replaceRaw(rawPos(cursor), rawPos(cursor + 1), "");
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveCursor(cursor - 1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveCursor(cursor + 1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveVertical(-1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveVertical(1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                moveCursor(lines.get(lineOf(cursor)).from(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                moveCursor(lines.get(lineOf(cursor)).to(), shift);
                return true;
            }
            default -> {
            }
        }
        if (ctrl) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A -> {
                    anchor = 0;
                    cursor = chars.size();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    copySelection(false);
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    copySelection(true);
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                    return true;
                }
                default -> {
                }
            }
        }
        return false;
    }

    private void copySelection(boolean cut) {
        if (!hasSelection()) {
            return;
        }
        String selected = raw.substring(rawPos(selMin()), rawPos(selMax()));
        Minecraft.getInstance().keyboardHandler.setClipboard(selected);
        if (cut) {
            deleteSelection();
        }
    }

    private void moveCursor(int to, boolean extendSelection) {
        if (extendSelection) {
            if (anchor < 0) {
                anchor = cursor;
            }
        } else {
            anchor = -1;
        }
        cursor = Math.max(0, Math.min(chars.size(), to));
        preferredColumnX = -1;
        scrollToCursor();
    }

    private void moveVertical(int delta, boolean extendSelection) {
        int line = lineOf(cursor);
        if (preferredColumnX < 0) {
            preferredColumnX = xOf(cursor);
        }
        int target = line + delta;
        if (target < 0 || target >= lines.size()) {
            return;
        }
        Line tl = lines.get(target);
        int x = 0;
        int pos = tl.from();
        while (pos < tl.to() && x + chars.get(pos).width() / 2 < preferredColumnX) {
            x += chars.get(pos).width();
            pos++;
        }
        if (extendSelection) {
            if (anchor < 0) {
                anchor = cursor;
            }
        } else {
            anchor = -1;
        }
        cursor = pos;
        scrollToCursor();
    }

    private void scrollToCursor() {
        int visibleLines = (height - PAD * 2) / LINE_H;
        int line = lineOf(cursor);
        if (line < scroll) {
            scroll = line;
        } else if (line >= scroll + visibleLines) {
            scroll = line - visibleLines + 1;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
