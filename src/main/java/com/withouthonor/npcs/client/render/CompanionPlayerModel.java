package com.withouthonor.npcs.client.render;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.PoseJson;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public class CompanionPlayerModel extends PlayerModel<CompanionEntity> {

    public CompanionPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(CompanionEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // Ваниль уже выставила riding для пассажиров (лодка и т.п.) — не перетирать
        this.riding = entity.isSitting() || this.riding;
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        applyPose(entity);
        reapplyAimArms(entity);

        com.withouthonor.npcs.compat.EmotecraftClientBridge emote =
                com.withouthonor.npcs.compat.Compat.emotecraftClient();
        if (emote != null && emote.applyEmote(entity,
                this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg)) {
            copyOverlays();
        }
    }

    private static final float LERP = 0.35F;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private ModelPart[] poseParts;

    private void applyPose(CompanionEntity entity) {
        if (poseParts == null) {
            poseParts = new ModelPart[]{this.head, this.body, this.rightArm,
                    this.leftArm, this.rightLeg, this.leftLeg};
        }
        ModelPart[] parts = poseParts;
        PoseJson.Pose p = entity.getPoseData();

        applyVisibility(p);
        boolean rot = p.freeze || p.hasAngles();
        if (!rot) {
            resetStaleAxes();
            copyOverlays();
            entity.setPoseRenderInit(false);
            return;
        }
        resetStaleAxes();
        float[] rendered = entity.renderedPose();
        boolean init = entity.isPoseRenderInit();
        for (int i = 0; i < 18; i++) {
            ModelPart part = parts[i / 3];
            float vanilla = axisOf(part, i % 3);
            float pose = p.angles[i] * DEG_TO_RAD;
            float target;
            if (i / 3 == 0) {
                target = vanilla + pose;
            } else if (p.freeze) {
                target = pose;
            } else {
                target = vanilla + pose;
            }
            // Кратчайшая дуга: без wrap'а скачок цели через ±180° лерпится
            // «в длинную сторону» — видимая быстрая раскрутка части тела.
            float delta = (float) Math.IEEEremainder(target - rendered[i], Math.PI * 2.0);
            rendered[i] = init ? rendered[i] + delta * LERP : target;
            setAxis(part, i % 3, rendered[i]);
        }
        entity.setPoseRenderInit(true);
        copyOverlays();
    }

    private void reapplyAimArms(CompanionEntity entity) {
        PoseJson.Pose p = entity.getPoseData();
        if (p.freeze || p.hasAngles()) {
            return;
        }
        net.minecraft.client.model.HumanoidModel.ArmPose pose = this.rightArmPose;
        boolean aim = true;
        if (pose == net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW) {
            this.rightArm.yRot = -0.1F + this.head.yRot;
            this.leftArm.yRot = 0.1F + this.head.yRot + 0.4F;
        } else if (pose == net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD) {
            this.rightArm.yRot = -0.3F + this.head.yRot;
            this.leftArm.yRot = 0.6F + this.head.yRot;
        } else if (pose == net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_CHARGE) {
            this.rightArm.yRot = -0.8F;
            float f = net.minecraft.world.item.CrossbowItem.getChargeDuration(entity.getUseItem());
            float f3 = f <= 0F ? 0F
                    : net.minecraft.util.Mth.clamp((float) entity.getTicksUsingItem(), 0F, f) / f;
            this.leftArm.yRot = net.minecraft.util.Mth.lerp(f3, 0.4F, 0.85F);
        } else {
            aim = false;
        }
        if (aim) {
            copyOverlays();
        }
    }

    private void applyVisibility(PoseJson.Pose p) {
        setPartVisible(this.head, this.hat, !p.hidden[0]);
        setPartVisible(this.body, this.jacket, !p.hidden[1]);
        setPartVisible(this.rightArm, this.rightSleeve, !p.hidden[2]);
        setPartVisible(this.leftArm, this.leftSleeve, !p.hidden[3]);
        setPartVisible(this.rightLeg, this.rightPants, !p.hidden[4]);
        setPartVisible(this.leftLeg, this.leftPants, !p.hidden[5]);
    }

    private static void setPartVisible(ModelPart part, ModelPart overlay, boolean vis) {
        part.visible = vis;
        overlay.visible = vis;
    }

    private void copyOverlays() {
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
    }

    private void resetStaleAxes() {
        this.head.zRot = 0F;
        this.body.xRot = 0F;
        this.body.zRot = 0F;
        this.rightArm.yRot = 0F;
        this.leftArm.yRot = 0F;
        this.rightLeg.yRot = 0F;
        this.rightLeg.zRot = 0F;
        this.leftLeg.yRot = 0F;
        this.leftLeg.zRot = 0F;
    }

    private static float axisOf(ModelPart part, int axis) {
        return axis == 0 ? part.xRot : axis == 1 ? part.yRot : part.zRot;
    }

    private static void setAxis(ModelPart part, int axis, float v) {
        if (axis == 0) {
            part.xRot = v;
        } else if (axis == 1) {
            part.yRot = v;
        } else {
            part.zRot = v;
        }
    }
}
