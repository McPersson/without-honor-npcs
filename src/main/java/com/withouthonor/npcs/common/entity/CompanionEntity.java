package com.withouthonor.npcs.common.entity;

import com.withouthonor.npcs.common.dialogue.DialogueRuntime;
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

    private boolean cfgIdleLook;
    private boolean cfgIdleWander;
    private int cfgIdleWanderRadius;
    private boolean cfgPanic;
    private boolean cfgPursueAttacker = true;
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
        rebuildCombatGoals(profile);
    }

    private void rebuildCombatGoals(CompanionProfile profile) {
        if (level().isClientSide) {
            return;
        }
        cacheFollowSettings(profile);

        double followRange = cfgAggroRange > 0 ? cfgAggroRange : 16.0D;
        if ("bow".equals(profile.getCombatPreset())) {
            followRange = Math.max(followRange, profile.getRangedRange() + 8.0F);
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
            } else if (weapon instanceof net.minecraft.world.item.TridentItem) {
                this.goalSelector.addGoal(2, new com.withouthonor.npcs.common.entity.ai.NpcTridentAttackGoal(
                        this, 1.0D, interval, range));
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

        if (ef) {
            com.withouthonor.npcs.compat.epicfight.EpicFightCompat.installMobCombat(this);
        }
    }

    private void addMeleeGoals(CompanionProfile profile) {
        if (cfgLeap) {
            this.goalSelector.addGoal(2,
                    new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(this, profile.getLeapStrength()));
        }
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(
                this, profile.getMeleeChaseSpeed(), false));
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
            double dy = target.getY(0.3333333333333333D) - trident.getY();
            trident.shoot(dx, dy + horizontal * 0.2D, dz, 1.6F, 4.0F);
            playSound(net.minecraft.sounds.SoundEvents.DROWNED_SHOOT, 1.0F,
                    1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
            level().addFreshEntity(trident);
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
            this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.PanicGoal(this, 1.5D));
        }
        if (cfgAvoidSun) {
            this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.RestrictSunGoal(this));
            this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.FleeSunGoal(this, 1.0D));
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
        if (cfgIdleLook) {
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
        cfgIdleLook = profile.isIdleLook();
        cfgPursueAttacker = profile.isPursueAttacker();
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
        cfgMobType = switch (profile.getMobType()) {
            case "undead" -> net.minecraft.world.entity.MobType.UNDEAD;
            case "arthropod" -> net.minecraft.world.entity.MobType.ARTHROPOD;
            case "water" -> net.minecraft.world.entity.MobType.WATER;
            case "illager" -> net.minecraft.world.entity.MobType.ILLAGER;
            default -> net.minecraft.world.entity.MobType.UNDEFINED;
        };
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
        if (damaged && !level().isClientSide && !cfgOnHurt.isEmpty()) {
            runReactions(cfgOnHurt, source.getEntity() instanceof ServerPlayer sp ? sp : nearestServerPlayer(16));
        }
        if (damaged && !level().isClientSide && cfgPursueAttacker && getTarget() == null
                && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker != this && isConfiguredAggressorTarget(attacker)) {
            double follow = getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
            if (distanceToSqr(attacker) > follow * follow) {
                setPursuitTarget(attacker);
            }
        }
        return damaged;
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
        String v = packEmote(id, name, author);
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

            if (tickCount % 200 == 0) {
                updateIndex();
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

            if (cfgBurnInSun && tickCount % 20 == 15) {
                tickSunBurn();
            }

            if (tickCount % 40 == 30
                    && followMode != com.withouthonor.npcs.common.entity.ai.FollowMode.NONE
                    && getFollowTarget() == null) {
                followTargetId = null;
                followMode = com.withouthonor.npcs.common.entity.ai.FollowMode.NONE;
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

    private void broadcastPhraseChat(String text) {
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

    private static final char EMOTE_SEP = (char) 31;

    public static String packEmote(String id, String name, String author) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String n = name == null ? "" : name;
        String a = author == null ? "" : author;
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
        if (!level().isClientSide && totemUsesLeft > 0
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
            return;
        }
        super.die(source);
        if (level().isClientSide) {
            return;
        }
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
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
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