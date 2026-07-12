package com.withouthonor.npcs.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import java.util.List;

/**
 * Клиентский мост анимаций каста Iron's Spells на player-модели NPC. Отдельный слой PlayerAnimator,
 * независимый от эмоций. Реализация только при ISS + Dist.CLIENT (player-anim приносит сам ISS).
 * Как и EmotecraftClientBridge — типы ModelPart/PoseStack тут ок, интерфейс грузится лишь на клиенте.
 */
public interface IronsSpellsClientBridge {

    /** Сущность ушла из клиентского мира — убрать её слой анимации и запись каста. */
    default void onEntityUnload(int entityId) {
    }

    /** Смена/выгрузка клиентского мира — сбросить все слои и записи каста. */
    default void clearLayers() {
    }

    /** Применить активную анимацию каста к костям модели. true — если что-то применено. */
    boolean applyCast(CompanionEntity npc, ModelPart head, ModelPart body, ModelPart rightArm,
                      ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg);

    /** Применить трансформ тела (root) активной анимации каста. */
    boolean applyCastBody(CompanionEntity npc, PoseStack poseStack, float partialTick);

    /**
     * Мировой визуал активного каста (луч ray of siphoning и т.п.) — SpellRenderingHelper ISS.
     * У мобов ISS зовётся из их рендерера, у игроков — RenderLivingEvent.Post с фильтром
     * instanceof Player, наш NPC мимо обоих путей — зовём сами после super.render.
     * Внутри ранний выход, если NPC не кастует — снаружи гейтить не нужно.
     */
    default void renderCastFx(CompanionEntity npc, PoseStack poseStack, MultiBufferSource buffer,
                              float partialTicks) {
    }

    /**
     * Слои рендера ISS для нашего рендерера (заряд копья/стрелы в руке, свечение глаз).
     * Фабрика в Impl — ISS-классы слоёв из CompanionRenderer не линкуем.
     */
    default List<RenderLayer<CompanionEntity, PlayerModel<CompanionEntity>>> createRenderLayers(
            RenderLayerParent<CompanionEntity, PlayerModel<CompanionEntity>> renderer) {
        return List.of();
    }
}
