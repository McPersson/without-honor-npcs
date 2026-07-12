package com.withouthonor.npcs.mixin;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.compat.ironsspells.NpcCastDriver;
import com.withouthonor.npcs.compat.ironsspells.NpcCastState;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.entity.mobs.SupportMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Инъекция интерфейса IMagicEntity (Iron's Spells 'n Spellbooks) в CompanionEntity.
 *
 * Применяется ТОЛЬКО при наличии ISS (гейт в WhMixinPlugin). Благодаря интерфейсу NPC
 * становится «родным» кастером для всей экосистемы ISS: клиент-синк каста
 * (SyncedSpellData.doSync → SyncEntityDataPacket идёт по instanceof IMagicEntity),
 * контр-заклинания, аддоны и т.п.
 *
 * Миксин намеренно тонкий: вся логика каста живёт в NpcCastDriver (compat-слой),
 * здесь только состояние (@Unique) и делегаты. NBT — под собственным ключом "WhMagic":
 * без ISS ключ игнорируется и отсыхает (см. план §3.1).
 */
@Mixin(CompanionEntity.class)
public abstract class IssCompanionMixin extends PathfinderMob implements IMagicEntity, SupportMob, NpcCastState {

    @Unique
    private MagicData wh$magicData;

    @Unique
    private boolean wh$hasUsedSingleAttack;

    @Unique
    private LivingEntity wh$supportTarget;

    /** Кэш спелла активного каста (NpcCastState) — живёт от initiate до finish/cancel. */
    @Unique
    private AbstractSpell wh$cachedCastSpell;

    /** Прицел сценарного каста (NpcCastState): цель, поставленная действием «Кастовать». */
    @Unique
    private LivingEntity wh$scriptedCastAim;

    protected IssCompanionMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Ленивая инициализация: конструктор CompanionEntity не трогаем. isMob=true — как у мобов ISS. */
    @Override
    public MagicData getMagicData() {
        if (wh$magicData == null) {
            wh$magicData = new MagicData(true);
            wh$magicData.setSyncedData(new SyncedSpellData(this));
        }
        return wh$magicData;
    }

    /**
     * Приём клиент-синка от ISS (SyncEntityDataPacket). Помимо записи данных воспроизводим
     * клиентский контракт ASCM по фронтам isCasting (своя реализация по наблюдаемому
     * поведению, сверено с v1.20.1-3.16.2):
     *  - спад (кастовал → перестал): castComplete у ASCM на клиенте сводится к
     *    resetCastingState (серверная ветка с onServerCastComplete тут недостижима) —
     *    делаем то же, чтобы не протухали castType/additionalCastData;
     *  - подъём (не кастовал → кастует) у INSTANT-спеллов: onClientPreCast — одноразовый
     *    клиентский визуал (туман blood step и т.п.). В отличие от ASCM состояние после
     *    INSTANT сразу НЕ сбрасываем: спад придёт синком с сервера, а наш слой анимаций
     *    каста (IronsSpellsClientImpl) читает isCasting из этих же данных.
     * Гард isClientSide заодно гарантирует, что серверные вызовы (если появятся) визуал
     * не сыграют; recreate каста после сейва — чисто серверный путь, сюда не заходит.
     */
    @Override
    public void setSyncedSpellData(SyncedSpellData syncedSpellData) {
        if (!level().isClientSide) {
            return;
        }
        MagicData magic = getMagicData();
        boolean wasCasting = magic.isCasting();
        magic.setSyncedData(syncedSpellData);
        boolean casting = magic.isCasting();
        if (!casting && wasCasting) {
            magic.resetCastingState();
        } else if (casting && !wasCasting) {
            AbstractSpell spell = SpellRegistry.getSpell(magic.getCastingSpellId());
            if (spell != null && spell != SpellRegistry.none() && spell.getCastType() == CastType.INSTANT) {
                spell.onClientPreCast(level(), magic.getCastingSpellLevel(), this, InteractionHand.MAIN_HAND, magic);
            }
        }
    }

    /** Не создаёт MagicData: иначе любой NPC обзаводится магсостоянием и тегом WhMagic в NBT. */
    @Override
    public boolean isCasting() {
        return wh$magicData != null && wh$magicData.isCasting();
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        NpcCastDriver.initiateCastSpell((CompanionEntity) (Object) this, spell, spellLevel);
    }

    @Override
    public void cancelCast() {
        NpcCastDriver.cancelCast((CompanionEntity) (Object) this);
    }

    @Override
    public void castComplete() {
        NpcCastDriver.castComplete((CompanionEntity) (Object) this);
    }

    @Override
    public void notifyDangerousProjectile(Projectile projectile) {
        // Фаза 0: уклонения/контр-реакции не реализуем
    }

    @Override
    public boolean setTeleportLocationBehindTarget(int distance) {
        return NpcCastDriver.setTeleportLocationBehindTarget((CompanionEntity) (Object) this, distance);
    }

    @Override
    public void setBurningDashDirectionData() {
        NpcCastDriver.setBurningDashDirectionData((CompanionEntity) (Object) this);
    }

    // getItemBySlot(EquipmentSlot) из IMagicEntity (deprecated) — уже унаследован от LivingEntity

    @Override
    public boolean isDrinkingPotion() {
        return false; // у NPC свой механизм зелий (potion-belt), ISS-питьё не используем
    }

    @Override
    public void startDrinkingPotion() {
        // no-op, см. isDrinkingPotion()
    }

    @Override
    public boolean getHasUsedSingleAttack() {
        return wh$hasUsedSingleAttack;
    }

    @Override
    public void setHasUsedSingleAttack(boolean bool) {
        this.wh$hasUsedSingleAttack = bool;
    }

    // SupportMob: цель поддержки (кого лечим/бафаем). Ставит FindSupportableTargetGoal, читает WizardSupportGoal.
    @Override
    public LivingEntity getSupportTarget() {
        return wh$supportTarget;
    }

    @Override
    public void setSupportTarget(LivingEntity target) {
        this.wh$supportTarget = target;
    }

    // NpcCastState: пер-NPC состояние статического NpcCastDriver (кэш спелла + сценарный прицел)
    @Override
    public AbstractSpell whGetCachedCastSpell() {
        return wh$cachedCastSpell;
    }

    @Override
    public void whSetCachedCastSpell(AbstractSpell spell) {
        this.wh$cachedCastSpell = spell;
    }

    @Override
    public LivingEntity whGetScriptedCastAim() {
        return wh$scriptedCastAim;
    }

    @Override
    public void whSetScriptedCastAim(LivingEntity aim) {
        this.wh$scriptedCastAim = aim;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void wh$saveMagic(CompoundTag tag, CallbackInfo ci) {
        if (wh$magicData == null) {
            return; // магия ни разу не трогалась — не мусорим в NBT
        }
        CompoundTag magic = new CompoundTag();
        wh$magicData.getSyncedData().saveNBTData(magic, level().registryAccess());
        tag.put("WhMagic", magic);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void wh$loadMagic(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("WhMagic")) {
            return;
        }
        // Грузим прямо в synced-данные, что держит getMagicData() — без лишнего throwaway-объекта.
        // Прерванный сейвом каст (isCasting без castType) NpcCastDriver.tick пересоздаёт с начала.
        getMagicData().getSyncedData().loadNBTData(tag.getCompound("WhMagic"), level().registryAccess());
    }
}
