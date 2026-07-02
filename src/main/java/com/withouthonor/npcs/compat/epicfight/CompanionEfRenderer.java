package com.withouthonor.npcs.compat.epicfight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.withouthonor.npcs.client.cache.ClientSkinCache;
import com.withouthonor.npcs.client.render.CompanionRenderer;
import com.withouthonor.npcs.client.render.TotemHandLayer;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

public class CompanionEfRenderer extends PHumanoidRenderer<
        CompanionEntity, CompanionMobPatch, PlayerModel<CompanionEntity>, CompanionRenderer, HumanoidMesh> {

    private static final String DEFAULT_SKIN_SPEC = "default:alessia";

    public CompanionEfRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
        super(Meshes.BIPED, context, entityType);
        this.addPatchedLayer(TotemHandLayer.class, new EfTotemLayer());
    }

    @Override
    public AssetAccessor<HumanoidMesh> getMeshProvider(CompanionMobPatch entitypatch) {
        return isSlim(entitypatch.getOriginal()) ? Meshes.ALEX : Meshes.BIPED;
    }

    @Override
    public void mulPoseStack(PoseStack poseStack, Armature armature, CompanionEntity entity,
                             CompanionMobPatch entitypatch, float partialTicks) {
        super.mulPoseStack(poseStack, armature, entity, entitypatch, partialTicks);
        CompanionEntity.RenderTransform t = entity.getRenderTransform();
        if (t.hasOffset()) {
            poseStack.translate(t.posX(), t.posY(), t.posZ());
        }
        if (t.hasRotation()) {
            if (t.rotY() != 0) {
                poseStack.mulPose(Axis.YP.rotationDegrees(t.rotY()));
            }
            if (t.rotX() != 0) {
                poseStack.mulPose(Axis.XP.rotationDegrees(t.rotX()));
            }
            if (t.rotZ() != 0) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(t.rotZ()));
            }
        }
        poseStack.scale(t.scaleX(), t.scaleY(), t.scaleZ());
    }

    private static boolean isSlim(CompanionEntity npc) {
        String skinName = npc.getSkinName();
        ClientSkinCache.Skin skin = ClientSkinCache.getInstance()
                .get(skinName.isEmpty() ? DEFAULT_SKIN_SPEC : skinName);
        return skin != null ? skin.slim() : skinName.isEmpty();
    }
}
