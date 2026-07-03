package com.withouthonor.npcs.compat.etched;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.compat.EtchedBridge;
import com.withouthonor.npcs.compat.EtchedClientBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Optional;

public final class EtchedBridgeImpl implements EtchedBridge, EtchedClientBridge {

    private static final boolean OK;
    private static Method IS_PLAYABLE;
    private static Method GET_STACK_MUSIC;
    private static Method TRACK_URL;
    private static Method TRACK_TITLE;
    private static Method GET_ETCHED_RECORD;

    static {
        boolean ok = false;
        try {
            Class<?> playable = Class.forName("gg.moonflower.etched.api.record.PlayableRecord");
            IS_PLAYABLE = playable.getMethod("isPlayableRecord", ItemStack.class);
            GET_STACK_MUSIC = playable.getMethod("getStackMusic", ItemStack.class);
            Class<?> track = Class.forName("gg.moonflower.etched.api.record.TrackData");
            TRACK_URL = track.getMethod("url");
            TRACK_TITLE = track.getMethod("title");
            Class<?> tracker = Class.forName("gg.moonflower.etched.api.sound.SoundTracker");
            GET_ETCHED_RECORD = tracker.getMethod("getEtchedRecord",
                    String.class, Component.class, Entity.class, boolean.class);
            ok = true;
        } catch (Throwable t) {

        }
        OK = ok;
    }

    private SoundInstance online;
    private String onlineUrl = "";

    @Override
    public boolean isAvailable() {
        return OK;
    }

    @Override
    public boolean isEtchedDisc(ItemStack stack) {
        if (!OK || stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            if ((Boolean) IS_PLAYABLE.invoke(null, stack)) {
                return true;
            }
        } catch (Throwable ignored) {

        }
        return firstTrack(stack) != null;
    }

    @Override
    public String extractUrl(ItemStack stack) {
        Object track = firstTrack(stack);
        if (track != null) {
            try {
                Object url = TRACK_URL.invoke(track);
                return url == null ? "" : url.toString();
            } catch (Throwable ignored) {

            }
        }
        return "";
    }

    @Override
    public String extractTitle(ItemStack stack) {
        Object track = firstTrack(stack);
        if (track != null) {
            try {
                Object title = TRACK_TITLE.invoke(track);
                if (title instanceof Component c) {
                    return c.getString();
                }
            } catch (Throwable ignored) {

            }
        }
        return "";
    }

    private Object firstTrack(ItemStack stack) {
        if (!OK || stack == null || stack.isEmpty()) {
            return null;
        }
        try {
            Object opt = GET_STACK_MUSIC.invoke(null, stack);
            if (opt instanceof Optional<?> o && o.isPresent()) {
                Object arr = o.get();
                if (arr != null && arr.getClass().isArray() && Array.getLength(arr) > 0) {
                    return Array.get(arr, 0);
                }
            }
        } catch (Throwable ignored) {

        }
        return null;
    }

    @Override
    public void playOnline(String url, String title, boolean loop) {
        if (!OK || url == null || url.isEmpty()) {
            return;
        }
        if (url.equals(onlineUrl) && online != null
                && Minecraft.getInstance().getSoundManager().isActive(online)) {
            return;
        }
        stopOnline();
        try {
            Entity player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            Component name = Component.literal(title == null || title.isEmpty() ? url : title);
            Object inst = GET_ETCHED_RECORD.invoke(null, url, name, player, loop);
            if (inst instanceof SoundInstance si) {
                online = si;
                onlineUrl = url;
                Minecraft.getInstance().getSoundManager().play(si);
            }
        } catch (Throwable t) {
            WHCompanions.LOGGER.warn("Etched: failed to play online track '{}': {}", url, t.toString());
            online = null;
            onlineUrl = "";
        }
    }

    @Override
    public void stopOnline() {
        if (online != null) {
            try {
                Minecraft.getInstance().getSoundManager().stop(online);
            } catch (Throwable ignored) {

            }
            online = null;
            onlineUrl = "";
        }
    }

    @Override
    public String currentUrl() {
        return onlineUrl;
    }

    @Override
    public SoundInstance currentSound() {
        return online;
    }
}
