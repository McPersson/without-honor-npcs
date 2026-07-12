package com.withouthonor.npcs.compat.ironsspells.client;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.compat.IronsSpellsClientBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.ChargeSpellLayer;
import io.redspace.ironsspellbooks.render.GlowingEyesLayer;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Проигрывает анимацию каста ISS ({@code spell.getCastStartAnimation().getForPlayer()}) на нашей
 * player-модели NPC — через тот же механизм PlayerAnimator, что и эмоции, но на СВОЁМ слое.
 *
 * Свой пакет не нужен: NPC — IMagicEntity, ISS синкает SyncedSpellData, так что на клиенте видно
 * {@code isCasting()} и {@code getCastingSpellId()}. Клиент-тик опрашивает видимых NPC и
 * стартует/останавливает анимацию. Если у заклинания нет player-анимации (isPass/Optional пуст) —
 * NPC кастует без жеста (как игрок в ISS), ничего не выдумываем.
 */
public final class IronsSpellsClientImpl implements IronsSpellsClientBridge {

    private static final String[] PA_BONES = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};

    private final Map<Integer, ModifierLayer<IAnimation>> layers = new HashMap<>();
    /** entityId → id заклинания, чья анимация сейчас играет (чтобы не перезапускать каждый тик). */
    private final Map<Integer, String> playing = new HashMap<>();

    public IronsSpellsClientImpl() {
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (!layers.isEmpty()) {
                layers.clear();
                playing.clear();
            }
            return;
        }
        // На паузе (одиночная игра / ESC) сервер стоит — не тикаем слои, иначе анимация уезжает вперёд
        if (mc.isPaused()) {
            return;
        }
        // Сверяем состояние каста у видимых NPC
        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof CompanionEntity npc) || !(ent instanceof IMagicEntity magic)) {
                continue;
            }
            String spellId = null;
            try {
                if (magic.isCasting()) {
                    spellId = magic.getMagicData().getCastingSpellId();
                }
            } catch (Throwable t) {
                spellId = null;
            }
            int id = npc.getId();
            if (spellId != null && !spellId.isEmpty()) {
                if (!spellId.equals(playing.get(id))) {
                    startAnim(npc, spellId);
                }
            } else if (playing.containsKey(id)) {
                stopAnim(id);
            }
        }
        // Тик слоёв + чистка неактивных. Запись в playing НЕ трогаем: жест может быть короче
        // каста, и без неё startAnim перезапускал бы анимацию каждый тик до конца каста.
        // playing гасится по !isCasting в опросе выше либо при уходе сущности.
        Iterator<Map.Entry<Integer, ModifierLayer<IAnimation>>> it = layers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ModifierLayer<IAnimation>> en = it.next();
            try {
                en.getValue().tick();
                if (!en.getValue().isActive()) {
                    it.remove();
                }
            } catch (Throwable t) {
                it.remove();
            }
        }
    }

    private void startAnim(CompanionEntity npc, String spellId) {
        if (!com.withouthonor.npcs.client.ClientPrefs.get().isMagicCastAnimations()) {
            return; // анимации каста отключены в config/wh_npcs-client.json
        }
        int id = npc.getId();
        // Спелл сменился посреди каста (recast): гасим прежний жест и СРАЗУ помечаем новый
        // spellId в playing — запись = «этот каст уже обработан», даже если жеста у спелла нет.
        // Иначе старая анимация продолжала бы играть, а startAnim ретраился бы каждый тик.
        ModifierLayer<IAnimation> old = layers.get(id);
        if (old != null) {
            try {
                old.setAnimation(null);
            } catch (Throwable ignored) {
                // слой почистится в проходе чистки
            }
        }
        playing.put(id, spellId);
        try {
            AbstractSpell spell = SpellRegistry.getSpell(spellId);
            if (spell == SpellRegistry.none()) {
                return;
            }
            AnimationHolder holder = spell.getCastStartAnimation();
            if (holder == null || holder.isPass) {
                return; // у заклинания нет жеста каста — оставляем NPC без анимации
            }
            Optional<ResourceLocation> animId = holder.getForPlayer();
            if (animId.isEmpty()) {
                return;
            }
            KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animId.get());
            if (anim == null) {
                return;
            }
            ModifierLayer<IAnimation> layer = layers.computeIfAbsent(id, k -> new ModifierLayer<>());
            layer.setAnimation(new KeyframeAnimationPlayer(anim));
        } catch (Throwable t) {
            // playing оставляем как маркер «обработано» — гасится по !isCasting/уходу сущности
        }
    }

    private void stopAnim(int entityId) {
        ModifierLayer<IAnimation> layer = layers.get(entityId);
        if (layer != null) {
            try {
                layer.setAnimation(null);
            } catch (Throwable ignored) {
                // слой почистится в проходе чистки
            }
        }
        playing.remove(entityId);
    }

    @Override
    public void onEntityUnload(int entityId) {
        layers.remove(entityId);
        playing.remove(entityId);
    }

    @Override
    public void clearLayers() {
        layers.clear();
        playing.clear();
    }

    @Override
    public boolean applyCast(CompanionEntity npc, ModelPart head, ModelPart body, ModelPart rightArm,
                             ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        ModifierLayer<IAnimation> layer = layers.get(npc.getId());
        if (layer == null) {
            return false;
        }
        try {
            if (!layer.isActive()) {
                return false;
            }
            float partial = Minecraft.getInstance().getFrameTime();
            layer.setupAnim(partial);
            applyBone(head, PA_BONES[0], layer, partial);
            applyBone(body, PA_BONES[1], layer, partial);
            applyBone(rightArm, PA_BONES[2], layer, partial);
            applyBone(leftArm, PA_BONES[3], layer, partial);
            applyBone(rightLeg, PA_BONES[4], layer, partial);
            applyBone(leftLeg, PA_BONES[5], layer, partial);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean applyCastBody(CompanionEntity npc, PoseStack ms, float partial) {
        ModifierLayer<IAnimation> layer = layers.get(npc.getId());
        if (layer == null) {
            return false;
        }
        try {
            if (!layer.isActive()) {
                return false;
            }
            layer.setupAnim(partial);
            Vec3f pos = layer.get3DTransform("body", TransformType.POSITION, partial, Vec3f.ZERO);
            ms.translate(pos.getX(), pos.getY() + 0.7, pos.getZ());
            Vec3f rot = layer.get3DTransform("body", TransformType.ROTATION, partial, Vec3f.ZERO);
            ms.mulPose(Axis.ZP.rotation(rot.getZ()));
            ms.mulPose(Axis.YP.rotation(rot.getY()));
            ms.mulPose(Axis.XP.rotation(rot.getX()));
            ms.translate(0.0, -0.7, 0.0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void renderCastFx(CompanionEntity npc, PoseStack poseStack, MultiBufferSource buffer,
                             float partialTicks) {
        try {
            // Дёшево: без активного каста SpellRenderingHelper не дёргаем вовсе
            if (!(npc instanceof IMagicEntity magic) || !magic.isCasting()) {
                return;
            }
            // ClientMagicData.getSyncedSpellData для IMagicEntity возвращает те же synced-данные,
            // что пришли в setSyncedSpellData; сигнатуры сверены с jar 1.20.1-3.16.2
            SpellRenderingHelper.renderSpellHelper(
                    ClientMagicData.getSyncedSpellData(npc), npc, poseStack, buffer, partialTicks);
        } catch (Throwable t) {
            // как и остальные ISS/PA-вызовы — визуал не должен ронять рендер
        }
    }

    @Override
    public List<RenderLayer<CompanionEntity, PlayerModel<CompanionEntity>>> createRenderLayers(
            RenderLayerParent<CompanionEntity, PlayerModel<CompanionEntity>> renderer) {
        // Generic-границы Vanilla-слоёв: <T extends LivingEntity, M extends HumanoidModel<T>> —
        // CompanionEntity/PlayerModel подходят (сверено с jar). ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ:
        // EnergySwirlLayer.Vanilla и EchoingStrikesHologramLayer.Vanilla НЕ вешаем — они
        // типизированы Player и кастуют сущность → ClassCastException на NPC.
        try {
            return List.of(
                    new ChargeSpellLayer.Vanilla<>(renderer),   // копьё/стрела в руке при зарядке
                    new GlowingEyesLayer.Vanilla<>(renderer));  // глаза abyssal shroud / planar sight
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static void applyBone(ModelPart part, String bone, ModifierLayer<IAnimation> layer, float partial) {
        Vec3f pos = layer.get3DTransform(bone, TransformType.POSITION, partial,
                new Vec3f(part.x, part.y, part.z));
        part.x = pos.getX();
        part.y = pos.getY();
        part.z = pos.getZ();
        Vec3f rot = layer.get3DTransform(bone, TransformType.ROTATION, partial,
                new Vec3f(clampRad(part.xRot), clampRad(part.yRot), clampRad(part.zRot)));
        part.setRotation(rot.getX(), rot.getY(), rot.getZ());
    }

    /** Нормализация угла в [-π, π] — как в эмоут-мосте, чтобы кости не «щёлкали» через 2π. */
    private static float clampRad(float f) {
        double b = ((double) f + Math.PI) % (Math.PI * 2);
        if (b < 0.0) {
            b += Math.PI * 2;
        }
        return (float) (b - Math.PI);
    }
}
