package com.withouthonor.npcs.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.block.SpawnerBlockEntity;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.level.Level;

public class SpawnerBlockEntityRenderer implements BlockEntityRenderer<SpawnerBlockEntity> {

    private static boolean loggedError;

    public SpawnerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(SpawnerBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        String skin = be.getDisplaySkin();
        if (skin.isEmpty()) {
            return;
        }
        CompanionEntity display = be.getOrCreateDisplay(level);
        if (display == null) {
            return;
        }
        display.setSkinName(skin);
        try {
            EntityRenderDispatcher erd = Minecraft.getInstance().getEntityRenderDispatcher();

            pose.pushPose();
            pose.translate(0.5, 0.4, 0.5);
            float f = 0.53125F;
            float scaleBase = Math.max(display.getBbWidth(), display.getBbHeight());
            if (scaleBase > 1.0F) {
                f /= scaleBase;
            }
            double deg = be.isEnabled() ? (level.getGameTime() + partialTick) * 4.0 : 0.0;
            pose.mulPose(Axis.YP.rotationDegrees((float) deg));
            pose.translate(0.0, -0.2, 0.0);
            pose.mulPose(Axis.XP.rotationDegrees(-30.0F));
            pose.scale(f, f, f);
            erd.setRenderShadow(false);

            erd.render(display, 0.0, 0.0, 0.0, 0.0F, partialTick, pose, buffer, LightTexture.FULL_BRIGHT);
            erd.setRenderShadow(true);
            pose.popPose();
        } catch (Exception e) {
            if (!loggedError) {
                loggedError = true;
                WHCompanions.LOGGER.warn("[SpawnerBER] display render failed (cage stays): {}", e.toString(), e);
            }
        }
    }
}
