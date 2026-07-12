package com.withouthonor.npcs.compat;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;

/**
 * Мост к Iron's Spells 'n Spellbooks. Никаких ISS-типов в сигнатурах —
 * интерфейс линкуется всегда, реализация только при наличии мода.
 */
public interface IronsSpellsBridge {

    boolean isAvailable();

    /**
     * Начать каст заклинания на NPC (server-side).
     *
     * @param spellId id спелла, например "irons_spellbooks:fireball"
     *                (без неймспейса подставится irons_spellbooks)
     * @return true, если каст стартовал (спелл найден и precast-условия выполнены)
     */
    boolean castSpell(CompanionEntity npc, String spellId, int spellLevel);

    /**
     * Сценарный каст с прицелом в собеседника (действие «Кастовать», targetSelf=false):
     * NPC берёт aimTarget боевой целью только ради прицела направленных заклинаний,
     * по завершении/отмене каста цель снимается — если её поставил именно этот вызов
     * (игрока, заагрившего NPC ещё до действия, не отпускаем).
     */
    boolean castSpellAt(CompanionEntity npc, String spellId, int spellLevel,
                        net.minecraft.world.entity.LivingEntity aimTarget);

    /** Серверный тик-драйвер каста; дёргается из CompanionEntity.tick() при ISS. */
    void tick(CompanionEntity npc);

    boolean isCastingNow(CompanionEntity npc);

    void cancel(CompanionEntity npc);

    /**
     * Собрать родную боевую цель мага (ISS WizardAttackGoal) по лоадауту профиля.
     * Возвращаемый тип — ванильный Goal (без ISS-типов в сигнатуре).
     *
     * @return цель, либо null если валидных спеллов в лоадауте нет (вызывающий откатится к ближнему бою)
     */
    net.minecraft.world.entity.ai.goal.Goal buildMageGoal(CompanionEntity npc, CompanionProfile profile);

    /** Цель-искатель союзника для поддержки (в targetSelector); ставит support-target через SupportMob. */
    net.minecraft.world.entity.ai.goal.Goal buildSupportFinderGoal(CompanionEntity npc);

    /** Цель каста поддержки по найденному союзнику (в goalSelector). null, если support-спеллов нет. */
    net.minecraft.world.entity.ai.goal.Goal buildSupportCastGoal(CompanionEntity npc, CompanionProfile profile);

    /** Все включённые спеллы ISS для пикера (DTO без ISS-типов). */
    java.util.List<SpellInfo> listSpells();

    /** Атрибуты магии для редактора: 3 общих + 9 школ × (сопротивление, сила). DTO без ISS-типов. */
    java.util.List<MagicAttrInfo> listMagicAttributes();

    /**
     * Только id тех же атрибутов — чтобы применять их к сущности через ForgeRegistries,
     * не линкуя классы ISS вне моста (план §3.1).
     */
    java.util.List<String> magicAttributeIds();

    /**
     * id спеллов из спеллбука в Curios-слоте "spellbook" сущности (ISS регистрирует этот слот
     * через Curios). Пусто без Curios/книги. На клиенте работает по synced-стекам трекаемой
     * сущности — экран «Магия» показывает книжные спеллы без отдельного S2C-запроса.
     */
    java.util.List<String> readEquippedSpellbookSpellIds(net.minecraft.world.entity.LivingEntity entity);
}
