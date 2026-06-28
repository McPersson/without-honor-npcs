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

    private static final ResourceLocation EMOTE_ATLAS =
            ResourceLocation.fromNamespaceAndPath("wh_npcs", "textures/entity/emotes.png");

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

    @Override
    public void render(CompanionEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        List<FormattedCharSequence> bubbleLines = bubbleLines(entity);
        renderSpeechBubble(entity, poseStack, buffer, packedLight, bubbleLines);
        renderEmote(entity, poseStack, buffer);
        renderIndicator(entity, poseStack, buffer, bubbleLines);
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
        EntityType<?> type = EntityType.byString(disguise).orElse(null);
        if (type == null) {
            return null;
        }
        try {
            delegate = type.create(npc.level());
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Disguise '{}' failed to create client delegate: {}", disguise, e.toString());
            delegate = null;
        }
        if (delegate == null) {
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

    private record BubbleCache(String text, List<FormattedCharSequence> lines) {
    }

    private final Map<Integer, BubbleCache> bubbleCache = new HashMap<>();
    private final Map<String, net.minecraft.network.chat.Component> titleCache = new HashMap<>();

    @Nullable
    private List<FormattedCharSequence> bubbleLines(CompanionEntity entity) {
        if (this.entityRenderDispatcher.distanceToSqr(entity) > 1024.0D) {
            return null;
        }
        int id = entity.getId();
        String text = com.withouthonor.npcs.client.ClientBubbles.get(id);
        if (text == null) {
            bubbleCache.remove(id);
            return null;
        }
        BubbleCache cached = bubbleCache.get(id);
        if (cached != null && cached.text().equals(text)) {
            return cached.lines();
        }
        List<FormattedCharSequence> lines =
                this.getFont().split(net.minecraft.network.chat.Component.literal(text), 110);
        bubbleCache.put(id, new BubbleCache(text, lines));
        return lines;
    }

    private void renderSpeechBubble(CompanionEntity entity, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight,
                                    @Nullable List<FormattedCharSequence> lines) {
        if (lines == null) {
            return;
        }
        var font = this.getFont();
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getNameTagOffsetY() + 0.35F + lines.size() * 0.22F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.02F, -0.02F, 0.02F);
        org.joml.Matrix4f matrix = poseStack.last().pose();
        int bg = 0x66000000;
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            float x = -font.width(line) / 2.0F;
            float y = i * 10.0F;
            font.drawInBatch(line, x, y, 0x20FFFFFF, false, matrix, buffer,
                    net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, bg, packedLight);
            font.drawInBatch(line, x, y, 0xFFFFFFFF, false, matrix, buffer,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
    }

    private void renderEmote(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer) {

        if (Minecraft.getInstance().screen instanceof DialogueScreen) {
            return;
        }
        com.withouthonor.npcs.common.dialogue.EmoteIcon icon =
                com.withouthonor.npcs.client.ClientEmotes.get(entity.getId());
        if (icon == null || this.entityRenderDispatcher.distanceToSqr(entity) > 1024.0D) {
            return;
        }
        drawHeadIcon(poseStack, buffer, icon, entity.getNameTagOffsetY() + 0.45F);
    }

    private void renderIndicator(CompanionEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                 @Nullable List<FormattedCharSequence> bubbleLines) {
        if (Minecraft.getInstance().screen instanceof DialogueScreen) {
            return;
        }

        if (com.withouthonor.npcs.client.ClientEmotes.get(entity.getId()) != null) {
            return;
        }
        com.withouthonor.npcs.common.dialogue.EmoteIcon icon =
                com.withouthonor.npcs.client.ClientIndicators.get(entity.getId());
        if (icon == null || this.entityRenderDispatcher.distanceToSqr(entity) > 1024.0D) {
            return;
        }

        float y = bubbleLines != null
                ? entity.getNameTagOffsetY() + 0.35F + bubbleLines.size() * 0.22F + 0.32F
                : entity.getNameTagOffsetY() + 0.45F;
        drawHeadIcon(poseStack, buffer, icon, y);
    }

    private void drawHeadIcon(PoseStack poseStack, MultiBufferSource buffer,
                              com.withouthonor.npcs.common.dialogue.EmoteIcon icon, float yOffset) {
        int frames = com.withouthonor.npcs.common.dialogue.EmoteIcon.COUNT;
        float u0 = icon.atlasIndex() / (float) frames;
        float u1 = (icon.atlasIndex() + 1) / (float) frames;
        poseStack.pushPose();

        poseStack.translate(0.0F, yOffset, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.4F, -0.4F, 0.4F);
        org.joml.Matrix4f m = poseStack.last().pose();
        var vc = buffer.getBuffer(RenderType.text(EMOTE_ATLAS));
        int light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        float h = 0.5F;
        vc.vertex(m, -h, -h, 0.0F).color(255, 255, 255, 255).uv(u0, 0.0F).uv2(light).endVertex();
        vc.vertex(m, -h,  h, 0.0F).color(255, 255, 255, 255).uv(u0, 1.0F).uv2(light).endVertex();
        vc.vertex(m,  h,  h, 0.0F).color(255, 255, 255, 255).uv(u1, 1.0F).uv2(light).endVertex();
        vc.vertex(m,  h, -h, 0.0F).color(255, 255, 255, 255).uv(u1, 0.0F).uv2(light).endVertex();
        poseStack.popPose();
    }

    @Override
    protected void renderNameTag(CompanionEntity entity, net.minecraft.network.chat.Component displayName,
                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.renderNameTag(entity, displayName, poseStack, buffer, packedLight);
        String title = entity.getTitle();
        if (title.isEmpty() || this.entityRenderDispatcher.distanceToSqr(entity) > 4096.0D) {
            return;
        }

        net.minecraft.network.chat.Component text = titleCache.computeIfAbsent(title, t ->
                net.minecraft.network.chat.Component.literal(t.indexOf('§') >= 0 ? t : "§7" + t));
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getNameTagOffsetY(), 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float scale = 0.025F * 0.75F;
        poseStack.scale(-scale, -scale, scale);
        org.joml.Matrix4f matrix = poseStack.last().pose();
        int bg = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        net.minecraft.client.gui.Font font = this.getFont();
        float x = -font.width(text) / 2.0F;

        float y = 12.5F;
        boolean visible = !entity.isDiscrete();
        font.drawInBatch(text, x, y, 0x20FFFFFF, false, matrix, buffer,
                visible ? net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH
                        : net.minecraft.client.gui.Font.DisplayMode.NORMAL, bg, packedLight);
        if (visible) {
            font.drawInBatch(text, x, y, -1, false, matrix, buffer,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
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
