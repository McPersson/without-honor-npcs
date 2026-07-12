package com.withouthonor.npcs.compat.ironsspells;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.CastingMobAimingData;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import io.redspace.ironsspellbooks.spells.fire.BurningDashSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Тик-драйвер каста для NPC — наша реализация контракта каста мобов ISS
 * (кто угодно LivingEntity может кастовать, если правильно вести состояние MagicData).
 * ISS-логика у них живёт в GeckoLib-базовом AbstractSpellCastingMob (ARR) — сюда
 * переписан только КОНТРАКТ по публичному API, не код.
 *
 * Поток каста (сверено с v1.20.1-3.16.2):
 *   initiateCastSpell: checkPreCastConditions → спец-данные (телепорт/рывок/луч) →
 *     MagicData.initiateCast(spell, level, effectiveCastTime, MOB, MAINHAND) → onServerPreCast
 *   tick (каждый серверный тик, пока isCasting):
 *     handleCastDuration → onServerCastTick →
 *     остаток 0: LONG/INSTANT → onCast один раз → complete;
 *     CONTINUOUS → onCast каждые 10 тиков, по истечении — complete без onCast.
 *   complete/cancel: onServerCastComplete(..., cancelled) — сам сбрасывает и синкает состояние.
 *
 * Мана и кулдауны NPC в фазе 0 не ведём (CastSource.MOB их и не потребляет).
 */
public final class NpcCastDriver {

    private NpcCastDriver() {
    }

    public static void initiateCastSpell(CompanionEntity npc, AbstractSpell spell, int spellLevel) {
        if (npc.level().isClientSide) {
            return; // клиентская часть (анимации каста) — фаза 2
        }
        if (spell == null || spell == SpellRegistry.none()) {
            return;
        }
        IMagicEntity magicEntity = (IMagicEntity) npc;
        MagicData magic = magicEntity.getMagicData();

        if (!spell.checkPreCastConditions(npc.level(), spellLevel, npc, magic)) {
            return;
        }

        // Спец-данные каста, которые контракт требует подготовить ДО initiateCast
        if (spell == SpellRegistry.TELEPORT_SPELL.get() || spell == SpellRegistry.FROST_STEP_SPELL.get()) {
            magicEntity.setTeleportLocationBehindTarget(10);
        } else if (spell == SpellRegistry.BLOOD_STEP_SPELL.get()) {
            magicEntity.setTeleportLocationBehindTarget(3);
        } else if (spell == SpellRegistry.BURNING_DASH_SPELL.get()) {
            magicEntity.setBurningDashDirectionData();
        } else if (spell == SpellRegistry.RAY_OF_SIPHONING_SPELL.get()) {
            magic.setAdditionalCastData(new CastingMobAimingData());
        }

        int duration = spell.getEffectiveCastTime(spellLevel, npc);
        magic.initiateCast(spell, spellLevel, duration, CastSource.MOB, SpellSelectionManager.MAINHAND);
        // Кэш на время каста (как поле castingSpell у ASCM) — tick() не дёргает реестр каждый тик
        ((NpcCastState) npc).whSetCachedCastSpell(spell);
        spell.onServerPreCast(npc.level(), spellLevel, npc, magic);
    }

    /** Серверный тик активного каста; ноль работы (и ноль аллокаций), когда NPC не кастует. */
    public static void tick(CompanionEntity npc) {
        if (!(npc instanceof IMagicEntity magicEntity) || !magicEntity.isCasting()) {
            return; // isCasting() не создаёт MagicData — ранний выход обязан идти до getMagicData()
        }
        MagicData magic = magicEntity.getMagicData();

        CastType castType = magic.getCastType();
        if (castType == null || castType == CastType.NONE) {
            // Каст восстановлен из NBT: syncedData хранит isCasting/id/level, но castType — поле
            // MagicData, его ставит только initiateCast. Пересоздаём каст с начала (как ASCM.recreateSpell):
            // сбрасываем «полу-каст», затем инициируем заново по сохранённым id+уровню.
            AbstractSpell restored = SpellRegistry.getSpell(magic.getCastingSpellId());
            int restoredLevel = magic.getCastingSpellLevel();
            magic.resetCastingState();
            if (restored != SpellRegistry.none()) {
                initiateCastSpell(npc, restored, restoredLevel);
            }
            return;
        }

        AbstractSpell spell = resolveActiveSpell(npc, magic);
        if (spell == null || spell == SpellRegistry.none()) {
            magic.resetCastingState(); // спелл исчез (снят аддон) — не зависаем в касте
            clearCastTransients(npc);
            return;
        }
        int spellLevel = magic.getCastingSpellLevel();

        magic.handleCastDuration();

        if (magic.isCasting()) {
            spell.onServerCastTick(npc.level(), spellLevel, npc, magic);
        }

        if (magic.getCastDurationRemaining() <= 0) {
            if (castType == CastType.LONG || castType == CastType.INSTANT) {
                spell.onCast(npc.level(), spellLevel, npc, CastSource.MOB, magic);
            }
            castComplete(npc);
        } else if (castType == CastType.CONTINUOUS
                && (magic.getCastDurationRemaining() + 1) % 10 == 0) {
            spell.onCast(npc.level(), spellLevel, npc, CastSource.MOB, magic);
        }
    }

    /** Штатное завершение каста. */
    public static void castComplete(CompanionEntity npc) {
        finishCast(npc, false);
    }

    /** Досрочная отмена (урон, смена цели и т.п.). */
    public static void cancelCast(CompanionEntity npc) {
        if (npc instanceof IMagicEntity magicEntity && magicEntity.isCasting()) {
            finishCast(npc, true);
        }
    }

    private static void finishCast(CompanionEntity npc, boolean cancelled) {
        if (npc.level().isClientSide || !(npc instanceof IMagicEntity magicEntity) || !magicEntity.isCasting()) {
            return; // handleCastDuration флаг не гасит (он в SyncedSpellData) — к этому моменту он ещё true
        }
        MagicData magic = magicEntity.getMagicData();
        AbstractSpell spell = resolveActiveSpell(npc, magic);
        if (spell != null && spell != SpellRegistry.none()) {
            // onServerCastComplete сам делает resetCastingState + синк
            spell.onServerCastComplete(npc.level(), magic.getCastingSpellLevel(), npc, magic, cancelled);
        } else {
            magic.resetCastingState();
        }
        clearCastTransients(npc);
    }

    /**
     * Активный спелл каста из пер-NPC-кэша; при рассинхроне с id в MagicData (recreate,
     * загрузка из NBT, внешний initiateCast) — перечитать из реестра и перекэшировать.
     */
    private static AbstractSpell resolveActiveSpell(CompanionEntity npc, MagicData magic) {
        NpcCastState state = (NpcCastState) npc;
        AbstractSpell cached = state.whGetCachedCastSpell();
        if (cached != null && cached.getSpellId().equals(magic.getCastingSpellId())) {
            return cached;
        }
        AbstractSpell spell = SpellRegistry.getSpell(magic.getCastingSpellId());
        state.whSetCachedCastSpell(spell == SpellRegistry.none() ? null : spell);
        return spell;
    }

    /**
     * Сброс пер-NPC состояния после любого окончания каста. Если цель ставилась действием
     * «Кастовать» (сценарный прицел) и всё ещё держится — снимаем, чтобы боевые цели
     * не продолжили атаковать собеседника; цель, взятую иначе (игрок сам заагрил,
     * смена цели в бою), не трогаем.
     */
    private static void clearCastTransients(CompanionEntity npc) {
        NpcCastState state = (NpcCastState) npc;
        state.whSetCachedCastSpell(null);
        LivingEntity aim = state.whGetScriptedCastAim();
        if (aim != null) {
            state.whSetScriptedCastAim(null);
            if (npc.getTarget() == aim) {
                npc.setTarget(null);
            }
        }
    }

    /**
     * Точка телепорта «за спиной цели» для teleport/frost step/blood step.
     * Своя упрощённая версия контракта: несколько попыток вокруг цели с проверкой
     * коллизий; не нашли — телепорт «на месте» (спелл всё равно отработает).
     */
    public static boolean setTeleportLocationBehindTarget(CompanionEntity npc, int distance) {
        MagicData magic = ((IMagicEntity) npc).getMagicData();
        LivingEntity target = npc.getTarget();
        if (target != null) {
            for (int i = 0; i < 12; i++) {
                float angle = (float) Math.toRadians(target.getYRot() + 180.0F + i * 30.0F);
                double d = Math.max(1.5D, distance - i * 0.5D);
                Vec3 pos = target.position().add(-Math.sin(angle) * d, 0.25D, Math.cos(angle) * d);
                // Прижать кандидата к земле (публичный Utils ISS) — noCollision сам по себе
                // охотно выбирает точку в воздухе
                pos = Utils.moveToRelativeGroundLevel(npc.level(), pos, 5);
                var box = npc.getBoundingBox().move(pos.subtract(npc.position()));
                if (npc.level().noCollision(npc, box)) {
                    magic.setAdditionalCastData(new TeleportSpell.TeleportData(pos));
                    return true;
                }
            }
        }
        magic.setAdditionalCastData(new TeleportSpell.TeleportData(npc.position()));
        return false;
    }

    /** Данные направления для Burning Dash — как требует контракт мобов ISS. */
    public static void setBurningDashDirectionData(CompanionEntity npc) {
        ((IMagicEntity) npc).getMagicData()
                .setAdditionalCastData(new BurningDashSpell.BurningDashDirectionOverrideCastData());
    }

    static void warnUnknownSpell(String spellId) {
        WHCompanions.LOGGER.warn("[WH ISS] Неизвестный спелл '{}'", spellId);
    }
}
