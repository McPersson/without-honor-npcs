package com.withouthonor.npcs.client.cache;

import com.mojang.blaze3d.platform.NativeImage;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.RequestSkinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Mod.EventBusSubscriber(modid = WHCompanions.MODID, value = Dist.CLIENT)
public class ClientSkinCache {

    private static final ClientSkinCache INSTANCE = new ClientSkinCache();

    private static final int MAX_BYTES = 262144;
    private static final int MAX_CONCURRENT_URL = 6;
    private static final int UPLOADS_PER_TICK = 8;
    private static final long FAIL_TTL_MS = 5L * 60L * 1000L;
    private static final Duration REQ_TIMEOUT = Duration.ofSeconds(15);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ExecutorService IO_EXEC = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "wh-skin-io");
        t.setDaemon(true);
        return t;
    });

    public record Skin(ResourceLocation location, boolean slim) {
    }

    private record Upload(String name, boolean slim, byte[] data) {
    }

    private final Map<String, Skin> skins = new HashMap<>();
    private final Map<String, Long> requested = new HashMap<>();
    private final Map<String, Long> failed = new HashMap<>();

    private final Semaphore urlPermits = new Semaphore(MAX_CONCURRENT_URL);
    private final Queue<Runnable> httpQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Upload> uploadQueue = new ConcurrentLinkedQueue<>();

    public static ClientSkinCache getInstance() {
        return INSTANCE;
    }

    @Nullable
    public Skin get(String rawName) {
        String name = lowered(rawName);
        Skin skin = skins.get(name);
        if (skin != null) {
            return skin;
        }
        long now = System.currentTimeMillis();
        Long failAt = failed.get(name);
        if (failAt != null) {
            if (now - failAt < FAIL_TTL_MS) {
                return null;
            }
            failed.remove(name);
            requested.remove(name);
        }
        boolean isUrl = name.startsWith("http://") || name.startsWith("https://");
        Long reqAt = requested.get(name);
        if (reqAt == null) {
            requested.put(name, now);
            if (isUrl) {
                submitUrl(rawName, name);
            } else {
                NetworkHandler.sendToServer(new RequestSkinPacket(name));
            }
        } else if (!isUrl && now - reqAt >= FAIL_TTL_MS) {
            // Ответ сервера потерялся — пробуем ещё раз
            requested.put(name, now);
            NetworkHandler.sendToServer(new RequestSkinPacket(name));
        }
        return null;
    }

    /** toLowerCase без аллокации, если строка уже в нижнем регистре (горячий путь рендера). */
    private static String lowered(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != Character.toLowerCase(c)) {
                return s.toLowerCase(Locale.ROOT);
            }
        }
        return s;
    }


    private void submitUrl(String original, String key) {
        String spec = original.trim();
        String lower = spec.toLowerCase(Locale.ROOT);
        boolean slim = lower.endsWith("#slim");
        if (lower.endsWith("#slim") || lower.endsWith("#wide")) {
            spec = spec.substring(0, spec.length() - 5).trim();
        }
        final String url = spec;
        final boolean slimFlag = slim;
        IO_EXEC.execute(() -> {
            byte[] cached = readDiskCache(key);
            if (cached != null) {
                uploadQueue.add(new Upload(key, slimFlag, cached));
                return;
            }
            httpQueue.add(() -> startHttp(url, key, slimFlag));
            pumpHttp();
        });
    }

    private void pumpHttp() {
        while (urlPermits.tryAcquire()) {
            Runnable task = httpQueue.poll();
            if (task == null) {
                urlPermits.release();
                break;
            }
            task.run();
        }
    }

    private void startHttp(String url, String key, boolean slim) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "image/png,image/*,*/*;q=0.8")
                    .timeout(REQ_TIMEOUT)
                    .GET().build();
        } catch (Exception e) {
            urlPermits.release();
            markFailed(key, "bad url: " + e);
            pumpHttp();
            return;
        }
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .whenComplete((resp, err) -> {
                    urlPermits.release();
                    pumpHttp();
                    byte[] data = err == null && resp.statusCode() == 200
                            && resp.body().length <= MAX_BYTES ? resp.body() : null;
                    if (data == null) {
                        markFailed(key, err != null ? err.toString()
                                : ("HTTP " + (resp != null ? resp.statusCode() : "?")));
                    } else {
                        IO_EXEC.execute(() -> writeDiskCache(key, data));
                        uploadQueue.add(new Upload(key, slim, data));
                    }
                });
    }

    private void markFailed(String key, String reason) {
        WHCompanions.LOGGER.warn("Client skin '{}' unavailable: {}", key, reason);
        Minecraft.getInstance().execute(() -> failed.put(key, System.currentTimeMillis()));
    }


    public void accept(String rawName, boolean slim, byte[] data) {
        uploadQueue.add(new Upload(rawName.toLowerCase(Locale.ROOT), slim, data));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            INSTANCE.drainUploads();
        }
    }

    private void drainUploads() {
        for (int i = 0; i < UPLOADS_PER_TICK; i++) {
            Upload u = uploadQueue.poll();
            if (u == null) {
                break;
            }
            doAccept(u.name(), u.slim(), u.data());
        }
    }

    private void doAccept(String name, boolean slim, byte[] data) {
        if (data.length == 0) {
            failed.put(name, System.currentTimeMillis());
            return;
        }
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
            if (image.getWidth() != 64 || image.getHeight() != 64) {
                WHCompanions.LOGGER.warn("Skin '{}' has unsupported size {}x{}, using default",
                        name, image.getWidth(), image.getHeight());
                image.close();
                failed.put(name, System.currentTimeMillis());
                return;
            }

            forceOpaque(image, 0, 0, 32, 16);
            forceOpaque(image, 0, 16, 64, 32);
            forceOpaque(image, 16, 48, 48, 64);

            String path = "skins/" + name.replaceAll("[^a-z0-9_.-]", "_");
            if (path.length() > 200) {
                path = path.substring(0, 180) + Integer.toHexString(name.hashCode());
            }
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(WHCompanions.MODID, path);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            skins.put(name, new Skin(location, slim));
        } catch (Exception e) {
            WHCompanions.LOGGER.error("Failed to load skin '{}'", name, e);
            failed.put(name, System.currentTimeMillis());
        }
    }


    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("wh_npcs").resolve("skincache");
    }

    private static String cacheFile(String key) {
        return "u_" + Integer.toHexString(key.hashCode());
    }

    @Nullable
    private byte[] readDiskCache(String key) {
        try {
            Path png = cacheDir().resolve(cacheFile(key) + ".png");
            if (!Files.isRegularFile(png)) {
                return null;
            }
            byte[] bytes = Files.readAllBytes(png);
            return bytes.length > 0 && bytes.length <= MAX_BYTES ? bytes : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void writeDiskCache(String key, byte[] data) {
        try {
            Path dir = cacheDir();
            Files.createDirectories(dir);
            Files.write(dir.resolve(cacheFile(key) + ".png"), data);
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to cache client skin '{}': {}", key, e.toString());
        }
    }

    private static void forceOpaque(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) | 0xFF000000);
            }
        }
    }
}
