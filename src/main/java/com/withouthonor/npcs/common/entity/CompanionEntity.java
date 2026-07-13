package com.withouthonor.npcs.common.entity;

import com.withouthonor.npcs.common.dialogue.DialogueRuntime;
import com.withouthonor.npcs.common.entity.ai.MobGroup;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.profile.PoseJson;
import com.withouthonor.npcs.common.storage.CompanionIndex;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

public class CompanionEntity extends PathfinderMob
        implements net.minecraft.world.item.trading.Merchant,
        net.minecraft.world.entity.monster.CrossbowAttackMob {

    private static final String TAG_PROFILE_ID = "ProfileId";
    private static final String TAG_FOLLOW = "FollowTarget";

    private static final EntityDataAccessor<String> DATA_SKIN_NAME =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DISGUISE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_TITLE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_SCHEDULE_EMOTE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_EMOTECRAFT_EMOTE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_IDLE_EMOTE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_COSMETIC_HEAD =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_COSMETIC_CHEST =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_COSMETIC_LEGS =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_COSMETIC_FEET =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_COSMETIC_MAINHAND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_COSMETIC_OFFHAND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final EntityDataAccessor<Boolean> DATA_HIDE_ARMOR =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_HIDE_MAINHAND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_HIDE_OFFHAND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_SITTING =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_PUSHABLE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_PASSABLE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_TOTEM_ARMED =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_EF_MODE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_ARROW =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final EntityDataAccessor<CompoundTag> DATA_TRANSFORM =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private static final EntityDataAccessor<CompoundTag> DATA_POSE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private static final float MAX_DISGUISE_SIZE = 4.0F;

    @Nullable
    private net.minecraft.world.entity.EntityDimensions disguiseDimensions;
    @Nullable
    private String disguiseDimensionsFor;

    @Nullable
    private UUID profileId;

    @Nullable
    private UUID followTargetId;
    private com.withouthonor.npcs.common.entity.ai.FollowMode followMode =
            com.withouthonor.npcs.common.entity.ai.FollowMode.NONE;

    @Nullable
    private net.minecraft.core.BlockPos followReturnPos;

    @Nullable
    private net.minecraft.nbt.ListTag pendingCurios;

    private boolean cfgTeleport = true;
    private boolean cfgTeleportOOS = true;
    private boolean cfgRun = true;
    private boolean cfgMatchSpeed = true;
    private boolean cfgOpenDoors = true;
    private boolean cfgAvoidDanger = true;
    private boolean cfgTeleportFx = true;
    private boolean cfgGroupSpacing = true;
    private int cfgDistanceTier = 1;
    private boolean cfgScheduleEnabled;
    private boolean cfgScheduleGlobal;
    private java.util.List<com.withouthonor.npcs.common.profile.ScheduleEntry> cfgSchedule = java.util.List.of();

    // Дефолт "cold" = поведение до профиля (раньше idle_look по умолчанию был включён).
    private String cfgLookMode = "cold";
    private int cfgLookRadius = 3;
    private boolean cfgBoatRide;
    private boolean cfgIdleWander;
    private int cfgIdleWanderRadius;
    private boolean cfgPanic;
    private boolean cfgPursueAttacker = true;
    // Провокация (кэш из профиля)
    private boolean cfgProvokeEnabled = true;
    private int cfgProvokeHits = 3;
    private int cfgProvokeHpPct = 15;
    private int cfgProvokeWindowTicks = 200;
    private boolean cfgProvokeIgnoreEscort = true;
    private int cfgForgiveAfterTicks = 400;
    /** Накопление случайных ударов игрока: удары, суммарный урон, тик последнего удара. */
    private static final class ProvokeEntry {
        int hits;
        float damage;
        int lastHitTick;
    }
    /** Транзиент, per-UUID, кламп 16 записей; чистка по окну лениво в hurt(). */
    private final java.util.Map<java.util.UUID, ProvokeEntry> provokeState = new java.util.HashMap<>();
    private int lastHurtByPlayerTick = -100000; // тик последнего удара игрока-цели — для прощения
    /** Игрок, взятый в цель ЗА УДАРЫ (порог провокации/реванш) — только такие цели прощаются.
     *  Цели из «кого атаковать»/природной вражды сюда не попадают и НЕ прощаются: 0.9.4b-регресс —
     *  прощение снимало никогда не бившую цель каждую секунду → рывки движения всех боевых пресетов. */
    @javax.annotation.Nullable
    private java.util.UUID forgivablePlayerUuid;
    /** Тик последнего фактического урона — метка для NpcPanicGoal. Транзиент. Паника завязана на неё,
     *  а не на lastHurtByMob: то поле читает и HurtByTargetGoal, поэтому провокация его чистит всегда. */
    private int recentlyPanicHurtTick = -100000;
    private boolean cfgEscortNoHarm = true; // #6: напарник не ранит сопровождаемого игрока
    // #3: эмоция перед смертью
    private String cfgDeathEmoteId = "";
    private String cfgDeathEmoteName = "";
    private String cfgDeathEmoteAuthor = "";
    private float cfgDeathEmoteSecs = 3.0F;
    private boolean deathStaged; // идёт инсценировка смерти (неуязвим, ИИ заморожен)
    private boolean deathStageFinished; // финальная смерть после сцены запущена (одноразовость)
    private boolean deathTailDone; // посмертный хвост (фразы/реакции/дропы/Graveyard) исполнен ровно раз
    private int deathStageTicks;
    @javax.annotation.Nullable
    private net.minecraft.world.damagesource.DamageSource deathStageSource; // снапшот убийцы
    private boolean cfgHoldPosition;
    private boolean cfgAvoidSun;
    private boolean cfgBurnInSun;
    private int cfgTotemCharges;
    private boolean cfgTotemRender;
    private int totemUsesLeft;
    private int appliedTotemCharges;
    private int cfgShieldHoldTicks = 30;
    private int cfgShieldCooldownTicks = 40;
    private net.minecraft.world.entity.MobType cfgMobType = net.minecraft.world.entity.MobType.UNDEFINED;
    // «Тип существа» для реакции чужих мобов (0.9.5 #4): читается WhCreatureAggroGoal. Ортогонален
    // cfgMobType (тот — про урон зачарований).
    private CreatureType cfgCreatureType = CreatureType.NEUTRAL;
    // Кастом-тип: группы-атакующие из профиля (применяются только при cfgCreatureType == CUSTOM).
    private java.util.Set<MobGroup> cfgCustomAttackers = java.util.Set.of();
    // «Природная вражда» по типу между NPC (нежить бьёт жителя, житель боится нежить). Дефолт вкл.
    private boolean cfgNaturalHostility = true;
    // Учтён ли этот NPC в CreatureAggroState (релевантен для агра/защиты); для симметричного дек/инкремента.
    private boolean aggroCounted;
    // Можно ли атаковать NPC (галка профиля). Неатакуемый — не «приманка» по типу: effectiveAttackers()
    // пуст (чужой моб не получает вечную неубиваемую цель) и в счётчик агра такой NPC не идёт.
    private boolean cfgAttackable = true;
    private boolean cfgFallDamage = true;
    private boolean cfgWebSlow = true;
    private boolean cfgCanDrown = true;
    private boolean cfgPoisonImmune;
    private int cfgAggroRange;
    private boolean cfgLeap;
    private String cfgAutoFollowMode = "none";
    private String cfgAutoFollowTarget = "";

    private java.util.List<net.minecraft.world.item.ItemStack> cfgPotionSelf = java.util.List.of();
    private java.util.List<net.minecraft.world.item.ItemStack> cfgPotionEnemy = java.util.List.of();
    private java.util.List<net.minecraft.world.item.ItemStack> cfgPotionAlly = java.util.List.of();
    private boolean cfgPotionSelfEnabled = true, cfgPotionSelfCombat;
    private boolean cfgPotionEnemyEnabled = true, cfgPotionAllyEnabled = true;
    private boolean cfgPotionEnemySeq, cfgPotionAllySeq;
    private int enemySeqIdx, allySeqIdx;
    private java.util.List<net.minecraft.world.item.ItemStack> cfgCombatBuff = java.util.List.of();
    private boolean cfgIsWitch;
    private boolean cfgSpareCreepers;
    private int potionCooldown;

    public CompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKIN_NAME, "");
        this.entityData.define(DATA_DISGUISE, "");
        this.entityData.define(DATA_TITLE, "");
        for (EntityDataAccessor<net.minecraft.world.item.ItemStack> accessor : COSMETIC_BY_SLOT.values()) {
            this.entityData.define(accessor, net.minecraft.world.item.ItemStack.EMPTY);
        }
        this.entityData.define(DATA_HIDE_ARMOR, false);
        this.entityData.define(DATA_HIDE_MAINHAND, false);
        this.entityData.define(DATA_HIDE_OFFHAND, false);
        this.entityData.define(DATA_SITTING, false);
        this.entityData.define(DATA_PUSHABLE, false);
        this.entityData.define(DATA_PASSABLE, false);
        this.entityData.define(DATA_TOTEM_ARMED, false);
        this.entityData.define(DATA_EF_MODE, false);
        this.entityData.define(DATA_ARROW, net.minecraft.world.item.ItemStack.EMPTY);
        this.entityData.define(DATA_TRANSFORM, new CompoundTag());
        this.entityData.define(DATA_POSE, new CompoundTag());
        this.entityData.define(DATA_SCHEDULE_EMOTE, "");
        this.entityData.define(DATA_EMOTECRAFT_EMOTE, "");
        this.entityData.define(DATA_IDLE_EMOTE, "");
    }

    private static final java.util.Map<net.minecraft.world.entity.EquipmentSlot,
            EntityDataAccessor<net.minecraft.world.item.ItemStack>> COSMETIC_BY_SLOT =
            new java.util.EnumMap<>(java.util.Map.of(
                    net.minecraft.world.entity.EquipmentSlot.HEAD, DATA_COSMETIC_HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, DATA_COSMETIC_CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS, DATA_COSMETIC_LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET, DATA_COSMETIC_FEET,
                    net.minecraft.world.entity.EquipmentSlot.MAINHAND, DATA_COSMETIC_MAINHAND,
                    net.minecraft.world.entity.EquipmentSlot.OFFHAND, DATA_COSMETIC_OFFHAND));

    public net.minecraft.world.item.ItemStack getCosmeticItem(net.minecraft.world.entity.EquipmentSlot slot) {
        return this.entityData.get(COSMETIC_BY_SLOT.get(slot));
    }

    public void setCosmeticItem(net.minecraft.world.entity.EquipmentSlot slot,
                                net.minecraft.world.item.ItemStack stack) {
        this.entityData.set(COSMETIC_BY_SLOT.get(slot), stack);
    }

    public boolean isHideArmor() {
        return this.entityData.get(DATA_HIDE_ARMOR);
    }

    public void setHideArmor(boolean hideArmor) {
        this.entityData.set(DATA_HIDE_ARMOR, hideArmor);
    }

    public boolean isHideMainhand() {
        return this.entityData.get(DATA_HIDE_MAINHAND);
    }

    public void setHideMainhand(boolean hide) {
        this.entityData.set(DATA_HIDE_MAINHAND, hide);
    }

    public boolean isHideOffhand() {
        return this.entityData.get(DATA_HIDE_OFFHAND);
    }

    public void setHideOffhand(boolean hide) {
        this.entityData.set(DATA_HIDE_OFFHAND, hide);
    }

    public net.minecraft.world.item.ItemStack getArrowItem() {
        return this.entityData.get(DATA_ARROW);
    }

    public void setArrowItem(net.minecraft.world.item.ItemStack stack) {
        this.entityData.set(DATA_ARROW, stack);
    }

    public net.minecraft.world.item.ItemStack getFunctionalItem(net.minecraft.world.entity.EquipmentSlot slot) {
        return super.getItemBySlot(slot);
    }

    @Override
    public net.minecraft.world.item.ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        if (level() != null && level().isClientSide()) {
            if (isHideArmor() && slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            if (isHideMainhand() && slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            if (isHideOffhand() && slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            net.minecraft.world.item.ItemStack cosmetic = getCosmeticItem(slot);
            if (!cosmetic.isEmpty()) {
                return cosmetic;
            }
        }
        return super.getItemBySlot(slot);
    }

    public String getTitle() {
        return this.entityData.get(DATA_TITLE);
    }

    public void setTitle(@Nullable String title) {
        this.entityData.set(DATA_TITLE, title == null ? "" : title);
    }

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN_NAME);
    }

    public void setSkinName(@Nullable String skinName) {
        this.entityData.set(DATA_SKIN_NAME, skinName == null ? "" : skinName);
    }

    public String getDisguise() {
        return this.entityData.get(DATA_DISGUISE);
    }

    public void setDisguise(@Nullable String disguise) {
        this.entityData.set(DATA_DISGUISE, disguise == null ? "" : disguise);
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_DISGUISE.equals(key)) {
            refreshDimensions();
        } else if (DATA_TRANSFORM.equals(key)) {
            decodeTransform();
        } else if (DATA_POSE.equals(key)) {
            decodePose();
            refreshDimensions();
        }
    }

    private void decodeTransform() {
        CompoundTag t = this.entityData.get(DATA_TRANSFORM);
        this.renderTransform = t.isEmpty() ? RenderTransform.IDENTITY : new RenderTransform(
                t.getFloat("rx"), t.getFloat("ry"), t.getFloat("rz"),
                t.getFloat("px"), t.getFloat("py"), t.getFloat("pz"),
                clampScale(t.getFloat("sx")), clampScale(t.getFloat("sy")), clampScale(t.getFloat("sz")));
    }

    private void decodePose() {
        CompoundTag t = this.entityData.get(DATA_POSE);
        PoseJson.Pose p = new PoseJson.Pose();
        if (!t.isEmpty()) {
            for (int i = 0; i < 18; i++) {
                p.angles[i] = t.getFloat("a" + i);
            }
            p.freeze = t.getBoolean("fz");
            for (int i = 0; i < 6; i++) {
                p.hidden[i] = t.getBoolean("h" + i);
            }
            p.bb[0] = t.getFloat("bw");
            p.bb[1] = t.getFloat("bh");
        }
        this.poseData = p;
    }

    private RenderTransform renderTransform = RenderTransform.IDENTITY;

    public RenderTransform getRenderTransform() {
        return renderTransform;
    }

    public void revertRenderTransformPreview() {
        decodeTransform();
        decodePose();
    }

    private PoseJson.Pose poseData = new PoseJson.Pose();

    private final float[] renderedPose = new float[18];
    private boolean poseRenderInit;
    // Отдельное состояние сглаживания позы для GUI-рендера (портрет диалога):
    // мир и GUI рендерят одну сущность с разными углами головы — общий лерп-массив
    // осциллировал между двумя целями (тряска + голова вбок). Клиент-only.
    private final float[] renderedPoseGui = new float[18];
    private boolean poseRenderInitGui;
    public static boolean GUI_POSE_CONTEXT;
    // GUI-превью вкладки «Поза» (редактор NPC): поворот трансформа применяется вокруг
    // ЦЕНТРА модели, чтобы модель не уезжала из рамки превью; позицию превью не применяет
    // само (передаёт нули). Ставится только вокруг dispatcher.render превью. Клиент-only.
    public static boolean GUI_PREVIEW_CONTEXT;

    private PoseJson.Pose cfgBasePose = new PoseJson.Pose();
    private float[] cfgBaseTransform = new float[]{0, 0, 0, 0, 0, 0, 1, 1, 1};

    public PoseJson.Pose getPoseData() {
        return poseData;
    }

    public float[] renderedPose() {
        return GUI_POSE_CONTEXT ? renderedPoseGui : renderedPose;
    }

    public boolean isPoseRenderInit() {
        return GUI_POSE_CONTEXT ? poseRenderInitGui : poseRenderInit;
    }

    public void setPoseRenderInit(boolean v) {
        if (GUI_POSE_CONTEXT) {
            this.poseRenderInitGui = v;
        } else {
            this.poseRenderInit = v;
        }
    }

    /** Сброс GUI-сглаживания позы при открытии портрета — иначе лерп стартует
     *  с устаревших углов прошлого диалога и голова «докручивается» на глазах. */
    public void resetGuiPoseLerp() {
        this.poseRenderInitGui = false;
    }

    public void setPoseClient(PoseJson.Pose pose) {
        PoseJson.Pose copy = new PoseJson.Pose();
        copy.copyFrom(pose);
        this.poseData = copy;
        refreshDimensions();
    }

    public void setPose(CompanionProfile profile) {
        setPoseData(profile.getPose());
    }

    public void setPoseData(PoseJson.Pose p) {
        if (p.isEmpty()) {
            this.entityData.set(DATA_POSE, new CompoundTag());
            return;
        }
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < 18; i++) {
            tag.putFloat("a" + i, p.angles[i]);
        }
        if (p.freeze) {
            tag.putBoolean("fz", true);
        }
        for (int i = 0; i < 6; i++) {
            if (p.hidden[i]) {
                tag.putBoolean("h" + i, true);
            }
        }
        if (p.bb[0] > 0F) {
            tag.putFloat("bw", p.bb[0]);
            tag.putFloat("bh", p.bb[1]);
        }
        this.entityData.set(DATA_POSE, tag);
    }

    public void applyScheduledVisuals(PoseJson.Pose pose, float[] transform9) {
        setPoseData(pose);
        if (transform9 != null && transform9.length == 9) {
            setRenderTransformValues(transform9[0], transform9[1], transform9[2],
                    transform9[3], transform9[4], transform9[5],
                    transform9[6], transform9[7], transform9[8]);
        }
    }

    public void revertScheduledVisuals() {
        setPoseData(cfgBasePose);
        setRenderTransformValues(cfgBaseTransform[0], cfgBaseTransform[1], cfgBaseTransform[2],
                cfgBaseTransform[3], cfgBaseTransform[4], cfgBaseTransform[5],
                cfgBaseTransform[6], cfgBaseTransform[7], cfgBaseTransform[8]);
    }

    public void setRenderTransformClient(float rotX, float rotY, float rotZ,
                                         float posX, float posY, float posZ,
                                         float scaleX, float scaleY, float scaleZ) {
        this.renderTransform = new RenderTransform(rotX, rotY, rotZ, posX, posY, posZ,
                clampScale(scaleX), clampScale(scaleY), clampScale(scaleZ));
    }

    public void setRenderTransform(CompanionProfile profile) {
        setRenderTransformValues(profile.getRotX(), profile.getRotY(), profile.getRotZ(),
                profile.getPosX(), profile.getPosY(), profile.getPosZ(),
                profile.getScaleX(), profile.getScaleY(), profile.getScaleZ());
    }

    public void setRenderTransformValues(float rx, float ry, float rz,
                                         float px, float py, float pz,
                                         float sx, float sy, float sz) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("rx", rx);
        tag.putFloat("ry", ry);
        tag.putFloat("rz", rz);
        tag.putFloat("px", px);
        tag.putFloat("py", py);
        tag.putFloat("pz", pz);
        tag.putFloat("sx", sx);
        tag.putFloat("sy", sy);
        tag.putFloat("sz", sz);
        this.entityData.set(DATA_TRANSFORM, tag);
    }

    private static float clampScale(float s) {
        return Math.max(0.1F, Math.min(4.0F, s));
    }

    @Override
    public float getNameTagOffsetY() {
        return super.getNameTagOffsetY() * renderTransform.scaleY();
    }

    public record RenderTransform(float rotX, float rotY, float rotZ,
                                  float posX, float posY, float posZ,
                                  float scaleX, float scaleY, float scaleZ) {
        public static final RenderTransform IDENTITY =
                new RenderTransform(0, 0, 0, 0, 0, 0, 1, 1, 1);

        public boolean hasRotation() {
            return rotX != 0 || rotY != 0 || rotZ != 0;
        }

        public boolean hasOffset() {
            return posX != 0 || posY != 0 || posZ != 0;
        }
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        String disguise = this.entityData == null ? "" : getDisguise();
        if (disguise.isEmpty()) {
            disguiseDimensionsFor = null;

            PoseJson.Pose pd = this.poseData;
            if (this.entityData != null && pd != null && pd.bb[0] > 0F) {
                return net.minecraft.world.entity.EntityDimensions.scalable(pd.bb[0], pd.bb[1]);
            }
            return super.getDimensions(pose);
        }
        if (!disguise.equals(disguiseDimensionsFor)) {
            disguiseDimensionsFor = disguise;
            disguiseDimensions = EntityType.byString(disguise)
                    .map(type -> {
                        net.minecraft.world.entity.EntityDimensions d = type.getDimensions();
                        return net.minecraft.world.entity.EntityDimensions.scalable(
                                Math.min(d.width, MAX_DISGUISE_SIZE), Math.min(d.height, MAX_DISGUISE_SIZE));
                    })
                    .orElse(null);
        }
        return disguiseDimensions != null ? disguiseDimensions : super.getDimensions(pose);
    }

    @Nullable
    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(@Nullable UUID profileId) {
        this.profileId = profileId;
    }

    /** Профиль создан спавнером только для этого NPC — удаляется вместе с ним. */
    private boolean transientProfile;

    public void setTransientProfile(boolean value) {
        this.transientProfile = value;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (profileId != null) {
            tag.putUUID(TAG_PROFILE_ID, profileId);
        }
        if (transientProfile) {
            tag.putBoolean("TransientProfile", true);
        }
        if (followTargetId != null
                && (followMode == com.withouthonor.npcs.common.entity.ai.FollowMode.FOLLOW
                || followMode == com.withouthonor.npcs.common.entity.ai.FollowMode.WAIT
                || followMode == com.withouthonor.npcs.common.entity.ai.FollowMode.RETURN)) {
            tag.putUUID(TAG_FOLLOW, followTargetId);
            tag.putByte("FollowMode", (byte) followMode.ordinal());
            if (followReturnPos != null) {
                tag.putIntArray("FollowReturn", new int[]{
                        followReturnPos.getX(), followReturnPos.getY(), followReturnPos.getZ()});
            }
        }

        CompoundTag cosmetics = new CompoundTag();
        for (var entry : COSMETIC_BY_SLOT.entrySet()) {
            net.minecraft.world.item.ItemStack stack = this.entityData.get(entry.getValue());
            if (!stack.isEmpty()) {
                cosmetics.put(entry.getKey().getName(), stack.save(new CompoundTag()));
            }
        }
        if (!cosmetics.isEmpty()) {
            tag.put("CosmeticItems", cosmetics);
        }
        if (isHideArmor()) {
            tag.putBoolean("HideArmor", true);
        }
        if (isHideMainhand()) {
            tag.putBoolean("HideMainhand", true);
        }
        if (isHideOffhand()) {
            tag.putBoolean("HideOffhand", true);
        }
        if (!getArrowItem().isEmpty()) {
            tag.put("ArrowItem", getArrowItem().save(new CompoundTag()));
        }
        if (cfgTotemCharges > 0) {
            tag.putInt("TotemUsesLeft", totemUsesLeft);
            tag.putInt("TotemCharges", cfgTotemCharges);
        }

        if (com.withouthonor.npcs.compat.Compat.curiosLoaded()) {
            net.minecraft.nbt.ListTag curios = new net.minecraft.nbt.ListTag();
            for (var e : com.withouthonor.npcs.compat.Compat.curios().getCurios(this)) {
                if (e.stack().isEmpty()) {
                    continue;
                }
                CompoundTag c = new CompoundTag();
                c.putString("slot", e.slotType());
                c.putInt("idx", e.index());
                c.put("item", e.stack().save(new CompoundTag()));
                curios.add(c);
            }
            if (!curios.isEmpty()) {
                tag.put("WhCurios", curios);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_PROFILE_ID)) {
            profileId = tag.getUUID(TAG_PROFILE_ID);
        }
        transientProfile = tag.getBoolean("TransientProfile");
        if (tag.hasUUID(TAG_FOLLOW)) {
            followTargetId = tag.getUUID(TAG_FOLLOW);
            com.withouthonor.npcs.common.entity.ai.FollowMode[] modes =
                    com.withouthonor.npcs.common.entity.ai.FollowMode.values();
            int m = tag.getByte("FollowMode");
            followMode = m > 0 && m < modes.length ? modes[m]
                    : com.withouthonor.npcs.common.entity.ai.FollowMode.FOLLOW;
            int[] r = tag.getIntArray("FollowReturn");
            followReturnPos = r.length == 3 ? new net.minecraft.core.BlockPos(r[0], r[1], r[2]) : null;
        } else {
            followTargetId = null;
            followMode = com.withouthonor.npcs.common.entity.ai.FollowMode.NONE;
        }
        CompoundTag cosmetics = tag.getCompound("CosmeticItems");
        for (var entry : COSMETIC_BY_SLOT.entrySet()) {
            String key = entry.getKey().getName();
            this.entityData.set(entry.getValue(), cosmetics.contains(key)
                    ? net.minecraft.world.item.ItemStack.of(cosmetics.getCompound(key))
                    : net.minecraft.world.item.ItemStack.EMPTY);
        }
        setHideArmor(tag.getBoolean("HideArmor"));
        setHideMainhand(tag.getBoolean("HideMainhand"));
        setHideOffhand(tag.getBoolean("HideOffhand"));
        setArrowItem(tag.contains("ArrowItem")
                ? net.minecraft.world.item.ItemStack.of(tag.getCompound("ArrowItem"))
                : net.minecraft.world.item.ItemStack.EMPTY);
        if (tag.contains("TotemUsesLeft")) {
            totemUsesLeft = tag.getInt("TotemUsesLeft");
            appliedTotemCharges = tag.getInt("TotemCharges");
        }

        pendingCurios = tag.contains("WhCurios")
                ? tag.getList("WhCurios", net.minecraft.nbt.Tag.TAG_COMPOUND) : null;
    }

    public void reapplyCuriosOnLoad() {
        net.minecraft.nbt.ListTag pending = pendingCurios;
        pendingCurios = null;
        if (!com.withouthonor.npcs.compat.Compat.curiosLoaded() || pending == null || pending.isEmpty()) {
            return;
        }

        com.withouthonor.npcs.compat.Compat.curios().resetCurios(this);
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag c = pending.getCompound(i);
            net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.of(c.getCompound("item"));
            if (!stack.isEmpty()) {
                com.withouthonor.npcs.compat.Compat.curios()
                        .setCurio(this, c.getString("slot"), c.getInt("idx"), stack);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        if (super.isInvulnerableTo(source)) {
            return true;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (!level().isClientSide) {
            if (deathStaged) {
                return true; // идёт инсценировка смерти — неуязвим (кроме bypass выше)
            }
            CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
            return profile == null || !profile.isAttackable();
        }
        return false;
    }

    public void applyCombatProfile(CompanionProfile profile) {
        this.entityData.set(DATA_EF_MODE, "epicfight".equals(profile.getCombatSystem()));
        setAttr(Attributes.MAX_HEALTH, profile.getMaxHealth());
        setAttr(Attributes.ATTACK_DAMAGE, profile.getAttackDamage());
        setAttr(Attributes.ARMOR, profile.getArmor());
        setAttr(Attributes.KNOCKBACK_RESISTANCE, profile.getKbResistance());
        setAttr(Attributes.MOVEMENT_SPEED, profile.isHoldPosition() ? 0.0D : profile.getMoveSpeed());
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
        applyMagicAttributes(profile);
        rebuildCombatGoals(profile);
    }

    /**
     * Атрибуты Iron's Spells (школьные резисты/силы + общие). Резолвим по строковому id через
     * ForgeRegistries — классы ISS сюда не тянем (план §3.1). Проставляем ВСЕ известные атрибуты,
     * а не только заданные: иначе снятое в редакторе значение осталось бы висеть на сущности.
     */
    private void applyMagicAttributes(CompanionProfile profile) {
        if (!com.withouthonor.npcs.compat.Compat.ironsSpellsLoaded()) {
            return;
        }
        for (String id : com.withouthonor.npcs.compat.Compat.ironsSpells().magicAttributeIds()) {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
            net.minecraft.world.entity.ai.attributes.Attribute attr = rl == null
                    ? null
                    : net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(rl);
            if (attr == null) {
                com.withouthonor.npcs.WHCompanions.LOGGER.warn("[WH ISS] Атрибут '{}' не найден в реестре", id);
                continue;
            }
            var instance = getAttribute(attr);
            if (instance != null) {
                instance.setBaseValue(profile.getMagicAttr(id));
            }
        }
    }

    private void rebuildCombatGoals(CompanionProfile profile) {
        if (level().isClientSide) {
            return;
        }
        if (com.withouthonor.npcs.compat.Compat.ironsSpellsLoaded()) {
            // Цели пересобираются (профиль сохранён/синкнут) — активный каст осиротел, гасим его.
            com.withouthonor.npcs.compat.Compat.ironsSpells().cancel(this);
        }
        // Смена профиля = новый боевой контекст: протухшая метка «прощаемой» цели не должна
        // пережить пересборку целей (иначе прощение снимало бы цель, взятую уже по новым правилам).
        forgivablePlayerUuid = null;
        cacheFollowSettings(profile);

        // followRange у дальнобойных пресетов ≥ дистанции атаки + запас: иначе TargetGoal
        // сбрасывал бы цель прямо на рабочей дистанции выстрела/каста/броска.
        double followRange = cfgAggroRange > 0 ? cfgAggroRange : 16.0D;
        if ("bow".equals(profile.getCombatPreset())) {
            followRange = Math.max(followRange, profile.getRangedRange() + 8.0F);
        } else if ("mage".equals(profile.getCombatPreset())) {
            // spellcastingRange ISS = 20 (захардкожен в WizardAttackGoal, его НЕ поднимаем — баланс ISS).
            followRange = Math.max(followRange, 28.0D);
        } else if ("potion".equals(profile.getCombatPreset())) {
            followRange = Math.max(followRange, profile.getPotionRange() + 8.0F);
        }
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(followRange);

        this.goalSelector.getAvailableGoals().forEach(net.minecraft.world.entity.ai.goal.WrappedGoal::stop);
        this.targetSelector.getAvailableGoals().forEach(net.minecraft.world.entity.ai.goal.WrappedGoal::stop);
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        registerGoals();

        String preset = profile.getCombatPreset();
        if ("passive".equals(preset)) {
            setTarget(null);
            return;
        }

        this.targetSelector.addGoal(1,
                new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));

        boolean ef = com.withouthonor.npcs.compat.Compat.epicFightLoaded()
                && "epicfight".equals(profile.getCombatSystem());
        if (!ef) {
        if ("bow".equals(preset)) {
            int interval = Math.max(5, Math.round(profile.getRangedIntervalSeconds() * 20.0F));
            float range = profile.getRangedRange();
            net.minecraft.world.item.Item weapon =
                    getFunctionalItem(net.minecraft.world.entity.EquipmentSlot.MAINHAND).getItem();
            net.minecraft.world.item.Item ammo = getArrowItem().getItem();
            boolean throwableAmmo = ammo instanceof net.minecraft.world.item.SnowballItem
                    || ammo instanceof net.minecraft.world.item.EggItem;
            if (weapon instanceof net.minecraft.world.item.CrossbowItem) {
                this.goalSelector.addGoal(2, new com.withouthonor.npcs.common.entity.ai.NpcCrossbowAttackGoal(
                        this, 1.0D, range));
            } else if (weapon instanceof net.minecraft.world.item.BowItem) {
                this.goalSelector.addGoal(2, new com.withouthonor.npcs.common.entity.ai.NpcBowAttackGoal(
                        this, 1.0D, interval, range));
            } else if (isThrowableSpear(getFunctionalItem(net.minecraft.world.entity.EquipmentSlot.MAINHAND))) {
                this.goalSelector.addGoal(2, new com.withouthonor.npcs.common.entity.ai.NpcTridentAttackGoal(
                        this, 1.0D, interval, range));
                // Фолбэк: если модовое копьё не метается (кэш) → цель отвалится, добиваем в melee (прио 3).
                // false — как ваниль-зомби (см. addMeleeGoals).
                this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(
                        this, profile.getMeleeChaseSpeed(), false));
            } else if (throwableAmmo) {
                this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.RangedAttackGoal(
                        this, 1.0D, interval, range));
            } else {
                addMeleeGoals(profile);
            }
        } else if ("potion".equals(preset)) {
            int pInterval = Math.max(5, Math.round(profile.getPotionIntervalSeconds() * 20.0F));
            float pRange = profile.getPotionRange();
            this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.RangedAttackGoal(
                    this, 1.0D, pInterval, pRange));

            if (cfgPotionAllyEnabled) {
                this.goalSelector.addGoal(3,
                        new com.withouthonor.npcs.common.entity.ai.WitchHealGoal(this));
            }
        } else if ("mage".equals(preset)) {
            net.minecraft.world.entity.ai.goal.Goal mageGoal =
                    com.withouthonor.npcs.compat.Compat.ironsSpellsLoaded()
                            ? com.withouthonor.npcs.compat.Compat.ironsSpells().buildMageGoal(this, profile)
                            : null;
            if (mageGoal != null) {
                this.goalSelector.addGoal(2, mageGoal);
                // Melee прио 3 — страховка на случай, когда каст-гол выбыл (WizardAttackGoal.canUse
                // при живой цели истинен всегда, так что обычно melee НЕ стартует — как у родных
                // ISS-магов; сработает лишь при смерти/подмене цели внутри ISS-гола).
                // false — как ваниль-зомби (см. addMeleeGoals).
                this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(
                        this, profile.getMeleeChaseSpeed(), false));
            } else {
                // Без ISS или пустой/невалидный лоадаут — откат к ближнему бою, NPC не беспомощен (план §3.1).
                if (com.withouthonor.npcs.compat.Compat.ironsSpellsLoaded()) {
                    com.withouthonor.npcs.WHCompanions.LOGGER.warn(
                            "[WH ISS] Пресет «Маг» без валидных спеллов — откат к ближнему бою");
                }
                addMeleeGoals(profile);
            }
            // Поддержка союзников: отдельная связка ISS (SupportMob + искатель цели + WizardSupportGoal).
            // Лечит/бафает своих (фракция/хозяин), приоритет выше атаки. shouldHealEntity-миксин — для AoE-хилов.
            if (com.withouthonor.npcs.compat.Compat.ironsSpellsLoaded() && profile.isSupportAllies()) {
                net.minecraft.world.entity.ai.goal.Goal supGoal =
                        com.withouthonor.npcs.compat.Compat.ironsSpells().buildSupportCastGoal(this, profile);
                if (supGoal != null) {
                    this.goalSelector.addGoal(1, supGoal);
                    // Приоритет 4 (хуже агро-целей 2/3): FindSupportableTargetGoal — это TargetGoal с
                    // Flag.TARGET, на равном приоритете он не даёт агро-цели запуститься.
                    this.targetSelector.addGoal(4,
                            com.withouthonor.npcs.compat.Compat.ironsSpells().buildSupportFinderGoal(this));
                }
            }
        } else {
            addMeleeGoals(profile);
        }

        if ("shield".equals(preset)) {
            this.goalSelector.addGoal(1, new com.withouthonor.npcs.common.entity.ai.ShieldBlockGoal(
                    this, cfgShieldHoldTicks, cfgShieldCooldownTicks));
        }
        }

        java.util.Set<String> cats = CompanionProfile.parseAggressorTargets(profile.getAggressorTargets());
        if (cats.contains("monsters") || cats.contains("animals") || cats.contains("villagers")
                || cats.contains("npcs") || cats.contains("players")) {
            this.targetSelector.addGoal(2,
                    new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                            this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                            e -> matchesAggressorCategory(e, cats)));
        }
        if (cats.contains("factions")) {

            this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                    this::isHostileFactionTarget));
        }
        // #4 Природная вражда: атакуем NPC вражеских типов (нежить→житель и т.п.). Только при боевом
        // пресете (passive вышел выше), поверх фракций/агрессоров, не трогая агро против ваниль-мобов.
        // Приоритет 5 — ниже явных агрессоров (2), фракций (3) И поиска цели для хила союзника (4):
        // осознанные настройки и роль поддержки главнее природного типа. Детерминированно: у мага-
        // поддержки вражда стартует, лишь когда некого лечить (support-finder не держит Flag.TARGET).
        if (cfgNaturalHostility && !cfgCreatureType.npcEnemies().isEmpty()) {
            this.targetSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                    this::isNaturalTypeEnemy));
        }

        if (ef) {
            com.withouthonor.npcs.compat.epicfight.EpicFightCompat.installMobCombat(this);
        }
    }

    private void addMeleeGoals(CompanionProfile profile) {
        if (cfgLeap) {
            this.goalSelector.addGoal(2,
                    new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(this, profile.getLeapStrength()));
        }
        // followingTargetEvenIfNotSeen=false — как ваниль-зомби: цель гаснет в конце пути и тут же
        // перезапускается со СВЕЖИМ путём. С true NPC застревал на краю дистанции удара у неподвижной
        // цели (tick не перепрокладывает путь, если цель не сдвинулась) — «стоит вплотную и не бьёт».
        // «Челнок» к игроку это не возвращает: follow-цели гардятся по getTarget() и MOVE в бою не берут.
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(
                this, profile.getMeleeChaseSpeed(), false));
    }

    /** Природный враг по типу: другой NPC, чей тип входит в мой npcEnemies (нежить бьёт жителя).
     *  Союзники по фракции исключены — их в одну фракцию собрали намеренно, тип это не переписывает. */
    private boolean isNaturalTypeEnemy(net.minecraft.world.entity.LivingEntity target) {
        return target instanceof CompanionEntity oc && oc != this
                && cfgCreatureType.npcEnemies().contains(oc.getCreatureType())
                && !isSameFaction(oc);
    }

    private boolean isHostileFactionTarget(net.minecraft.world.entity.LivingEntity target) {
        if (target instanceof CompanionEntity) {
            return isHostileNpc(target);
        }
        if (target instanceof Player player) {
            return isHostileFactionPlayer(player);
        }
        return false;
    }

    private boolean isHostileFactionPlayer(Player player) {
        if (profileId == null) {
            return false;
        }
        CompanionProfile mine = ProfileManager.get().get(profileId);
        if (mine == null || mine.getFaction() == null) {
            return false;
        }
        var faction = com.withouthonor.npcs.common.reputation.FactionRegistry.get()
                .byId(mine.getFaction());
        if (faction == null) {
            return false;
        }
        var server = level().getServer();
        if (server == null) {
            return false;
        }
        int rep = com.withouthonor.npcs.common.storage.PlayerStateManager.get(server)
                .getReputation(player.getUUID(), faction.getId());
        return faction.isHostileValue(rep);
    }

    private boolean matchesAggressorCategory(net.minecraft.world.entity.LivingEntity e, java.util.Set<String> cats) {
        if (e == this) {
            return false;
        }
        if (e instanceof Player) {
            return cats.contains("players");
        }
        if (e instanceof CompanionEntity) {
            return cats.contains("npcs");
        }
        if (e instanceof net.minecraft.world.entity.npc.AbstractVillager) {
            return cats.contains("villagers");
        }
        if (e instanceof net.minecraft.world.entity.animal.Animal) {
            return cats.contains("animals");
        }
        if (e instanceof net.minecraft.world.entity.monster.Monster) {
            if (cfgSpareCreepers && e instanceof net.minecraft.world.entity.monster.Creeper) {
                return false;
            }
            return cats.contains("monsters");
        }
        return false;
    }

    public boolean isConfiguredAggressorTarget(net.minecraft.world.entity.LivingEntity e) {
        CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
        if (profile == null) {
            return false;
        }
        java.util.Set<String> cats = CompanionProfile.parseAggressorTargets(profile.getAggressorTargets());
        if (matchesAggressorCategory(e, cats)) {
            return true;
        }
        return cats.contains("factions") && isHostileFactionTarget(e);
    }

    private boolean isHostileNpc(net.minecraft.world.entity.LivingEntity target) {
        if (!(target instanceof CompanionEntity other) || profileId == null || other.getProfileId() == null) {
            return false;
        }
        CompanionProfile mine = ProfileManager.get().get(profileId);
        CompanionProfile theirs = ProfileManager.get().get(other.getProfileId());
        if (mine == null || theirs == null || mine.getFaction() == null || theirs.getFaction() == null) {
            return false;
        }
        var faction = com.withouthonor.npcs.common.reputation.FactionRegistry.get()
                .byId(mine.getFaction());
        return faction != null && faction.getHostileTo().contains(theirs.getFaction());
    }

    public boolean isSameFaction(CompanionEntity other) {
        if (profileId == null || other.getProfileId() == null) {
            return false;
        }
        CompanionProfile mine = ProfileManager.get().get(profileId);
        CompanionProfile theirs = ProfileManager.get().get(other.getProfileId());
        return mine != null && theirs != null && mine.getFaction() != null
                && mine.getFaction().equals(theirs.getFaction());
    }

    public boolean hasRangedWeapon() {
        return getFunctionalItem(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem;
    }

    // --- Метаемые копья (ваниль-трезубец + модовые через FakePlayer) ---

    /** Датапак-тег для ручного расширения списка метаемых копий (по умолчанию пуст). */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> THROWABLE_SPEARS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wh_npcs", "throwable_spears"));

    /** Фиктивный профиль FakePlayer, от лица которого модовое копьё вызывает releaseUsing. */
    private static final com.mojang.authlib.GameProfile WH_SPEAR_PROFILE = new com.mojang.authlib.GameProfile(
            java.util.UUID.fromString("b5c1e0a2-6f7d-4c3a-9e10-77a5c0de5000"), "[wh_npcs_spear]");

    /** Item'ы, у которых модовый бросок не сработал в рантайме — больше не метаем (транзиент, identity). */
    private final java.util.Set<net.minecraft.world.item.Item> nonThrowableSpears =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Ваниль-тег (command tag) наших метательных снарядов — по нему секундный скан находит «зависшие» возвратные. */
    private static final String THROWN_PROJECTILE_TAG = "wh_npc_thrown";

    /** Транзиент: NPC хотя бы раз метал снаряд — без этого секундный скан возвратных снарядов не делаем. */
    private boolean hasThrownProjectiles;

    /** Метаемое копьё: ваниль-трезубец, датапак-тег, forge:tools/tridents, либо SPEAR-анимация с заметным зарядом (не лук/арбалет). */
    public static boolean isThrowableSpear(net.minecraft.world.item.ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        if (s.getItem() instanceof net.minecraft.world.item.TridentItem || s.is(THROWABLE_SPEARS)
                || s.is(net.minecraftforge.common.Tags.Items.TOOLS_TRIDENTS)) { // модовые трезубцы, помеченные конвенцией Forge
            return true;
        }
        return s.getUseAnimation() == net.minecraft.world.item.UseAnim.SPEAR
                && s.getUseDuration() >= 20
                && !(s.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem);
    }

    /** isThrowableSpear с учётом рантайм-кэша неудач — метать это копьё или уйти в melee. */
    public boolean canThrowSpear(net.minecraft.world.item.ItemStack s) {
        return isThrowableSpear(s) && !nonThrowableSpears.contains(s.getItem());
    }

    /**
     * Бросок модового копья через FakePlayer: releaseUsing мода спавнит ИХ снаряд с ИХ уроном/эффектами.
     * После спавна перевешиваем owner на NPC (kill-credit + friendly-fire гейты). Не сработало → кэш.
     */
    private void throwModdedSpear(net.minecraft.world.entity.LivingEntity target,
                                  net.minecraft.world.item.ItemStack weapon, double dx, double dz, double horizontal) {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        net.minecraftforge.common.util.FakePlayer fake =
                net.minecraftforge.common.util.FakePlayerFactory.get(serverLevel, WH_SPEAR_PROFILE);
        fake.setPos(getX(), getEyeY() - fake.getEyeHeight(), getZ());
        double dy = target.getEyeY() - getEyeY();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontal) * (180.0D / Math.PI)));
        // Spartan и др. читают и СТАРЫЕ углы — выставляем оба.
        fake.setYRot(yaw);
        fake.yRotO = yaw;
        fake.yHeadRot = yaw;
        fake.setXRot(pitch);
        fake.xRotO = pitch;
        net.minecraft.world.item.ItemStack copy = weapon.copy(); // копия: NPC оружие не теряет, мутации мода уходят с копией
        fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, copy);
        int timeLeft = Math.max(0, copy.getUseDuration() - 20); // 20 тиков заряда — покрывают пороги обследованных модов
        try {
            try {
                copy.getItem().releaseUsing(copy, serverLevel, fake, timeLeft);
            } catch (Throwable t) {
                com.withouthonor.npcs.WHCompanions.LOGGER.warn(
                        "[WH] Модовое копьё '{}' бросилось с ошибкой, отключаю метание: {}",
                        weapon.getItem(), t.toString());
                nonThrowableSpears.add(weapon.getItem());
                return;
            }
            java.util.List<net.minecraft.world.entity.projectile.Projectile> spawned =
                    serverLevel.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,
                            fake.getBoundingBox().inflate(2.0D), p -> p.tickCount == 0 && p.getOwner() == fake);
            if (spawned.isEmpty()) {
                nonThrowableSpears.add(weapon.getItem()); // мод не заспавнил снаряд — больше не пытаемся, уходим в melee
                return;
            }
            for (net.minecraft.world.entity.projectile.Projectile p : spawned) {
                p.setOwner(this); // kill-credit NPC + наши friendly-fire гейты (#6)
                p.addTag(THROWN_PROJECTILE_TAG); // метка для скана возвратных: у не-игрока снаряд подобрать некому
                if (p instanceof net.minecraft.world.entity.projectile.AbstractArrow aa) {
                    aa.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
                }
            }
            hasThrownProjectiles = true;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        } finally {
            // FakePlayer кэшируется до выгрузки мира — копия копья не должна висеть у него в руке.
            fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    @Override
    public void performRangedAttack(net.minecraft.world.entity.LivingEntity target, float power) {
        if (cfgIsWitch) {
            throwSplashAt(target, harmfulBeltPotion());
            return;
        }
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        net.minecraft.world.item.ItemStack weapon =
                getFunctionalItem(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        if (weapon.getItem() instanceof net.minecraft.world.item.TridentItem) {
            var trident = new net.minecraft.world.entity.projectile.ThrownTrident(level(), this, weapon.copy());
            trident.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            trident.addTag(THROWN_PROJECTILE_TAG); // Loyalty вернёт его к NPC, а «всасывание» есть только в playerTouch
            hasThrownProjectiles = true;
            double dy = target.getY(0.3333333333333333D) - trident.getY();
            trident.shoot(dx, dy + horizontal * 0.2D, dz, 1.6F, 4.0F);
            playSound(net.minecraft.sounds.SoundEvents.DROWNED_SHOOT, 1.0F,
                    1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
            level().addFreshEntity(trident);
            return;
        }
        if (canThrowSpear(weapon)) {
            throwModdedSpear(target, weapon, dx, dz, horizontal);
            return;
        }
        net.minecraft.world.item.ItemStack ammo = getArrowItem();
        if (ammo.getItem() instanceof net.minecraft.world.item.SnowballItem) {
            var snowball = new net.minecraft.world.entity.projectile.Snowball(level(), this);
            double dy = target.getY(0.3333333333333333D) - snowball.getY();
            snowball.shoot(dx, dy + horizontal * 0.13D, dz, 1.6F, 6.0F);
            playSound(net.minecraft.sounds.SoundEvents.SNOWBALL_THROW, 1.0F,
                    1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
            level().addFreshEntity(snowball);
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            return;
        }
        if (ammo.getItem() instanceof net.minecraft.world.item.EggItem) {
            var egg = new net.minecraft.world.entity.projectile.ThrownEgg(level(), this);
            egg.setItem(ammo);
            double dy = target.getY(0.3333333333333333D) - egg.getY();
            egg.shoot(dx, dy + horizontal * 0.13D, dz, 1.6F, 6.0F);
            playSound(net.minecraft.sounds.SoundEvents.EGG_THROW, 1.0F,
                    1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
            level().addFreshEntity(egg);
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            return;
        }
        net.minecraft.world.item.ItemStack arrowStack =
                ammo.getItem() instanceof net.minecraft.world.item.ArrowItem
                        ? ammo
                        : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW);
        var arrow = net.minecraft.world.entity.projectile.ProjectileUtil.getMobArrow(this, arrowStack, power);
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        arrow.shoot(dx, dy + horizontal * 0.2D, dz, 1.6F, 4.0F);
        playSound(net.minecraft.sounds.SoundEvents.SKELETON_SHOOT, 1.0F,
                1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }

    private boolean chargingCrossbow;

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.chargingCrossbow = charging;
    }

    public boolean isChargingCrossbow() {
        return this.chargingCrossbow;
    }

    @Override
    public void shootCrossbowProjectile(net.minecraft.world.entity.LivingEntity target,
                                        net.minecraft.world.item.ItemStack weapon,
                                        net.minecraft.world.entity.projectile.Projectile projectile,
                                        float projectileAngle) {
        net.minecraft.world.entity.LivingEntity aim = target != null ? target : getTarget();
        if (aim != null) {
            this.shootCrossbowProjectile(this, aim, projectile, projectileAngle, 1.6F);
            return;
        }
        net.minecraft.world.phys.Vec3 look = getViewVector(1.0F);
        projectile.shoot(look.x, look.y, look.z, 1.6F, 1.0F);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public net.minecraft.world.item.ItemStack getProjectile(net.minecraft.world.item.ItemStack weapon) {
        if (!(weapon.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem pw)) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        net.minecraft.world.item.ItemStack held = net.minecraft.world.item.ProjectileWeaponItem
                .getHeldProjectile(this, pw.getSupportedHeldProjectiles());
        if (!held.isEmpty()) {
            return held;
        }
        net.minecraft.world.item.ItemStack arrow = getArrowItem();
        if (arrow.getItem() instanceof net.minecraft.world.item.ArrowItem) {
            return arrow.copy();
        }
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW);
    }

    private void setAttr(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        var instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // Буфер Curios, снятый в die() ДО super.die(): иначе Curios API роняет их на LivingDropsEvent
    // и чистит слоты, а снимок для Graveyard читал бы уже пустые. Живёт лишь на время смерти.
    private net.minecraft.nbt.ListTag deathCuriosSnapshot;

    /** Текущее содержимое Curios-слотов в NBT-виде секции "Curios" снапшота. */
    private net.minecraft.nbt.ListTag curiosToTag() {
        net.minecraft.nbt.ListTag curios = new net.minecraft.nbt.ListTag();
        for (var e : com.withouthonor.npcs.compat.Compat.curios().getCurios(this)) {
            if (e.stack().isEmpty()) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putString("slot", e.slotType());
            c.putInt("idx", e.index());
            c.put("item", e.stack().save(new CompoundTag()));
            curios.add(c);
        }
        return curios;
    }

    public CompoundTag saveEquipmentSnapshot() {
        CompoundTag tag = new CompoundTag();
        net.minecraft.nbt.ListTag functional = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.ListTag cosmetic = new net.minecraft.nbt.ListTag();
        for (var slot : com.withouthonor.npcs.network.SaveEquipmentPacket.SLOTS) {
            functional.add(getFunctionalItem(slot).save(new CompoundTag()));
            cosmetic.add(getCosmeticItem(slot).save(new CompoundTag()));
        }
        tag.put("Func", functional);
        tag.put("Cosm", cosmetic);
        tag.putBoolean("HideArmor", isHideArmor());
        tag.putBoolean("HideMainhand", isHideMainhand());
        tag.putBoolean("HideOffhand", isHideOffhand());
        tag.put("Arrow", getArrowItem().save(new CompoundTag()));
        if (com.withouthonor.npcs.compat.Compat.curiosLoaded()) {
            // Во время смерти слоты уже очищены — берём буфер, снятый до super.die().
            net.minecraft.nbt.ListTag curios = deathCuriosSnapshot != null ? deathCuriosSnapshot : curiosToTag();
            if (!curios.isEmpty()) {
                tag.put("Curios", curios);
            }
        }
        return tag;
    }

    public void loadEquipmentSnapshot(CompoundTag tag) {
        var slots = com.withouthonor.npcs.network.SaveEquipmentPacket.SLOTS;
        net.minecraft.nbt.ListTag functional = tag.getList("Func", net.minecraft.nbt.Tag.TAG_COMPOUND);
        net.minecraft.nbt.ListTag cosmetic = tag.getList("Cosm", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < slots.length && i < functional.size(); i++) {
            setItemSlot(slots[i], net.minecraft.world.item.ItemStack.of(functional.getCompound(i)));
            setDropChance(slots[i], 0.0F);
        }
        for (int i = 0; i < slots.length && i < cosmetic.size(); i++) {
            setCosmeticItem(slots[i], net.minecraft.world.item.ItemStack.of(cosmetic.getCompound(i)));
        }
        setHideArmor(tag.getBoolean("HideArmor"));
        setHideMainhand(tag.getBoolean("HideMainhand"));
        setHideOffhand(tag.getBoolean("HideOffhand"));
        setArrowItem(tag.contains("Arrow")
                ? net.minecraft.world.item.ItemStack.of(tag.getCompound("Arrow"))
                : net.minecraft.world.item.ItemStack.EMPTY);
        if (com.withouthonor.npcs.compat.Compat.curiosLoaded()) {
            // Снапшот — полное состояние: нет секции Curios = слоты пустые.
            com.withouthonor.npcs.compat.Compat.curios().resetCurios(this);
            net.minecraft.nbt.ListTag curios = tag.getList("Curios", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < curios.size(); i++) {
                CompoundTag c = curios.getCompound(i);
                net.minecraft.world.item.ItemStack stack =
                        net.minecraft.world.item.ItemStack.of(c.getCompound("item"));
                if (!stack.isEmpty()) {
                    com.withouthonor.npcs.compat.Compat.curios()
                            .setCurio(this, c.getString("slot"), c.getInt("idx"), stack);
                }
            }
        }
    }

    @Override
    protected void registerGoals() {

        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(2,
                new com.withouthonor.npcs.common.entity.ai.PursueAttackerGoal(this));

        this.goalSelector.addGoal(3,
                new com.withouthonor.npcs.common.entity.ai.FollowPlayerGoal(this));

        this.goalSelector.addGoal(4,
                new com.withouthonor.npcs.common.entity.ai.ScheduleGoal(this));

        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) {
            ground.setCanOpenDoors(cfgOpenDoors);
        }
        if (cfgOpenDoors) {
            this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        }
        float danger = cfgAvoidDanger ? 16.0F : 0.0F;
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_FIRE, danger);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_FIRE, danger);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_OTHER, danger);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_OTHER, danger);

        if (cfgPanic) {
            // Свой PanicGoal: триггер по метке урона (recentlyPanicHurtTick), а не по lastHurtByMob —
            // то поле провокация чистит всегда (см. suppressAggro), иначе паника не срабатывала бы.
            this.goalSelector.addGoal(1, new com.withouthonor.npcs.common.entity.ai.NpcPanicGoal(this, 1.5D));
        }
        if (cfgAvoidSun) {
            this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.RestrictSunGoal(this));
            // Свой FleeSun без требования гореть — иначе ванильный сработал бы лишь при burn_in_sun.
            this.goalSelector.addGoal(5, new com.withouthonor.npcs.common.entity.ai.AvoidSunGoal(this, 1.0D));
        }
        // #4 Природный страх: NPC-«жертва» (напр. житель) убегает от NPC своего хищного типа (нежить).
        // Работает при любом стиле, даже пассивном (registerGoals зовётся до passive-выхода). Приоритет 1 —
        // страх доминирует над боевыми MOVE-голами (жертва бежит, а не дерётся). Союзники по фракции — не враги.
        if (cfgNaturalHostility && CreatureType.hasPredators(cfgCreatureType)) {
            // Скан хищника троттлится ~раз в 10 тиков (как агро/защита), а не каждый кадр.
            // Гейт getTarget()==null: жертва в бою (боевой «стражник-житель») дерётся, а не бежит.
            this.goalSelector.addGoal(1, new com.withouthonor.npcs.common.entity.ai.ThrottledAvoidEntityGoal<>(
                    this, CompanionEntity.class, 8.0F, 1.0D, 1.3D,
                    e -> e instanceof CompanionEntity oc
                            // Хищник с выключенной «природной враждой» не агрит по типу — и не пугает.
                            && oc.isNaturalHostilityEnabled()
                            && oc.getCreatureType().npcEnemies().contains(cfgCreatureType)
                            && !isSameFaction(oc),
                    10, () -> getTarget() == null));
        }
        if (!"none".equals(cfgAutoFollowMode)) {
            this.goalSelector.addGoal(3,
                    new com.withouthonor.npcs.common.entity.ai.AutoFollowGoal(this));
        }
        if (cfgIdleWander) {
            this.goalSelector.addGoal(6,
                    new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
            if (cfgIdleWanderRadius > 0) {
                restrictTo(blockPosition(), cfgIdleWanderRadius);
                this.goalSelector.addGoal(5,
                        new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 1.0D));
            } else {
                clearRestriction();
            }
        }
        if ("lively".equals(cfgLookMode)) {
            // Живой взгляд: следит головой и корпусом; RandomLookAround — фолбэк, когда цель неактивна
            this.goalSelector.addGoal(7,
                    new com.withouthonor.npcs.common.entity.ai.WatchPlayerGoal(this));
            this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        } else if ("cold".equals(cfgLookMode)) {
            this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(
                    this, Player.class, 8.0F));
            this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        }
    }

    private int cfgResMelee, cfgResProjectile, cfgResExplosion, cfgResFire, cfgResFall, cfgResMagic;

    private java.util.List<com.withouthonor.npcs.common.dialogue.action.DialogueAction>
            cfgOnHurt = java.util.List.of(), cfgOnDeath = java.util.List.of(),
            cfgOnInteract = java.util.List.of(), cfgOnApproach = java.util.List.of();
    private int cfgApproachRange;
    private final java.util.Set<java.util.UUID> reactInRange = new java.util.HashSet<>();

    private boolean cfgBossbarEnabled;
    private net.minecraft.world.BossEvent.BossBarColor cfgBossbarColor = net.minecraft.world.BossEvent.BossBarColor.RED;
    private int cfgBossbarRadius = 32;
    private int combatBarUntil;
    @javax.annotation.Nullable
    private net.minecraft.server.level.ServerBossEvent bossBar;

    private void cacheFollowSettings(CompanionProfile profile) {
        cfgTeleport = profile.isFollowTeleport();
        cfgTeleportOOS = profile.isFollowTeleportOutOfSight();
        cfgRun = profile.isFollowRun();
        cfgMatchSpeed = profile.isFollowMatchSpeed();
        cfgDistanceTier = profile.followDistanceTier();
        cfgOpenDoors = profile.isOpenDoors();
        cfgAvoidDanger = profile.isAvoidDanger();
        cfgTeleportFx = profile.isTeleportFx();
        cfgGroupSpacing = profile.isGroupSpacing();
        cfgScheduleEnabled = profile.isScheduleEnabled();
        cfgScheduleGlobal = profile.isScheduleGlobal();
        cfgSchedule = java.util.List.copyOf(profile.getSchedule());

        cfgBasePose = new PoseJson.Pose();
        cfgBasePose.copyFrom(profile.getPose());
        cfgBaseTransform = new float[]{
                profile.getRotX(), profile.getRotY(), profile.getRotZ(),
                profile.getPosX(), profile.getPosY(), profile.getPosZ(),
                profile.getScaleX(), profile.getScaleY(), profile.getScaleZ()};
        cfgLookMode = profile.getLookMode();
        // Клампим на сервере — профиль мог прийти из внешнего json
        cfgLookRadius = Math.max(1, Math.min(16, profile.getLookRadius()));
        cfgBoatRide = profile.isBoatRide();
        cfgPursueAttacker = profile.isPursueAttacker();
        cfgProvokeEnabled = profile.isProvokeEnabled();
        cfgProvokeHits = profile.getProvokeHits();
        cfgProvokeHpPct = profile.getProvokeHpPct();
        cfgProvokeWindowTicks = profile.getProvokeWindowSec() * 20;
        cfgProvokeIgnoreEscort = profile.isProvokeIgnoreEscort();
        cfgForgiveAfterTicks = profile.getForgiveAfterSec() * 20;
        cfgEscortNoHarm = profile.isEscortNoHarmOwner();
        cfgHoldPosition = profile.isHoldPosition();
        cfgIdleWander = profile.isIdleWander();
        cfgIdleWanderRadius = profile.getIdleWanderRadius();
        cfgPanic = profile.isPanicWhenHurt();
        cfgAvoidSun = profile.isAvoidSun();
        cfgBurnInSun = profile.isBurnInSun();
        this.entityData.set(DATA_PUSHABLE, profile.isPushable());
        this.entityData.set(DATA_PASSABLE, profile.isPassable());
        this.cfgTotemCharges = profile.getTotemCharges();
        this.cfgTotemRender = profile.isTotemRender();
        if (this.cfgTotemCharges != this.appliedTotemCharges) {
            this.appliedTotemCharges = this.cfgTotemCharges;
            this.totemUsesLeft = this.cfgTotemCharges;
        }
        updateTotemArmed();
        this.cfgShieldHoldTicks = Math.round(profile.getShieldHoldSeconds() * 20.0F);
        this.cfgShieldCooldownTicks = Math.round(profile.getShieldCooldownSeconds() * 20.0F);
        this.entityData.set(DATA_IDLE_EMOTE, profile.getIdleEmoteId().isEmpty() ? ""
                : packEmote(profile.getIdleEmoteId(), profile.getIdleEmoteName(), profile.getIdleEmoteAuthor()));
        cfgDeathEmoteId = profile.getDeathEmoteId();
        cfgDeathEmoteName = profile.getDeathEmoteName();
        cfgDeathEmoteAuthor = profile.getDeathEmoteAuthor();
        cfgDeathEmoteSecs = profile.getDeathEmoteSecs();
        cfgMobType = switch (profile.getMobType()) {
            case "undead" -> net.minecraft.world.entity.MobType.UNDEAD;
            case "arthropod" -> net.minecraft.world.entity.MobType.ARTHROPOD;
            case "water" -> net.minecraft.world.entity.MobType.WATER;
            case "illager" -> net.minecraft.world.entity.MobType.ILLAGER;
            default -> net.minecraft.world.entity.MobType.UNDEFINED;
        };
        cfgCreatureType = CreatureType.byId(profile.getCreatureType());
        cfgCustomAttackers = parseGroups(profile.getCreatureCustomAttackers());
        cfgNaturalHostility = profile.isNaturalHostility();
        // Атакуемость — ДО пересчёта счётчика: гейт effectiveAttackers() зависит от неё, а update()
        // симметрично декрементит, если NPC стал неатакуемым при смене профиля (без рассинхрона).
        cfgAttackable = profile.isAttackable();
        if (!level().isClientSide) {
            // Счётчик релевантных NPC для дешёвого гейта canUse (агра И защита).
            boolean now = !effectiveAttackers().isEmpty() || !effectiveDefenders().isEmpty();
            com.withouthonor.npcs.common.entity.ai.CreatureAggroState.update(aggroCounted, now);
            aggroCounted = now;
        }
        cfgFallDamage = profile.isFallDamage();
        cfgWebSlow = profile.isWebSlow();
        cfgCanDrown = profile.isCanDrown();
        cfgPoisonImmune = profile.isPoisonImmune();
        cfgAggroRange = profile.getAggroRange();
        cfgLeap = profile.isLeapAtTarget();
        cfgAutoFollowMode = profile.getAutoFollowMode();
        cfgAutoFollowTarget = profile.getAutoFollowTarget();
        cfgPotionSelf = potionStacks(profile.getPotionSelf());
        cfgPotionEnemy = potionStacks(profile.getPotionEnemy());
        cfgPotionAlly = potionStacks(profile.getPotionAlly());
        cfgPotionSelfEnabled = profile.isPotionSelfEnabled();
        cfgPotionSelfCombat = profile.isPotionSelfCombat();
        cfgPotionEnemyEnabled = profile.isPotionEnemyEnabled();
        cfgPotionAllyEnabled = profile.isPotionAllyEnabled();
        cfgPotionEnemySeq = profile.isPotionEnemySeq();
        cfgPotionAllySeq = profile.isPotionAllySeq();
        enemySeqIdx = 0;
        allySeqIdx = 0;
        cfgCombatBuff = potionStacks(profile.getPotionCombatBuff());
        cfgIsWitch = "potion".equals(profile.getCombatPreset());
        cfgSpareCreepers = profile.isGuardSpareCreepers();
        cfgResMelee = profile.getResMelee();
        cfgResProjectile = profile.getResProjectile();
        cfgResExplosion = profile.getResExplosion();
        cfgResFire = profile.getResFire();
        cfgResFall = profile.getResFall();
        cfgResMagic = profile.getResMagic();
        cfgOnHurt = java.util.List.copyOf(profile.getOnHurt());
        cfgOnDeath = java.util.List.copyOf(profile.getOnDeath());
        cfgOnInteract = java.util.List.copyOf(profile.getOnInteract());
        cfgOnApproach = java.util.List.copyOf(profile.getOnApproach());
        cfgApproachRange = profile.getApproachRange();
        reactInRange.clear();
        cfgBossbarEnabled = profile.isBossbarEnabled();
        cfgBossbarColor = parseBarColor(profile.getBossbarColor());
        cfgBossbarRadius = profile.getBossbarRadius();
    }

    private static net.minecraft.world.BossEvent.BossBarColor parseBarColor(String c) {
        try {
            return net.minecraft.world.BossEvent.BossBarColor.valueOf(c.toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            return net.minecraft.world.BossEvent.BossBarColor.RED;
        }
    }

    private void tickBossBar() {
        if (!cfgBossbarEnabled) {
            if (bossBar != null) {
                bossBar.removeAllPlayers();
                bossBar = null;
            }
            return;
        }
        if (getTarget() != null) {
            combatBarUntil = tickCount + 200;
        }
        if (tickCount >= combatBarUntil || !isAlive()) {
            if (bossBar != null) {
                bossBar.removeAllPlayers();
                bossBar.setVisible(false);
            }
            return;
        }
        if (bossBar == null) {
            bossBar = new net.minecraft.server.level.ServerBossEvent(
                    getDisplayName(), cfgBossbarColor, net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
        }
        bossBar.setName(getDisplayName());
        bossBar.setColor(cfgBossbarColor);
        bossBar.setProgress(net.minecraft.util.Mth.clamp(getHealth() / getMaxHealth(), 0F, 1F));
        bossBar.setVisible(true);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            double r2 = (double) cfgBossbarRadius * cfgBossbarRadius;
            for (ServerPlayer p : new java.util.ArrayList<>(bossBar.getPlayers())) {
                if (p.level() != sl || !p.isAlive() || distanceToSqr(p) > r2) {
                    bossBar.removePlayer(p);
                }
            }
            for (ServerPlayer p : sl.players()) {
                if (distanceToSqr(p) <= r2) {
                    bossBar.addPlayer(p);
                }
            }
        }
    }

    @Override
    public net.minecraft.world.entity.MobType getMobType() {
        return cfgMobType;
    }

    /** «Тип существа» для реакции чужих мобов (0.9.5 #4). */
    public CreatureType getCreatureType() {
        return cfgCreatureType;
    }

    /** Тумблер «природной вражды» этого NPC: выключенный хищник не агрит по типу и НЕ пугает жертв. */
    public boolean isNaturalHostilityEnabled() {
        return cfgNaturalHostility;
    }

    /** Тик последнего фактического урона — триггер паники (см. NpcPanicGoal). */
    public int getRecentlyPanicHurtTick() {
        return recentlyPanicHurtTick;
    }

    /** Эффективные группы-атакующие: у кастома — из профиля, иначе — из пресета типа.
     *  Неатакуемый NPC (профиль запрещает урон) для чужих мобов пуст — иначе моб зациклится
     *  на неубиваемой цели. Защитники/страх не гейтим — они на атакуемость не завязаны. */
    public java.util.Set<MobGroup> effectiveAttackers() {
        if (!cfgAttackable) {
            return java.util.Set.of();
        }
        return cfgCreatureType == CreatureType.CUSTOM ? cfgCustomAttackers : cfgCreatureType.attackers();
    }

    /** Эффективные группы-защитники (пока только из пресета типа). */
    public java.util.Set<MobGroup> effectiveDefenders() {
        return cfgCreatureType.defenders();
    }

    private static java.util.Set<MobGroup> parseGroups(java.util.List<String> ids) {
        if (ids.isEmpty()) {
            return java.util.Set.of();
        }
        java.util.EnumSet<MobGroup> set = java.util.EnumSet.noneOf(MobGroup.class);
        for (String id : ids) {
            MobGroup g = MobGroup.byId(id);
            if (g != null) {
                set.add(g);
            }
        }
        return set;
    }

    /**
     * #4 Защитники реагируют на нападение на NPC-«жертву» (как ваниль-голем идёт на того, кто ударил
     * жителя). Ваниль завязана на репутацию деревни, которой у нас нет, поэтому оповещаем сами из hurt():
     * это ловит и ваншот (hurt зовётся до die). Нацеливаем ближних защитников на атакующего, если они
     * сейчас без цели (не перебиваем их бой). Другой NPC / игрок в креативе-спектаторе — не цель.
     */
    private void alertDefenders(net.minecraft.world.entity.LivingEntity attacker) {
        if (effectiveDefenders().isEmpty() || attacker instanceof CompanionEntity) {
            return;
        }
        if (attacker instanceof net.minecraft.world.entity.player.Player p
                && (p.isSpectator() || p.isCreative())) {
            return;
        }
        net.minecraft.world.phys.AABB box = getBoundingBox().inflate(10.0D, 8.0D, 10.0D);
        for (net.minecraft.world.entity.Mob defender : level().getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class, box, this::isDefender)) {
            if (defender.getTarget() == null || !defender.getTarget().isAlive()) {
                defender.setTarget(attacker);
            }
        }
    }

    private boolean isDefender(net.minecraft.world.entity.Mob m) {
        for (MobGroup g : effectiveDefenders()) {
            if (g.matches(m)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0) {
            int res = resistanceFor(source);
            if (res != 0) {
                amount *= Math.max(0F, 1F - res / 100F);
                if (amount <= 0F) {
                    return false;
                }
            }
        }
        boolean damaged = super.hurt(source, amount);
        if (damaged && !level().isClientSide) {
            // Метка для паники — от ЛЮБОГО урона (как ваниль), ДО и независимо от провокации:
            // suppressAggro всегда чистит lastHurtByMob, NpcPanicGoal читает этот тик.
            recentlyPanicHurtTick = tickCount;
        }
        // Провокация: до порога терпим случайные удары ИГРОКА (super.hurt уже вызвал setLastHurtByMob).
        // suppressed=true → аргро подавлено (гасим и HurtByTargetGoal, и ветку PursueAttackerGoal).
        boolean suppressed = damaged && !level().isClientSide && handleProvocation(source, amount);
        // Полностью заблокированный щитом удар: ваниль СТАВИТ setLastHurtByMob, но возвращает false →
        // без гейта щитовик агрится с первого удара в поднятый щит. Блок СЧИТАЕТСЯ ударом (hits++),
        // но в счётчик урона идёт фактический урон = 0.
        if (!damaged && !level().isClientSide
                && source.getEntity() instanceof ServerPlayer blockedBy
                && getLastHurtByMob() == blockedBy) {
            handleProvocation(source, 0F);
        }
        if (damaged && !level().isClientSide && !cfgOnHurt.isEmpty()) {
            // Реакции на удар играются независимо от провокации.
            runReactions(cfgOnHurt, source.getEntity() instanceof ServerPlayer sp ? sp : nearestServerPlayer(16));
        }
        if (damaged && !level().isClientSide && !suppressed && cfgPursueAttacker && getTarget() == null
                && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker != this && isConfiguredAggressorTarget(attacker)) {
            double follow = getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
            if (distanceToSqr(attacker) > follow * follow) {
                setPursuitTarget(attacker);
            }
        }
        // Защитники (напр. големы у жителя) идут на атакующего — включая игрока, но ТОЛЬКО когда агро
        // реально состоялось: подавленный провокацией случайный тычок игрока — не повод голему получить
        // вечную цель. FakePlayer (машины/турели модов) — не цель защитников (как в handleProvocation).
        if (damaged && !level().isClientSide && !suppressed
                && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity att
                && att != this
                && !(att instanceof net.minecraftforge.common.util.FakePlayer)) {
            alertDefenders(att);
        }
        return damaged;
    }

    /**
     * Порог терпимости к случайным ударам игрока. Возвращает true, если аргро подавлено
     * (накопили удар, порог ещё не достигнут) — тогда снимаем lastHurtByMob, чтобы HurtByTargetGoal
     * не сработал. Порог = N ударов ИЛИ ≥X% max HP за окно (что раньше). Осознанная враждебность
     * (агро фракций) не трогается — этот гейт только про прямой урон игрока-«не-врага».
     */
    private boolean handleProvocation(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!cfgProvokeEnabled || !(source.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        // FakePlayer (машины/турели модов) — не «случайный удар игрока»: агрим как на обычного моба.
        if (player instanceof net.minecraftforge.common.util.FakePlayer) {
            return false;
        }
        // Прощение — per-target: тик обновляем только если бьёт ТЕКУЩАЯ цель, иначе удар любого
        // игрока (даже сопровождаемого) откладывал бы прощение чужой цели.
        if (player == getTarget()) {
            lastHurtByPlayerTick = tickCount;
            // Настроенную цель («кого атаковать»/фракции) прощаемой НЕ делаем: это осознанная
            // агрессия, её не «прощаем» (регресс 0.9.4b). Прощаемы только цели за удары/реванш.
            if (!isConfiguredAggressorTarget(player)) {
                forgivablePlayerUuid = player.getUUID();
            }
        }
        // Игрок во враждебном тире репутации — настоящий враг, провокация не применяется.
        if (isHostileFactionPlayer(player)) {
            return false;
        }
        // Сопровождаемый игрок никогда не агрит (счётчики даже не копятся).
        if (cfgProvokeIgnoreEscort) {
            Player escort = followedPlayer();
            if (escort != null && escort.getUUID().equals(player.getUUID())) {
                return suppressAggro();
            }
        }
        pruneProvokeState();
        ProvokeEntry e = provokeState.computeIfAbsent(player.getUUID(), k -> new ProvokeEntry());
        if (tickCount - e.lastHitTick > cfgProvokeWindowTicks) {
            e.hits = 0;
            e.damage = 0F; // окно истекло — начинаем заново
        }
        e.hits++;
        e.damage += amount;
        e.lastHitTick = tickCount;
        boolean byHits = e.hits >= cfgProvokeHits;
        boolean byDamage = cfgProvokeHpPct > 0 && e.damage >= getMaxHealth() * (cfgProvokeHpPct / 100.0F);
        if (byHits || byDamage) {
            provokeState.remove(player.getUUID()); // порог пройден — агрим нормально, счётчик сброшен
            lastHurtByPlayerTick = tickCount; // этот игрок сейчас станет целью — стартуем окно прощения
            forgivablePlayerUuid = player.getUUID(); // цель взята ЗА УДАРЫ — её можно простить
            return false;
        }
        return suppressAggro();
    }

    /** Провокация подавляет агро: lastHurtByMob снимаем ВСЕГДА (его читает HurtByTargetGoal —
     *  иначе боевой паникёр агрился с первого удара). Паника от этого не ломается: NpcPanicGoal
     *  завязан на recentlyPanicHurtTick, а не на lastHurtByMob. */
    private boolean suppressAggro() {
        setLastHurtByMob(null);
        return true;
    }

    /** Удаляет истёкшие по окну записи + держит карту в пределах 16 (защита от утечки). */
    private void pruneProvokeState() {
        provokeState.entrySet().removeIf(en -> tickCount - en.getValue().lastHitTick > cfgProvokeWindowTicks);
        // >= 16: prune зовётся ДО computeIfAbsent — освобождаем место под новую запись заранее,
        // иначе карта разрасталась до 17.
        while (provokeState.size() >= 16) {
            var oldest = provokeState.entrySet().stream()
                    .min(java.util.Comparator.comparingInt(en -> en.getValue().lastHitTick));
            if (oldest.isEmpty()) {
                break;
            }
            provokeState.remove(oldest.get().getKey());
        }
    }

    /** Прощение: цель-игрок, взятая ЗА УДАРЫ, долго не бьёт → снять цель (из серверного тика).
     *  Прощаются ТОЛЬКО цели из forgivablePlayerUuid (реванш/порог провокации). Цели из
     *  «кого атаковать»/природной вражды — настроенная агрессия, их не «прощаем» (иначе
     *  цель сбрасывалась каждую секунду и NPC двигался рывками — регресс 0.9.4b). */
    private void tickForgiveness() {
        if (!cfgProvokeEnabled || cfgForgiveAfterTicks <= 0) {
            return;
        }
        if (!(getTarget() instanceof ServerPlayer player)) {
            return;
        }
        if (forgivablePlayerUuid == null || !forgivablePlayerUuid.equals(player.getUUID())) {
            return; // цель не за удары — прощение не для неё
        }
        if (isHostileFactionPlayer(player)) {
            return; // враг по фракции — не прощаем
        }
        if (tickCount - lastHurtByPlayerTick >= cfgForgiveAfterTicks) {
            forgivablePlayerUuid = null;
            setTarget(null);
            setLastHurtByMob(null);
        }
    }

    private int resistanceFor(net.minecraft.world.damagesource.DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            return cfgResExplosion;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
            return cfgResProjectile;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return cfgResFire;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return cfgResFall;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            return cfgResMagic;
        }
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.LivingEntity) {
            return cfgResMelee;
        }
        return 0;
    }

    private void tickSunBurn() {
        if (isSunBurnTick()
                && getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()) {
            setSecondsOnFire(8);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier,
                                   net.minecraft.world.damagesource.DamageSource source) {
        return cfgFallDamage && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state,
                                 net.minecraft.world.phys.Vec3 motionMultiplier) {

        if (cfgWebSlow || !state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            super.makeStuckInBlock(state, motionMultiplier);
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return !cfgCanDrown || super.canBreatheUnderwater();
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        if (cfgPoisonImmune && effect.getEffect() == net.minecraft.world.effect.MobEffects.POISON) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    public boolean scheduleActive() {
        if (!cfgScheduleEnabled || cfgSchedule.isEmpty()) {
            return false;
        }

        return followMode == com.withouthonor.npcs.common.entity.ai.FollowMode.NONE
                || getFollowTarget() == null;
    }

    public java.util.List<com.withouthonor.npcs.common.profile.ScheduleEntry> getSchedule() {
        return cfgSchedule;
    }

    private boolean scheduleYawFrozen;

    public void setScheduleYawFrozen(boolean frozen) {
        this.scheduleYawFrozen = frozen;
    }

    public boolean isScheduleYawFrozen() {
        return scheduleYawFrozen;
    }

    public boolean isSitting() {
        return this.entityData.get(DATA_SITTING);
    }

    public boolean isEpicFightMode() {
        return this.entityData.get(DATA_EF_MODE);
    }

    public void setSitting(boolean sitting) {
        if (isSitting() != sitting) {
            this.entityData.set(DATA_SITTING, sitting);
        }
    }

    @Override
    public boolean isPushable() {
        if (this.entityData.get(DATA_PASSABLE)) {
            return false;
        }
        return this.entityData.get(DATA_PUSHABLE);
    }

    @Override
    protected void pushEntities() {
        if (this.entityData.get(DATA_PASSABLE)) {
            return;
        }
        super.pushEntities();
    }

    public boolean isTotemArmed() {
        return this.entityData.get(DATA_TOTEM_ARMED);
    }

    private void updateTotemArmed() {
        this.entityData.set(DATA_TOTEM_ARMED, cfgTotemRender && totemUsesLeft > 0);
    }

    public void refreshTotem() {
        this.totemUsesLeft = this.cfgTotemCharges;
        updateTotemArmed();
    }

    /**
     * Считает ли этот NPC {@code other} своим союзником для поддержки (лечения/бафов) Iron's Spells.
     * Зовётся из миксина в Utils.shouldHealEntity (ISS про наши фракции/следование сам не знает).
     * Союзник = хозяин (кого следуем) или другой NPC той же фракции. Только при включённой опции.
     *
     * Строго серверный метод: Utils.shouldHealEntity зовётся в т.ч. из тика снарядов ISS, который
     * идёт на обеих сторонах, а ProfileManager.get() на клиенте кидает IllegalStateException.
     */
    public boolean whIsSupportAlly(net.minecraft.world.entity.Entity other) {
        if (level().isClientSide || other == null || other == this || profileId == null) {
            return false;
        }
        CompanionProfile p = ProfileManager.get().get(profileId);
        if (p == null || !p.isSupportAllies()) {
            return false;
        }
        // followedPlayer() покрывает ОБА пути следования (ручное FOLLOW и авто-следование из профиля);
        // followTargetId ставит только ручное, поэтому по нему одному авто-следование не распознавалось.
        Player followed = followedPlayer();
        if (followed != null && followed.getUUID().equals(other.getUUID())) {
            return true;
        }
        if (other instanceof CompanionEntity oc && oc.profileId != null) {
            String mine = p.getFaction();
            if (mine == null || mine.isEmpty()) {
                return false;
            }
            CompanionProfile op = ProfileManager.get().get(oc.profileId);
            return op != null && mine.equals(op.getFaction());
        }
        return false;
    }

    @Nullable
    public Player getFollowTarget() {
        if (followTargetId == null || level().isClientSide) {
            return null;
        }
        Player p = level().getPlayerByUUID(followTargetId);
        return p != null && p.isAlive() ? p : null;
    }

    public com.withouthonor.npcs.common.entity.ai.FollowMode getFollowMode() {
        return followMode;
    }

    public void startFollowing(Player player) {
        if (followMode == com.withouthonor.npcs.common.entity.ai.FollowMode.NONE) {
            this.followReturnPos = blockPosition();
        }
        this.followTargetId = player.getUUID();
        this.followMode = com.withouthonor.npcs.common.entity.ai.FollowMode.FOLLOW;
    }

    public void waitHere() {
        if (followTargetId != null) {
            this.followMode = com.withouthonor.npcs.common.entity.ai.FollowMode.WAIT;
            getNavigation().stop();
        }
    }

    public void sayGoodbye() {
        if (followTargetId != null) {
            this.followMode = com.withouthonor.npcs.common.entity.ai.FollowMode.RETURN;
        }
    }

    public void finishReturn() {
        this.followTargetId = null;
        this.followMode = com.withouthonor.npcs.common.entity.ai.FollowMode.NONE;
        getNavigation().stop();
    }

    @Nullable
    public net.minecraft.core.BlockPos getFollowReturnPos() {
        return followReturnPos;
    }

    public boolean isFollowingPlayer(Player player) {
        return followMode != com.withouthonor.npcs.common.entity.ai.FollowMode.NONE
                && followTargetId != null && followTargetId.equals(player.getUUID());
    }

    public boolean shouldTeleportFollow() {
        return cfgTeleport;
    }

    public boolean cfgRun() {
        return cfgRun;
    }

    public boolean cfgMatchSpeed() {
        return cfgMatchSpeed;
    }

    public boolean cfgPursueAttacker() {
        return cfgPursueAttacker;
    }

    public boolean cfgHoldPosition() {
        return cfgHoldPosition;
    }

    @javax.annotation.Nullable
    private net.minecraft.world.entity.LivingEntity pursuitTarget;
    private int pursuitExpireTick;

    public void setPursuitTarget(net.minecraft.world.entity.LivingEntity attacker) {
        this.pursuitTarget = attacker;
        this.pursuitExpireTick = this.tickCount + 240;
    }

    @javax.annotation.Nullable
    public net.minecraft.world.entity.LivingEntity getPursuitTarget() {
        return this.pursuitTarget;
    }

    public int getPursuitExpireTick() {
        return this.pursuitExpireTick;
    }

    public void clearPursuitTarget() {
        this.pursuitTarget = null;
    }

    public boolean cfgTeleportOutOfSight() {
        return cfgTeleportOOS;
    }

    public boolean cfgTeleportFx() {
        return cfgTeleportFx;
    }

    public boolean cfgGroupSpacing() {
        return cfgGroupSpacing;
    }

    public int cfgDistanceTier() {
        return cfgDistanceTier;
    }

    public String cfgAutoFollowMode() {
        return cfgAutoFollowMode;
    }

    public int cfgLookRadius() {
        return cfgLookRadius;
    }

    public String cfgAutoFollowTarget() {
        return cfgAutoFollowTarget;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (isAggressive()) {
            return InteractionResult.PASS;
        }
        // Общая для обеих сторон: иначе клиент вернёт SUCCESS и предмет
        // не получит interactLivingEntity на клиенте (рассинхрон замаха).
        if ((player.getItemInHand(hand).getItem()
                instanceof com.withouthonor.npcs.common.item.MemorionFeatherItem
                || player.getItemInHand(hand).getItem()
                instanceof com.withouthonor.npcs.common.item.NpcMoverItem)
                && player.hasPermissions(2)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {

            if (serverPlayer.isShiftKeyDown() && isFollowingPlayer(serverPlayer)) {
                waitHere();
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "wh_npcs.msg.wait_here", getName()), true);
                return InteractionResult.CONSUME;
            }

            if (DialogueRuntime.tryOpen(serverPlayer, this) || openTrade(serverPlayer)) {
                return InteractionResult.CONSUME;
            }

            CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
            boolean reacted = false;
            if (profile != null && !profile.getInteractPhrases().isEmpty()) {
                speakFromPool(profile, profile.getInteractPhrases(), serverPlayer);
                reacted = true;
            }
            if (!cfgOnInteract.isEmpty()) {
                runReactions(cfgOnInteract, serverPlayer);
                reacted = true;
            }
            return reacted ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private Player tradingPlayer;
    @Nullable
    private net.minecraft.world.item.trading.MerchantOffers tradingOffers;

    private record OfferRef(String key, boolean shared) {
    }

    private final java.util.Map<net.minecraft.world.item.trading.MerchantOffer, OfferRef> tradeOfferKeys =
            new java.util.HashMap<>();

    public boolean openTrade(ServerPlayer player) {
        if (tradingPlayer != null && tradingPlayer != player) {
            return false;
        }
        CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
        if (profile == null || profile.getOffers().isEmpty()) {
            return false;
        }
        var states = com.withouthonor.npcs.common.storage.PlayerStateManager.get(player.server);
        String pid = profileId.toString();
        long now = System.currentTimeMillis();
        long restockMs = profile.getRestockMinutes() * 60_000L;

        if (restockMs > 0) {
            long start = states.getTradeWindow(player.getUUID(), pid);
            if (start > 0 && now - start >= restockMs) {
                states.resetTrades(player.getUUID(), pid);
            }
            long globalStart = states.getGlobalTradeWindow(pid);
            if (globalStart > 0 && now - globalStart >= restockMs) {
                states.resetGlobalTrades(pid);
            }
        }

        float priceMult = 1.0F;
        if (profile.getFaction() != null) {
            var faction = com.withouthonor.npcs.common.reputation.FactionRegistry.get()
                    .byId(profile.getFaction());
            if (faction != null) {
                priceMult = faction.tierFor(states.getReputation(player.getUUID(), faction.getId()))
                        .priceMult();
                if (priceMult <= 0.0F) {
                    return false;
                }
            }
        }
        var offers = new net.minecraft.world.item.trading.MerchantOffers();
        tradeOfferKeys.clear();
        var ctx = new com.withouthonor.npcs.common.dialogue.condition.DialogueCondition.Context(player, this);
        for (int i = 0; i < profile.getOffers().size(); i++) {
            var offer = profile.getOffers().get(i);
            if (!com.withouthonor.npcs.common.dialogue.condition.DialogueCondition
                    .testAll(offer.conditions(), ctx)) {
                continue;
            }
            String key = pid + "#" + i;
            int uses = offer.sharedLimit()
                    ? states.getGlobalTradeUses(key)
                    : states.getTradeUses(player.getUUID(), key);
            var merchantOffer = offer.toMerchantOffer(uses);
            if (merchantOffer == null) {
                continue;
            }
            if (priceMult != 1.0F) {

                int diff = Math.round(offer.costA().count() * (priceMult - 1.0F));
                diff = Math.max(1 - offer.costA().count(), diff);
                if (diff != 0) {
                    merchantOffer.setSpecialPriceDiff(diff);
                }
            }
            tradeOfferKeys.put(merchantOffer, new OfferRef(key, offer.sharedLimit()));
            offers.add(merchantOffer);
        }
        if (offers.isEmpty()) {
            return false;
        }
        tradingOffers = offers;
        setTradingPlayer(player);

        net.minecraft.network.chat.Component title = this.getDisplayName();
        if (restockMs > 0) {
            long start = states.getTradeWindow(player.getUUID(), pid);
            if (start == 0) {
                start = states.getGlobalTradeWindow(pid);
            }
            if (start > 0) {
                long minutes = Math.max(1, (restockMs - (now - start) + 59_999) / 60_000);
                net.minecraft.network.chat.Component left = minutes >= 60
                        ? net.minecraft.network.chat.Component.translatable("wh_npcs.msg.trade.time_hm", minutes / 60, minutes % 60)
                        : net.minecraft.network.chat.Component.translatable("wh_npcs.msg.trade.time_m", minutes);
                title = net.minecraft.network.chat.Component.translatable("wh_npcs.msg.trade.restock_in",
                        this.getName().getString(), left);
            }
        }
        openTradingScreen(player, title, 0);
        return true;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public net.minecraft.world.item.trading.MerchantOffers getOffers() {
        return tradingOffers != null ? tradingOffers : new net.minecraft.world.item.trading.MerchantOffers();
    }

    @Override
    public void overrideOffers(net.minecraft.world.item.trading.MerchantOffers offers) {

    }

    @Override
    public void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer) {
        if (level().isClientSide) {
            return;
        }

        OfferRef ref = tradeOfferKeys.get(offer);
        if (ref != null && tradingPlayer instanceof ServerPlayer serverPlayer) {
            var states = com.withouthonor.npcs.common.storage.PlayerStateManager.get(serverPlayer.server);
            String pid = ref.key().substring(0, ref.key().indexOf('#'));
            if (ref.shared()) {
                states.addGlobalTradeUse(ref.key());
                if (states.getGlobalTradeWindow(pid) == 0L) {
                    states.setGlobalTradeWindow(pid, System.currentTimeMillis());
                }
            } else {
                states.addTradeUse(serverPlayer.getUUID(), ref.key());
                if (states.getTradeWindow(serverPlayer.getUUID(), pid) == 0L) {
                    states.setTradeWindow(serverPlayer.getUUID(), pid, System.currentTimeMillis());
                }
            }
        }

        if (offer.getXp() > 0 && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.entity.ExperienceOrb.award(serverLevel, position(), offer.getXp());
        }
    }

    @Override
    public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getNotifyTradeSound() {
        return net.minecraft.sounds.SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide;
    }

    public void setScheduleEmote(String id) {
        setScheduleEmote(id, "", "");
    }

    public void setScheduleEmote(String id, String name, String author) {
        setScheduleEmote(id, name, author, 0);
    }

    public void setScheduleEmote(String id, String name, String author, int nonce) {
        // Смена nonce меняет строку — защита от равного значения не блокирует повтор.
        String v = packEmote(id, name, author, nonce);
        if (!this.entityData.get(DATA_SCHEDULE_EMOTE).equals(v)) {
            this.entityData.set(DATA_SCHEDULE_EMOTE, v);
        }
    }

    private String clientAppliedEmote = "";

    private void reconcileScheduleEmote() {
        String dialogue = this.entityData.get(DATA_EMOTECRAFT_EMOTE);
        String schedule = this.entityData.get(DATA_SCHEDULE_EMOTE);
        String want = !dialogue.isEmpty() ? dialogue
                : (!schedule.isEmpty() ? schedule : this.entityData.get(DATA_IDLE_EMOTE));
        if (want.equals(clientAppliedEmote)) {
            return;
        }
        clientAppliedEmote = want;
        if (want.isEmpty()) {
            com.withouthonor.npcs.compat.Compat.emotecraft().stopOn(this);
        } else {
            String[] parts = want.split(java.util.regex.Pattern.quote(String.valueOf(EMOTE_SEP)), -1);
            String id = parts[0];
            String name = parts.length > 1 ? parts[1] : "";
            String author = parts.length > 2 ? parts[2] : "";
            com.withouthonor.npcs.compat.Compat.emotecraft().playOn(this, id, name, author);
        }
    }

    /** #3: идёт ли сцена смерти (эмоция перед смертью) — ранний гейт для целей движения. */
    public boolean isDeathStaged() {
        return deathStaged;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            reconcileScheduleEmote();
            if (this.swinging) {
                this.updateSwingTime();
            }
        }
        if (!level().isClientSide) {

            // #3 Инсценировка смерти: держим ИИ замороженным, по таймеру — настоящая смерть.
            // После запуска финальной смерти (deathStageFinished) блок больше не входит:
            // ваниль сама доигрывает tickDeath и убирает труп.
            if (deathStaged && !deathStageFinished) {
                setTarget(null); // страховка: цели движения гейтятся и через isDeathStaged()
                getNavigation().stop();
                if (--deathStageTicks <= 0) {
                    deathStageFinished = true; // одноразово: die() ниже — финальный
                    sendEmotecraftEmote(""); // стоп эмоции (как действие StopEmotecraftEmote)
                    net.minecraft.world.damagesource.DamageSource s = deathStageSource != null
                            ? deathStageSource : damageSources().generic();
                    setHealth(0.0F); // иначе ваниль tickDeath при 1 HP никогда не удалит труп
                    die(s); // deathStageFinished==true → мимо тотема и инсценировки в super.die
                }
                return; // пока умираем — остальной тик не нужен
            }

            // Тик-драйвер каста Iron's Spells (ноль работы без активного каста; без ISS — noop)
            if (com.withouthonor.npcs.compat.Compat.ironsSpellsLoaded()) {
                com.withouthonor.npcs.compat.Compat.ironsSpells().tick(this);
            }

            if (tickCount % 200 == 0) {
                updateIndex();
            }

            if (tickCount % 20 == 0) {
                tickForgiveness();
            }

            // #4: возвратные снаряды (Loyalty-трезубец, модовые копья вроде Extinction Spear) летят к владельцу-NPC,
            // но «всасывание» ваниль/моды делают только в playerTouch — у моба подобрать снаряд некому,
            // и он вечно кружит у головы. Секундный скан вокруг NPC: наш тег + снаряд «пожил» → discard.
            // Воткнувшееся в землю копьё далеко от NPC — вне бокса; прилетевшее обратно — ровно наш случай.
            if (hasThrownProjectiles && tickCount % 20 == 6) {
                for (net.minecraft.world.entity.projectile.Projectile p : level().getEntitiesOfClass(
                        net.minecraft.world.entity.projectile.Projectile.class, getBoundingBox().inflate(3.0D))) {
                    if (p.getTags().contains(THROWN_PROJECTILE_TAG) && p.tickCount > 60) {
                        p.discard();
                    }
                }
            }

            if (!pendingActions.isEmpty()) {
                tickPendingActions();
            }

            if (tickCount % 40 == 0) {
                tryAmbientBubble();
            }

            if (tickCount % 40 == 20) {
                updateIndicators();
            }

            if (tickCount % 10 == 3) {
                tickApproachReaction();
            }

            if (tickCount % 20 == 10) {
                applyRegen();
            }

            if (tickCount % 5 == 0) {
                tickBossBar();
            }

            if (tickCount % 10 == 5 && cfgPotionSelfEnabled && !cfgPotionSelf.isEmpty()) {
                tickPotionUse();
            }

            // Каждый тик, как ваниль-нежить: isSunBurnTick вероятностный (~4%/тик), при проверке раз
            // в секунду NPC загорался бы в среднем через ~25 с — казалось, что не работает.
            if (cfgBurnInSun) {
                tickSunBurn();
            }

            if (tickCount % 40 == 30
                    && followMode != com.withouthonor.npcs.common.entity.ai.FollowMode.NONE
                    && getFollowTarget() == null) {
                followTargetId = null;
                followMode = com.withouthonor.npcs.common.entity.ai.FollowMode.NONE;
            }

            if (tickCount % 20 == 17) {
                tickBoatRide();
            }

            if (cfgScheduleGlobal && tickCount % 40 == 13 && scheduleActive()
                    && level().getServer() != null) {
                com.withouthonor.npcs.common.storage.GlobalScheduleManager
                        .get(level().getServer()).update(this);
            }

            if (cfgHoldPosition) {
                net.minecraft.world.entity.LivingEntity holdT = getTarget();
                if (holdT != null && holdT.isAlive()) {
                    double dx = holdT.getX() - getX();
                    double dz = holdT.getZ() - getZ();
                    float desired = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
                    this.yBodyRot = turnTowards(this.yBodyRot, desired, 12.0F);
                }
            }

            if (scheduleYawFrozen) {
                setYRot(0F);
                this.yRotO = 0F;
                this.yBodyRot = 0F;
                this.yBodyRotO = 0F;
                setYHeadRot(0F);
                this.yHeadRotO = 0F;
            }
        }
    }

    /** Поездка в лодке вместе с игроком (v1): садимся к сопровождаемому, выходим следом. */
    private void tickBoatRide() {
        Player p = followedPlayer();
        if (isPassenger()) {
            if (getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat
                    && (!cfgBoatRide || p == null || p.getVehicle() != getVehicle())) {
                stopRiding();
            }
            return;
        }
        if (!cfgBoatRide || p == null) {
            return;
        }
        if (p.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat boat
                && boat.getPassengers().size() < 2
                && distanceToSqr(boat) < 64.0D) {
            startRiding(boat);
        }
    }

    /** Игрок, за которым NPC идёт прямо сейчас: разовое следование (диалог)
     *  или постоянное «за игроком» (AutoFollowGoal, только когда разовое неактивно). */
    @javax.annotation.Nullable
    private Player followedPlayer() {
        if (followMode == com.withouthonor.npcs.common.entity.ai.FollowMode.FOLLOW) {
            return getFollowTarget();
        }
        if (followMode == com.withouthonor.npcs.common.entity.ai.FollowMode.NONE
                && "player".equals(cfgAutoFollowMode) && !cfgAutoFollowTarget.isBlank()
                && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            // Резолвим так же, как AutoFollowGoal.resolveTarget: по нику, живой, в том же мире
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayerByName(cfgAutoFollowTarget);
            if (sp != null && sp.isAlive() && sp.level() == level()) {
                return sp;
            }
        }
        return null;
    }

    // --- Отложенные действия диалога (действие «Ждать», #2) ---
    private static final class PendingActions {
        final java.util.List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> actions;
        final java.util.UUID playerUuid;
        int ticksLeft;
        PendingActions(java.util.List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> a,
                       java.util.UUID uuid, int ticks) {
            this.actions = a;
            this.playerUuid = uuid;
            this.ticksLeft = ticks;
        }
    }
    /** Транзиент (НЕ переживает рестарт мира), кап 8: остаток списка после wait, ждёт таймера. */
    private final java.util.List<PendingActions> pendingActions = new java.util.ArrayList<>();

    /** Поставить остаток действий на отложенный запуск (зовётся из DialogueAction.executeFrom). */
    public void queueDelayedActions(
            java.util.List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> actions,
            java.util.UUID playerUuid, int ticks) {
        if (actions.isEmpty()) {
            return;
        }
        if (pendingActions.size() >= 8) {
            com.withouthonor.npcs.WHCompanions.LOGGER.warn(
                    "Очередь wait-действий NPC '{}' переполнена (кап 8) — старейшая запись отброшена",
                    getName().getString());
            pendingActions.remove(0); // переполнение — роняем старейшую
        }
        pendingActions.add(new PendingActions(actions, playerUuid, ticks));
    }

    /** Тик отложенных действий: по нулю таймера — резолв игрока и исполнение остатка. */
    private void tickPendingActions() {
        net.minecraft.server.MinecraftServer server = level().getServer();
        if (pendingActions.isEmpty() || server == null) {
            return;
        }
        // Два прохода: сначала снимаем созревшие из списка, ПОТОМ исполняем — иначе цепочка
        // wait→…→wait добавит новую запись через executeFrom прямо во время итерации (CME).
        java.util.List<PendingActions> ready = null;
        java.util.Iterator<PendingActions> it = pendingActions.iterator();
        while (it.hasNext()) {
            PendingActions p = it.next();
            if (--p.ticksLeft > 0) {
                continue;
            }
            it.remove();
            if (ready == null) {
                ready = new java.util.ArrayList<>();
            }
            ready.add(p);
        }
        if (ready == null) {
            return;
        }
        for (PendingActions p : ready) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.playerUuid);
            if (player == null || !player.isAlive() || player.level() != level() || !isAlive()) {
                continue; // игрок офлайн/в другом мире/мёртв или NPC мёртв — тихий сброс остатка
            }
            com.withouthonor.npcs.common.dialogue.action.DialogueAction.executeFrom(p.actions, 0,
                    new com.withouthonor.npcs.common.dialogue.condition.DialogueCondition.Context(player, this));
        }
    }

    /** Сопровождаемый игрок (оба пути следования) — для гейтов дружественного огня/провокации. */
    @javax.annotation.Nullable
    public Player escortedPlayer() {
        return followedPlayer();
    }

    /** #6: настройка «напарник не ранит сопровождаемого» (кэш профиля). */
    public boolean escortNoHarm() {
        return cfgEscortNoHarm;
    }

    /** #6: разрешён ли урон по своей фракции. Фракции нет/не найдена → true (гейт неприменим). */
    public boolean factionAllowsFriendlyFire() {
        if (profileId == null) {
            return true;
        }
        CompanionProfile p = ProfileManager.get().get(profileId);
        if (p == null || p.getFaction() == null) {
            return true;
        }
        var f = com.withouthonor.npcs.common.reputation.FactionRegistry.get().byId(p.getFaction());
        return f == null || f.isFriendlyFire();
    }

    private static float turnTowards(float from, float to, float maxDelta) {
        float d = net.minecraft.util.Mth.wrapDegrees(to - from);
        d = net.minecraft.util.Mth.clamp(d, -maxDelta, maxDelta);
        return from + d;
    }

    private void applyRegen() {
        if (!isAlive() || getHealth() >= getMaxHealth()) {
            return;
        }
        CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
        if (profile != null && profile.getRegenPerSecond() > 0.0F) {
            heal(profile.getRegenPerSecond());
        }
    }

    public java.util.List<net.minecraft.world.item.ItemStack> getPotionBelt() {
        return cfgPotionSelf;
    }

    private static java.util.List<net.minecraft.world.item.ItemStack> potionStacks(
            java.util.List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> specs) {
        return specs.stream()
                .map(com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec::toStack)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public boolean isWitch() {
        return cfgIsWitch;
    }

    private void tickPotionUse() {

        float fireMalus = hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)
                ? 0.0F : (cfgAvoidDanger ? 16.0F : 0.0F);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_FIRE, fireMalus);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_FIRE, fireMalus);
        if (potionCooldown > 0) {
            potionCooldown--;
            return;
        }
        if (cfgPotionSelfCombat && getTarget() == null) {
            return;
        }
        var fr = net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE;
        var wb = net.minecraft.world.effect.MobEffects.WATER_BREATHING;
        if ((isOnFire() || isInLava()) && !hasEffect(fr) && drinkForEffect(fr)) {
            return;
        }

        if (cfgMobType != net.minecraft.world.entity.MobType.UNDEAD
                && getHealth() < getMaxHealth() * 0.4F
                && (drinkForEffect(net.minecraft.world.effect.MobEffects.HEAL)
                || drinkForEffect(net.minecraft.world.effect.MobEffects.REGENERATION))) {
            return;
        }
        if (isInWater() && getAirSupply() < 80 && !hasEffect(wb) && drinkForEffect(wb)) {
            return;
        }
        if (getTarget() != null
                && !hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)
                && !hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)
                && (drinkForEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)
                || drinkForEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST))) {
            return;
        }
    }

    private boolean drinkForEffect(net.minecraft.world.effect.MobEffect target) {
        for (net.minecraft.world.item.ItemStack potion : cfgPotionSelf) {
            var effects = net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(potion);
            boolean match = false;
            for (var eff : effects) {
                if (eff.getEffect() == target) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            for (var eff : effects) {
                addEffect(new net.minecraft.world.effect.MobEffectInstance(eff));
            }
            level().playSound(null, getX(), getY(), getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_DRINK, getSoundSource(), 1.0F, 1.0F);
            if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                        getX(), getY() + 1.2D, getZ(), 6, 0.2D, 0.3D, 0.2D, 0.0D);
            }
            potionCooldown = 100;
            return true;
        }
        return false;
    }

    public void throwSplashAt(net.minecraft.world.entity.LivingEntity target,
                              net.minecraft.world.item.ItemStack splash) {
        net.minecraft.world.entity.projectile.ThrownPotion thrown =
                new net.minecraft.world.entity.projectile.ThrownPotion(level(), this);
        thrown.setItem(splash);
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - 1.1D - thrown.getY();
        double dz = target.getZ() - getZ();
        double dh = Math.sqrt(dx * dx + dz * dz);
        thrown.shoot(dx, dy + dh * 0.2D, dz, 0.75F, 8.0F);
        playSound(net.minecraft.sounds.SoundEvents.SPLASH_POTION_THROW, 1.0F,
                0.8F + getRandom().nextFloat() * 0.4F);
        level().addFreshEntity(thrown);
    }

    public net.minecraft.world.item.ItemStack harmfulBeltPotion() {
        if (cfgPotionEnemyEnabled && !cfgPotionEnemy.isEmpty()) {
            int i = cfgPotionEnemySeq
                    ? enemySeqIdx++ % cfgPotionEnemy.size()
                    : getRandom().nextInt(cfgPotionEnemy.size());
            return asSplash(cfgPotionEnemy.get(i));
        }
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPLASH_POTION),
                net.minecraft.world.item.alchemy.Potions.POISON);
    }

    public net.minecraft.world.item.ItemStack beneficialBeltPotion() {
        if (cfgPotionAllyEnabled && !cfgPotionAlly.isEmpty()) {
            int i = cfgPotionAllySeq
                    ? allySeqIdx++ % cfgPotionAlly.size()
                    : getRandom().nextInt(cfgPotionAlly.size());
            return asSplash(cfgPotionAlly.get(i));
        }
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPLASH_POTION),
                net.minecraft.world.item.alchemy.Potions.HEALING);
    }

    private static net.minecraft.world.item.ItemStack asSplash(net.minecraft.world.item.ItemStack p) {
        if (p.is(net.minecraft.world.item.Items.SPLASH_POTION)
                || p.is(net.minecraft.world.item.Items.LINGERING_POTION)) {
            return p.copy();
        }
        net.minecraft.world.item.ItemStack splash =
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPLASH_POTION);
        if (p.getTag() != null) {
            splash.setTag(p.getTag().copy());
        }
        return splash;
    }

    private long nextAmbientAt;

    private void tryAmbientBubble() {
        CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
        if (profile == null || profile.getAmbientPhrases().isEmpty()) {
            return;
        }
        boolean bubbles = profile.isBubblesEnabled();
        boolean chat = profile.isBubblesToChat();
        if (!bubbles && !chat) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAmbientAt) {
            return;
        }
        Player nearest = level().getNearestPlayer(this, profile.getAmbientRadius());
        if (nearest == null) {
            return;
        }
        String text = profile.getAmbientPhrases()
                .get(this.random.nextInt(profile.getAmbientPhrases().size()));
        if (nearest instanceof ServerPlayer serverPlayer) {
            text = com.withouthonor.npcs.common.dialogue.Placeholders.apply(text,
                    new com.withouthonor.npcs.common.dialogue.condition.DialogueCondition.Context(
                            serverPlayer, this));
        }
        if (bubbles) {
            sendBubble(text);
        }
        if (chat) {
            broadcastPhraseChat(text);
        }
        long cooldownMs = profile.getAmbientCooldownSeconds() * 1000L;
        nextAmbientAt = now + cooldownMs + this.random.nextInt((int) (cooldownMs / 2) + 1);
    }

    // КД реплик/эмоций диалогов: wall-clock, не сохраняется — как nextAmbientAt.
    private final java.util.Map<String, Long> sayCooldowns = new java.util.HashMap<>();

    /** true — КД по ключу истёк (и ставит новый); false — ещё не время. */
    public boolean trySayCooldown(String key, long cdMs) {
        long now = System.currentTimeMillis();
        Long expiry = sayCooldowns.get(key);
        if (expiry != null && now < expiry) {
            return false;
        }
        if (sayCooldowns.size() > 256) {
            // Не даём карте расти бесконечно: сперва чистим истёкшие записи.
            sayCooldowns.values().removeIf(v -> now >= v);
        }
        sayCooldowns.put(key, now + cdMs);
        return true;
    }

    private int nextCombatPhraseAt;

    private void speakFromPool(CompanionProfile profile, java.util.List<String> pool,
                              @Nullable ServerPlayer ctxPlayer) {
        boolean bubbles = profile.isBubblesEnabled();
        boolean chat = profile.isBubblesToChat();
        if ((!bubbles && !chat) || pool.isEmpty()) {
            return;
        }
        String text = pool.get(this.random.nextInt(pool.size()));
        if (ctxPlayer != null) {
            text = com.withouthonor.npcs.common.dialogue.Placeholders.apply(text,
                    new com.withouthonor.npcs.common.dialogue.condition.DialogueCondition.Context(
                            ctxPlayer, this));
        }
        if (bubbles) {
            sendBubble(text);
        }
        if (chat) {
            broadcastPhraseChat(text);
        }
    }

    public void broadcastPhraseChat(String text) {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal(
                        "<" + getName().getString() + "> ")
                .withStyle(net.minecraft.ChatFormatting.GRAY)
                .append(net.minecraft.network.chat.Component.literal(text));
        double r2 = 24.0 * 24.0;
        for (ServerPlayer p : sl.players()) {
            if (distanceToSqr(p) <= r2) {
                p.sendSystemMessage(msg);
            }
        }
    }

    @Nullable
    private ServerPlayer nearestServerPlayer(double radius) {
        Player p = level().getNearestPlayer(this, radius);
        return p instanceof ServerPlayer sp ? sp : null;
    }

    private void runReactions(java.util.List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> actions,
                              @Nullable ServerPlayer player) {
        if (actions.isEmpty() || player == null) {
            return;
        }
        com.withouthonor.npcs.common.dialogue.action.DialogueAction.executeAll(actions,
                new com.withouthonor.npcs.common.dialogue.condition.DialogueCondition.Context(player, this));
    }

    private void tickApproachReaction() {
        if (cfgApproachRange <= 0 || cfgOnApproach.isEmpty()
                || !(level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        double r2 = (double) cfgApproachRange * cfgApproachRange;
        java.util.Set<java.util.UUID> now = new java.util.HashSet<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && distanceToSqr(p) <= r2) {
                now.add(p.getUUID());
                if (!reactInRange.contains(p.getUUID())) {
                    runReactions(cfgOnApproach, p);
                }
            }
        }
        reactInRange.retainAll(now);
        reactInRange.addAll(now);
    }

    @Override
    public void setTarget(@Nullable net.minecraft.world.entity.LivingEntity target) {
        boolean wasNull = getTarget() == null;
        super.setTarget(target);
        setAggressive(target != null);
        if (!level().isClientSide && target != null && wasNull) {

            if (!cfgCombatBuff.isEmpty()) {
                drinkBuff(cfgCombatBuff.get(0));
            }
            if (tickCount >= nextCombatPhraseAt) {
                CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
                if (profile != null && !profile.getCombatPhrases().isEmpty()) {
                    speakFromPool(profile, profile.getCombatPhrases(), nearestServerPlayer(16));
                    nextCombatPhraseAt = tickCount + 100;
                }
            }
        }
    }

    private void drinkBuff(net.minecraft.world.item.ItemStack potion) {
        for (var eff : net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(potion)) {
            addEffect(new net.minecraft.world.effect.MobEffectInstance(eff));
        }
        level().playSound(null, getX(), getY(), getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_DRINK, getSoundSource(), 1.0F, 1.0F);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                    getX(), getY() + 1.2D, getZ(), 6, 0.2D, 0.3D, 0.2D, 0.0D);
        }
    }

    @Override
    public void awardKillScore(net.minecraft.world.entity.Entity killed, int score,
                               net.minecraft.world.damagesource.DamageSource source) {
        super.awardKillScore(killed, score, source);
        if (!level().isClientSide) {
            CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
            if (profile != null && !profile.getKillPhrases().isEmpty()) {
                speakFromPool(profile, profile.getKillPhrases(), nearestServerPlayer(16));
            }
        }
    }

    public void sendBubble(String text) {
        int duration = Math.max(60, Math.min(200, 40 + text.length() * 2));
        com.withouthonor.npcs.network.NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new com.withouthonor.npcs.network.SpeechBubblePacket(getId(), text, duration));
    }

    public void sendEmote(com.withouthonor.npcs.common.dialogue.EmoteIcon icon) {
        com.withouthonor.npcs.network.NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new com.withouthonor.npcs.network.EmotePacket(getId(), icon.atlasIndex(), 50));
    }

    public void sendEmotecraftEmote(String emoteId) {
        sendEmotecraftEmote(emoteId, "", "");
    }

    public void sendEmotecraftEmote(String emoteId, String emoteName, String emoteAuthor) {
        this.entityData.set(DATA_EMOTECRAFT_EMOTE, packEmote(emoteId, emoteName, emoteAuthor));
    }

    public void sendEmotecraftEmote(String emoteId, String emoteName, String emoteAuthor, int nonce) {
        this.entityData.set(DATA_EMOTECRAFT_EMOTE, packEmote(emoteId, emoteName, emoteAuthor, nonce));
    }

    // Счётчик nonce диалоговых эмоций: делает строку уникальной для перезапуска.
    private int dialogueEmoteNonce;

    public int nextDialogueEmoteNonce() {
        if (++dialogueEmoteNonce == 0) {
            dialogueEmoteNonce = 1;
        }
        return dialogueEmoteNonce;
    }

    private static final char EMOTE_SEP = (char) 31;

    public static String packEmote(String id, String name, String author) {
        return packEmote(id, name, author, 0);
    }

    public static String packEmote(String id, String name, String author, int nonce) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String n = name == null ? "" : name;
        String a = author == null ? "" : author;
        if (nonce != 0) {
            // 4-й сегмент клиент игнорирует: reconcile читает только parts[0..2],
            // но смена nonce меняет строку целиком и заставляет проиграть заново.
            return id + EMOTE_SEP + n + EMOTE_SEP + a + EMOTE_SEP + nonce;
        }
        if (n.isEmpty() && a.isEmpty()) {
            return id;
        }
        return id + EMOTE_SEP + n + EMOTE_SEP + a;
    }

    private final java.util.Map<java.util.UUID, Byte> lastIndicator = new java.util.HashMap<>();

    private void updateIndicators() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
        boolean active = profile != null && profile.isIndicatorsEnabled() && profile.hasIndicatorEntry();
        if (!active) {

            if (!lastIndicator.isEmpty()) {
                for (java.util.Map.Entry<java.util.UUID, Byte> e : lastIndicator.entrySet()) {
                    if (e.getValue() != 0) {
                        ServerPlayer sp = serverLevel.getServer().getPlayerList().getPlayer(e.getKey());
                        if (sp != null) {
                            com.withouthonor.npcs.network.NetworkHandler.sendToPlayer(
                                    new com.withouthonor.npcs.network.IndicatorPacket(getId(), 0), sp);
                        }
                    }
                }
                lastIndicator.clear();
            }
            return;
        }
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(16.0D))) {
            if (!(p instanceof ServerPlayer sp)) {
                continue;
            }
            seen.add(sp.getUUID());
            byte ind = active ? computeIndicatorFor(sp, profile) : 0;
            Byte last = lastIndicator.get(sp.getUUID());
            boolean changed = last == null ? ind != 0 : last != ind;
            if (changed) {
                lastIndicator.put(sp.getUUID(), ind);
                com.withouthonor.npcs.network.NetworkHandler.sendToPlayer(
                        new com.withouthonor.npcs.network.IndicatorPacket(getId(), ind), sp);
            }
        }

        java.util.Iterator<java.util.Map.Entry<java.util.UUID, Byte>> it = lastIndicator.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<java.util.UUID, Byte> e = it.next();
            if (!seen.contains(e.getKey())) {
                if (e.getValue() != 0) {
                    ServerPlayer sp = serverLevel.getServer().getPlayerList().getPlayer(e.getKey());
                    if (sp != null) {
                        com.withouthonor.npcs.network.NetworkHandler.sendToPlayer(
                                new com.withouthonor.npcs.network.IndicatorPacket(getId(), 0), sp);
                    }
                }
                it.remove();
            }
        }
    }

    private byte computeIndicatorFor(ServerPlayer player, CompanionProfile profile) {
        var ctx = new com.withouthonor.npcs.common.dialogue.condition.DialogueCondition.Context(player, this);
        for (com.withouthonor.npcs.common.dialogue.EntryPoint entry : profile.getEntryPoints()) {
            if (entry.matches(ctx)) {
                com.withouthonor.npcs.common.dialogue.EmoteIcon ind = entry.getIndicator();
                return ind == null ? 0 : (byte) (ind.atlasIndex() + 1);
            }
        }
        return 0;
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (!level().isClientSide && com.withouthonor.npcs.compat.Compat.ironsSpellsLoaded()) {
            // Иначе CONTINUOUS-каст остаётся «висеть»: onServerCastComplete(cancelled) не отработает.
            com.withouthonor.npcs.compat.Compat.ironsSpells().cancel(this);
        }
        // Н6: при финальном die() после инсценировки тотем-ветку осознанно пропускаем — даже если
        // профиль перезарядили во время сцены. Сцена смерти уже сыграна, спасать поздно: иначе
        // NPC останется «замороженным» в staged-состоянии при живом тотеме.
        if (!level().isClientSide && !deathStageFinished && totemUsesLeft > 0
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            totemUsesLeft--;
            updateTotemArmed();
            this.setHealth(1.0F);
            this.removeAllEffects();
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.REGENERATION, 900, 1));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 1));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 800, 0));
            this.level().broadcastEntityEvent(this, (byte) 35);
            return; // тотем спас — очередь wait НЕ чистим, NPC жив
        }
        // #3 Инсценированная смерть: тотем не спас → если задана эмоция смерти, играем её при 1 HP,
        // затем (по таймеру, из tick) финальный die() (deathStageFinished) идёт мимо инсценировки
        // в super.die + хвост. /kill (bypass) добивает сразу. Гейт Emotecraft — без мода ветка пропускается.
        if (!level().isClientSide && !deathStaged && !deathStageFinished && !cfgDeathEmoteId.isEmpty()
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && com.withouthonor.npcs.compat.Compat.emotecraftLoaded()) {
            deathStaged = true;
            deathStageTicks = Math.max(1, Math.round(cfgDeathEmoteSecs * 20.0F));
            deathStageSource = source;
            setHealth(1.0F);
            setTarget(null);
            getNavigation().stop();
            sendEmotecraftEmote(cfgDeathEmoteId, cfgDeathEmoteName, cfgDeathEmoteAuthor,
                    nextDialogueEmoteNonce());
            return; // до super.die — NPC ещё жив, играет эмоцию
        }
        // Настоящая смерть: отложенные действия wait умирают вместе с NPC (мёртвый не тикает).
        pendingActions.clear();
        // Закрываем сцену жёстко: если сюда попали bypass-ударом (/kill) посреди инсценировки,
        // таймер в tick() не должен потом снова войти в staged-блок и повторно позвать die().
        deathStaged = false;
        deathStageFinished = true;
        // Curios: снять в буфер и очистить слоты ДО super.die(). Curios API на LivingDropsEvent
        // (внутри super.die) увидит пустые слоты и ничего не уронит; Graveyard возьмёт из буфера —
        // при респавне снаряжение вернётся, симметрично setDropChance=0 у обычной экипировки.
        if (!level().isClientSide && com.withouthonor.npcs.compat.Compat.curiosLoaded()) {
            deathCuriosSnapshot = curiosToTag();
            com.withouthonor.npcs.compat.Compat.curios().resetCurios(this);
        }
        super.die(source); // при повторном вызове ваниль сама no-op (this.dead)
        if (level().isClientSide) {
            return;
        }
        // Посмертный хвост (фразы/react_death/репутация/дропы/Graveyard) — строго один раз,
        // даже если die() позовут повторно уже по мёртвому NPC.
        if (deathTailDone) {
            return;
        }
        deathTailDone = true;
        CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;

        if (profile != null && !profile.getDeathPhrases().isEmpty()) {
            speakFromPool(profile, profile.getDeathPhrases(),
                    source.getEntity() instanceof ServerPlayer sp ? sp : null);
        }

        if (!cfgOnDeath.isEmpty()) {
            runReactions(cfgOnDeath, source.getEntity() instanceof ServerPlayer sp ? sp : nearestServerPlayer(16));
        }

        if (source.getEntity() instanceof ServerPlayer killer && profile != null && profile.getFaction() != null) {
            var faction = com.withouthonor.npcs.common.reputation.FactionRegistry.get()
                    .byId(profile.getFaction());
            if (faction != null && faction.getKillPenalty() > 0) {
                int value = com.withouthonor.npcs.common.storage.PlayerStateManager.get(killer.server)
                        .addReputation(killer.getUUID(), faction.getId(), -faction.getKillPenalty());
                killer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "wh_npcs.msg.reputation_penalty", faction.getName(),
                        faction.getKillPenalty(), value, faction.tierFor(value).name())
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }

        if (profile != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (CompanionProfile.DropEntry drop : profile.getDrops()) {
                if (drop.chancePercent() > 0 && this.random.nextInt(100) < drop.chancePercent()) {
                    spawnAtLocation(drop.item().toStack());
                }
            }
            if (profile.getDeathXpMax() > 0) {
                net.minecraft.world.entity.ExperienceOrb.award(serverLevel, position(),
                        net.minecraft.util.Mth.nextInt(this.random,
                                profile.getDeathXpMin(), profile.getDeathXpMax()));
            }
        }

        MinecraftServer server = level().getServer();
        if (server != null) {
            com.withouthonor.npcs.common.storage.Graveyard.get(server).record(this, profile);
        }
    }

    @Override
    public double getMyRidingOffset() {
        // Как у игрока (−0.35): иначе в лодке NPC сидит заметно выше соседа-игрока.
        return getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat ? -0.35D
                : super.getMyRidingOffset();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            if (aggroCounted) {
                com.withouthonor.npcs.common.entity.ai.CreatureAggroState.update(true, false);
                aggroCounted = false;
            }
            if (bossBar != null) {
                bossBar.removeAllPlayers();
                bossBar = null;
            }
            if (reason.shouldDestroy()) {
                MinecraftServer server = level().getServer();
                if (server != null) {
                    CompanionIndex.get(server).remove(getUUID());
                    com.withouthonor.npcs.common.storage.GlobalScheduleManager.get(server).forget(server, getUUID());
                    if (transientProfile && profileId != null) {
                        ProfileManager.get().delete(profileId);
                    }
                }
            }
        }
        super.remove(reason);
    }

    public void updateIndex() {
        MinecraftServer server = level().getServer();
        if (server == null) {
            return;
        }
        CompanionProfile profile = profileId != null ? ProfileManager.get().get(profileId) : null;
        String name = profile != null ? profile.getName() : getName().getString();

        String stripped = net.minecraft.ChatFormatting.stripFormatting(name);
        CompanionIndex.get(server).update(new CompanionIndex.Entry(
                getUUID(), stripped != null ? stripped : name, level().dimension(), blockPosition(), profileId));
    }
}