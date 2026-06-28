package com.withouthonor.npcs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.block.TriggerBlockEntity;
import com.withouthonor.npcs.common.block.TriggerClientIndex;
import com.withouthonor.npcs.common.block.TriggerHelperBlockEntity;
import com.withouthonor.npcs.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = WHCompanions.MODID, value = Dist.CLIENT)
public final class ClientTriggerRender {

    private static final int RENDER_DIST = 64;
    private static final double RENDER_DIST_SQ = RENDER_DIST * RENDER_DIST;
    private static final double INSET = 0.08;
    private static final float[] PURPLE = {0.72F, 0.25F, 0.95F};
    private static final float[] GREY = {0.6F, 0.6F, 0.6F};

    private record Marker(int x, int y, int z, float r, float g, float b) {
    }

    private static final List<Marker> SCRATCH = new ArrayList<>();

    private ClientTriggerRender() {
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            TriggerClientIndex.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !holdingTool(player)) {
            return;
        }
        if (TriggerClientIndex.ENTRIES.isEmpty()) {
            return;
        }

        BlockPos eye = player.blockPosition();
        SCRATCH.clear();
        for (BlockEntity be : TriggerClientIndex.ENTRIES) {
            if (be.isRemoved() || eye.distSqr(be.getBlockPos()) > RENDER_DIST_SQ) {
                continue;
            }
            float[] color;
            if (be instanceof TriggerBlockEntity) {
                color = PURPLE;
            } else if (be instanceof TriggerHelperBlockEntity helper) {
                BlockPos link = helper.getTriggerPos();
                color = link == null ? GREY : groupColor(link);
            } else {
                continue;
            }
            BlockPos pos = be.getBlockPos();
            SCRATCH.add(new Marker(pos.getX(), pos.getY(), pos.getZ(), color[0], color[1], color[2]));
        }
        if (SCRATCH.isEmpty()) {
            return;
        }

        PoseStack ps = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer fill = buf.getBuffer(RenderType.debugFilledBox());
        for (Marker mk : SCRATCH) {
            LevelRenderer.addChainedFilledBoxVertices(ps, fill,
                    mk.x + INSET, mk.y + INSET, mk.z + INSET,
                    mk.x + 1 - INSET, mk.y + 1 - INSET, mk.z + 1 - INSET,
                    mk.r, mk.g, mk.b, 0.30F);
        }
        buf.endBatch(RenderType.debugFilledBox());

        VertexConsumer lines = buf.getBuffer(RenderType.lines());
        for (Marker mk : SCRATCH) {
            LevelRenderer.renderLineBox(ps, lines,
                    mk.x + INSET, mk.y + INSET, mk.z + INSET,
                    mk.x + 1 - INSET, mk.y + 1 - INSET, mk.z + 1 - INSET,
                    mk.r, mk.g, mk.b, 0.9F);
        }
        buf.endBatch(RenderType.lines());

        ps.popPose();
    }

    private static boolean holdingTool(LocalPlayer player) {
        return isTool(player.getMainHandItem()) || isTool(player.getOffhandItem());
    }

    private static boolean isTool(ItemStack stack) {
        return stack.is(ModItems.MEMORION_FEATHER.get())
                || stack.is(ModItems.TRIGGER.get())
                || stack.is(ModItems.TRIGGER_HELPER.get());
    }

    private static float[] groupColor(BlockPos triggerPos) {
        int hash = triggerPos.hashCode();
        float hue = (((hash % 360) + 360) % 360) / 360.0F;
        int rgb = Mth.hsvToRgb(hue, 0.75F, 0.95F);
        return new float[]{((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F};
    }
}
