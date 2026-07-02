package com.withouthonor.npcs.compat.epicfight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.client.render.CompanionOverlays;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.registry.ModEntities;
import com.withouthonor.npcs.compat.Compat;
import com.withouthonor.npcs.compat.EmotecraftClientBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;

public final class EpicFightClientCompat {

    private EpicFightClientCompat() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(EpicFightClientCompat::onAddPatchedRenderers);
        MinecraftForge.EVENT_BUS.addListener(EpicFightClientCompat::onRenderLevelStage);
    }

    public static boolean isEpicFightRendered(CompanionEntity npc) {
        if (!npc.isEpicFightMode()) {
            return false;
        }
        EmotecraftClientBridge emote = Compat.emotecraftClient();
        return emote == null || !emote.isPlaying(npc);
    }

    private static void onAddPatchedRenderers(PatchedRenderersEvent.Add event) {
        EntityRendererProvider.Context context = event.getContext();
        event.addPatchedEntityRenderer(ModEntities.COMPANION.get(),
                entityType -> new CompanionEfRenderer(context, entityType).initLayerLast(context, entityType));
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        boolean drew = false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof CompanionEntity npc) || !isEpicFightRendered(npc)) {
                continue;
            }
            double dx = Mth.lerp(partial, npc.xOld, npc.getX()) - cam.x;
            double dy = Mth.lerp(partial, npc.yOld, npc.getY()) - cam.y;
            double dz = Mth.lerp(partial, npc.zOld, npc.getZ()) - cam.z;
            int light = LevelRenderer.getLightColor(mc.level, npc.blockPosition());
            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            CompanionOverlays.INSTANCE.renderAllForEpicFight(npc, poseStack, buffer, light, partial);
            poseStack.popPose();
            drew = true;
        }
        if (drew) {
            buffer.endBatch();
        }
    }
}
