package com.withouthonor.npcs.compat.emotecraft;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

final class EmotecraftRegistry {

    record Entry(String id, String name, String author, @Nullable ResourceLocation icon) {
    }

    private static final boolean OK;
    private static Field LIST_F;
    private static Field NAME_F;
    private static Field AUTHOR_F;
    private static Method GET_EMOTE_M;
    private static Method GET_ICON_M;
    private static Method TEXT_STR_M;
    private static Method LIST_GET_M;

    static {
        boolean ok = false;
        try {
            Class<?> holder = Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
            LIST_F = holder.getField("list");
            NAME_F = holder.getField("name");
            AUTHOR_F = holder.getField("author");
            GET_EMOTE_M = holder.getMethod("getEmote");
            GET_ICON_M = holder.getMethod("getIconIdentifier");
            TEXT_STR_M = Class.forName("io.github.kosmx.emotes.executor.dataTypes.Text").getMethod("getString");
            Object listObj = LIST_F.get(null);
            LIST_GET_M = listObj.getClass().getMethod("get", Object.class);
            ok = true;
        } catch (Throwable t) {

        }
        OK = ok;
    }

    private EmotecraftRegistry() {
    }

    static boolean available() {
        return OK;
    }

    static List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        if (!OK) {
            return out;
        }
        try {
            Object listObj = LIST_F.get(null);
            for (Object holder : (Iterable<?>) listObj) {
                try {
                    UUID uuid = (UUID) ((Supplier<?>) holder).get();
                    if (uuid == null || GET_EMOTE_M.invoke(holder) == null) {
                        continue;
                    }
                    out.add(new Entry(uuid.toString(),
                            textOf(NAME_F.get(holder), uuid.toString()),
                            textOf(AUTHOR_F.get(holder), ""),
                            iconOf(holder)));
                } catch (Throwable ignored) {

                }
            }
        } catch (Throwable ignored) {

        }
        return out;
    }

    @Nullable
    static KeyframeAnimation animation(String id) {
        if (!OK) {
            return null;
        }
        try {
            Object listObj = LIST_F.get(null);
            Object holder = LIST_GET_M.invoke(listObj, UUID.fromString(id));
            return holder == null ? null : (KeyframeAnimation) GET_EMOTE_M.invoke(holder);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    static KeyframeAnimation animationByName(String name, String author) {
        if (!OK || name == null || name.isBlank()) {
            return null;
        }
        try {
            Object listObj = LIST_F.get(null);
            for (Object holder : (Iterable<?>) listObj) {
                try {
                    Object emote = GET_EMOTE_M.invoke(holder);
                    if (emote == null) {
                        continue;
                    }
                    if (!name.equalsIgnoreCase(textOf(NAME_F.get(holder), ""))) {
                        continue;
                    }
                    if (author != null && !author.isBlank()
                            && !author.equalsIgnoreCase(textOf(AUTHOR_F.get(holder), ""))) {
                        continue;
                    }
                    return (KeyframeAnimation) emote;
                } catch (Throwable ignored) {

                }
            }
        } catch (Throwable ignored) {

        }
        return null;
    }

    private static String textOf(@Nullable Object textObj, String fallback) {
        if (textObj == null) {
            return fallback;
        }
        try {
            String s = (String) TEXT_STR_M.invoke(textObj);
            return s == null || s.isBlank() ? fallback : s;
        } catch (Throwable t) {
            return fallback;
        }
    }

    @Nullable
    private static ResourceLocation iconOf(Object holder) {
        try {
            Object iid = GET_ICON_M.invoke(holder);
            if (iid != null) {
                Object rl = iid.getClass().getMethod("get").invoke(iid);
                if (rl instanceof ResourceLocation r) {
                    return r;
                }
            }
        } catch (Throwable ignored) {

        }
        return null;
    }
}
