package com.withouthonor.npcs.compat.ironsspells;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Таблица «самобафф-спелл → эффект», по которой умные цели ({@link NpcWizardAttackGoal},
 * {@link NpcWizardSupportGoal}) понимают, что бафф уже висит и перекастовывать его рано.
 *
 * Состав собран по байткоду базового ISS 3.16.2 (onCast → addEffect/forceAddEffect):
 * включены только однозначные длительные баффы на кастера/цель поддержки. НЕ включены:
 *  - транзиентные эффекты движения/атаки (ascension, burning_dash, volt_strike, blood_step);
 *  - дебаффы на врагов (blight, slow);
 *  - побочные эффекты атакующих спеллов (fall_damage_immunity у shadow_slash).
 * Спеллов аддонов в таблице нет — они фильтру не подпадают и ведут себя как раньше
 * (ИИ может их перекастовывать, ровно как ванильный WizardAttackGoal).
 */
final class NpcBuffTable {

    /** spellId (полный ResourceLocation-стринг, как возвращает AbstractSpell.getSpellId()) → id эффекта. */
    private static final Map<String, String> SPELL_TO_EFFECT = Map.ofEntries(
            Map.entry("irons_spellbooks:angel_wing", "irons_spellbooks:angel_wings"),
            Map.entry("irons_spellbooks:charge", "irons_spellbooks:charged"),
            Map.entry("irons_spellbooks:thunderstorm", "irons_spellbooks:thunderstorm"),
            Map.entry("irons_spellbooks:frostbite", "irons_spellbooks:frostbite"),
            Map.entry("irons_spellbooks:echoing_strikes", "irons_spellbooks:echoing_strikes"),
            Map.entry("irons_spellbooks:evasion", "irons_spellbooks:evasion"),
            Map.entry("irons_spellbooks:heartstop", "irons_spellbooks:heartstop"),
            Map.entry("irons_spellbooks:gluttony", "irons_spellbooks:gluttony"),
            Map.entry("irons_spellbooks:spider_aspect", "irons_spellbooks:spider_aspect"),
            Map.entry("irons_spellbooks:invisibility", "irons_spellbooks:true_invisibility"),
            Map.entry("irons_spellbooks:abyssal_shroud", "irons_spellbooks:abyssal_shroud"),
            Map.entry("irons_spellbooks:planar_sight", "irons_spellbooks:planar_sight"),
            Map.entry("irons_spellbooks:oakskin", "irons_spellbooks:oakskin"),
            Map.entry("irons_spellbooks:haste", "irons_spellbooks:hastened"),
            Map.entry("irons_spellbooks:fortify", "irons_spellbooks:fortify"));

    /** Кэш резолва в реестре: Optional.empty() = эффекта в реестре нет, запись игнорируется. */
    private static final Map<String, Optional<MobEffect>> RESOLVED = new HashMap<>();

    private NpcBuffTable() {
    }

    /**
     * Эффект, который накладывает этот спелл, или null, если спелл не из таблицы
     * (не самобафф, аддонный и т.п.) либо эффект отсутствует в реестре.
     * Зовётся только с серверного тика целей — синхронизация кэшу не нужна.
     */
    @Nullable
    static MobEffect effectFor(AbstractSpell spell) {
        String effectId = SPELL_TO_EFFECT.get(spell.getSpellId());
        if (effectId == null) {
            return null;
        }
        return RESOLVED.computeIfAbsent(effectId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? Optional.empty()
                    : Optional.ofNullable(ForgeRegistries.MOB_EFFECTS.getValue(rl));
        }).orElse(null);
    }
}
