package com.withouthonor.npcs.compat.ironsspells;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.compat.IronsSpellsBridge;
import com.withouthonor.npcs.compat.MagicAttrInfo;
import com.withouthonor.npcs.compat.SpellInfo;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.entity.mobs.goals.FindSupportableTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardSupportGoal;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Реализация моста ISS. Инстанцируется только при загруженном моде (Compat.ironsSpells()),
 * поэтому здесь можно свободно линковать ISS-типы.
 */
public final class IronsSpellsBridgeImpl implements IronsSpellsBridge {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean castSpell(CompanionEntity npc, String spellId, int spellLevel) {
        if (npc.level().isClientSide) {
            return false;
        }
        AbstractSpell spell = resolveSpell(spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            NpcCastDriver.warnUnknownSpell(spellId);
            return false;
        }
        IMagicEntity magicEntity = (IMagicEntity) npc;
        if (magicEntity.isCasting()) {
            NpcCastDriver.cancelCast(npc);
        }
        int lvl = Mth.clamp(spellLevel, spell.getMinLevel(), spell.getMaxLevel());
        NpcCastDriver.initiateCastSpell(npc, spell, lvl);
        // INSTANT тоже проходит через initiateCast (duration 0, сработает на ближайшем тике)
        return magicEntity.isCasting();
    }

    @Override
    public boolean castSpellAt(CompanionEntity npc, String spellId, int spellLevel, LivingEntity aimTarget) {
        if (npc.level().isClientSide) {
            return false;
        }
        // Отменяем текущий каст ДО подмены цели: его clearCastTransients может снять
        // сценарный прицел прошлого каста и сбросил бы только что поставленную цель.
        if (((IMagicEntity) npc).isCasting()) {
            NpcCastDriver.cancelCast(npc);
        }
        // Если aim уже был целью ДО действия (игрок сам заагрил NPC) — цель после каста не трогаем.
        boolean targetWasAim = npc.getTarget() == aimTarget;
        if (aimTarget != null && !targetWasAim) {
            npc.setTarget(aimTarget); // прицел для направленных заклинаний = собеседник
        }
        boolean started = castSpell(npc, spellId, spellLevel);
        if (aimTarget != null && !targetWasAim) {
            if (started) {
                // Пометка драйверу: по finishCast/cancelCast снять эту цель, если она ещё держится
                ((NpcCastState) npc).whSetScriptedCastAim(aimTarget);
            } else if (npc.getTarget() == aimTarget) {
                npc.setTarget(null); // каст не стартовал — не оставляем NPC в бою с собеседником
            }
        }
        return started;
    }

    @Override
    public void tick(CompanionEntity npc) {
        NpcCastDriver.tick(npc);
    }

    @Override
    public boolean isCastingNow(CompanionEntity npc) {
        return npc instanceof IMagicEntity magicEntity && magicEntity.isCasting();
    }

    @Override
    public void cancel(CompanionEntity npc) {
        NpcCastDriver.cancelCast(npc);
    }

    @Override
    public Goal buildMageGoal(CompanionEntity npc, CompanionProfile profile) {
        List<AbstractSpell> attack = new ArrayList<>();
        List<AbstractSpell> defense = new ArrayList<>();
        List<AbstractSpell> movement = new ArrayList<>();
        List<AbstractSpell> support = new ArrayList<>();
        for (CompanionProfile.MagicSpell ms : profile.getMagicSpells()) {
            AbstractSpell spell = resolveSpell(ms.id());
            if (spell == null || spell == SpellRegistry.none()) {
                NpcCastDriver.warnUnknownSpell(ms.id()); // спелл снятого аддона — пропускаем, лоадаут в профиле цел
                continue;
            }
            List<AbstractSpell> bucket = switch (ms.category()) {
                case "defense" -> defense;
                case "movement" -> movement;
                case "support", "support_heal", "support_buff" -> support;
                default -> attack;
            };
            // Вес 1..3 = столько же копий в пуле: родной механизм веса ISS (так настроен их Priest).
            for (int w = Math.max(1, ms.weight()); w > 0; w--) {
                bucket.add(spell);
            }
        }
        // Книжные/imbued спеллы: категория/вес берутся из book_overrides по id спелла (дефолт —
        // атака, вес 1), ручной лоадаут главнее. Спеллбук в Curios активен всегда (книга = включатель),
        // оружие — по галочке. Учитываем ДО проверки «лоадаут пуст», чтобы маг с одной книгой кастовал.
        for (BookSpell bs : resolveExtraSpells(npc, profile)) {
            List<AbstractSpell> bucket = switch (bs.category()) {
                case "defense" -> defense;
                case "movement" -> movement;
                case "support", "support_heal", "support_buff" -> support;
                default -> attack;
            };
            for (int w = Math.max(1, bs.weight()); w > 0; w--) {
                bucket.add(bs.spell());
            }
        }
        if (attack.isEmpty() && defense.isEmpty() && movement.isEmpty() && support.isEmpty()) {
            return null; // валидных спеллов нет — цель не создаём, вызывающий откатится к ближнему бою
        }
        int minTicks = Math.max(1, Math.round(profile.getMagicMinInterval() * 20.0F));
        int maxTicks = Math.max(minTicks, Math.round(profile.getMagicMaxInterval() * 20.0F));
        // Наш сабкласс WizardAttackGoal: анти-перекаст баффов, фильтр дальности, живой датчик снарядов.
        return new NpcWizardAttackGoal((IMagicEntity) npc, profile.getMeleeChaseSpeed(), minTicks, maxTicks)
                .setSpells(attack, defense, movement, support)
                .setSpellQuality(profile.getMagicMinQuality(), profile.getMagicMaxQuality())
                .setAllowFleeing(profile.isMagicFlee());
    }

    // Сырые типы намеренно: дженерик-баунды (Mob & SupportMob / PathfinderMob & IMagicEntity) даёт миксин
    // в рантайме, javac их на CompanionEntity не видит. В рантайме NPC реально реализует эти интерфейсы.
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Goal buildSupportFinderGoal(CompanionEntity npc) {
        // Порог здоровья тот же, что в WizardSupportGoal.canUse (0.9 от максимума): без него finder
        // цепляется за здорового союзника и держит Flag.TARGET, мешая агро-цели искать врагов.
        Predicate<LivingEntity> ally = e -> npc.whIsSupportAlly(e) && e.getHealth() < e.getMaxHealth() * 0.9F;
        return new FindSupportableTargetGoal(npc, LivingEntity.class, true, ally);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Goal buildSupportCastGoal(CompanionEntity npc, CompanionProfile profile) {
        // WizardSupportGoal держит два раздельных списка: лечение (когда союзник плох) и баффы
        // (когда терпимо). Раскладываем по категории; легаси "support" считаем лечением.
        List<AbstractSpell> healing = new ArrayList<>();
        List<AbstractSpell> buffs = new ArrayList<>();
        for (CompanionProfile.MagicSpell ms : profile.getMagicSpells()) {
            List<AbstractSpell> bucket = switch (ms.category()) {
                case "support_buff" -> buffs;
                case "support_heal", "support" -> healing;
                default -> null;
            };
            if (bucket == null) {
                continue;
            }
            AbstractSpell spell = resolveSpell(ms.id());
            if (spell != null && spell != SpellRegistry.none()) {
                // Вес 1..3 = столько же копий в пуле (родной механизм веса ISS)
                for (int w = Math.max(1, ms.weight()); w > 0; w--) {
                    bucket.add(spell);
                }
            }
        }
        // Книжные/imbued спеллы с support-оверрайдом тоже идут в поддержку союзников (раньше книжная
        // поддержка терялась целиком). Категория/вес — из book_overrides по id.
        for (BookSpell bs : resolveExtraSpells(npc, profile)) {
            List<AbstractSpell> bucket = switch (bs.category()) {
                case "support_buff" -> buffs;
                case "support_heal", "support" -> healing;
                default -> null;
            };
            if (bucket == null) {
                continue;
            }
            for (int w = Math.max(1, bs.weight()); w > 0; w--) {
                bucket.add(bs.spell());
            }
        }
        if (healing.isEmpty() && buffs.isEmpty()) {
            return null; // нет support-спеллов — цель поддержки не нужна
        }
        int minTicks = Math.max(1, Math.round(profile.getMagicMinInterval() * 20.0F));
        int maxTicks = Math.max(minTicks, Math.round(profile.getMagicMaxInterval() * 20.0F));
        // Наш сабкласс WizardSupportGoal: не перекастовывает бафф, уже висящий на союзнике.
        WizardSupportGoal goal = new NpcWizardSupportGoal(npc, profile.getMeleeChaseSpeed(), minTicks, maxTicks);
        goal.setSpells(healing, buffs);
        goal.setSpellQuality(profile.getMagicMinQuality(), profile.getMagicMaxQuality());
        return goal;
    }

    /** Книжный/imbued спелл с назначенными категорией и весом (из book_overrides либо дефолт). */
    private record BookSpell(AbstractSpell spell, String category, int weight) {
    }

    /**
     * Спеллы из спеллбука в Curios и (по галочке) с imbued-оружия, с применёнными оверрайдами
     * категории/веса по id. id, уже стоящие в ручном лоадауте, пропускаются (ручной главнее);
     * источники дедупятся между собой по id. Ключ оверрайда == getSpellId() == resource.toString().
     */
    private List<BookSpell> resolveExtraSpells(CompanionEntity npc, CompanionProfile profile) {
        List<AbstractSpell> raw = new ArrayList<>();
        if (profile.isMagicSpellsFromWeapon()) {
            raw.addAll(readContainerSpells(npc.getMainHandItem()));
        }
        raw.addAll(readEquippedSpellbookSpells(npc));
        if (raw.isEmpty()) {
            return List.of();
        }
        Set<String> manual = new HashSet<>();
        for (CompanionProfile.MagicSpell ms : profile.getMagicSpells()) {
            manual.add(ms.id());
        }
        List<BookSpell> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (AbstractSpell spell : raw) {
            if (spell == null || spell == SpellRegistry.none()) {
                continue;
            }
            String id = spell.getSpellId();
            if (manual.contains(id) || !seen.add(id)) {
                continue; // ручной лоадаут главнее; дубли между книгой/оружием отсекаем
            }
            CompanionProfile.MagicSpell ov = profile.bookOverride(id);
            out.add(new BookSpell(spell, ov != null ? ov.category() : "attack", ov != null ? ov.weight() : 1));
        }
        return out;
    }

    @Override
    public List<SpellInfo> listSpells() {
        List<SpellInfo> out = new ArrayList<>();
        for (AbstractSpell spell : SpellRegistry.getEnabledSpells()) {
            if (spell == null || spell == SpellRegistry.none()) {
                continue;
            }
            ResourceLocation id = spell.getSpellResource();
            String name = Component.translatable("spell." + id.getNamespace() + "." + id.getPath()).getString();
            org.joml.Vector3f c = spell.getSchoolType().getTargetingColor();
            int color = 0xFF000000
                    | (Mth.clamp((int) (c.x() * 255.0F), 0, 255) << 16)
                    | (Mth.clamp((int) (c.y() * 255.0F), 0, 255) << 8)
                    | Mth.clamp((int) (c.z() * 255.0F), 0, 255);
            out.add(new SpellInfo(id.toString(), name, spell.getSpellIconResource(), color));
        }
        out.sort(Comparator.comparing(SpellInfo::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** Общие атрибуты магии — в порядке показа. */
    private static final RegistryObject<?>[] GENERAL_ATTRS = {
            AttributeRegistry.SPELL_POWER,
            AttributeRegistry.SPELL_RESIST,
            AttributeRegistry.CAST_TIME_REDUCTION,
    };

    /** Школа → (сопротивление, сила). Школы аддонов сюда не попадают: их атрибуты именуются как угодно. */
    private static final Object[][] SCHOOL_ATTRS = {
            {SchoolRegistry.FIRE_RESOURCE, AttributeRegistry.FIRE_MAGIC_RESIST, AttributeRegistry.FIRE_SPELL_POWER},
            {SchoolRegistry.ICE_RESOURCE, AttributeRegistry.ICE_MAGIC_RESIST, AttributeRegistry.ICE_SPELL_POWER},
            {SchoolRegistry.LIGHTNING_RESOURCE, AttributeRegistry.LIGHTNING_MAGIC_RESIST,
                    AttributeRegistry.LIGHTNING_SPELL_POWER},
            {SchoolRegistry.HOLY_RESOURCE, AttributeRegistry.HOLY_MAGIC_RESIST, AttributeRegistry.HOLY_SPELL_POWER},
            {SchoolRegistry.ENDER_RESOURCE, AttributeRegistry.ENDER_MAGIC_RESIST, AttributeRegistry.ENDER_SPELL_POWER},
            {SchoolRegistry.BLOOD_RESOURCE, AttributeRegistry.BLOOD_MAGIC_RESIST, AttributeRegistry.BLOOD_SPELL_POWER},
            {SchoolRegistry.EVOCATION_RESOURCE, AttributeRegistry.EVOCATION_MAGIC_RESIST,
                    AttributeRegistry.EVOCATION_SPELL_POWER},
            {SchoolRegistry.NATURE_RESOURCE, AttributeRegistry.NATURE_MAGIC_RESIST,
                    AttributeRegistry.NATURE_SPELL_POWER},
            {SchoolRegistry.ELDRITCH_RESOURCE, AttributeRegistry.ELDRITCH_MAGIC_RESIST,
                    AttributeRegistry.ELDRITCH_SPELL_POWER},
    };

    @Override
    public List<MagicAttrInfo> listMagicAttributes() {
        List<MagicAttrInfo> out = new ArrayList<>();
        for (RegistryObject<?> ro : GENERAL_ATTRS) {
            out.add(new MagicAttrInfo(ro.getId().toString(), attrName(ro), "", 0, false));
        }
        for (Object[] row : SCHOOL_ATTRS) {
            SchoolType school = SchoolRegistry.getSchool((ResourceLocation) row[0]);
            if (school == null) {
                continue; // школа выключена конфигом ISS
            }
            String name = school.getDisplayName().getString();
            int color = argb(school.getTargetingColor());
            RegistryObject<?> resist = (RegistryObject<?>) row[1];
            RegistryObject<?> power = (RegistryObject<?>) row[2];
            out.add(new MagicAttrInfo(resist.getId().toString(), attrName(resist), name, color, false));
            out.add(new MagicAttrInfo(power.getId().toString(), attrName(power), name, color, true));
        }
        return out;
    }

    @Override
    public List<String> magicAttributeIds() {
        List<String> ids = new ArrayList<>();
        for (RegistryObject<?> ro : GENERAL_ATTRS) {
            ids.add(ro.getId().toString());
        }
        for (Object[] row : SCHOOL_ATTRS) {
            ids.add(((RegistryObject<?>) row[1]).getId().toString());
            ids.add(((RegistryObject<?>) row[2]).getId().toString());
        }
        return ids;
    }

    private static String attrName(RegistryObject<?> ro) {
        return Component.translatable(((Attribute) ro.get()).getDescriptionId()).getString();
    }

    private static int argb(org.joml.Vector3f c) {
        return 0xFF000000
                | (Mth.clamp((int) (c.x() * 255.0F), 0, 255) << 16)
                | (Mth.clamp((int) (c.y() * 255.0F), 0, 255) << 8)
                | Mth.clamp((int) (c.z() * 255.0F), 0, 255);
    }

    @Override
    public List<String> readEquippedSpellbookSpellIds(LivingEntity entity) {
        List<String> ids = new ArrayList<>();
        for (AbstractSpell sp : readEquippedSpellbookSpells(entity)) {
            ids.add(sp.getSpellId());
        }
        return ids;
    }

    /**
     * Заклинания из спеллбука в Curios-слоте "spellbook" сущности. ISS зависит от Curios жёстко,
     * но гейт curiosLoaded оставлен для симметрии с остальным compat-кодом. Идём через наш
     * Curios-мост — классы Curios не линкуются вне своего моста.
     */
    private static List<AbstractSpell> readEquippedSpellbookSpells(LivingEntity entity) {
        List<AbstractSpell> spells = new ArrayList<>();
        if (!com.withouthonor.npcs.compat.Compat.curiosLoaded()) {
            return spells;
        }
        for (com.withouthonor.npcs.compat.CuriosBridge.CurioSlotEntry e
                : com.withouthonor.npcs.compat.Compat.curios().getCurios(entity)) {
            if ("spellbook".equals(e.slotType())) {
                spells.addAll(readContainerSpells(e.stack()));
            }
        }
        return spells;
    }

    /** Заклинания из ISpellContainer предмета (спеллбук или imbued-оружие), кроме none(). */
    private static List<AbstractSpell> readContainerSpells(ItemStack stack) {
        List<AbstractSpell> spells = new ArrayList<>();
        if (stack == null || stack.isEmpty() || !ISpellContainer.isSpellContainer(stack)) {
            return spells;
        }
        ISpellContainer container = ISpellContainer.get(stack);
        if (container == null) {
            return spells;
        }
        for (SpellSlot slot : container.getAllSpells()) {
            // getAllSpells() отдаёт массив на ВСЕ слоты книги — пустые = null. Пропускаем.
            if (slot == null) {
                continue;
            }
            AbstractSpell sp = slot.getSpell();
            if (sp != null && sp != SpellRegistry.none()) {
                spells.add(sp);
            }
        }
        return spells;
    }

    /** "fireball" без неймспейса трактуем как irons_spellbooks:fireball. Учитываем, что
     *  ResourceLocationArgument в команде сам подставляет "minecraft:" — у ISS в этом
     *  неймспейсе спеллов не бывает, поэтому при промахе пробуем irons_spellbooks. */
    private static AbstractSpell resolveSpell(String spellId) {
        ResourceLocation id = ResourceLocation.tryParse(
                spellId.contains(":") ? spellId : "irons_spellbooks:" + spellId);
        if (id == null) {
            return null; // кривой id — сообщение отдаст вызывающий
        }
        AbstractSpell spell = SpellRegistry.getSpell(id);
        if ((spell == null || spell == SpellRegistry.none()) && "minecraft".equals(id.getNamespace())) {
            spell = SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", id.getPath()));
        }
        return spell;
    }
}
