package com.withouthonor.npcs.client.gui;

import com.withouthonor.npcs.client.cache.ClientSkinCache;
import com.withouthonor.npcs.common.dialogue.action.MonologueLine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class MonologueScreen extends Screen {

    private static final long INTRO_MS = 320L;
    private static final long OUTRO_MS = 260L;

    private final List<MonologueLine> lines;

    private int page;
    private int shown;

    private long openMs = -1L;
    private boolean closing;
    private long closeMs;

    private MonologueScreen(List<MonologueLine> lines) {
        super(Component.translatable("wh_npcs.ui.monologue.title"));
        this.lines = lines;
    }

    public static void show(List<MonologueLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        Minecraft.getInstance().setScreen(new MonologueScreen(lines));
    }

    @Override
    protected void init() {
        if (openMs < 0L) {
            openMs = System.currentTimeMillis();
        }
    }

    private MonologueLine cur() {
        return page >= 0 && page < lines.size() ? lines.get(page) : new MonologueLine("", "", "");
    }

    private String currentText() {
        return cur().text();
    }

    private float slide() {
        long now = System.currentTimeMillis();
        float in = easeOut(clamp01((now - openMs) / (float) INTRO_MS));
        if (closing) {
            float out = 1f - easeIn(clamp01((now - closeMs) / (float) OUTRO_MS));
            return Math.min(in, out);
        }
        return in;
    }

    private boolean introDone() {
        return !closing && System.currentTimeMillis() - openMs >= INTRO_MS;
    }

    @Override
    public void tick() {

        if (introDone()) {
            int len = currentText().length();
            if (shown < len) {
                shown = Math.min(len, shown + 2);
            }
        }

        if (closing && System.currentTimeMillis() - closeMs >= OUTRO_MS && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {

        int barH = 62;
        int barX = 20;
        int barW = width - 40;
        int barY = height - barH - 10;
        int blockTop = barY - 6;
        int blockH = height - blockTop;

        float a = slide();
        int yOff = Math.round((1f - a) * blockH);

        g.fill(0, 0, width, Math.round(a * 22f), 0xC8000000);
        g.fill(0, blockTop + yOff, width, height, 0xC8000000);

        MonologueLine line = cur();
        String full = line.text();
        String visible = full.substring(0, Math.min(shown, full.length()));
        drawBar(g, font, barX, barY + yOff, barW, barH, line.name(), line.portrait(), visible);

        if (a >= 1f && !closing && shown >= full.length()) {
            String hint = (page + 1 < lines.size()
                            ? Component.translatable("wh_npcs.ui.monologue.next").getString()
                            : Component.translatable("wh_npcs.ui.monologue.close").getString())
                    + "   " + (page + 1) + "/" + lines.size();
            g.drawString(font, "§8" + hint, barX + barW - font.width(hint) - 10, barY + barH - 11,
                    0xFFFFFFFF, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    public static void drawBar(GuiGraphics g, Font font, int barX, int barY, int barW, int barH,
                               String speaker, String portrait, String visibleText) {
        VanillaUIHelper.drawWindow(g, barX, barY, barW, barH);

        int textX = barX + 12;
        if (portrait != null && !portrait.isBlank()) {
            ClientSkinCache.Skin skin = ClientSkinCache.getInstance().get(portrait);
            int px = barX + 8;
            int py = barY + 7;
            int s = barH - 14;
            if (skin != null) {
                g.blit(skin.location(), px, py, s, s, 8.0F, 8.0F, 8, 8, 64, 64);
                g.blit(skin.location(), px, py, s, s, 40.0F, 8.0F, 8, 8, 64, 64);
            } else {
                g.fill(px, py, px + s, py + s, 0xFF3A2C20);
            }
            VanillaUIHelper.drawInsetFrame(g, px, py, s, s);
            textX = px + s + 10;
        }
        if (speaker != null && !speaker.isBlank()) {
            g.drawString(font, Component.literal(speaker), textX, barY + 8, VanillaUIHelper.TEXT_YELLOW, false);
        }
        List<FormattedCharSequence> lines = font.split(Component.literal(visibleText), barX + barW - 14 - textX);
        int ty = barY + 21;
        for (int i = 0; i < Math.min(lines.size(), 3); i++) {
            g.drawString(font, lines.get(i), textX, ty, VanillaUIHelper.TEXT_WHITE, false);
            ty += 11;
        }
    }

    private void advance() {
        if (closing) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - openMs < INTRO_MS) {
            openMs = now - INTRO_MS;
        }
        int len = currentText().length();
        if (shown < len) {
            shown = len;
        } else if (page + 1 < lines.size()) {
            page++;
            shown = 0;
        } else {
            closing = true;
            closeMs = now;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            advance();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            advance();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {

            if (!closing) {
                closing = true;
                closeMs = System.currentTimeMillis();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float easeIn(float t) {
        return t * t * t;
    }
}
