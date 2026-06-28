package com.withouthonor.npcs.common.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UrlSkinRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static UrlSkinRegistry instance;

    public record UrlSkin(String url, String addedBy, String name) {
    }

    private final Path file;
    private final List<UrlSkin> skins = new ArrayList<>();

    private UrlSkinRegistry(Path file) {
        this.file = file;
    }

    public static UrlSkinRegistry get() {
        if (instance == null) {
            throw new IllegalStateException("UrlSkinRegistry is not initialized (server not started?)");
        }
        return instance;
    }

    public static void init(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT)
                .resolve("wh_npcs").resolve("skins").resolve("url_registry.json");
        instance = new UrlSkinRegistry(path);
        instance.load();
    }

    public static void shutdown() {
        instance = null;
    }

    private void load() {
        skins.clear();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            for (var e : JsonParser.parseReader(reader).getAsJsonArray()) {
                JsonObject json = e.getAsJsonObject();
                skins.add(new UrlSkin(json.get("url").getAsString(),
                        json.has("added_by") ? json.get("added_by").getAsString() : "",
                        json.has("name") ? json.get("name").getAsString() : ""));
            }
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to read url skin registry", e);
        }
    }

    private void save() {
        JsonArray array = new JsonArray();
        for (UrlSkin skin : skins) {
            JsonObject json = new JsonObject();
            json.addProperty("url", skin.url());
            json.addProperty("added_by", skin.addedBy());
            if (!skin.name().isEmpty()) {
                json.addProperty("name", skin.name());
            }
            array.add(json);
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to save url skin registry", e);
        }
    }

    public List<UrlSkin> all() {
        return List.copyOf(skins);
    }

    public boolean add(String url, String addedBy) {
        String key = url.toLowerCase(Locale.ROOT);
        for (UrlSkin skin : skins) {
            if (skin.url().toLowerCase(Locale.ROOT).equals(key)) {
                return false;
            }
        }
        skins.add(new UrlSkin(url, addedBy, ""));
        save();
        return true;
    }

    @javax.annotation.Nullable
    public UrlSkin remove(String url) {
        String key = url.toLowerCase(Locale.ROOT);
        for (int i = 0; i < skins.size(); i++) {
            if (skins.get(i).url().toLowerCase(Locale.ROOT).equals(key)) {
                UrlSkin removed = skins.remove(i);
                save();
                return removed;
            }
        }
        return null;
    }

    public void rename(String url, String name) {
        String key = url.toLowerCase(Locale.ROOT);
        for (int i = 0; i < skins.size(); i++) {
            if (skins.get(i).url().toLowerCase(Locale.ROOT).equals(key)) {
                skins.set(i, new UrlSkin(skins.get(i).url(), skins.get(i).addedBy(), name.trim()));
                save();
                return;
            }
        }
    }
}
