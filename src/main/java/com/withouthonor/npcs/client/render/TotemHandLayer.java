package com.withouthonor.npcs.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TotemHandLayer extends RenderLayer<CompanionEntity, PlayerModel<CompanionEntity>> {

    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);

    private final ItemInHandRenderer itemInHandRenderer;

    public TotemHandLayer(RenderLayerParent<CompanionEntity, PlayerModel<CompanionEntity>> parent,
                          ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CompanionEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isTotemArmed() || !entity.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
            return;
        }
        HumanoidArm arm = entity.getMainArm().getOpposite();
        boolean left = arm == HumanoidArm.LEFT;
        poseStack.pushPose();
        this.getParentModel().translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate((left ? -1 : 1) / 16.0F, 0.125, -0.625);
        this.itemInHandRenderer.renderItem(entity, TOTEM,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
