package com.withouthonor.npcs.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.model.geom.ModelPart;

public interface EmotecraftClientBridge {

    boolean applyEmote(CompanionEntity npc, ModelPart head, ModelPart body, ModelPart rightArm,
                       ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg);

    boolean applyBodyTransform(CompanionEntity npc, PoseStack poseStack, float partialTick);

    boolean isPlaying(CompanionEntity npc);
}
