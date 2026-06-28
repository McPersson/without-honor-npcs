package com.withouthonor.npcs.client.gui;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import javax.annotation.Nullable;

public class ImageViewScreen extends Screen {

    private final ResourceLocation texture;
    private final int imageW;
    private final int imageH;
    private final String caption;
    @Nullable
    private final Screen parent;

    private boolean soundPlayed;

    public ImageViewScreen(@Nullable Screen parent, ResourceLocation texture,
                           int imageW, int imageH, String caption) {
        super(Component.empty());
        this.parent = parent;
        this.texture = texture;
        this.imageW = Math.max(1, imageW);
        this.imageH = Math.max(1, imageH);
        this.caption = caption.isBlank() ? Component.translatable("wh_npcs.ui.image_view.default_caption").getString() : caption;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (parent != null && minecraft != null) {
            minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        onClose();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        if (!soundPlayed && minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.0F);
            soundPlayed = true;
        }

        float aspect = (float) imageW / imageH;
        int boxH = (int) (height * 0.78F);
        int boxW = (int) (width * 0.75F);
        int drawH = boxH;
        int drawW = (int) (drawH * aspect);
        if (drawW > boxW) {
            drawW = boxW;
            drawH = (int) (drawW / aspect);
        }
        int x = (width - drawW) / 2;
        int y = (height - drawH) / 2 - 8;

        float cx = x + drawW / 2.0F;
        float cy = y + drawH / 2.0F;
        float nx = clamp((mouseX - cx) / (drawW / 2.0F), -1.0F, 1.0F);
        float ny = clamp((mouseY - cy) / (drawH / 2.0F), -1.0F, 1.0F);
        float maxDeg = 6.0F;

        g.pose().pushPose();
        g.pose().translate(cx, cy, 200.0F);
        g.pose().scale(1.04F, 1.04F, 1.0F);
        g.pose().mulPose(Axis.XP.rotationDegrees(ny * maxDeg));
        g.pose().mulPose(Axis.YP.rotationDegrees(-nx * maxDeg));
        g.pose().translate(-drawW / 2.0F, -drawH / 2.0F, 0.0F);

        g.blit(texture, 0, 0, 0, 0, drawW, drawH, drawW, drawH);
        drawPremiumBorder(g, 0, 0, drawW, drawH);

        g.pose().popPose();

        g.drawCenteredString(font, Component.literal(caption), width / 2, y + drawH + 12,
                VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.image_view.close_hint").getString(), width / 2, y + drawH + 24, 0xFFFFFFFF);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static void drawPremiumBorder(GuiGraphics g, int x, int y, int w, int h) {
        int outer = 0x22FFFFFF;
        int inner = 0xAAFFFFFF;
        int micro = 0x33FFFFFF;

        g.fill(x - 1, y - 1, x + w + 1, y, outer);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, outer);
        g.fill(x - 1, y, x, y + h, outer);
        g.fill(x + w, y, x + w + 1, y + h, outer);

        g.fill(x, y, x + w, y + 1, inner);
        g.fill(x, y + h - 1, x + w, y + h, inner);
        g.fill(x, y, x + 1, y + h, inner);
        g.fill(x + w - 1, y, x + w, y + h, inner);

        g.fill(x + 2, y + 2, x + w - 2, y + 3, micro);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, micro);
        g.fill(x + 2, y + 2, x + 3, y + h - 2, micro);
        g.fill(x + w - 3, y + 2, x + w - 2, y + h - 2, micro);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
