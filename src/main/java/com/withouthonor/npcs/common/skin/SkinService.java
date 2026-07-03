package com.withouthonor.npcs.common.skin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.WHCompanions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SkinService {

    private static final Gson GSON = new Gson();
    public static final int MAX_SKIN_BYTES = 262144;
    private static final long FAIL_TTL_MS = 5L * 60L * 1000L;
    private static final Duration REQ_TIMEOUT = Duration.ofSeconds(15);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static SkinService instance;

    private final MinecraftServer server;
    private final Path skinsDir;
    private final Map<String, CompletableFuture<SkinData>> pending = new HashMap<>();
    private final Map<String, Long> failed = new HashMap<>();

    public record SkinData(boolean slim, byte[] bytes) {
    }

    private SkinService(MinecraftServer server, Path skinsDir) {
        this.server = server;
        this.skinsDir = skinsDir;
    }

    public static SkinService get() {
        if (instance == null) {
            throw new IllegalStateException("SkinService is not initialized (server not started?)");
        }
        return instance;
    }

    public static void init(MinecraftServer server) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("skins");
        instance = new SkinService(server, dir);
    }

    public static void shutdown() {
        instance = null;
    }

    public void forget(String rawSpec) {
        String spec = rawSpec.trim();
        String lower = spec.toLowerCase(Locale.ROOT);
        if (lower.endsWith("#slim") || lower.endsWith("#wide")) {
            spec = spec.substring(0, spec.length() - 5).trim();
        }
        failed.remove(spec.toLowerCase(Locale.ROOT));
    }

    private void markFail(String key) {
        failed.put(key, System.currentTimeMillis());
    }

    private boolean recentlyFailed(String key) {
        Long t = failed.get(key);
        if (t == null) {
            return false;
        }
        if (System.currentTimeMillis() - t >= FAIL_TTL_MS) {
            failed.remove(key);
            return false;
        }
        return true;
    }

    public CompletableFuture<SkinData> fetch(String rawSpec) {
        String spec = rawSpec.trim();
        Boolean forcedSlim = null;
        String lower = spec.toLowerCase(Locale.ROOT);
        if (lower.endsWith("#slim")) {
            forcedSlim = true;
            spec = spec.substring(0, spec.length() - 5).trim();
        } else if (lower.endsWith("#wide")) {
            forcedSlim = false;
            spec = spec.substring(0, spec.length() - 5).trim();
        }
        String key = spec.toLowerCase(Locale.ROOT);
        if (recentlyFailed(key)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Skin fetch already failed: " + spec));
        }
        DefaultSkins.DefaultSkin defaultSkin = DefaultSkins.bySpec(spec);
        if (defaultSkin != null) {
            boolean slim = forcedSlim != null ? forcedSlim : defaultSkin.slim();
            if (DefaultSkins.isBundled(defaultSkin.id())) {
                return fetchBundled(defaultSkin.id(), key, slim);
            }
            return fetchByUrl(defaultSkin.url(), key, slim);
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return fetchByUrl(spec, key, forcedSlim != null && forcedSlim);
        }
        if (key.endsWith(".png")) {
            return fetchLocalFile(spec, forcedSlim != null
                    ? forcedSlim : key.endsWith("_slim.png"));
        }
        final Boolean force = forcedSlim;
        CompletableFuture<SkinData> base = fetchByNickname(key);
        return force == null ? base
                : base.thenApply(data -> new SkinData(force, data.bytes()));
    }

    private CompletableFuture<SkinData> fetchByNickname(String name) {
        if (!name.matches("[a-z0-9_]{1,16}")) {
            markFail(name);
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bad player name: " + name));
        }
        String cacheName = "name_" + name;
        return pending.computeIfAbsent(name, n -> download(n).handleAsync((data, err) -> {
            pending.remove(n);
            if (err == null && data != null) {
                writeDiskCache(cacheName, data);
                return data;
            }
            SkinData stale = readDiskCache(cacheName);
            if (stale != null) {
                WHCompanions.LOGGER.warn("Skin fetch for '{}' failed, using cached copy: {}",
                        n, err != null ? err.getMessage() : "no data");
                return stale;
            }
            markFail(n);
            throw new java.util.concurrent.CompletionException(err != null ? err
                    : new IllegalStateException("No skin for " + n));
        }, server));
    }

    private CompletableFuture<SkinData> fetchBundled(String id, String key, boolean slim) {
        try (var in = SkinService.class.getResourceAsStream(DefaultSkins.bundledResourcePath(id))) {
            if (in == null) {
                markFail(key);
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Bundled skin resource missing: " + id));
            }
            return CompletableFuture.completedFuture(new SkinData(slim, in.readAllBytes()));
        } catch (IOException e) {
            markFail(key);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<SkinData> fetchLocalFile(String fileName, boolean slim) {
        String key = fileName.toLowerCase(Locale.ROOT);
        if (!fileName.matches("[a-zA-Z0-9_\\-]{1,64}\\.png")) {
            markFail(key);
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bad skin file name: " + fileName));
        }
        try {
            java.nio.file.Path file = skinsDir.resolve(fileName);
            if (!Files.isRegularFile(file)) {
                markFail(key);
                return CompletableFuture.failedFuture(new IllegalStateException("Skin file not found: " + fileName));
            }
            return CompletableFuture.completedFuture(new SkinData(slim, Files.readAllBytes(file)));
        } catch (IOException e) {
            markFail(key);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<SkinData> fetchByUrl(String url, String key, boolean slim) {
        String cacheName = "url_" + Integer.toHexString(key.hashCode());
        SkinData cached = readDiskCache(cacheName);
        if (cached != null) {
            return CompletableFuture.completedFuture(slim == cached.slim()
                    ? cached : new SkinData(slim, cached.bytes()));
        }
        return pending.computeIfAbsent(key, k -> getBytes(url)
                .thenApply(bytes -> new SkinData(slim, bytes))
                .whenCompleteAsync((data, err) -> {
                    pending.remove(k);
                    if (err != null) {
                        markFail(k);
                        WHCompanions.LOGGER.warn("Failed to fetch skin url '{}': {}", url, err.getMessage());
                    } else {
                        writeDiskCache(cacheName, data);
                    }
                }, server));
    }

    private SkinData readDiskCache(String name) {
        Path png = skinsDir.resolve(name + ".png");
        if (!Files.exists(png)) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(png);
            Path meta = skinsDir.resolve(name + ".meta");
            boolean slim = Files.exists(meta)
                    && "slim".equals(Files.readString(meta, StandardCharsets.UTF_8).trim());
            return new SkinData(slim, bytes);
        } catch (IOException e) {
            WHCompanions.LOGGER.warn("Failed to read cached skin '{}'", name, e);
            return null;
        }
    }

    private void writeDiskCache(String name, SkinData data) {
        try {
            Files.createDirectories(skinsDir);
            Files.write(skinsDir.resolve(name + ".png"), data.bytes());
            Files.writeString(skinsDir.resolve(name + ".meta"),
                    data.slim() ? "slim" : "classic", StandardCharsets.UTF_8);
        } catch (IOException e) {
            WHCompanions.LOGGER.warn("Failed to cache skin '{}'", name, e);
        }
    }

    private CompletableFuture<SkinData> download(String name) {
        return getJson("https://api.mojang.com/users/profiles/minecraft/" + name)
                .thenCompose(profile -> getJson(
                        "https://sessionserver.mojang.com/session/minecraft/profile/" + profile.get("id").getAsString()))
                .thenCompose(session -> {
                    JsonObject textures = decodeTexturesProperty(session);
                    JsonObject skin = textures.getAsJsonObject("textures").getAsJsonObject("SKIN");
                    boolean slim = skin.has("metadata")
                            && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
                    return getBytes(skin.get("url").getAsString())
                            .thenApply(bytes -> new SkinData(slim, bytes));
                });
    }

    private static JsonObject decodeTexturesProperty(JsonObject session) {
        var properties = session.getAsJsonArray("properties");
        for (var p : properties) {
            JsonObject prop = p.getAsJsonObject();
            if ("textures".equals(prop.get("name").getAsString())) {
                String json = new String(Base64.getDecoder().decode(prop.get("value").getAsString()), StandardCharsets.UTF_8);
                return GSON.fromJson(json, JsonObject.class);
            }
        }
        throw new IllegalStateException("No textures property in session profile");
    }

    private static CompletableFuture<JsonObject> getJson(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT).timeout(REQ_TIMEOUT).GET().build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
                    }
                    return GSON.fromJson(response.body(), JsonObject.class);
                });
    }

    private static CompletableFuture<byte[]> getBytes(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/png,image/*,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(REQ_TIMEOUT)
                .GET().build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
                    }
                    if (response.body().length > MAX_SKIN_BYTES) {
                        throw new IllegalStateException("Skin too large: " + response.body().length);
                    }
                    return response.body();
                });
    }

    public enum SkinSaveStatus {
        OK, REPLACED, TOO_BIG, BAD_FORMAT, BAD_SIZE, ERROR
    }

    public record SkinSaveResult(SkinSaveStatus status, String name) {
    }

    public SkinSaveResult saveSkinFile(String baseName, byte[] bytes) {
        if (bytes.length > MAX_SKIN_BYTES) {
            return new SkinSaveResult(SkinSaveStatus.TOO_BIG, "");
        }
        byte[] png;
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(bytes));
            if (img == null) {
                return new SkinSaveResult(SkinSaveStatus.BAD_FORMAT, "");
            }
            if (img.getWidth() != 64 || img.getHeight() != 64) {
                return new SkinSaveResult(SkinSaveStatus.BAD_SIZE, "");
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", out);
            png = out.toByteArray();
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Skin normalize failed: {}", e.toString());
            return new SkinSaveResult(SkinSaveStatus.BAD_FORMAT, "");
        }
        String safe = baseName == null ? "" : baseName.replaceAll("[^A-Za-z0-9_-]", "_");
        if (safe.length() > 60) {
            safe = safe.substring(0, 60);
        }
        if (safe.isEmpty()) {
            safe = "skin";
        }
        if (safe.startsWith("url_")) {
            safe = "s_" + safe;
        }
        String fn = safe + ".png";
        try {
            Files.createDirectories(skinsDir);
            Path file = skinsDir.resolve(fn);
            boolean existed = Files.isRegularFile(file);
            Files.write(file, png);
            return new SkinSaveResult(existed ? SkinSaveStatus.REPLACED : SkinSaveStatus.OK, fn);
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to save uploaded skin '{}'", fn, e);
            return new SkinSaveResult(SkinSaveStatus.ERROR, "");
        }
    }
}
