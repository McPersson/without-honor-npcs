package com.withouthonor.npcs.common.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.registry.ModBlockEntities;
import com.withouthonor.npcs.common.registry.ModEntities;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SpawnerBlockEntity extends BlockEntity {

    private final List<String> presets = new ArrayList<>();
    private boolean random = true;
    private int delaySeconds = 10;
    private int maxAlive = 4;
    private int activationRange = 16;
    private int breakXp = 0;
    private float hardness = 2.0F;
    private boolean enabled = true;
    private String displaySkin = "";

    private int seqIndex;
    private int cooldown;
    private final Set<UUID> spawned = new HashSet<>();
    @javax.annotation.Nullable
    private CompanionEntity displayEntity;

    public SpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPAWNER.get(), pos, state);
    }

    public int getBreakXp() {
        return breakXp;
    }

    public float getHardness() {
        return hardness;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isActive() {
        return enabled && !presets.isEmpty();
    }

    public boolean hasPresets() {
        return !presets.isEmpty();
    }

    public String getDisplaySkin() {
        return displaySkin;
    }

    private void recomputeDisplaySkin() {
        displaySkin = "";
        if (presets.isEmpty() || !(level instanceof ServerLevel sl)) {
            return;
        }
        java.nio.file.Path path = exportsDir(sl).resolve(sanitize(presets.get(0)) + ".json");
        if (!java.nio.file.Files.isRegularFile(path)) {
            return;
        }
        try (Reader r = java.nio.file.Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
            if (json.has("skin_player_name")) {
                displaySkin = json.get("skin_player_name").getAsString();
            }
        } catch (Exception ignored) {

        }
    }

    public CompanionEntity getOrCreateDisplay(net.minecraft.world.level.Level level) {
        if (displayEntity == null) {
            displayEntity = ModEntities.COMPANION.get().create(level);
            if (displayEntity != null) {

                displayEntity.moveTo(worldPosition.getX() + 0.5, worldPosition.getY(),
                        worldPosition.getZ() + 0.5, 0.0F, 0.0F);
            }
        }
        return displayEntity;
    }

    public record Config(List<String> presets, boolean random, int delaySeconds, int maxAlive,
                         int activationRange, int breakXp, float hardness, boolean enabled) {

        public static void write(FriendlyByteBuf buf, Config c) {
            buf.writeCollection(c.presets, (b, s) -> b.writeUtf(s, 80));
            buf.writeBoolean(c.random);
            buf.writeVarInt(c.delaySeconds);
            buf.writeVarInt(c.maxAlive);
            buf.writeVarInt(c.activationRange);
            buf.writeVarInt(c.breakXp);
            buf.writeFloat(c.hardness);
            buf.writeBoolean(c.enabled);
        }

        public static Config read(FriendlyByteBuf buf) {
            return new Config(
                    buf.readCollection(ArrayList::new, b -> b.readUtf(80)),
                    buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readBoolean());
        }
    }

    public Config toConfig() {
        return new Config(new ArrayList<>(presets), random, delaySeconds, maxAlive,
                activationRange, breakXp, hardness, enabled);
    }

    public void applyConfig(Config c) {
        presets.clear();
        presets.addAll(c.presets());
        random = c.random();
        delaySeconds = Math.max(1, Math.min(3600, c.delaySeconds()));
        maxAlive = Math.max(1, Math.min(64, c.maxAlive()));
        activationRange = Math.max(1, Math.min(64, c.activationRange()));
        breakXp = Math.max(0, Math.min(1000, c.breakXp()));
        hardness = Math.max(-1.0F, Math.min(1000.0F, c.hardness()));
        enabled = c.enabled();
        seqIndex = 0;
        cooldown = 0;
        recomputeDisplaySkin();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos,
                                  BlockState state, SpawnerBlockEntity be) {
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            be.tick(sl, pos);
        }
    }

    private void tick(ServerLevel level, BlockPos pos) {
        if (!enabled || presets.isEmpty()) {
            return;
        }
        spawned.removeIf(id -> !(level.getEntity(id) instanceof CompanionEntity e) || !e.isAlive());
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (!level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, activationRange)) {
            return;
        }
        if (spawned.size() >= maxAlive) {
            return;
        }
        if (spawnOne(level, pos)) {
            cooldown = delaySeconds * 20;
            setChanged();
        } else {
            cooldown = 100;
        }
    }

    private boolean spawnOne(ServerLevel level, BlockPos pos) {
        String name = nextPreset();
        Path path = exportsDir(level).resolve(sanitize(name) + ".json");
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            JsonObject json;
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(r).getAsJsonObject();
            }
            json.addProperty("id", UUID.randomUUID().toString());
            CompanionProfile profile = CompanionProfile.fromJson(json);
            ProfileManager.get().save(profile);

            CompanionEntity npc = ModEntities.COMPANION.get().create(level);
            if (npc == null) {
                return false;
            }
            double x = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 4.0;
            double z = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 4.0;
            double y = pos.getY() + 1.0;
            float yaw = level.getRandom().nextFloat() * 360.0F;
            npc.moveTo(x, y, z, yaw, 0.0F);
            npc.setYBodyRot(yaw);
            npc.setYHeadRot(yaw);
            npc.setProfileId(profile.getId());
            npc.setTransientProfile(true);
            npc.setCustomName(Component.literal(json.has("name") ? json.get("name").getAsString() : "NPC"));
            com.withouthonor.npcs.network.ProfileSharePackets.applyEquipment(json, npc);
            level.addFreshEntity(npc);
            spawned.add(npc.getUUID());
            return true;
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Spawner failed for preset {}: {}", name, e.getMessage());
            return false;
        }
    }

    private String nextPreset() {
        if (random) {
            return presets.get(level == null ? 0 : level.getRandom().nextInt(presets.size()));
        }
        String s = presets.get(seqIndex % presets.size());
        seqIndex = (seqIndex + 1) % presets.size();
        return s;
    }

    private static Path exportsDir(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("exports");
    }

    public static String sanitize(String raw) {
        String s = raw.replaceAll("[^A-Za-z0-9._-]", "");
        while (s.startsWith(".")) {
            s = s.substring(1);
        }
        if (s.toLowerCase(Locale.ROOT).endsWith(".json")) {
            s = s.substring(0, s.length() - 5);
        }
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (String p : presets) {
            net.minecraft.nbt.StringTag st = net.minecraft.nbt.StringTag.valueOf(p);
            list.add(st);
        }
        tag.put("Presets", list);
        tag.putBoolean("Random", random);
        tag.putInt("DelaySeconds", delaySeconds);
        tag.putInt("MaxAlive", maxAlive);
        tag.putInt("ActivationRange", activationRange);
        tag.putInt("BreakXp", breakXp);
        tag.putFloat("Hardness", hardness);
        tag.putBoolean("Enabled", enabled);
        tag.putString("DisplaySkin", displaySkin);
        ListTag ids = new ListTag();
        for (UUID id : spawned) {
            ids.add(NbtUtils.createUUID(id));
        }
        tag.put("Spawned", ids);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        presets.clear();
        for (Tag t : tag.getList("Presets", Tag.TAG_STRING)) {
            presets.add(t.getAsString());
        }
        random = !tag.contains("Random") || tag.getBoolean("Random");
        delaySeconds = tag.contains("DelaySeconds") ? tag.getInt("DelaySeconds") : 10;
        maxAlive = tag.contains("MaxAlive") ? tag.getInt("MaxAlive") : 4;
        activationRange = tag.contains("ActivationRange") ? tag.getInt("ActivationRange") : 16;
        breakXp = tag.getInt("BreakXp");
        hardness = tag.contains("Hardness") ? tag.getFloat("Hardness") : 2.0F;
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        displaySkin = tag.getString("DisplaySkin");
        spawned.clear();
        if (tag.contains("Spawned", Tag.TAG_LIST)) {
            for (Tag t : tag.getList("Spawned", Tag.TAG_INT_ARRAY)) {
                spawned.add(NbtUtils.loadUUID(t));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) {
            load(pkt.getTag());
        }
    }
}
