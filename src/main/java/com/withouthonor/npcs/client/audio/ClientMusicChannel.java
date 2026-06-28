package com.withouthonor.npcs.client.audio;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

final class ClientMusicChannel {

    private static Field soundEngineField;
    private static Field channelMapField;
    private static boolean resolved;
    private static boolean broken;

    private ClientMusicChannel() {
    }

    static boolean setVolume(SoundInstance sound, float gain) {
        ChannelAccess.ChannelHandle handle = handleFor(sound);
        if (handle == null) {
            return false;
        }
        handle.execute(channel -> channel.setVolume(gain));
        return true;
    }

    static boolean setPaused(SoundInstance sound, boolean paused) {
        ChannelAccess.ChannelHandle handle = handleFor(sound);
        if (handle == null) {
            return false;
        }
        handle.execute(channel -> {
            if (paused) {
                channel.pause();
            } else {
                channel.unpause();
            }
        });
        return true;
    }

    @javax.annotation.Nullable
    private static ChannelAccess.ChannelHandle handleFor(SoundInstance sound) {
        if (sound == null) {
            return null;
        }
        resolve();
        if (broken) {
            return null;
        }
        try {
            SoundManager manager = Minecraft.getInstance().getSoundManager();
            Object engine = soundEngineField.get(manager);
            if (engine == null) {
                return null;
            }
            Object mapObj = channelMapField.get(engine);
            if (!(mapObj instanceof Map<?, ?> map)) {
                return null;
            }
            Object handle = map.get(sound);
            return handle instanceof ChannelAccess.ChannelHandle ch ? ch : null;
        } catch (Throwable t) {
            broken = true;
            return null;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            soundEngineField = findByType(SoundManager.class, SoundEngine.class);
            channelMapField = findMapByValue(SoundEngine.class, ChannelAccess.ChannelHandle.class);
            if (soundEngineField == null || channelMapField == null) {
                broken = true;
            }
        } catch (Throwable t) {
            broken = true;
        }
    }

    private static Field findByType(Class<?> owner, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    private static Field findMapByValue(Class<?> owner, Class<?> valueType) {
        for (Field f : owner.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())
                    && f.getGenericType() instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length == 2 && valueType.equals(args[1])) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return null;
    }
}
