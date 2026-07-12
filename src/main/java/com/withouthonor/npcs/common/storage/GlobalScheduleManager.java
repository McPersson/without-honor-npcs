package com.withouthonor.npcs.common.storage;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.entity.ai.ScheduleGoal;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.profile.ScheduleEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GlobalScheduleManager extends SavedData {

    private static final String DATA_NAME = "wh_npcs_global_schedule";
    private static final int MAX_CONCURRENT = 3;

    private static final TicketType<ChunkPos> LOAD_TICKET =
            TicketType.create("wh_npcs_global", Comparator.comparingLong(ChunkPos::toLong));

    // Источнику нужна догрузка САМОЙ сущности (спайк: дистанции 1 не хватает), цели — только
    // блоки/персист (дистанция 1 — как ваниль POST_TELEPORT). Дистанции add/remove обязаны совпадать.
    private static final int SRC_TICKET_DIST = 2;
    private static final int DST_TICKET_DIST = 1;

    private static final class Rec {
        UUID profileId;
        ResourceKey<Level> dim;
        BlockPos lastPos;
        int lastEntryTime = -1;
    }

    private static final class Pending {
        BlockPos target;
        ChunkPos src;
        ChunkPos dst;
        ResourceKey<Level> dim;
        long deadline;
    }

    public record Info(UUID uuid, String name, ResourceKey<Level> dim, BlockPos pos, boolean loaded) {
    }

    private final Map<UUID, Rec> records = new HashMap<>();
    private final Map<UUID, Pending> pending = new HashMap<>();

    public static GlobalScheduleManager get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(GlobalScheduleManager::load, GlobalScheduleManager::new, DATA_NAME);
    }

    public void update(CompanionEntity npc) {
        if (npc.getProfileId() == null) {
            return;
        }
        MinecraftServer server = npc.level().getServer();
        if (server == null) {
            return;
        }
        CompanionProfile profile = ProfileManager.get().get(npc.getProfileId());
        if (profile == null) {
            return;
        }
        ScheduleEntry entry = activeEntry(server, profile);
        Rec r = records.computeIfAbsent(npc.getUUID(), u -> new Rec());
        UUID profileId = npc.getProfileId();
        ResourceKey<Level> dim = npc.level().dimension();
        BlockPos pos = npc.blockPosition();
        int entryTime = entry != null ? entry.time() : -1;
        if (profileId.equals(r.profileId) && dim.equals(r.dim)
                && pos.equals(r.lastPos) && entryTime == r.lastEntryTime) {
            return;
        }
        r.profileId = profileId;
        r.dim = dim;
        r.lastPos = pos;
        r.lastEntryTime = entryTime;
        setDirty();
    }

    public void forget(MinecraftServer server, UUID uuid) {
        if (records.remove(uuid) != null) {
            setDirty();
        }
        Pending p = pending.remove(uuid);
        if (p != null) {
            releaseTickets(server, p);
        }
    }

    public void tick(MinecraftServer server) {
        tickPending(server);
        if (server.getTickCount() % 20 == 0) {
            sweep(server);
        }
    }

    private void sweep(MinecraftServer server) {
        long now = server.getTickCount();
        Iterator<Map.Entry<UUID, Rec>> it = records.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Rec> e = it.next();
            UUID uuid = e.getKey();
            Rec r = e.getValue();
            if (pending.containsKey(uuid)) {
                continue;
            }
            CompanionProfile profile = ProfileManager.get().get(r.profileId);
            if (profile == null || !profile.isScheduleGlobal() || !profile.isScheduleEnabled()
                    || profile.getSchedule().isEmpty()) {
                it.remove();
                setDirty();
                continue;
            }
            ScheduleEntry entry = activeEntry(server, profile);
            if (entry == null || entry.time() == r.lastEntryTime || r.lastPos == null || r.dim == null) {
                continue;
            }
            BlockPos target = entry.pos();
            if (r.lastPos.distSqr(target) <= 4) {
                r.lastEntryTime = entry.time();
                setDirty();
                continue;
            }
            ServerLevel level = server.getLevel(r.dim);
            if (level == null) {
                continue;
            }
            CompanionEntity loaded = loadedNpc(level, uuid);
            if (loaded != null) {
                relocate(loaded, target);
                r.lastPos = target;
                r.lastEntryTime = entry.time();
                setDirty();
            } else {
                if (pending.size() >= MAX_CONCURRENT) {
                    continue;
                }
                ChunkPos src = new ChunkPos(r.lastPos);
                ChunkPos dst = new ChunkPos(target);
                level.getChunkSource().addRegionTicket(LOAD_TICKET, src, SRC_TICKET_DIST, src);
                level.getChunkSource().addRegionTicket(LOAD_TICKET, dst, DST_TICKET_DIST, dst);
                Pending p = new Pending();
                p.target = target;
                p.src = src;
                p.dst = dst;
                p.dim = r.dim;
                p.deadline = now + 120;
                pending.put(uuid, p);
                WHCompanions.LOGGER.info("Global schedule: loading chunks {} -> {} to relocate {}", src, dst, uuid);
            }
        }
    }

    private void tickPending(MinecraftServer server) {
        if (pending.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        Iterator<Map.Entry<UUID, Pending>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> e = it.next();
            UUID uuid = e.getKey();
            Pending p = e.getValue();
            Rec r = records.get(uuid);
            if (r == null || r.dim == null) {
                releaseTickets(server, p);
                it.remove();
                continue;
            }
            ServerLevel level = server.getLevel(r.dim);
            if (level == null) {
                releaseTickets(server, p);
                it.remove();
                continue;
            }
            CompanionEntity npc = loadedNpc(level, uuid);
            if (npc != null) {
                // Тикеты ставились на уровне p.dim (мог отличаться от текущего r.dim после смены
                // измерения NPC) — релизить строго через server-перегрузку, иначе утечка force-чанков.
                releaseTickets(server, p);
                relocate(npc, p.target);
                r.lastPos = p.target;
                CompanionProfile profile = ProfileManager.get().get(r.profileId);
                ScheduleEntry entry = profile != null ? activeEntry(server, profile) : null;
                if (entry != null) {
                    r.lastEntryTime = entry.time();
                }
                setDirty();
                WHCompanions.LOGGER.info("Global schedule: relocated {} to {}", uuid, p.target);
                it.remove();
            } else if (now > p.deadline) {
                WHCompanions.LOGGER.warn("Global schedule: NPC {} not found to relocate (gone?), dropping", uuid);
                releaseTickets(server, p); // по p.dim, не по текущему r.dim (см. выше)
                records.remove(uuid);
                setDirty();
                it.remove();
            }
        }
    }

    private static void relocate(CompanionEntity npc, BlockPos target) {
        npc.getNavigation().stop();
        npc.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, npc.getYRot(), npc.getXRot());
    }

    private static void releaseTickets(ServerLevel level, Pending p) {
        if (p.src != null) {
            level.getChunkSource().removeRegionTicket(LOAD_TICKET, p.src, SRC_TICKET_DIST, p.src);
        }
        if (p.dst != null) {
            level.getChunkSource().removeRegionTicket(LOAD_TICKET, p.dst, DST_TICKET_DIST, p.dst);
        }
    }

    private static void releaseTickets(MinecraftServer server, Pending p) {
        ServerLevel level = p.dim != null ? server.getLevel(p.dim) : null;
        if (level != null) {
            releaseTickets(level, p);
        } else {
            WHCompanions.LOGGER.warn("Global schedule: dimension {} gone, chunk tickets not released", p.dim);
        }
    }

    public java.util.List<Info> list(MinecraftServer server) {
        java.util.List<Info> out = new java.util.ArrayList<>();
        for (Map.Entry<UUID, Rec> e : records.entrySet()) {
            Rec r = e.getValue();
            if (r.profileId == null || r.dim == null || r.lastPos == null) {
                continue;
            }
            CompanionProfile profile = ProfileManager.get().get(r.profileId);
            if (profile == null || !profile.isScheduleGlobal()) {
                continue;
            }
            ServerLevel level = server.getLevel(r.dim);
            boolean loaded = level != null && level.getEntity(e.getKey()) != null;
            String name = net.minecraft.ChatFormatting.stripFormatting(profile.getName());
            out.add(new Info(e.getKey(), name != null ? name : profile.getName(), r.dim, r.lastPos, loaded));
        }
        return out;
    }

    @javax.annotation.Nullable
    public Info info(MinecraftServer server, UUID uuid) {
        Rec r = records.get(uuid);
        if (r == null || r.dim == null || r.lastPos == null) {
            return null;
        }
        CompanionProfile profile = r.profileId != null ? ProfileManager.get().get(r.profileId) : null;
        ServerLevel level = server.getLevel(r.dim);
        boolean loaded = level != null && level.getEntity(uuid) != null;
        return new Info(uuid, profile != null ? profile.getName() : "NPC", r.dim, r.lastPos, loaded);
    }

    public void disable(MinecraftServer server, UUID uuid) {
        Rec r = records.get(uuid);
        if (r == null || r.profileId == null) {
            return;
        }
        UUID pid = r.profileId;
        CompanionProfile profile = ProfileManager.get().get(pid);
        if (profile != null) {
            profile.setScheduleGlobal(false);
            ProfileManager.get().save(profile);
            com.withouthonor.npcs.common.profile.ProfileSync.applyToLoadedEntities(server, profile);
        }
        Iterator<Map.Entry<UUID, Rec>> it = records.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Rec> en = it.next();
            if (pid.equals(en.getValue().profileId)) {
                Pending p = pending.remove(en.getKey());
                if (p != null) {
                    releaseTickets(server, p);
                }
                it.remove();
            }
        }
        setDirty();
    }

    private static CompanionEntity loadedNpc(ServerLevel level, UUID uuid) {
        return level.getEntity(uuid) instanceof CompanionEntity c ? c : null;
    }

    private static ScheduleEntry activeEntry(MinecraftServer server, CompanionProfile profile) {
        List<ScheduleEntry> schedule = profile.getSchedule();
        if (schedule.isEmpty()) {
            return null;
        }
        int now = ScheduleGoal.minutesOfDay(server.overworld().getDayTime());
        ScheduleEntry best = null;
        int bestTime = -1;
        ScheduleEntry latest = null;
        int latestTime = -1;
        for (ScheduleEntry e : schedule) {
            if (e.time() > latestTime) {
                latestTime = e.time();
                latest = e;
            }
            if (e.time() <= now && e.time() > bestTime) {
                bestTime = e.time();
                best = e;
            }
        }
        return best != null ? best : latest;
    }

    private static GlobalScheduleManager load(CompoundTag tag) {
        GlobalScheduleManager m = new GlobalScheduleManager();
        ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                Rec r = new Rec();
                r.profileId = t.getUUID("ProfileId");
                r.dim = ResourceKey.create(Registries.DIMENSION,
                        ResourceLocation.parse(t.getString("Dim")));
                r.lastPos = new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z"));
                r.lastEntryTime = t.getInt("Entry");
                m.records.put(t.getUUID("Uuid"), r);
            } catch (Exception ignored) {
            }
        }
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        records.forEach((uuid, r) -> {
            if (r.dim == null || r.lastPos == null || r.profileId == null) {
                return;
            }
            CompoundTag t = new CompoundTag();
            t.putUUID("Uuid", uuid);
            t.putUUID("ProfileId", r.profileId);
            t.putString("Dim", r.dim.location().toString());
            t.putInt("X", r.lastPos.getX());
            t.putInt("Y", r.lastPos.getY());
            t.putInt("Z", r.lastPos.getZ());
            t.putInt("Entry", r.lastEntryTime);
            list.add(t);
        });
        tag.put("Records", list);
        return tag;
    }

    private GlobalScheduleManager() {
    }
}
