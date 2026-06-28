package com.withouthonor.npcs.common.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class ProfileManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static ProfileManager instance;

    private final Path profilesDir;
    private final Map<UUID, CompanionProfile> profiles = new HashMap<>();

    private ProfileManager(Path profilesDir) {
        this.profilesDir = profilesDir;
    }

    public static ProfileManager get() {
        if (instance == null) {
            throw new IllegalStateException("ProfileManager is not initialized (server not started?)");
        }
        return instance;
    }

    public static void init(MinecraftServer server) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("profiles");
        instance = new ProfileManager(dir);
        instance.loadAll();
    }

    public static void shutdown() {
        instance = null;
    }

    private void loadAll() {
        profiles.clear();
        if (!Files.isDirectory(profilesDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(profilesDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(this::loadFile);
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to list profiles in {}", profilesDir, e);
        }
        WHCompanions.LOGGER.info("Loaded {} companion profile(s)", profiles.size());
    }

    private void loadFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            CompanionProfile profile = CompanionProfile.fromJson(GSON.fromJson(reader, com.google.gson.JsonObject.class));
            profiles.put(profile.getId(), profile);
        } catch (Exception e) {
            WHCompanions.LOGGER.error("Failed to read profile {}", file.getFileName(), e);
        }
    }

    @Nullable
    public CompanionProfile get(UUID id) {
        return profiles.get(id);
    }

    public Collection<CompanionProfile> all() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    public CompanionProfile create(String name) {
        CompanionProfile profile = new CompanionProfile(UUID.randomUUID(), name);
        profiles.put(profile.getId(), profile);
        save(profile);
        return profile;
    }

    public void save(CompanionProfile profile) {
        profiles.put(profile.getId(), profile);
        Path target = profilesDir.resolve(profile.getId() + ".json");
        Path tmp = profilesDir.resolve(profile.getId() + ".json.tmp");
        try {
            Files.createDirectories(profilesDir);
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(profile.toJson(), writer);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to save profile {}", profile.getId(), e);
        }
    }

    public boolean delete(UUID id) {
        if (profiles.remove(id) == null) {
            return false;
        }
        try {
            Files.deleteIfExists(profilesDir.resolve(id + ".json"));
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to delete profile file {}", id, e);
        }
        return true;
    }
}
