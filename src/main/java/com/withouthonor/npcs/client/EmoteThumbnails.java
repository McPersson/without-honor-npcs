package com.withouthonor.npcs.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.withouthonor.npcs.WHCompanions;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class EmoteThumbnails {

    private static final int CAP = 96;

    private static final EmoteThumbnails INSTANCE = new EmoteThumbnails();

    public static EmoteThumbnails get() {
        return INSTANCE;
    }

    private final Map<String, Entry> entries = new HashMap<>();
    private int idCounter;

    private int gen;

    private EmoteThumbnails() {
    }

    @Nullable
    public Thumb getReady(String icon) {
        if (icon == null || icon.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        Entry e = entries.get(icon);
        if (e != null) {
            e.lastUse = now;
            return e.state == Entry.READY ? new Thumb(e.rl, e.w, e.h) : null;
        }
        Entry created = new Entry();
        created.lastUse = now;
        entries.put(icon, created);
        requestLoad(icon, created);
        return null;
    }

    public record Thumb(ResourceLocation rl, int w, int h) {
    }

    private void requestLoad(String icon, Entry e) {
        final int myGen = gen;
        final Path path = emotesDir().resolve(icon);
        Util.backgroundExecutor().execute(() -> {
            final NativeImage img;
            try (InputStream in = Files.newInputStream(path)) {
                img = NativeImage.read(in);
            } catch (Throwable t) {
                Minecraft.getInstance().execute(() -> {
                    if (myGen == gen) {
                        e.state = Entry.FAILED;
                    }
                });
                return;
            }
            Minecraft.getInstance().execute(() -> {
                if (myGen != gen) {
                    img.close();
                    return;
                }
                try {
                    e.w = img.getWidth();
                    e.h = img.getHeight();
                    ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(
                            WHCompanions.MODID, "emote_thumb/t" + (idCounter++));
                    Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(img));
                    e.rl = rl;
                    e.state = Entry.READY;
                    evictIfNeeded();
                } catch (Throwable t) {
                    img.close();
                    e.state = Entry.FAILED;
                }
            });
        });
    }

    private void evictIfNeeded() {
        int ready = 0;
        for (Entry e : entries.values()) {
            if (e.state == Entry.READY) {
                ready++;
            }
        }
        while (ready > CAP) {
            Map.Entry<String, Entry> oldest = null;
            for (Map.Entry<String, Entry> en : entries.entrySet()) {
                if (en.getValue().state != Entry.READY) {
                    continue;
                }
                if (oldest == null || en.getValue().lastUse < oldest.getValue().lastUse) {
                    oldest = en;
                }
            }
            if (oldest == null) {
                break;
            }
            release(oldest.getValue());
            entries.remove(oldest.getKey());
            ready--;
        }
    }

    private static void release(Entry e) {
        if (e.rl != null) {
            try {
                Minecraft.getInstance().getTextureManager().release(e.rl);
            } catch (Throwable ignored) {

            }
            e.rl = null;
        }
    }

    public void clear() {
        gen++;
        for (Entry e : entries.values()) {
            release(e);
        }
        entries.clear();
    }

    private static Path emotesDir() {
        return FMLPaths.GAMEDIR.get().resolve("emotes");
    }

    private static final class Entry {
        static final int LOADING = 0;
        static final int READY = 1;
        static final int FAILED = 2;

        @Nullable
        ResourceLocation rl;
        int state = LOADING;
        int w, h;
        long lastUse;
    }
}
