package com.withouthonor.npcs.compat.epicfight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;

public class EfTotemLayer extends PatchedLayer<CompanionEntity, CompanionMobPatch,
        PlayerModel<CompanionEntity>, RenderLayer<CompanionEntity, PlayerModel<CompanionEntity>>> {

    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);

    @Override
    protected void renderLayer(CompanionMobPatch entitypatch, CompanionEntity entity,
                               RenderLayer<CompanionEntity, PlayerModel<CompanionEntity>> vanillaLayer,
                               PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                               OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
        if (!entity.isTotemArmed() || !entity.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
            return;
        }
        ClientEngine.getInstance().renderEngine.getItemRenderer(TOTEM).renderItemInHand(
                TOTEM, entitypatch, InteractionHand.OFF_HAND, poses, buffer, poseStack, packedLight, partialTicks);
    }
}
