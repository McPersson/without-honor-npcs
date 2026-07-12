package com.withouthonor.npcs.compat.ironsspells;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * «Поумневший» боевой каст-мозг NPC поверх protected-контракта {@link WizardAttackGoal}.
 * Реализация выбора спелла НАША (не перенос кода ISS): используются только унаследованные
 * protected-поля/геттеры базы.
 *
 * Отличия от базы:
 *  1. Анти-перекаст: самобаффы из {@link NpcBuffTable}, чей эффект уже висит на NPC, не выбираются.
 *  2. Дальность: спеллы, чей shouldAIStopCasting говорит «сейчас не достанет» (лучи/зоны без
 *     линии видимости или вне дистанции), отсеиваются ещё на выборе, а не после (база выбрала бы
 *     такой спелл и просто простояла тик впустую).
 *  3. Пустая после фильтров категория выбывает из розыгрыша — берётся следующая по весу.
 *  4. Датчик снарядов (см. {@link #tick()}) оживляет родной вес защиты.
 */
public class NpcWizardAttackGoal extends WizardAttackGoal {

    /**
     * Свои штрафы анти-повтора категории (не копия чисел ISS, порядок величин тот же):
     * defense/support после самих себя почти выпадают из розыгрыша, movement штрафуется мягче —
     * два рывка подряд допустимы, а вот два щита подряд выглядят глупо.
     */
    private static final int REPEAT_PENALTY_DEFENSE = 100;
    private static final int REPEAT_PENALTY_SUPPORT = 100;
    private static final int REPEAT_PENALTY_MOVEMENT = 50;

    public NpcWizardAttackGoal(IMagicEntity abstractSpellCastingMob, double speedModifier,
                               int castIntervalMin, int castIntervalMax) {
        super(abstractSpellCastingMob, speedModifier, castIntervalMin, castIntervalMax);
    }

    /**
     * Датчик входящих снарядов. В базовом ISS getDefenseWeight() прибавляет projectileCount * 95,
     * но само поле никто никогда не инкрементит — датчик «мёртвый». Здесь мы его оживляем:
     * раз в 3 тика считаем снаряды, летящие в сторону NPC, и NPC начинает реально поднимать
     * щиты/уклонения под обстрелом.
     */
    @Override
    public void tick() {
        if (mob.tickCount % 3 == 0) {
            projectileCount = countIncomingProjectiles();
        }
        super.tick();
    }

    private int countIncomingProjectiles() {
        int count = 0;
        Vec3 center = mob.position().add(0.0, mob.getBbHeight() * 0.5, 0.0);
        for (Projectile proj : mob.level().getEntitiesOfClass(Projectile.class, mob.getBoundingBox().inflate(16.0))) {
            Entity owner = proj.getOwner();
            if (owner == mob) {
                continue; // свои же снаряды — не повод для паники
            }
            // Дружественный огонь не в счёт: хозяин (следуемый игрок) и соратники по фракции.
            // whIsSupportAlly дешёвый и строго серверный — цели ISS тикают только на сервере.
            if (owner != null && mob instanceof CompanionEntity npc && npc.whIsSupportAlly(owner)) {
                continue;
            }
            Vec3 velocity = proj.getDeltaMovement();
            if (velocity.lengthSqr() < 1.0E-4) {
                continue; // лежащая стрела/зависший снаряд — не угроза
            }
            Vec3 toMob = center.subtract(proj.position());
            if (toMob.lengthSqr() < 1.0E-6 // снаряд уже «в нас»
                    || velocity.normalize().dot(toMob.normalize()) > 0.5) { // летит в нашу сторону
                count++;
            }
        }
        return count;
    }

    /**
     * Двухступенчатый выбор спелла (своя реализация поверх родных весов):
     *  1. Вес каждой категории — родные getAttackWeight()/getDefenseWeight()/... минус наш штраф
     *     анти-повтора, если категория кастовалась последней (сверка по ссылке на protected-список,
     *     как это делает и база).
     *  2. Внутри категории кандидаты фильтруются: (а) самобафф, уже висящий на NPC, (б) спелл,
     *     который по shouldAIStopCasting сейчас не достанет до цели. Категория, опустевшая после
     *     фильтров, выбывает из розыгрыша — автоматически «пробуем следующую по весу».
     * Всё пусто/веса неположительные → SpellRegistry.none(), ровно как база при пустоте
     * (doSpellAction переварит none() как «каст не состоялся»).
     *
     * Ветку drinksPotions базы намеренно не воспроизводим: наш мост никогда не зовёт
     * setDrinksPotions(), у NPC зелий нет.
     */
    @Override
    protected AbstractSpell getNextSpellType() {
        // Порядок фиксированный: attack, defense, movement, support (как у базы).
        List<ArrayList<AbstractSpell>> raw = List.of(attackSpells, defenseSpells, movementSpells, supportSpells);
        int supportWeight = getSupportWeight() - (lastSpellCategory == supportSpells ? REPEAT_PENALTY_SUPPORT : 0);
        supportWeight = applyProactiveBuffFloor(supportWeight);
        int[] weights = {
                getAttackWeight(), // атаку после атаки не штрафуем: молотить боевыми подряд — норма
                getDefenseWeight() - (lastSpellCategory == defenseSpells ? REPEAT_PENALTY_DEFENSE : 0),
                getMovementWeight() - (lastSpellCategory == movementSpells ? REPEAT_PENALTY_MOVEMENT : 0),
                supportWeight
        };
        List<ArrayList<AbstractSpell>> pickRaw = new ArrayList<>(4);
        List<List<AbstractSpell>> pickCandidates = new ArrayList<>(4);
        List<Integer> pickWeights = new ArrayList<>(4);
        int total = 0;
        for (int i = 0; i < raw.size(); i++) {
            if (weights[i] <= 0 || raw.get(i).isEmpty()) {
                continue;
            }
            List<AbstractSpell> candidates = filterCastable(raw.get(i));
            if (candidates.isEmpty()) {
                continue; // всё уже забаффано/не достаёт — категория выбывает из розыгрыша
            }
            pickRaw.add(raw.get(i));
            pickCandidates.add(candidates);
            pickWeights.add(weights[i]);
            total += weights[i];
        }
        if (total <= 0) {
            return SpellRegistry.none(); // контракт базы: нечего кастовать
        }
        int roll = mob.getRandom().nextInt(total);
        for (int i = 0; i < pickWeights.size(); i++) {
            roll -= pickWeights.get(i);
            if (roll < 0) {
                // Анти-повтору нужна ссылка на РОДНОЙ список: база и мы сравниваем категории по ==.
                lastSpellCategory = pickRaw.get(i);
                List<AbstractSpell> candidates = pickCandidates.get(i);
                // Равномерный рандом по отфильтрованным; дубликаты в списке (вес спелла из
                // профиля) сами по себе повышают шанс — нас это устраивает.
                return candidates.get(mob.getRandom().nextInt(candidates.size()));
            }
        }
        return SpellRegistry.none(); // недостижимо при total > 0, но контракт держим
    }

    /**
     * #2 Проактивный самобафф. Родной getSupportWeight растёт только от нехватки HP, поэтому на
     * полном здоровье баффы почти не выбирались (лечение и бафф делят один вес). Если среди
     * самоподдержки есть кастуемый бафф, которого на NPC ещё нет, поднимаем вес поддержки до уровня
     * атаки — маг успевает забаффаться в начале боя. Как только баффы наложены, filterCastable их
     * убирает, кастуемых баффов не остаётся, и вес поддержки возвращается к лечению (по HP как в ISS).
     */
    private int applyProactiveBuffFloor(int supportWeight) {
        int floor = getAttackWeight();
        if (supportWeight >= floor) {
            return supportWeight;
        }
        for (AbstractSpell spell : supportSpells) {
            MobEffect buff = NpcBuffTable.effectFor(spell);
            if (buff == null || mob.hasEffect(buff)) {
                continue; // не бафф либо уже висит
            }
            if (target != null && spell.shouldAIStopCasting(1, mob, target)) {
                continue; // сейчас не скастуется
            }
            return floor; // есть кастуемый неналоженный самобафф → поднять вес поддержки
        }
        return supportWeight;
    }

    /** Кандидаты категории после анти-перекаста и проверки дальности. */
    private List<AbstractSpell> filterCastable(List<AbstractSpell> spells) {
        List<AbstractSpell> out = new ArrayList<>(spells.size());
        for (AbstractSpell spell : spells) {
            MobEffect buff = NpcBuffTable.effectFor(spell);
            if (buff != null && mob.hasEffect(buff)) {
                continue; // самобафф ещё висит — перекастовывать рано
            }
            // Уровень 1 как консервативная оценка: реализации shouldAIStopCasting в базовом ISS
            // на уровень не завязаны (проверяют дистанцию/линию видимости).
            if (target != null && spell.shouldAIStopCasting(1, mob, target)) {
                continue; // сейчас не долетит/не достанет
            }
            out.add(spell);
        }
        return out;
    }
}
