package com.withouthonor.npcs.common.storage;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.profile.ProfileSync;
import com.withouthonor.npcs.common.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Graveyard extends SavedData {

    private static final String DATA_NAME = "wh_npcs_graveyard";
    private static final int MAX_ENTRIES = 64;

    // Самоистекающий тикет (10 с) для асинхронной догрузки чанка респавна: раньше spawnAt делал
    // синхронный getChunk на главном потоке — разовый фриз-спайк. Релиз не нужен — истекает сам.
    private static final TicketType<ChunkPos> RESPAWN_TICKET =
            TicketType.create("wh_npcs_respawn", Comparator.comparingLong(ChunkPos::toLong), 200);

    public record Entry(UUID profileId, String name, ResourceKey<Level> dim,
                        double x, double y, double z, float yaw,
                        CompoundTag equipment, long respawnAt, long diedAt,
                        @Nullable UUID ownerUuid) {
    }

    private record Target(ServerLevel level, double x, double y, double z, float yaw) {
    }

    private final List<Entry> entries = new ArrayList<>();

    private final Map<UUID, Integer> respawnsUsed = new HashMap<>();

    public static Graveyard get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(Graveyard::load, Graveyard::new, DATA_NAME);
    }

    public void record(CompanionEntity npc, @Nullable CompanionProfile profile) {
        if (npc.getProfileId() == null) {
            return;
        }
        String name = profile != null ? profile.getName() : npc.getName().getString();
        String stripped = ChatFormatting.stripFormatting(name);
        long respawnAt = computeRespawnAt(npc.getProfileId(), profile);
        UUID owner = npc.getFollowTarget() != null ? npc.getFollowTarget().getUUID() : null;
        entries.add(new Entry(npc.getProfileId(), stripped != null ? stripped : name,
                npc.level().dimension(), npc.getX(), npc.getY(), npc.getZ(), npc.getYRot(),
                npc.saveEquipmentSnapshot(), respawnAt, System.currentTimeMillis(), owner));
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
        setDirty();
    }

    private long computeRespawnAt(UUID profileId, @Nullable CompanionProfile profile) {
        if (profile == null || !profile.isAttackable() || !"respawn".equals(profile.getDeathBehavior())) {
            return 0L;
        }
        int max = profile.getRespawnMax();
        if (max > 0 && respawnsUsed.getOrDefault(profileId, 0) >= max) {
            return 0L;
        }
        int min = profile.getRespawnSeconds();
        int top = profile.getRespawnSecondsMax();
        int delay = top > min ? min + (int) (Math.random() * (top - min + 1)) : min;
        return System.currentTimeMillis() + delay * 1000L;
    }

    public void tickRespawns(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entry.respawnAt() <= 0 || now < entry.respawnAt()) {
                continue;
            }
            CompanionProfile profile = ProfileManager.get().get(entry.profileId());
            Target target = respawnTarget(server, entry, profile);
            if (target == null) {
                continue;
            }
            // Чанк не загружен → тикет и ждём следующего свипа (20 тиков); запись остаётся.
            ChunkPos cp = new ChunkPos(SectionPos.blockToSectionCoord(target.x()),
                    SectionPos.blockToSectionCoord(target.z()));
            if (!target.level().hasChunk(cp.x, cp.z)) {
                target.level().getChunkSource().addRegionTicket(RESPAWN_TICKET, cp, 1, cp);
                continue;
            }
            entries.remove(i);
            setDirty();
            if (spawnAt(server, entry, target)) {
                respawnsUsed.merge(entry.profileId(), 1, Integer::sum);
                setDirty();
            }
        }
    }

    public boolean revive(MinecraftServer server, String name) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).name().equalsIgnoreCase(name)) {
                Entry entry = entries.remove(i);
                setDirty();
                Target target = deathTarget(server, entry);
                return target != null && spawnAt(server, entry, target);
            }
        }
        return false;
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (Entry entry : entries) {
            String quoted = entry.name().contains(" ") ? "\"" + entry.name() + "\"" : entry.name();
            if (!names.contains(quoted)) {
                names.add(quoted);
            }
        }
        return names;
    }

    @Nullable
    private Target respawnTarget(MinecraftServer server, Entry entry, @Nullable CompanionProfile profile) {
        String mode = profile != null ? profile.getRespawnLocation() : "death";
        if ("home".equals(mode) && profile != null && profile.hasRespawnHome()) {
            ServerLevel lvl = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(profile.getRespawnHomeDim())));
            if (lvl != null) {
                return new Target(lvl, profile.getRespawnHomeX() + 0.5, profile.getRespawnHomeY(),
                        profile.getRespawnHomeZ() + 0.5, entry.yaw());
            }
        } else if ("owner".equals(mode) && entry.ownerUuid() != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.ownerUuid());
            if (owner != null && owner.level() instanceof ServerLevel ol) {
                return new Target(ol, owner.getX(), owner.getY(), owner.getZ(), owner.getYRot());
            }

        }
        return deathTarget(server, entry);
    }

    @Nullable
    private static Target deathTarget(MinecraftServer server, Entry entry) {
        ServerLevel lvl = server.getLevel(entry.dim());
        return lvl != null ? new Target(lvl, entry.x(), entry.y(), entry.z(), entry.yaw()) : null;
    }

    private boolean spawnAt(MinecraftServer server, Entry entry, Target target) {
        ServerLevel level = target.level();

        level.getChunk(SectionPos.blockToSectionCoord(target.x()), SectionPos.blockToSectionCoord(target.z()));
        CompanionEntity npc = ModEntities.COMPANION.get().create(level);
        if (npc == null) {
            return false;
        }
        npc.moveTo(target.x(), target.y(), target.z(), target.yaw(), 0.0F);
        npc.setProfileId(entry.profileId());
        CompanionProfile profile = ProfileManager.get().get(entry.profileId());
        if (profile != null) {
            npc.setCustomName(ProfileSync.coloredName(profile));
            npc.setSkinName(profile.getSkinPlayerName());
            npc.setDisguise(profile.getDisguise());
            npc.setTitle(profile.isShowTitle() ? profile.getTitle() : "");
            npc.setRenderTransform(profile);
            npc.setPose(profile);
            npc.applyCombatProfile(profile);
            npc.setHealth(npc.getMaxHealth());
        }
        npc.loadEquipmentSnapshot(entry.equipment());
        level.addFreshEntity(npc);
        npc.updateIndex();
        return true;
    }

    private static Graveyard load(CompoundTag tag) {
        Graveyard graveyard = new Graveyard();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                graveyard.entries.add(new Entry(
                        t.getUUID("ProfileId"),
                        t.getString("Name"),
                        ResourceKey.create(Registries.DIMENSION,
                                ResourceLocation.parse(t.getString("Dim"))),
                        t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"), t.getFloat("Yaw"),
                        t.getCompound("Equipment"),
                        t.getLong("RespawnAt"), t.getLong("DiedAt"),
                        t.hasUUID("Owner") ? t.getUUID("Owner") : null));
            } catch (Exception ignored) {

            }
        }
        CompoundTag used = tag.getCompound("RespawnsUsed");
        for (String key : used.getAllKeys()) {
            try {
                graveyard.respawnsUsed.put(UUID.fromString(key), used.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return graveyard;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag t = new CompoundTag();
            t.putUUID("ProfileId", entry.profileId());
            t.putString("Name", entry.name());
            t.putString("Dim", entry.dim().location().toString());
            t.putDouble("X", entry.x());
            t.putDouble("Y", entry.y());
            t.putDouble("Z", entry.z());
            t.putFloat("Yaw", entry.yaw());
            t.put("Equipment", entry.equipment());
            t.putLong("RespawnAt", entry.respawnAt());
            t.putLong("DiedAt", entry.diedAt());
            if (entry.ownerUuid() != null) {
                t.putUUID("Owner", entry.ownerUuid());
            }
            list.add(t);
        }
        tag.put("Entries", list);
        CompoundTag used = new CompoundTag();
        respawnsUsed.forEach((id, n) -> used.putInt(id.toString(), n));
        tag.put("RespawnsUsed", used);
        return tag;
    }

    private Graveyard() {
    }
}
