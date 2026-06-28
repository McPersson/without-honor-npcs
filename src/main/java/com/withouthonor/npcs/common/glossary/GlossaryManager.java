package com.withouthonor.npcs.common.glossary;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GlossaryManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GlossaryManager instance;

    private final Path file;
    private final Map<String, GlossaryTerm> terms = new LinkedHashMap<>();

    private GlossaryManager(Path file) {
        this.file = file;
    }

    public static GlossaryManager get() {
        if (instance == null) {
            throw new IllegalStateException("GlossaryManager is not initialized (server not started?)");
        }
        return instance;
    }

    public static void init(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("glossary.json");
        instance = new GlossaryManager(path);
        instance.load();
    }

    public static void shutdown() {
        instance = null;
    }

    public void load() {
        terms.clear();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            for (JsonElement e : JsonParser.parseReader(reader).getAsJsonArray()) {
                GlossaryTerm term = GlossaryTerm.fromJson(e.getAsJsonObject());
                terms.put(term.getId(), term);
            }
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to read glossary.json", e);
        }
    }

    private void saveToFile() {
        JsonArray array = new JsonArray();
        terms.values().forEach(term -> array.add(term.toJson()));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to write glossary.json", e);
        }
    }

    public void put(GlossaryTerm term) {
        terms.put(term.getId(), term);
        saveToFile();
    }

    public boolean delete(String id) {
        if (terms.remove(id) == null) {
            return false;
        }
        saveToFile();
        return true;
    }

    public List<GlossaryTerm> all() {
        return List.copyOf(terms.values());
    }

    @Nullable
    public GlossaryTerm byId(String id) {
        return terms.get(id);
    }
}
