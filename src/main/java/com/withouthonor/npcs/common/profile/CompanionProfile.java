package com.withouthonor.npcs.common.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.withouthonor.npcs.common.dialogue.EntryPoint;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompanionProfile {

    private final UUID id;
    private String name = "Companion";
    private String title = "";

    private boolean showTitle;

    @Nullable
    private String skinPlayerName;

    @Nullable
    private String nameColor;

    @Nullable
    private String disguise;

    @Nullable
    private String voiceSound;

    private String portraitImage = "";

    private boolean portraitShow;

    @Nullable
    private String faction;

    private float voicePitch = 1.0F;

    private final List<EntryPoint> entryPoints = new ArrayList<>();

    private final List<com.withouthonor.npcs.common.trade.TradeOffer> offers = new ArrayList<>();

    private int restockMinutes;

    public record DropEntry(com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec item,
                            int chancePercent) {

        public static DropEntry fromJson(JsonObject json) {
            return new DropEntry(
                    com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec
                            .fromJson(json.getAsJsonObject("item")),
                    json.has("chance") ? json.get("chance").getAsInt() : 100);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.add("item", item.toJson());
            json.addProperty("chance", chancePercent);
            return json;
        }
    }

    private boolean attackable;
    private float maxHealth = 20.0F;
    private float attackDamage = 3.0F;
    private float armor;
    private float kbResistance;

    private int resMelee, resProjectile, resExplosion, resFire, resFall, resMagic;
    private float moveSpeed = 0.25F;

    private String combatPreset = "passive";
    private String combatSystem = "vanilla";

    private boolean guardSpareCreepers;

    private String aggressorTargets = "";

    private boolean bossbarEnabled;
    private String bossbarColor = "red";
    private int bossbarRadius = 32;

    private String deathBehavior = "respawn";
    private int respawnSeconds = 30;

    private String respawnLocation = "death";
    private int respawnHomeX;
    private int respawnHomeY;
    private int respawnHomeZ;

    private String respawnHomeDim = "";

    private int respawnSecondsMax;

    private int respawnMax;
    private int deathXpMin;
    private int deathXpMax;
    private final List<DropEntry> drops = new ArrayList<>();

    private final List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> potionSelf =
            new ArrayList<>();
    private final List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> potionEnemy =
            new ArrayList<>();
    private final List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> potionAlly =
            new ArrayList<>();
    private boolean potionSelfEnabled = true, potionSelfCombat;
    private boolean potionEnemyEnabled = true, potionEnemyCombat;
    private boolean potionAllyEnabled = true, potionAllyCombat;

    private boolean potionEnemySeq, potionAllySeq;

    private final List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> potionCombatBuff =
            new ArrayList<>();

    private boolean bubblesEnabled = true;

    private boolean indicatorsEnabled = false;

    private final List<String> ambientPhrases = new ArrayList<>();

    private final List<String> combatPhrases = new ArrayList<>();

    private final List<String> interactPhrases = new ArrayList<>();

    private final List<String> deathPhrases = new ArrayList<>();

    private final List<String> killPhrases = new ArrayList<>();

    private boolean bubblesToChat;

    private final List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> onHurt = new ArrayList<>();
    private final List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> onDeath = new ArrayList<>();
    private final List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> onInteract = new ArrayList<>();
    private final List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> onApproach = new ArrayList<>();

    private int approachRange;

    private int ambientRadius = 8;

    private int ambientCooldownSeconds = 25;

    private float rangedIntervalSeconds = 1.25F;

    private float rangedRange = 16.0F;

    private float potionIntervalSeconds = 3.0F;
    private float potionRange = 10.0F;

    private float meleeChaseSpeed = 1.2F;

    private float leapStrength = 0.4F;

    private float regenPerSecond;

    private boolean followTeleport = true;

    private boolean followTeleportOutOfSight = true;

    private boolean followRun = true;

    private boolean followMatchSpeed = true;

    private String followDistance = "normal";

    private boolean openDoors = true;

    private boolean avoidDanger = true;

    private boolean teleportFx = true;

    private boolean groupSpacing = true;

    private final java.util.List<ScheduleEntry> schedule = new java.util.ArrayList<>();
    private boolean scheduleEnabled;
    private boolean scheduleGlobal;

    /** Режим взгляда: "off" / "cold" (ваниль LookAt) / "lively" (следит корпусом). */
    private String lookMode = "cold";

    /** Радиус «живого» взгляда в блоках (1..16). */
    private int lookRadius = 3;

    private boolean boatRide = true;

    private boolean idleWander;

    private int idleWanderRadius;

    private boolean panicWhenHurt;

    private boolean pursueAttacker = true;

    private boolean holdPosition;

    private boolean avoidSun;

    private boolean burnInSun;

    private boolean pushable;

    private boolean passable;

    private int totemCharges;

    private boolean totemRender;

    private float shieldHoldSeconds = 1.5F;

    private float shieldCooldownSeconds = 2.0F;

    private String idleEmoteId = "";
    private String idleEmoteName = "";
    private String idleEmoteAuthor = "";

    private String mobType = "undefined";

    private boolean fallDamage = true;

    private boolean webSlow = true;

    private boolean canDrown = true;

    private boolean poisonImmune;

    private int aggroRange;

    private boolean leapAtTarget;

    private float rotX, rotY, rotZ;
    private float posX, posY, posZ;
    private float scaleX = 1.0F, scaleY = 1.0F, scaleZ = 1.0F;

    private final PoseJson.Pose pose = new PoseJson.Pose();

    private String autoFollowMode = "none";

    private String autoFollowTarget = "";

    public CompanionProfile(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    @Nullable
    public String getSkinPlayerName() {
        return skinPlayerName;
    }

    public void setSkinPlayerName(@Nullable String skinPlayerName) {
        this.skinPlayerName = skinPlayerName;
    }

    @Nullable
    public String getNameColor() {
        return nameColor;
    }

    public void setNameColor(@Nullable String nameColor) {
        this.nameColor = nameColor;
    }

    @Nullable
    public String getDisguise() {
        return disguise;
    }

    public void setDisguise(@Nullable String disguise) {
        this.disguise = disguise;
    }

    @Nullable
    public String getVoiceSound() {
        return voiceSound;
    }

    public void setVoiceSound(@Nullable String voiceSound) {
        this.voiceSound = voiceSound;
    }

    public String getPortraitImage() {
        return portraitImage;
    }

    public void setPortraitImage(String portraitImage) {
        this.portraitImage = portraitImage == null ? "" : portraitImage;
    }

    public boolean isPortraitShow() {
        return portraitShow;
    }

    public void setPortraitShow(boolean portraitShow) {
        this.portraitShow = portraitShow;
    }

    public float getVoicePitch() {
        return voicePitch;
    }

    public void setVoicePitch(float voicePitch) {
        this.voicePitch = Math.max(0.5F, Math.min(2.0F, voicePitch));
    }

    @Nullable
    public String getFaction() {
        return faction;
    }

    public void setFaction(@Nullable String faction) {
        this.faction = faction;
    }

    public List<EntryPoint> getEntryPoints() {
        return entryPoints;
    }

    public List<com.withouthonor.npcs.common.trade.TradeOffer> getOffers() {
        return offers;
    }

    public int getRestockMinutes() {
        return restockMinutes;
    }

    public void setRestockMinutes(int restockMinutes) {
        this.restockMinutes = Math.max(0, restockMinutes);
    }

    public boolean isAttackable() {
        return attackable;
    }

    public void setAttackable(boolean attackable) {
        this.attackable = attackable;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = Math.max(1.0F, Math.min(1024.0F, maxHealth));
    }

    public float getAttackDamage() {
        return attackDamage;
    }

    public void setAttackDamage(float attackDamage) {
        this.attackDamage = Math.max(0.0F, Math.min(1024.0F, attackDamage));
    }

    public float getArmor() {
        return armor;
    }

    public void setArmor(float armor) {
        this.armor = Math.max(0.0F, Math.min(30.0F, armor));
    }

    public float getKbResistance() {
        return kbResistance;
    }

    public void setKbResistance(float kbResistance) {
        this.kbResistance = Math.max(0.0F, Math.min(1.0F, kbResistance));
    }

    public int getResMelee() {
        return resMelee;
    }

    public int getResProjectile() {
        return resProjectile;
    }

    public int getResExplosion() {
        return resExplosion;
    }

    public int getResFire() {
        return resFire;
    }

    public int getResFall() {
        return resFall;
    }

    public int getResMagic() {
        return resMagic;
    }

    private static int clampRes(int v) {
        return Math.max(-100, Math.min(100, v));
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = Math.max(0.0F, Math.min(1.0F, moveSpeed));
    }

    public String getCombatPreset() {
        return combatPreset;
    }

    public void setCombatPreset(String combatPreset) {
        this.combatPreset = combatPreset;
    }

    public String getCombatSystem() {
        return combatSystem;
    }

    public void setCombatSystem(String combatSystem) {
        this.combatSystem = "epicfight".equals(combatSystem) ? "epicfight" : "vanilla";
    }

    public boolean isGuardSpareCreepers() {
        return guardSpareCreepers;
    }

    public String getAggressorTargets() {
        return aggressorTargets;
    }

    public void setAggressorTargets(String aggressorTargets) {
        // Нормализуем в тот же формат, что и fromJson: категории через запятую.
        this.aggressorTargets = String.join(",", parseAggressorTargets(aggressorTargets));
    }

    public boolean isBossbarEnabled() {
        return bossbarEnabled;
    }

    public String getBossbarColor() {
        return bossbarColor;
    }

    public int getBossbarRadius() {
        return bossbarRadius;
    }

    public static java.util.Set<String> parseAggressorTargets(String raw) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        switch (raw) {
            case "faction" -> {
                out.add("players");
                return out;
            }
            case "all_mobs" -> {
                out.add("monsters");
                out.add("animals");
                out.add("villagers");
                out.add("npcs");
                return out;
            }
            case "peaceful" -> {
                out.add("animals");
                out.add("villagers");
                return out;
            }
            default -> {
            }
        }
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    public String getDeathBehavior() {
        return deathBehavior;
    }

    public void setDeathBehavior(String deathBehavior) {
        this.deathBehavior = deathBehavior;
    }

    public int getRespawnSeconds() {
        return respawnSeconds;
    }

    public void setRespawnSeconds(int respawnSeconds) {
        this.respawnSeconds = Math.max(1, respawnSeconds);
    }

    public String getRespawnLocation() {
        return respawnLocation;
    }

    public int getRespawnHomeX() {
        return respawnHomeX;
    }

    public int getRespawnHomeY() {
        return respawnHomeY;
    }

    public int getRespawnHomeZ() {
        return respawnHomeZ;
    }

    public String getRespawnHomeDim() {
        return respawnHomeDim;
    }

    public boolean hasRespawnHome() {
        return !respawnHomeDim.isEmpty();
    }

    public int getRespawnSecondsMax() {
        return respawnSecondsMax;
    }

    public int getRespawnMax() {
        return respawnMax;
    }

    public List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> getPotionSelf() {
        return potionSelf;
    }

    public List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> getPotionEnemy() {
        return potionEnemy;
    }

    public List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> getPotionAlly() {
        return potionAlly;
    }

    public List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> getPotionCombatBuff() {
        return potionCombatBuff;
    }

    public boolean isPotionSelfEnabled() {
        return potionSelfEnabled;
    }

    public boolean isPotionSelfCombat() {
        return potionSelfCombat;
    }

    public boolean isPotionEnemyEnabled() {
        return potionEnemyEnabled;
    }

    public boolean isPotionEnemyCombat() {
        return potionEnemyCombat;
    }

    public boolean isPotionAllyEnabled() {
        return potionAllyEnabled;
    }

    public boolean isPotionEnemySeq() {
        return potionEnemySeq;
    }

    public boolean isPotionAllySeq() {
        return potionAllySeq;
    }

    public boolean isPotionAllyCombat() {
        return potionAllyCombat;
    }

    public int getDeathXpMin() {
        return deathXpMin;
    }

    public int getDeathXpMax() {
        return deathXpMax;
    }

    public void setDeathXp(int min, int max) {
        this.deathXpMin = Math.max(0, min);
        this.deathXpMax = Math.max(this.deathXpMin, max);
    }

    public List<DropEntry> getDrops() {
        return drops;
    }

    public boolean isBubblesEnabled() {
        return bubblesEnabled;
    }

    public void setBubblesEnabled(boolean bubblesEnabled) {
        this.bubblesEnabled = bubblesEnabled;
    }

    public boolean isIndicatorsEnabled() {
        return indicatorsEnabled;
    }

    public void setIndicatorsEnabled(boolean indicatorsEnabled) {
        this.indicatorsEnabled = indicatorsEnabled;
    }

    public boolean hasIndicatorEntry() {
        for (EntryPoint e : entryPoints) {
            if (e.getIndicator() != null) {
                return true;
            }
        }
        return false;
    }

    public List<String> getAmbientPhrases() {
        return ambientPhrases;
    }

    public List<String> getCombatPhrases() {
        return combatPhrases;
    }

    public List<String> getInteractPhrases() {
        return interactPhrases;
    }

    public List<String> getDeathPhrases() {
        return deathPhrases;
    }

    public List<String> getKillPhrases() {
        return killPhrases;
    }

    public boolean isBubblesToChat() {
        return bubblesToChat;
    }

    public List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> getOnHurt() {
        return onHurt;
    }

    public List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> getOnDeath() {
        return onDeath;
    }

    public List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> getOnInteract() {
        return onInteract;
    }

    public List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> getOnApproach() {
        return onApproach;
    }

    public int getApproachRange() {
        return approachRange;
    }

    public int getAmbientRadius() {
        return ambientRadius;
    }

    public void setAmbientRadius(int radius) {
        this.ambientRadius = Math.max(2, Math.min(32, radius));
    }

    public int getAmbientCooldownSeconds() {
        return ambientCooldownSeconds;
    }

    public void setAmbientCooldownSeconds(int seconds) {
        this.ambientCooldownSeconds = Math.max(5, Math.min(3600, seconds));
    }

    public float getRangedIntervalSeconds() {
        return rangedIntervalSeconds;
    }

    public void setRangedIntervalSeconds(float seconds) {
        this.rangedIntervalSeconds = Math.max(0.3F, Math.min(10.0F, seconds));
    }

    public float getRangedRange() {
        return rangedRange;
    }

    public void setRangedRange(float range) {
        this.rangedRange = Math.max(4.0F, Math.min(32.0F, range));
    }

    public float getPotionIntervalSeconds() {
        return potionIntervalSeconds;
    }

    public void setPotionIntervalSeconds(float seconds) {
        this.potionIntervalSeconds = Math.max(0.5F, Math.min(10.0F, seconds));
    }

    public float getPotionRange() {
        return potionRange;
    }

    public void setPotionRange(float range) {
        this.potionRange = Math.max(4.0F, Math.min(32.0F, range));
    }

    public float getMeleeChaseSpeed() {
        return meleeChaseSpeed;
    }

    public void setMeleeChaseSpeed(float speed) {
        this.meleeChaseSpeed = Math.max(0.5F, Math.min(2.0F, speed));
    }

    public float getLeapStrength() {
        return leapStrength;
    }

    public void setLeapStrength(float strength) {
        this.leapStrength = Math.max(0.1F, Math.min(1.0F, strength));
    }

    public float getRegenPerSecond() {
        return regenPerSecond;
    }

    public void setRegenPerSecond(float regenPerSecond) {
        this.regenPerSecond = Math.max(0.0F, Math.min(100.0F, regenPerSecond));
    }

    public boolean isFollowTeleport() {
        return followTeleport;
    }

    public void setFollowTeleport(boolean followTeleport) {
        this.followTeleport = followTeleport;
    }

    public boolean isFollowTeleportOutOfSight() {
        return followTeleportOutOfSight;
    }

    public boolean isFollowRun() {
        return followRun;
    }

    public boolean isFollowMatchSpeed() {
        return followMatchSpeed;
    }

    public int followDistanceTier() {
        return switch (followDistance) {
            case "close" -> 0;
            case "far" -> 2;
            default -> 1;
        };
    }

    public boolean isOpenDoors() {
        return openDoors;
    }

    public boolean isAvoidDanger() {
        return avoidDanger;
    }

    public boolean isTeleportFx() {
        return teleportFx;
    }

    public boolean isGroupSpacing() {
        return groupSpacing;
    }

    public java.util.List<ScheduleEntry> getSchedule() {
        return schedule;
    }

    public boolean isScheduleEnabled() {
        return scheduleEnabled;
    }

    public boolean isScheduleGlobal() {
        return scheduleGlobal;
    }

    public void setScheduleGlobal(boolean scheduleGlobal) {
        this.scheduleGlobal = scheduleGlobal;
    }

    public void setScheduleEnabled(boolean scheduleEnabled) {
        this.scheduleEnabled = scheduleEnabled;
    }

    public boolean isIdleLook() {
        return !"off".equals(lookMode);
    }

    public String getLookMode() {
        return lookMode;
    }

    public int getLookRadius() {
        return lookRadius;
    }

    public boolean isBoatRide() {
        return boatRide;
    }

    public boolean isIdleWander() {
        return idleWander;
    }

    public int getIdleWanderRadius() {
        return idleWanderRadius;
    }

    public boolean isPanicWhenHurt() {
        return panicWhenHurt;
    }

    public boolean isPursueAttacker() {
        return pursueAttacker;
    }

    public boolean isHoldPosition() {
        return holdPosition;
    }

    public boolean isAvoidSun() {
        return avoidSun;
    }

    public boolean isBurnInSun() {
        return burnInSun;
    }

    public boolean isPushable() {
        return pushable;
    }

    public boolean isPassable() {
        return passable;
    }

    public int getTotemCharges() {
        return totemCharges;
    }

    public boolean isTotemRender() {
        return totemRender;
    }

    public float getShieldHoldSeconds() {
        return shieldHoldSeconds;
    }

    public float getShieldCooldownSeconds() {
        return shieldCooldownSeconds;
    }

    public String getIdleEmoteId() {
        return idleEmoteId;
    }

    public String getIdleEmoteName() {
        return idleEmoteName;
    }

    public String getIdleEmoteAuthor() {
        return idleEmoteAuthor;
    }

    public String getMobType() {
        return mobType;
    }

    public boolean isFallDamage() {
        return fallDamage;
    }

    public boolean isWebSlow() {
        return webSlow;
    }

    public boolean isCanDrown() {
        return canDrown;
    }

    public boolean isPoisonImmune() {
        return poisonImmune;
    }

    public int getAggroRange() {
        return aggroRange;
    }

    public boolean isLeapAtTarget() {
        return leapAtTarget;
    }

    public float getRotX() {
        return rotX;
    }

    public float getRotY() {
        return rotY;
    }

    public float getRotZ() {
        return rotZ;
    }

    public float getPosX() {
        return posX;
    }

    public float getPosY() {
        return posY;
    }

    public float getPosZ() {
        return posZ;
    }

    public float getScaleX() {
        return clampScale(scaleX);
    }

    public float getScaleY() {
        return clampScale(scaleY);
    }

    public float getScaleZ() {
        return clampScale(scaleZ);
    }

    private static float clampScale(float s) {
        return Math.max(0.1F, Math.min(4.0F, s));
    }

    public PoseJson.Pose getPose() {
        return pose;
    }

    public boolean hasPose() {
        return !pose.isEmpty();
    }

    public String getAutoFollowMode() {
        return autoFollowMode;
    }

    public String getAutoFollowTarget() {
        return autoFollowTarget;
    }

    public static CompanionProfile fromJson(JsonObject json) {
        if (!json.has("id")) {
            throw new JsonParseException("Profile without 'id'");
        }
        CompanionProfile profile = new CompanionProfile(
                UUID.fromString(json.get("id").getAsString()),
                json.has("name") ? json.get("name").getAsString() : "Companion");
        if (json.has("title")) {
            profile.title = json.get("title").getAsString();
        }
        if (json.has("show_title")) {
            profile.showTitle = json.get("show_title").getAsBoolean();
        }

        JsonElement skin = json.has("skin_player_name") ? json.get("skin_player_name") : json.get("skinPlayerName");
        if (skin != null && !skin.isJsonNull()) {
            profile.skinPlayerName = skin.getAsString();
        }
        if (json.has("name_color") && !json.get("name_color").isJsonNull()) {
            profile.nameColor = json.get("name_color").getAsString();
        }
        if (json.has("disguise") && !json.get("disguise").isJsonNull()) {
            profile.disguise = json.get("disguise").getAsString();
        }
        if (json.has("voice_sound") && !json.get("voice_sound").isJsonNull()) {
            profile.voiceSound = json.get("voice_sound").getAsString();
        }
        if (json.has("portrait_image") && !json.get("portrait_image").isJsonNull()) {
            profile.portraitImage = json.get("portrait_image").getAsString();
        }
        profile.portraitShow = json.has("portrait_show") && json.get("portrait_show").getAsBoolean();
        if (json.has("faction") && !json.get("faction").isJsonNull()) {
            profile.faction = json.get("faction").getAsString();
        }
        if (json.has("voice_pitch")) {
            profile.setVoicePitch(json.get("voice_pitch").getAsFloat());
        }
        if (json.has("entry_points")) {
            for (JsonElement e : json.getAsJsonArray("entry_points")) {
                profile.entryPoints.add(EntryPoint.fromJson(e.getAsJsonObject()));
            }
        }
        if (json.has("offers")) {
            for (JsonElement e : json.getAsJsonArray("offers")) {
                profile.offers.add(com.withouthonor.npcs.common.trade.TradeOffer.fromJson(e.getAsJsonObject()));
            }
        }
        if (json.has("restock_minutes")) {
            profile.setRestockMinutes(json.get("restock_minutes").getAsInt());
        }

        profile.attackable = json.has("attackable") && json.get("attackable").getAsBoolean();
        if (json.has("max_health")) {
            profile.setMaxHealth(json.get("max_health").getAsFloat());
        }
        if (json.has("attack_damage")) {
            profile.setAttackDamage(json.get("attack_damage").getAsFloat());
        }
        if (json.has("armor")) {
            profile.setArmor(json.get("armor").getAsFloat());
        }
        if (json.has("kb_resistance")) {
            profile.setKbResistance(json.get("kb_resistance").getAsFloat());
        }
        if (json.has("move_speed")) {
            profile.setMoveSpeed(json.get("move_speed").getAsFloat());
        }
        if (json.has("res_melee")) {
            profile.resMelee = clampRes(json.get("res_melee").getAsInt());
        }
        if (json.has("res_projectile")) {
            profile.resProjectile = clampRes(json.get("res_projectile").getAsInt());
        }
        if (json.has("res_explosion")) {
            profile.resExplosion = clampRes(json.get("res_explosion").getAsInt());
        }
        if (json.has("res_fire")) {
            profile.resFire = clampRes(json.get("res_fire").getAsInt());
        }
        if (json.has("res_fall")) {
            profile.resFall = clampRes(json.get("res_fall").getAsInt());
        }
        if (json.has("res_magic")) {
            profile.resMagic = clampRes(json.get("res_magic").getAsInt());
        }
        profile.guardSpareCreepers = json.has("guard_spare_creepers")
                && json.get("guard_spare_creepers").getAsBoolean();

        String rawPreset = json.has("combat_preset") ? json.get("combat_preset").getAsString() : "passive";
        String rawTargets = json.has("aggressor_targets") ? json.get("aggressor_targets").getAsString() : "";
        java.util.Set<String> cats = parseAggressorTargets(rawTargets);
        String style;
        switch (rawPreset) {
            case "passive" -> style = "passive";
            case "defensive" -> style = "melee";
            case "guard" -> {
                style = "melee";
                cats.add("monsters");
                cats.add("factions");
            }
            case "aggressor" -> {
                style = "melee";
                cats.add("factions");
            }
            case "archer" -> {
                style = "bow";
                cats.add("monsters");
                cats.add("factions");
            }
            case "witch" -> {
                style = "potion";
                cats.add("monsters");
                cats.add("factions");
            }
            case "melee", "bow", "potion", "shield" -> style = rawPreset;
            default -> style = "passive";
        }
        profile.combatPreset = style;
        String rawSystem = json.has("combat_system") ? json.get("combat_system").getAsString() : "vanilla";
        profile.combatSystem = "epicfight".equals(rawSystem) ? "epicfight" : "vanilla";
        profile.aggressorTargets = String.join(",", cats);
        profile.bossbarEnabled = json.has("bossbar_enabled") && json.get("bossbar_enabled").getAsBoolean();
        if (json.has("bossbar_color")) {
            profile.bossbarColor = json.get("bossbar_color").getAsString();
        }
        if (json.has("bossbar_radius")) {
            profile.bossbarRadius = Math.max(4, Math.min(128, json.get("bossbar_radius").getAsInt()));
        }
        if (json.has("death_behavior")) {
            profile.deathBehavior = json.get("death_behavior").getAsString();
        }
        if (json.has("respawn_seconds")) {
            profile.setRespawnSeconds(json.get("respawn_seconds").getAsInt());
        }
        if (json.has("respawn_location")) {
            profile.respawnLocation = json.get("respawn_location").getAsString();
        }
        if (json.has("respawn_home")) {
            JsonObject h = json.getAsJsonObject("respawn_home");
            profile.respawnHomeX = h.get("x").getAsInt();
            profile.respawnHomeY = h.get("y").getAsInt();
            profile.respawnHomeZ = h.get("z").getAsInt();
            profile.respawnHomeDim = h.has("dim") ? h.get("dim").getAsString() : "";
        }
        if (json.has("respawn_seconds_max")) {
            profile.respawnSecondsMax = json.get("respawn_seconds_max").getAsInt();
        }
        if (json.has("respawn_max")) {
            profile.respawnMax = json.get("respawn_max").getAsInt();
        }
        profile.setDeathXp(
                json.has("death_xp_min") ? json.get("death_xp_min").getAsInt() : 0,
                json.has("death_xp_max") ? json.get("death_xp_max").getAsInt() : 0);
        if (json.has("drops")) {
            for (JsonElement e : json.getAsJsonArray("drops")) {
                profile.drops.add(DropEntry.fromJson(e.getAsJsonObject()));
            }
        }
        readPotions(json, "potion_self", profile.potionSelf);
        readPotions(json, "potion_enemy", profile.potionEnemy);
        readPotions(json, "potion_ally", profile.potionAlly);
        readPotions(json, "potion_combat_buff", profile.potionCombatBuff);

        if (json.has("potion_belt") && profile.potionSelf.isEmpty()) {
            readPotions(json, "potion_belt", profile.potionSelf);
        }
        profile.potionSelfEnabled = !json.has("potion_self_off");
        profile.potionSelfCombat = json.has("potion_self_combat");
        profile.potionEnemyEnabled = !json.has("potion_enemy_off");
        profile.potionEnemyCombat = json.has("potion_enemy_combat");
        profile.potionAllyEnabled = !json.has("potion_ally_off");
        profile.potionAllyCombat = json.has("potion_ally_combat");
        profile.potionEnemySeq = json.has("potion_enemy_seq");
        profile.potionAllySeq = json.has("potion_ally_seq");
        profile.bubblesEnabled = !json.has("bubbles_enabled") || json.get("bubbles_enabled").getAsBoolean();
        profile.indicatorsEnabled = json.has("indicators_enabled")
                && json.get("indicators_enabled").getAsBoolean();
        if (json.has("ambient_phrases")) {
            for (JsonElement e : json.getAsJsonArray("ambient_phrases")) {
                profile.ambientPhrases.add(e.getAsString());
            }
        }
        readPhrases(json, "combat_phrases", profile.combatPhrases);
        readPhrases(json, "interact_phrases", profile.interactPhrases);
        readPhrases(json, "death_phrases", profile.deathPhrases);
        readPhrases(json, "kill_phrases", profile.killPhrases);
        profile.bubblesToChat = json.has("bubbles_to_chat") && json.get("bubbles_to_chat").getAsBoolean();
        readActions(json, "react_hurt", profile.onHurt);
        readActions(json, "react_death", profile.onDeath);
        readActions(json, "react_interact", profile.onInteract);
        readActions(json, "react_approach", profile.onApproach);
        if (json.has("react_approach_range")) {
            profile.approachRange = Math.max(0, Math.min(16, json.get("react_approach_range").getAsInt()));
        }
        if (json.has("ambient_radius")) {
            profile.setAmbientRadius(json.get("ambient_radius").getAsInt());
        }
        if (json.has("ambient_cooldown_s")) {
            profile.setAmbientCooldownSeconds(json.get("ambient_cooldown_s").getAsInt());
        }
        if (json.has("ranged_interval_s")) {
            profile.setRangedIntervalSeconds(json.get("ranged_interval_s").getAsFloat());
        }
        if (json.has("ranged_range")) {
            profile.setRangedRange(json.get("ranged_range").getAsFloat());
        }
        if (json.has("potion_interval_s")) {
            profile.setPotionIntervalSeconds(json.get("potion_interval_s").getAsFloat());
        }
        if (json.has("potion_range")) {
            profile.setPotionRange(json.get("potion_range").getAsFloat());
        }
        if (json.has("melee_chase_speed")) {
            profile.setMeleeChaseSpeed(json.get("melee_chase_speed").getAsFloat());
        }
        if (json.has("leap_strength")) {
            profile.setLeapStrength(json.get("leap_strength").getAsFloat());
        }
        if (json.has("regen_per_second")) {
            profile.setRegenPerSecond(json.get("regen_per_second").getAsFloat());
        }
        if (json.has("follow_teleport")) {
            profile.followTeleport = json.get("follow_teleport").getAsBoolean();
        }
        if (json.has("follow_teleport_oos")) {
            profile.followTeleportOutOfSight = json.get("follow_teleport_oos").getAsBoolean();
        }
        if (json.has("follow_run")) {
            profile.followRun = json.get("follow_run").getAsBoolean();
        }
        if (json.has("follow_match_speed")) {
            profile.followMatchSpeed = json.get("follow_match_speed").getAsBoolean();
        }
        if (json.has("follow_distance")) {
            profile.followDistance = json.get("follow_distance").getAsString();
        }
        if (json.has("open_doors")) {
            profile.openDoors = json.get("open_doors").getAsBoolean();
        }
        if (json.has("avoid_danger")) {
            profile.avoidDanger = json.get("avoid_danger").getAsBoolean();
        }
        if (json.has("teleport_fx")) {
            profile.teleportFx = json.get("teleport_fx").getAsBoolean();
        }
        if (json.has("group_spacing")) {
            profile.groupSpacing = json.get("group_spacing").getAsBoolean();
        }
        if (json.has("schedule")) {
            for (JsonElement e : json.getAsJsonArray("schedule")) {
                profile.schedule.add(ScheduleEntry.fromJson(e.getAsJsonObject()));
            }
        }
        profile.scheduleEnabled = json.has("schedule_enabled") && json.get("schedule_enabled").getAsBoolean();
        profile.scheduleGlobal = json.has("schedule_global") && json.get("schedule_global").getAsBoolean();
        if (json.has("look_mode")) {
            profile.lookMode = json.get("look_mode").getAsString();
        } else {
            // Миграция со старого флага idle_look: true → "cold", false → "off"
            profile.lookMode = !json.has("idle_look") || json.get("idle_look").getAsBoolean()
                    ? "cold" : "off";
        }
        if (json.has("look_radius")) {
            profile.lookRadius = Math.max(1, Math.min(16, json.get("look_radius").getAsInt()));
        }
        profile.boatRide = !json.has("boat_ride") || json.get("boat_ride").getAsBoolean();
        profile.pursueAttacker = !json.has("pursue_attacker") || json.get("pursue_attacker").getAsBoolean();
        profile.holdPosition = json.has("hold_position") && json.get("hold_position").getAsBoolean();
        profile.idleWander = json.has("idle_wander") && json.get("idle_wander").getAsBoolean();
        if (json.has("idle_wander_radius")) {
            profile.idleWanderRadius = json.get("idle_wander_radius").getAsInt();
        }
        profile.panicWhenHurt = json.has("panic_when_hurt") && json.get("panic_when_hurt").getAsBoolean();
        profile.avoidSun = json.has("avoid_sun") && json.get("avoid_sun").getAsBoolean();
        profile.burnInSun = json.has("burn_in_sun") && json.get("burn_in_sun").getAsBoolean();
        profile.pushable = json.has("pushable") && json.get("pushable").getAsBoolean();
        profile.passable = json.has("passable") && json.get("passable").getAsBoolean();
        profile.totemCharges = json.has("totem_charges")
                ? Math.max(0, Math.min(99, json.get("totem_charges").getAsInt())) : 0;
        profile.totemRender = json.has("totem_render") && json.get("totem_render").getAsBoolean();
        if (json.has("shield_hold_s")) {
            profile.shieldHoldSeconds = Math.max(0.3F, Math.min(5.0F, json.get("shield_hold_s").getAsFloat()));
        }
        if (json.has("shield_cooldown_s")) {
            profile.shieldCooldownSeconds = Math.max(0.5F, Math.min(10.0F, json.get("shield_cooldown_s").getAsFloat()));
        }
        if (json.has("idle_emote")) {
            profile.idleEmoteId = json.get("idle_emote").getAsString();
            profile.idleEmoteName = json.has("idle_emote_name") ? json.get("idle_emote_name").getAsString() : "";
            profile.idleEmoteAuthor = json.has("idle_emote_author") ? json.get("idle_emote_author").getAsString() : "";
        }
        if (json.has("mob_type")) {
            profile.mobType = json.get("mob_type").getAsString();
        }
        profile.fallDamage = !json.has("fall_damage") || json.get("fall_damage").getAsBoolean();
        profile.webSlow = !json.has("web_slow") || json.get("web_slow").getAsBoolean();
        profile.canDrown = !json.has("can_drown") || json.get("can_drown").getAsBoolean();
        profile.poisonImmune = json.has("poison_immune") && json.get("poison_immune").getAsBoolean();
        if (json.has("aggro_range")) {
            profile.aggroRange = json.get("aggro_range").getAsInt();
        }
        profile.leapAtTarget = json.has("leap_at_target") && json.get("leap_at_target").getAsBoolean();
        if (json.has("rot_x")) {
            profile.rotX = json.get("rot_x").getAsFloat();
        }
        if (json.has("rot_y")) {
            profile.rotY = json.get("rot_y").getAsFloat();
        }
        if (json.has("rot_z")) {
            profile.rotZ = json.get("rot_z").getAsFloat();
        }
        if (json.has("pos_x")) {
            profile.posX = json.get("pos_x").getAsFloat();
        }
        if (json.has("pos_y")) {
            profile.posY = json.get("pos_y").getAsFloat();
        }
        if (json.has("pos_z")) {
            profile.posZ = json.get("pos_z").getAsFloat();
        }
        if (json.has("scale_x")) {
            profile.scaleX = json.get("scale_x").getAsFloat();
        }
        if (json.has("scale_y")) {
            profile.scaleY = json.get("scale_y").getAsFloat();
        }
        if (json.has("scale_z")) {
            profile.scaleZ = json.get("scale_z").getAsFloat();
        }
        PoseJson.read(json, profile.pose);
        if (json.has("auto_follow")) {
            profile.autoFollowMode = json.get("auto_follow").getAsString();
        }
        if (json.has("auto_follow_target")) {
            profile.autoFollowTarget = json.get("auto_follow_target").getAsString();
        }
        return profile;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("name", name);
        json.addProperty("title", title);
        if (showTitle) {
            json.addProperty("show_title", true);
        }
        if (skinPlayerName != null) {
            json.addProperty("skin_player_name", skinPlayerName);
        }
        if (nameColor != null) {
            json.addProperty("name_color", nameColor);
        }
        if (disguise != null) {
            json.addProperty("disguise", disguise);
        }
        if (voiceSound != null) {
            json.addProperty("voice_sound", voiceSound);
            json.addProperty("voice_pitch", voicePitch);
        }
        if (!portraitImage.isEmpty()) {
            json.addProperty("portrait_image", portraitImage);
        }
        if (portraitShow) {
            json.addProperty("portrait_show", true);
        }
        if (faction != null) {
            json.addProperty("faction", faction);
        }
        if (!entryPoints.isEmpty()) {
            JsonArray entries = new JsonArray();
            entryPoints.forEach(e -> entries.add(e.toJson()));
            json.add("entry_points", entries);
        }
        if (!offers.isEmpty()) {
            JsonArray offersJson = new JsonArray();
            offers.forEach(o -> offersJson.add(o.toJson()));
            json.add("offers", offersJson);
        }
        if (restockMinutes > 0) {
            json.addProperty("restock_minutes", restockMinutes);
        }

        if (attackable) {
            json.addProperty("attackable", true);
        }
        json.addProperty("max_health", maxHealth);
        json.addProperty("attack_damage", attackDamage);
        json.addProperty("armor", armor);
        json.addProperty("kb_resistance", kbResistance);
        if (resMelee != 0) json.addProperty("res_melee", resMelee);
        if (resProjectile != 0) json.addProperty("res_projectile", resProjectile);
        if (resExplosion != 0) json.addProperty("res_explosion", resExplosion);
        if (resFire != 0) json.addProperty("res_fire", resFire);
        if (resFall != 0) json.addProperty("res_fall", resFall);
        if (resMagic != 0) json.addProperty("res_magic", resMagic);
        json.addProperty("move_speed", moveSpeed);
        json.addProperty("combat_preset", combatPreset);
        if (!"vanilla".equals(combatSystem)) {
            json.addProperty("combat_system", combatSystem);
        }
        if (guardSpareCreepers) {
            json.addProperty("guard_spare_creepers", true);
        }
        if (!aggressorTargets.isEmpty()) {
            json.addProperty("aggressor_targets", aggressorTargets);
        }
        if (bossbarEnabled) {
            json.addProperty("bossbar_enabled", true);
            json.addProperty("bossbar_color", bossbarColor);
            json.addProperty("bossbar_radius", bossbarRadius);
        }
        json.addProperty("death_behavior", deathBehavior);
        json.addProperty("respawn_seconds", respawnSeconds);
        if (!"death".equals(respawnLocation)) {
            json.addProperty("respawn_location", respawnLocation);
        }
        if (!respawnHomeDim.isEmpty()) {
            JsonObject h = new JsonObject();
            h.addProperty("x", respawnHomeX);
            h.addProperty("y", respawnHomeY);
            h.addProperty("z", respawnHomeZ);
            h.addProperty("dim", respawnHomeDim);
            json.add("respawn_home", h);
        }
        if (respawnSecondsMax > 0) {
            json.addProperty("respawn_seconds_max", respawnSecondsMax);
        }
        if (respawnMax > 0) {
            json.addProperty("respawn_max", respawnMax);
        }
        if (deathXpMax > 0) {
            json.addProperty("death_xp_min", deathXpMin);
            json.addProperty("death_xp_max", deathXpMax);
        }
        if (!drops.isEmpty()) {
            JsonArray dropsJson = new JsonArray();
            drops.forEach(d -> dropsJson.add(d.toJson()));
            json.add("drops", dropsJson);
        }
        writePotions(json, "potion_self", potionSelf);
        writePotions(json, "potion_enemy", potionEnemy);
        writePotions(json, "potion_ally", potionAlly);
        writePotions(json, "potion_combat_buff", potionCombatBuff);
        if (!potionSelfEnabled) json.addProperty("potion_self_off", true);
        if (potionSelfCombat) json.addProperty("potion_self_combat", true);
        if (!potionEnemyEnabled) json.addProperty("potion_enemy_off", true);
        if (potionEnemyCombat) json.addProperty("potion_enemy_combat", true);
        if (!potionAllyEnabled) json.addProperty("potion_ally_off", true);
        if (potionAllyCombat) json.addProperty("potion_ally_combat", true);
        if (potionEnemySeq) json.addProperty("potion_enemy_seq", true);
        if (potionAllySeq) json.addProperty("potion_ally_seq", true);
        json.addProperty("ranged_interval_s", rangedIntervalSeconds);
        json.addProperty("ranged_range", rangedRange);
        json.addProperty("potion_interval_s", potionIntervalSeconds);
        json.addProperty("potion_range", potionRange);
        json.addProperty("melee_chase_speed", meleeChaseSpeed);
        json.addProperty("leap_strength", leapStrength);
        if (regenPerSecond > 0.0F) {
            json.addProperty("regen_per_second", regenPerSecond);
        }
        if (!followTeleport) {
            json.addProperty("follow_teleport", false);
        }
        if (!followTeleportOutOfSight) {
            json.addProperty("follow_teleport_oos", false);
        }
        if (!followRun) {
            json.addProperty("follow_run", false);
        }
        if (!pursueAttacker) {
            json.addProperty("pursue_attacker", false);
        }
        if (holdPosition) {
            json.addProperty("hold_position", true);
        }
        if (!followMatchSpeed) {
            json.addProperty("follow_match_speed", false);
        }
        if (!"normal".equals(followDistance)) {
            json.addProperty("follow_distance", followDistance);
        }
        if (!openDoors) {
            json.addProperty("open_doors", false);
        }
        if (!avoidDanger) {
            json.addProperty("avoid_danger", false);
        }
        if (!teleportFx) {
            json.addProperty("teleport_fx", false);
        }
        if (!groupSpacing) {
            json.addProperty("group_spacing", false);
        }
        if (!schedule.isEmpty()) {
            JsonArray sched = new JsonArray();
            schedule.forEach(e -> sched.add(e.toJson()));
            json.add("schedule", sched);
        }
        if (scheduleGlobal) {
            json.addProperty("schedule_global", true);
        }
        if (scheduleEnabled) {
            json.addProperty("schedule_enabled", true);
        }
        if (!"cold".equals(lookMode)) {
            json.addProperty("look_mode", lookMode);
        }
        if ("off".equals(lookMode)) {
            // Легаси-флаг для совместимости старых версий/экспорта
            json.addProperty("idle_look", false);
        }
        if (lookRadius != 3) {
            json.addProperty("look_radius", lookRadius);
        }
        if (!boatRide) {
            json.addProperty("boat_ride", false);
        }
        if (idleWander) {
            json.addProperty("idle_wander", true);
        }
        if (idleWanderRadius > 0) {
            json.addProperty("idle_wander_radius", idleWanderRadius);
        }
        if (panicWhenHurt) {
            json.addProperty("panic_when_hurt", true);
        }
        if (avoidSun) {
            json.addProperty("avoid_sun", true);
        }
        if (burnInSun) {
            json.addProperty("burn_in_sun", true);
        }
        if (pushable) {
            json.addProperty("pushable", true);
        }
        if (passable) {
            json.addProperty("passable", true);
        }
        if (totemCharges > 0) {
            json.addProperty("totem_charges", totemCharges);
        }
        if (totemRender) {
            json.addProperty("totem_render", true);
        }
        if (shieldHoldSeconds != 1.5F) {
            json.addProperty("shield_hold_s", shieldHoldSeconds);
        }
        if (shieldCooldownSeconds != 2.0F) {
            json.addProperty("shield_cooldown_s", shieldCooldownSeconds);
        }
        if (!idleEmoteId.isEmpty()) {
            json.addProperty("idle_emote", idleEmoteId);
            json.addProperty("idle_emote_name", idleEmoteName);
            json.addProperty("idle_emote_author", idleEmoteAuthor);
        }
        if (!"undefined".equals(mobType)) {
            json.addProperty("mob_type", mobType);
        }
        if (!fallDamage) {
            json.addProperty("fall_damage", false);
        }
        if (!webSlow) {
            json.addProperty("web_slow", false);
        }
        if (!canDrown) {
            json.addProperty("can_drown", false);
        }
        if (poisonImmune) {
            json.addProperty("poison_immune", true);
        }
        if (aggroRange > 0) {
            json.addProperty("aggro_range", aggroRange);
        }
        if (leapAtTarget) {
            json.addProperty("leap_at_target", true);
        }
        if (rotX != 0) {
            json.addProperty("rot_x", rotX);
        }
        if (rotY != 0) {
            json.addProperty("rot_y", rotY);
        }
        if (rotZ != 0) {
            json.addProperty("rot_z", rotZ);
        }
        if (posX != 0) {
            json.addProperty("pos_x", posX);
        }
        if (posY != 0) {
            json.addProperty("pos_y", posY);
        }
        if (posZ != 0) {
            json.addProperty("pos_z", posZ);
        }
        if (scaleX != 1.0F) {
            json.addProperty("scale_x", scaleX);
        }
        if (scaleY != 1.0F) {
            json.addProperty("scale_y", scaleY);
        }
        if (scaleZ != 1.0F) {
            json.addProperty("scale_z", scaleZ);
        }
        PoseJson.write(json, pose);
        if (!"none".equals(autoFollowMode)) {
            json.addProperty("auto_follow", autoFollowMode);
            json.addProperty("auto_follow_target", autoFollowTarget);
        }
        if (!bubblesEnabled) {
            json.addProperty("bubbles_enabled", false);
        }
        if (indicatorsEnabled) {
            json.addProperty("indicators_enabled", true);
        }
        if (!ambientPhrases.isEmpty()) {
            JsonArray phrases = new JsonArray();
            ambientPhrases.forEach(phrases::add);
            json.add("ambient_phrases", phrases);
        }
        writePhrases(json, "combat_phrases", combatPhrases);
        writePhrases(json, "interact_phrases", interactPhrases);
        writePhrases(json, "death_phrases", deathPhrases);
        writePhrases(json, "kill_phrases", killPhrases);
        if (bubblesToChat) {
            json.addProperty("bubbles_to_chat", true);
        }
        writeActions(json, "react_hurt", onHurt);
        writeActions(json, "react_death", onDeath);
        writeActions(json, "react_interact", onInteract);
        writeActions(json, "react_approach", onApproach);
        if (approachRange > 0) {
            json.addProperty("react_approach_range", approachRange);
        }
        json.addProperty("ambient_radius", ambientRadius);
        json.addProperty("ambient_cooldown_s", ambientCooldownSeconds);
        return json;
    }

    private static void readPhrases(JsonObject json, String key, List<String> out) {
        if (json.has(key)) {
            for (JsonElement e : json.getAsJsonArray(key)) {
                out.add(e.getAsString());
            }
        }
    }

    private static void writePhrases(JsonObject json, String key, List<String> list) {
        if (!list.isEmpty()) {
            JsonArray array = new JsonArray();
            list.forEach(array::add);
            json.add(key, array);
        }
    }

    private static void readPotions(JsonObject json, String key,
                                    List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> out) {
        if (json.has(key)) {
            for (JsonElement e : json.getAsJsonArray(key)) {
                if (out.size() >= 3) {
                    break;
                }
                out.add(com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec
                        .fromJson(e.getAsJsonObject()));
            }
        }
    }

    private static void writePotions(JsonObject json, String key,
                                     List<com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec> list) {
        if (!list.isEmpty()) {
            JsonArray arr = new JsonArray();
            list.forEach(p -> arr.add(p.toJson()));
            json.add(key, arr);
        }
    }

    private static void readActions(JsonObject json, String key,
                                    List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> out) {
        if (json.has(key)) {
            try {
                out.addAll(com.withouthonor.npcs.common.dialogue.action.ActionTypes.parseList(
                        json.getAsJsonArray(key)));
            } catch (Exception ignored) {

            }
        }
    }

    private static void writeActions(JsonObject json, String key,
                                     List<com.withouthonor.npcs.common.dialogue.action.DialogueAction> list) {
        if (!list.isEmpty()) {
            JsonArray array = new JsonArray();
            for (var a : list) {
                array.add(a.toJson());
            }
            json.add(key, array);
        }
    }
}
