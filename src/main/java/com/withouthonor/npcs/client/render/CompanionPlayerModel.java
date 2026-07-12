package com.withouthonor.npcs.client.render;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.PoseJson;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public class CompanionPlayerModel extends PlayerModel<CompanionEntity> {

    // База позиций из запечённой модели (слои анимации их ещё не трогали). Ваниль setupAnim
    // сбрасывает y рук и y/z ног каждый кадр сама, а x/z рук и x ног — нет: их зачищаем в resetStaleAxes.
    private final float baseRightArmX, baseLeftArmX, baseRightArmZ, baseLeftArmZ, baseRightLegX, baseLeftLegX;

    public CompanionPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
        this.baseRightArmX = this.rightArm.x;
        this.baseLeftArmX = this.leftArm.x;
        this.baseRightArmZ = this.rightArm.z;
        this.baseLeftArmZ = this.leftArm.z;
        this.baseRightLegX = this.rightLeg.x;
        this.baseLeftLegX = this.leftLeg.x;
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
        boolean animated = emote != null && emote.applyEmote(entity,
                this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg);
        // Анимация каста ISS — отдельный слой, поверх эмоции (если каст активен, он перекрывает жест).
        com.withouthonor.npcs.compat.IronsSpellsClientBridge cast =
                com.withouthonor.npcs.compat.Compat.ironsSpellsClient();
        if (cast != null && cast.applyCast(entity,
                this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg)) {
            animated = true;
        }
        if (animated) {
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
        // Поза щита (щит в любой руке): resetStaleAxes зануляет yRot каждый кадр — как у лука,
        // возвращаем yRot блока. Ваниль HumanoidModel.poseRightArm/poseLeftArm (ArmPose.BLOCK):
        // правая yRot = -π/6, левая зеркально +π/6. xRot переживает reset — его не трогаем.
        boolean block = false;
        if (this.rightArmPose == net.minecraft.client.model.HumanoidModel.ArmPose.BLOCK) {
            this.rightArm.yRot = -0.5235988F;
            block = true;
        }
        if (this.leftArmPose == net.minecraft.client.model.HumanoidModel.ArmPose.BLOCK) {
            this.leftArm.yRot = 0.5235988F;
            block = true;
        }
        if (aim || block) {
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
        // Позиции: анимационные слои (каст/эмоции) пишут part.x/y/z, а ваниль setupAnim
        // сбрасывает только head.y и body.y — остальные оси зачищаем сами, иначе смещение залипает
        this.head.x = 0F;
        this.head.z = 0F;
        this.body.x = 0F;
        this.body.z = 0F;
        // Позиции рук/ног по неванильным осям — обратно к базе, иначе смещение position-кейфрейма
        // каста/эмоции залипает (y рук и y/z ног ваниль сбрасывает сама, их не трогаем)
        this.rightArm.x = this.baseRightArmX;
        this.leftArm.x = this.baseLeftArmX;
        this.rightArm.z = this.baseRightArmZ;
        this.leftArm.z = this.baseLeftArmZ;
        this.rightLeg.x = this.baseRightLegX;
        this.leftLeg.x = this.baseLeftLegX;
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
