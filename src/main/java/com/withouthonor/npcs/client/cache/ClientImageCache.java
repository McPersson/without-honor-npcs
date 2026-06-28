package com.withouthonor.npcs.client.cache;

import com.mojang.blaze3d.platform.NativeImage;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.RequestImagePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClientImageCache {

    public enum State {
        LOADING, READY, FAILED
    }

    public record Entry(State state, @Nullable ResourceLocation location, int width, int height) {
    }

    private static final ClientImageCache INSTANCE = new ClientImageCache();

    private final Map<String, Entry> images = new HashMap<>();
    private final Map<String, byte[][]> partial = new HashMap<>();

    public static ClientImageCache getInstance() {
        return INSTANCE;
    }

    public Entry get(String name) {
        Entry entry = images.get(name);
        if (entry != null) {
            return entry;
        }
        Entry loading = new Entry(State.LOADING, null, 0, 0);
        images.put(name, loading);
        NetworkHandler.sendToServer(new RequestImagePacket(name));
        return loading;
    }

    public void receiveChunk(String name, int index, int total, byte[] data) {
        if (total <= 0) {
            images.put(name, new Entry(State.FAILED, null, 0, 0));
            partial.remove(name);
            return;
        }
        byte[][] chunks = partial.computeIfAbsent(name, n -> new byte[total][]);
        if (chunks.length != total || index < 0 || index >= total) {
            return;
        }
        chunks[index] = data;
        for (byte[] chunk : chunks) {
            if (chunk == null) {
                return;
            }
        }
        partial.remove(name);
        assemble(name, chunks);
    }

    private void assemble(String name, byte[][] chunks) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] chunk : chunks) {
                out.write(chunk);
            }
            NativeImage image = NativeImage.read(new ByteArrayInputStream(out.toByteArray()));
            String path = "dlgimg/" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(WHCompanions.MODID, path);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            images.put(name, new Entry(State.READY, location, image.getWidth(), image.getHeight()));
        } catch (Exception e) {
            WHCompanions.LOGGER.error("Failed to assemble dialogue image '{}'", name, e);
            images.put(name, new Entry(State.FAILED, null, 0, 0));
        }
    }
}
