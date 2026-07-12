package com.withouthonor.npcs.compat.ironsspells;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;

/**
 * Пер-NPC состояние драйвера каста. NpcCastDriver — статическая утилита (один на всех),
 * поэтому «живое» состояние конкретного каста живёт на самой сущности: интерфейс
 * реализует IssCompanionMixin через @Unique-поля (по образцу IMagicEntity/SupportMob).
 * Линкует ISS-тип, поэтому лежит в compat.ironsspells и грузится только при ISS.
 */
public interface NpcCastState {

    /** Кэш активного спелла на время каста (как поле castingSpell у ASCM) — не дёргать реестр каждый тик. */
    AbstractSpell whGetCachedCastSpell();

    void whSetCachedCastSpell(AbstractSpell spell);

    /** Прицел сценарного каста (действие «Кастовать»): цель, которую поставило само действие
     *  и которую надо снять по завершении/отмене каста. null — цель ставилась не действием. */
    LivingEntity whGetScriptedCastAim();

    void whSetScriptedCastAim(LivingEntity aim);
}
