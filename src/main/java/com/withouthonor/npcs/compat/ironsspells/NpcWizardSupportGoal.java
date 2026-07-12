package com.withouthonor.npcs.compat.ironsspells;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.SupportMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardSupportGoal;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.PathfinderMob;

import java.util.ArrayList;
import java.util.List;

/**
 * «Поумневшая» цель поддержки поверх protected-контракта {@link WizardSupportGoal}:
 * не перекастовывает бафф, чей эффект уже висит на цели поддержки.
 *
 * База зовёт getSpell(List) из getNextSpellType для ОБОИХ списков (лечение и баффы) —
 * фильтруем только баффы (сверка ссылки со своим protected-полем buffSpells): лечить
 * можно всегда, у хила «висящего эффекта» не бывает. Баффы аддонов в {@link NpcBuffTable}
 * не значатся и проходят без фильтра, как в базе.
 */
public class NpcWizardSupportGoal<T extends PathfinderMob & SupportMob & IMagicEntity>
        extends WizardSupportGoal<T> {

    public NpcWizardSupportGoal(T mob, double speedModifier, int castIntervalMin, int castIntervalMax) {
        super(mob, speedModifier, castIntervalMin, castIntervalMax);
    }

    @Override
    protected AbstractSpell getSpell(List<AbstractSpell> spells) {
        // target здесь — СОЮЗНИК, которого поддерживаем (protected-поле базы, ставится в canUse).
        if (spells == buffSpells && target != null && !spells.isEmpty()) {
            List<AbstractSpell> fresh = new ArrayList<>(spells.size());
            for (AbstractSpell spell : spells) {
                MobEffect buff = NpcBuffTable.effectFor(spell);
                if (buff != null && target.hasEffect(buff)) {
                    continue; // бафф ещё действует — не жжём каст впустую
                }
                fresh.add(spell);
            }
            if (!fresh.isEmpty()) {
                // Равномерный рандом по оставшимся — тот же принцип выбора, что у базы.
                return fresh.get(mob.getRandom().nextInt(fresh.size()));
            }
            // Все баффы уже висят — отдаём слово базе (перекаст обновит длительность,
            // контракт «пустого ответа не бывает» сохранён).
        }
        return super.getSpell(spells);
    }
}
