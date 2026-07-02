package com.withouthonor.npcs.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.client.gui.DialogueScreen;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CompanionOverlays {

    public static final CompanionOverlays INSTANCE = new CompanionOverlays();

    private static final ResourceLocation EMOTE_ATLAS =
            ResourceLocation.fromNamespaceAndPath("wh_npcs", "textures/entity/emotes.png");

    private record BubbleCache(String text, List<FormattedCharSequence> lines) {
    }

    private final Map<Integer, BubbleCache> bubbleCache = new HashMap<>();
    private final Map<String, Component> titleCache = new HashMap<>();

    private CompanionOverlays() {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static EntityRenderDispatcher dispatcher() {
        return Minecraft.getInstance().getEntityRenderDispatcher();
    }

    public void renderOverlays(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                               int packedLight, float partialTicks) {
        List<FormattedCharSequence> bubbleLines = bubbleLines(entity);
        renderSpeechBubble(entity, poseStack, buffer, packedLight, bubbleLines);
        renderEmote(entity, poseStack, buffer);
        renderIndicator(entity, poseStack, buffer, bubbleLines);
    }

    public void renderAllForEpicFight(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                      int packedLight, float partialTicks) {
        renderName(entity, poseStack, buffer, packedLight);
        renderTitle(entity, poseStack, buffer, packedLight);
        renderOverlays(entity, poseStack, buffer, packedLight, partialTicks);
    }

    private void renderName(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (Minecraft.getInstance().screen instanceof DialogueScreen) {
            return;
        }
        if (!entity.shouldShowName() || dispatcher().distanceToSqr(entity) > 4096.0D) {
            return;
        }
        Component name = entity.getDisplayName();
        Font f = font();
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getNameTagOffsetY(), 0.0F);
        poseStack.mulPose(dispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        org.joml.Matrix4f matrix = poseStack.last().pose();
        int bg = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        float x = -f.width(name) / 2.0F;
        boolean seeThrough = !entity.isDiscrete();
        f.drawInBatch(name, x, 0.0F, 0x20FFFFFF, false, matrix, buffer,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, bg, packedLight);
        if (seeThrough) {
            f.drawInBatch(name, x, 0.0F, -1, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
    }

    public void renderTitle(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String title = entity.getTitle();
        if (title.isEmpty() || dispatcher().distanceToSqr(entity) > 4096.0D) {
            return;
        }
        Component text = titleCache.computeIfAbsent(title, t ->
                Component.literal(t.indexOf('§') >= 0 ? t : "§7" + t));
        Font f = font();
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getNameTagOffsetY(), 0.0F);
        poseStack.mulPose(dispatcher().cameraOrientation());
        float scale = 0.025F * 0.75F;
        poseStack.scale(-scale, -scale, scale);
        org.joml.Matrix4f matrix = poseStack.last().pose();
        int bg = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        float x = -f.width(text) / 2.0F;
        float y = 12.5F;
        boolean visible = !entity.isDiscrete();
        f.drawInBatch(text, x, y, 0x20FFFFFF, false, matrix, buffer,
                visible ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, bg, packedLight);
        if (visible) {
            f.drawInBatch(text, x, y, -1, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
    }

    @Nullable
    public List<FormattedCharSequence> bubbleLines(CompanionEntity entity) {
        if (dispatcher().distanceToSqr(entity) > 1024.0D) {
            return null;
        }
        int id = entity.getId();
        String text = com.withouthonor.npcs.client.ClientBubbles.get(id);
        if (text == null) {
            bubbleCache.remove(id);
            return null;
        }
        BubbleCache cached = bubbleCache.get(id);
        if (cached != null && cached.text().equals(text)) {
            return cached.lines();
        }
        List<FormattedCharSequence> lines = font().split(Component.literal(text), 110);
        bubbleCache.put(id, new BubbleCache(text, lines));
        return lines;
    }

    private void renderSpeechBubble(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                    int packedLight, @Nullable List<FormattedCharSequence> lines) {
        if (lines == null) {
            return;
        }
        Font f = font();
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getNameTagOffsetY() + 0.35F + lines.size() * 0.22F, 0.0F);
        poseStack.mulPose(dispatcher().cameraOrientation());
        poseStack.scale(-0.02F, -0.02F, 0.02F);
        org.joml.Matrix4f matrix = poseStack.last().pose();
        int bg = 0x66000000;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            float x = -f.width(line) / 2.0F;
            float y = i * 10.0F;
            f.drawInBatch(line, x, y, 0x20FFFFFF, false, matrix, buffer,
                    Font.DisplayMode.SEE_THROUGH, bg, packedLight);
            f.drawInBatch(line, x, y, 0xFFFFFFFF, false, matrix, buffer,
                    Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
    }

    private void renderEmote(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (Minecraft.getInstance().screen instanceof DialogueScreen) {
            return;
        }
        com.withouthonor.npcs.common.dialogue.EmoteIcon icon =
                com.withouthonor.npcs.client.ClientEmotes.get(entity.getId());
        if (icon == null || dispatcher().distanceToSqr(entity) > 1024.0D) {
            return;
        }
        drawHeadIcon(poseStack, buffer, icon, entity.getNameTagOffsetY() + 0.45F);
    }

    private void renderIndicator(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                 @Nullable List<FormattedCharSequence> bubbleLines) {
        if (Minecraft.getInstance().screen instanceof DialogueScreen) {
            return;
        }
        if (com.withouthonor.npcs.client.ClientEmotes.get(entity.getId()) != null) {
            return;
        }
        com.withouthonor.npcs.common.dialogue.EmoteIcon icon =
                com.withouthonor.npcs.client.ClientIndicators.get(entity.getId());
        if (icon == null || dispatcher().distanceToSqr(entity) > 1024.0D) {
            return;
        }
        float y = bubbleLines != null
                ? entity.getNameTagOffsetY() + 0.35F + bubbleLines.size() * 0.22F + 0.32F
                : entity.getNameTagOffsetY() + 0.45F;
        drawHeadIcon(poseStack, buffer, icon, y);
    }

    private void drawHeadIcon(PoseStack poseStack, MultiBufferSource buffer,
                              com.withouthonor.npcs.common.dialogue.EmoteIcon icon, float yOffset) {
        int frames = com.withouthonor.npcs.common.dialogue.EmoteIcon.COUNT;
        float u0 = icon.atlasIndex() / (float) frames;
        float u1 = (icon.atlasIndex() + 1) / (float) frames;
        poseStack.pushPose();
        poseStack.translate(0.0F, yOffset, 0.0F);
        poseStack.mulPose(dispatcher().cameraOrientation());
        poseStack.scale(-0.4F, -0.4F, 0.4F);
        org.joml.Matrix4f m = poseStack.last().pose();
        var vc = buffer.getBuffer(RenderType.text(EMOTE_ATLAS));
        int light = LightTexture.FULL_BRIGHT;
        float h = 0.5F;
        vc.vertex(m, -h, -h, 0.0F).color(255, 255, 255, 255).uv(u0, 0.0F).uv2(light).endVertex();
        vc.vertex(m, -h, h, 0.0F).color(255, 255, 255, 255).uv(u0, 1.0F).uv2(light).endVertex();
        vc.vertex(m, h, h, 0.0F).color(255, 255, 255, 255).uv(u1, 1.0F).uv2(light).endVertex();
        vc.vertex(m, h, -h, 0.0F).color(255, 255, 255, 255).uv(u1, 0.0F).uv2(light).endVertex();
        poseStack.popPose();
    }
}
