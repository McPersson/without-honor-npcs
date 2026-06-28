package com.withouthonor.npcs.client.cache;

import com.mojang.blaze3d.platform.NativeImage;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.RequestSkinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ClientSkinCache {

    private static final ClientSkinCache INSTANCE = new ClientSkinCache();

    private static final int MAX_BYTES = 262144;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final java.net.http.HttpClient HTTP = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();

    public record Skin(ResourceLocation location, boolean slim) {
    }

    private final Map<String, Skin> skins = new HashMap<>();
    private final Set<String> requested = new HashSet<>();
    private final Set<String> failed = new HashSet<>();

    public static ClientSkinCache getInstance() {
        return INSTANCE;
    }

    @Nullable
    public Skin get(String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        Skin skin = skins.get(name);
        if (skin != null) {
            return skin;
        }
        if (!failed.contains(name) && requested.add(name)) {
            if (name.startsWith("http://") || name.startsWith("https://")) {
                fetchUrlClient(rawName, name);
            } else {
                NetworkHandler.sendToServer(new RequestSkinPacket(name));
            }
        }
        return null;
    }

    private void fetchUrlClient(String original, String key) {
        String spec = original.trim();
        String lower = spec.toLowerCase(Locale.ROOT);
        boolean slim = lower.endsWith("#slim");
        if (lower.endsWith("#slim") || lower.endsWith("#wide")) {
            spec = spec.substring(0, spec.length() - 5).trim();
        }
        final String url = spec;
        final boolean slimFlag = slim;
        java.net.http.HttpRequest request;
        try {
            request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "image/png,image/*,*/*;q=0.8")
                    .GET().build();
        } catch (Exception e) {
            failed.add(key);
            return;
        }
        HTTP.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray())
                .whenComplete((resp, err) -> {
                    byte[] data = err == null && resp.statusCode() == 200
                            && resp.body().length <= MAX_BYTES ? resp.body() : null;
                    if (err != null) {
                        WHCompanions.LOGGER.warn("Client skin url fetch failed '{}': {}", url, err.toString());
                    } else if (data == null) {
                        WHCompanions.LOGGER.warn("Client skin url '{}' HTTP {} (size {})",
                                url, resp.statusCode(), resp.body().length);
                    }
                    Minecraft.getInstance().execute(() -> {
                        if (data == null) {
                            failed.add(key);
                        } else {
                            accept(key, slimFlag, data);
                        }
                    });
                });
    }

    public void accept(String rawName, boolean slim, byte[] data) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (data.length == 0) {
            failed.add(name);
            return;
        }
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
            if (image.getWidth() != 64 || image.getHeight() != 64) {

                WHCompanions.LOGGER.warn("Skin '{}' has unsupported size {}x{}, using default",
                        name, image.getWidth(), image.getHeight());
                image.close();
                failed.add(name);
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
            failed.add(name);
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
