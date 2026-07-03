package com.withouthonor.npcs.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.client.cache.ClientSkinCache;
import com.withouthonor.npcs.client.gui.DialogueScreen;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CompanionRenderer extends MobRenderer<CompanionEntity, PlayerModel<CompanionEntity>> {

    private static final String DEFAULT_SKIN_SPEC = "default:alessia";

    private final PlayerModel<CompanionEntity> wideModel;
    private final PlayerModel<CompanionEntity> slimModel;

    public CompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new CompanionPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new CompanionPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        this.addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(
                this, context.getItemInHandRenderer()));
        this.addLayer(new net.minecraft.client.renderer.entity.layers.CustomHeadLayer<>(
                this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new TotemHandLayer(this, context.getItemInHandRenderer()));

        if (com.withouthonor.npcs.compat.Compat.curiosLoaded()) {
            this.addLayer(new com.withouthonor.npcs.compat.curios.CuriosNpcLayer(this));
        }
    }

    private final Map<CompanionEntity, Entity> disguiseDelegates = new WeakHashMap<>();
    private final Map<Entity, EntityRenderer<? super Entity>> disguiseRenderers = new WeakHashMap<>();
    // Негативный кэш: не пересоздавать сломанный/неизвестный облик каждый кадр
    private static final java.util.Set<String> failedDisguises = new java.util.HashSet<>();

    @Override
    public void render(CompanionEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        CompanionOverlays.INSTANCE.renderOverlays(entity, poseStack, buffer, packedLight, partialTicks);
        String disguise = entity.getDisguise();
        if (!disguise.isEmpty()) {
            Entity delegate = delegateFor(entity, disguise);
            if (delegate != null) {
                renderDisguised(entity, delegate, entityYaw, partialTicks, poseStack, buffer, packedLight);
                return;
            }
        }
        ClientSkinCache.Skin skin = skinOf(entity);

        boolean slim = skin != null ? skin.slim() : entity.getSkinName().isEmpty();
        this.model = slim ? slimModel : wideModel;
        applyArmPoses(entity);

        CompanionEntity.RenderTransform t = entity.getRenderTransform();
        double dy = (entity.isSitting() ? -0.4D : 0.0D) + t.posY();
        if (entity.isSitting() || t.hasOffset()) {

            poseStack.pushPose();
            poseStack.translate(t.posX(), dy, t.posZ());
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
        } else {
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    private void applyArmPoses(CompanionEntity entity) {
        net.minecraft.client.model.HumanoidModel.ArmPose main = armPose(entity, net.minecraft.world.InteractionHand.MAIN_HAND);
        net.minecraft.client.model.HumanoidModel.ArmPose off = armPose(entity, net.minecraft.world.InteractionHand.OFF_HAND);
        boolean right = entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        this.model.rightArmPose = right ? main : off;
        this.model.leftArmPose = right ? off : main;
    }

    private static net.minecraft.client.model.HumanoidModel.ArmPose armPose(CompanionEntity e,
                                                                            net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = e.getItemInHand(hand);
        if (stack.isEmpty()) {
            return net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        }
        if (e.getUsedItemHand() == hand && e.getUseItemRemainingTicks() > 0) {
            net.minecraft.world.item.UseAnim anim = stack.getUseAnimation();
            if (anim == net.minecraft.world.item.UseAnim.BOW) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
            if (anim == net.minecraft.world.item.UseAnim.CROSSBOW) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
            if (anim == net.minecraft.world.item.UseAnim.SPEAR) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.THROW_SPEAR;
            }
            if (anim == net.minecraft.world.item.UseAnim.BLOCK) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.BLOCK;
            }
            if (anim == net.minecraft.world.item.UseAnim.SPYGLASS) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.SPYGLASS;
            }
        } else if (!e.swinging && stack.getItem() instanceof net.minecraft.world.item.CrossbowItem
                && net.minecraft.world.item.CrossbowItem.isCharged(stack)) {
            return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
    }

    @Override
    protected void setupRotations(CompanionEntity entity, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);

        CompanionEntity.RenderTransform t = entity.getRenderTransform();
        if (t.hasRotation()) {
            if (t.rotY() != 0) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(t.rotY()));
            }
            if (t.rotX() != 0) {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(t.rotX()));
            }
            if (t.rotZ() != 0) {
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(t.rotZ()));
            }
        }

        com.withouthonor.npcs.compat.EmotecraftClientBridge emote =
                com.withouthonor.npcs.compat.Compat.emotecraftClient();
        if (emote != null) {
            emote.applyBodyTransform(entity, poseStack, partialTicks);
        }
    }

    @Nullable
    private Entity delegateFor(CompanionEntity npc, String disguise) {
        Entity delegate = disguiseDelegates.get(npc);
        if (delegate != null
                && disguise.equals(EntityType.getKey(delegate.getType()).toString())) {
            return delegate;
        }
        if (failedDisguises.contains(disguise)) {
            return null;
        }
        EntityType<?> type = EntityType.byString(disguise).orElse(null);
        if (type == null) {
            failedDisguises.add(disguise);
            return null;
        }
        try {
            delegate = type.create(npc.level());
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Disguise '{}' failed to create client delegate: {}", disguise, e.toString());
            delegate = null;
        }
        if (delegate == null) {
            failedDisguises.add(disguise);
            disguiseDelegates.remove(npc);
            return null;
        }
        disguiseDelegates.put(npc, delegate);
        return delegate;
    }

    private void renderDisguised(CompanionEntity npc, Entity delegate, float entityYaw, float partialTicks,
                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        delegate.setPos(npc.getX(), npc.getY(), npc.getZ());
        delegate.setYRot(npc.getYRot());
        delegate.yRotO = npc.yRotO;
        delegate.setXRot(npc.getXRot());
        delegate.xRotO = npc.xRotO;
        if (delegate instanceof LivingEntity living) {
            living.yBodyRot = npc.yBodyRot;
            living.yBodyRotO = npc.yBodyRotO;
            living.yHeadRot = npc.yHeadRot;
            living.yHeadRotO = npc.yHeadRotO;
            living.setOnGround(npc.onGround());

            if (delegate.tickCount != npc.tickCount) {
                delegate.tickCount = npc.tickCount;
                living.walkAnimation.update(npc.walkAnimation.speed(), 0.4F);
                if (living instanceof Mob mob) {
                    mob.setNoAi(true);
                }
            }
        } else {
            delegate.tickCount = npc.tickCount;
        }
        try {
            EntityRenderer<? super Entity> renderer = disguiseRenderers.computeIfAbsent(delegate,
                    d -> Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(d));
            renderer.render(delegate, entityYaw, partialTicks, poseStack, buffer, packedLight);
        } catch (Exception e) {

            WHCompanions.LOGGER.warn("Disguise '{}' renderer failed, falling back: {}",
                    npc.getDisguise(), e.toString());
            disguiseDelegates.remove(npc);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CompanionEntity entity) {
        ClientSkinCache.Skin skin = skinOf(entity);

        return skin != null ? skin.location()
                : net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin();
    }

    @Nullable
    private static ClientSkinCache.Skin skinOf(CompanionEntity entity) {
        String skinName = entity.getSkinName();
        return ClientSkinCache.getInstance().get(skinName.isEmpty() ? DEFAULT_SKIN_SPEC : skinName);
    }

    @Override
    protected RenderType getRenderType(CompanionEntity entity, boolean visible, boolean translucentToPlayer, boolean glowing) {

        if (visible && !translucentToPlayer && !glowing) {
            return RenderType.entityCutoutNoCull(this.getTextureLocation(entity));
        }
        return super.getRenderType(entity, visible, translucentToPlayer, glowing);
    }

    @Override
    protected void renderNameTag(CompanionEntity entity, net.minecraft.network.chat.Component displayName,
                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.renderNameTag(entity, displayName, poseStack, buffer, packedLight);
        CompanionOverlays.INSTANCE.renderTitle(entity, poseStack, buffer, packedLight);
    }

    @Override
    protected boolean shouldShowName(CompanionEntity entity) {

        if (Minecraft.getInstance().screen instanceof DialogueScreen) {
            return false;
        }
        return super.shouldShowName(entity);
    }

    @Override
    protected void scale(CompanionEntity entity, PoseStack poseStack, float partialTick) {

        CompanionEntity.RenderTransform t = entity.getRenderTransform();
        poseStack.scale(0.9375F * t.scaleX(), 0.9375F * t.scaleY(), 0.9375F * t.scaleZ());
    }
}
