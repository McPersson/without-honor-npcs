package com.withouthonor.npcs.common.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompanionIndex extends SavedData {

    private static final String DATA_NAME = "wh_npcs_index";

    public record Entry(UUID id, String name, ResourceKey<Level> dimension, BlockPos pos, @Nullable UUID profileId) {
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static CompanionIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(CompanionIndex::load, CompanionIndex::new, DATA_NAME);
    }

    private static CompanionIndex load(CompoundTag tag) {
        CompanionIndex index = new CompanionIndex();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                UUID id = t.getUUID("Id");
                String name = t.getString("Name");
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim")));
                BlockPos pos = BlockPos.of(t.getLong("Pos"));
                UUID profileId = t.hasUUID("ProfileId") ? t.getUUID("ProfileId") : null;
                index.entries.put(id, new Entry(id, name, dim, pos, profileId));
            } catch (Exception ignored) {

            }
        }
        return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", e.id());
            t.putString("Name", e.name());
            t.putString("Dim", e.dimension().location().toString());
            t.putLong("Pos", e.pos().asLong());
            if (e.profileId() != null) {
                t.putUUID("ProfileId", e.profileId());
            }
            list.add(t);
        }
        tag.put("Entries", list);
        return tag;
    }

    public void update(Entry entry) {
        Entry old = entries.put(entry.id(), entry);
        if (!entry.equals(old)) {
            setDirty();
        }
    }

    public void remove(UUID id) {
        if (entries.remove(id) != null) {
            setDirty();
        }
    }

    @Nullable
    public Entry byId(UUID id) {
        return entries.get(id);
    }

    @Nullable
    public Entry byName(String name) {
        for (Entry e : entries.values()) {
            if (e.name().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    public Collection<Entry> all() {
        return Collections.unmodifiableCollection(entries.values());
    }
}
