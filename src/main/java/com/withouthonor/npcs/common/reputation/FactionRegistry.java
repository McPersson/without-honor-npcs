package com.withouthonor.npcs.common.reputation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FactionRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static FactionRegistry instance;

    private final Path file;
    private final Map<String, Faction> factions = new LinkedHashMap<>();

    private FactionRegistry(Path file) {
        this.file = file;
    }

    public static FactionRegistry get() {
        if (instance == null) {
            throw new IllegalStateException("FactionRegistry is not initialized (server not started?)");
        }
        return instance;
    }

    public static void init(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("factions.json");
        instance = new FactionRegistry(path);
        instance.load();
    }

    public static void shutdown() {
        instance = null;
    }

    @Nullable
    public String load() {
        factions.clear();
        if (!Files.isRegularFile(file)) {
            writeSample();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            for (JsonElement e : JsonParser.parseReader(reader).getAsJsonArray()) {
                Faction faction = Faction.fromJson(e.getAsJsonObject());
                factions.put(faction.getId(), faction);
            }
            return null;
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to read factions.json", e);
            return e.getMessage();
        }
    }

    private void writeSample() {
        factions.put("example", new Faction("example", "Example", 0xFF55FFFF, 10, Faction.DEFAULT_TIERS));
        saveToFile();
        factions.clear();
    }

    private void saveToFile() {
        JsonArray array = new JsonArray();
        factions.values().forEach(faction -> array.add(faction.toJson()));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to write factions.json", e);
        }
    }

    public void put(Faction faction) {
        factions.put(faction.getId(), faction);
        saveToFile();
    }

    public boolean delete(String id) {
        if (factions.remove(id) == null) {
            return false;
        }
        saveToFile();
        return true;
    }

    public List<Faction> all() {
        return List.copyOf(factions.values());
    }

    @Nullable
    public Faction byId(String id) {
        return factions.get(id);
    }

    public List<String> ids() {
        return new ArrayList<>(factions.keySet());
    }
}
