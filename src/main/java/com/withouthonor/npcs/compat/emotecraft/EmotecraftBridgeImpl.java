package com.withouthonor.npcs.compat.emotecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.compat.EmotecraftBridge;
import com.withouthonor.npcs.compat.EmotecraftClientBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationJson;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class EmotecraftBridgeImpl implements EmotecraftBridge, EmotecraftClientBridge {

    private static final String[] PA_BONES = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};

    private static final java.lang.reflect.Type EMOTE_LIST_TYPE =
            com.google.gson.reflect.TypeToken.getParameterized(List.class, KeyframeAnimation.class).getType();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(EMOTE_LIST_TYPE, new AnimationJson())
            .create();

    private final Map<Integer, ModifierLayer<IAnimation>> layers = new HashMap<>();

    private final Map<String, KeyframeAnimation> cache = new HashMap<>();

    public EmotecraftBridgeImpl() {
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<EmoteRef> listEmotes() {

        if (EmotecraftRegistry.available()) {
            List<EmoteRef> reg = new ArrayList<>();
            for (EmotecraftRegistry.Entry e : EmotecraftRegistry.all()) {
                reg.add(new EmoteRef(e.id(), e.name(), e.author(), "", e.icon()));
            }
            if (!reg.isEmpty()) {
                return reg;
            }
        }

        List<EmoteRef> out = new ArrayList<>();
        Path dir = emotesDir();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                return n.endsWith(".json") || n.endsWith(".emotecraft");
            }).sorted().forEach(p -> {
                String file = p.getFileName().toString();
                String base = file.contains(".") ? file.substring(0, file.lastIndexOf('.')) : file;

                String icon = Files.isRegularFile(dir.resolve(base + ".png")) ? base + ".png" : "";
                JsonObject meta = readMeta(p);
                out.add(new EmoteRef(file, readMetaName(meta, base), readMetaAuthor(meta), icon, null));
            });
        } catch (IOException e) {
            WHCompanions.LOGGER.warn("Failed to read emotes folder: {}", e.toString());
        }
        return out;
    }

    @Override
    public void playOn(CompanionEntity npc, String emoteId, String emoteName, String emoteAuthor) {
        try {
            boolean reg = EmotecraftRegistry.available();
            WHCompanions.LOGGER.info(
                    "Emotecraft playOn: entity={}, uuid='{}', name='{}', author='{}', registry={}, regSize={}",
                    npc.getId(), emoteId, emoteName, emoteAuthor, reg, reg ? EmotecraftRegistry.all().size() : 0);
            KeyframeAnimation anim = reg ? EmotecraftRegistry.animation(emoteId) : null;
            if (anim == null && reg) {
                anim = EmotecraftRegistry.animationByName(emoteName, emoteAuthor);
                if (anim != null) {
                    WHCompanions.LOGGER.info(
                            "Emotecraft: emote uuid '{}' not in local registry, matched by name '{}' / author '{}'",
                            emoteId, emoteName, emoteAuthor);
                }
            }
            if (anim == null) {
                anim = loadEmote(emoteId);
            }
            if (anim == null) {
                WHCompanions.LOGGER.warn(
                        "Emotecraft: could not resolve emote on this client (uuid='{}', name='{}', author='{}', registry={})",
                        emoteId, emoteName, emoteAuthor, reg);
                return;
            }
            ModifierLayer<IAnimation> layer = layers.computeIfAbsent(npc.getId(), k -> new ModifierLayer<>());
            layer.setAnimation(new KeyframeAnimationPlayer(anim));
        } catch (Throwable t) {
            WHCompanions.LOGGER.warn("Emotecraft: failed to play emote '{}': {}", emoteId, t.toString());
        }
    }

    @Override
    public boolean isPlaying(CompanionEntity npc) {
        ModifierLayer<IAnimation> layer = layers.get(npc.getId());
        try {
            return layer != null && layer.isActive();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void stopOn(CompanionEntity npc) {
        ModifierLayer<IAnimation> layer = layers.get(npc.getId());
        if (layer != null) {
            try {
                layer.setAnimation(null);
            } catch (Throwable ignored) {

            }
        }
    }

    @Override
    public boolean applyBodyTransform(CompanionEntity npc, PoseStack ms, float partial) {
        ModifierLayer<IAnimation> layer = layers.get(npc.getId());
        if (layer == null) {
            return false;
        }
        try {
            if (!layer.isActive()) {
                return false;
            }
            layer.setupAnim(partial);

            Vec3f pos = layer.get3DTransform("body", TransformType.POSITION, partial, Vec3f.ZERO);
            ms.translate(pos.getX(), pos.getY() + 0.7, pos.getZ());
            Vec3f rot = layer.get3DTransform("body", TransformType.ROTATION, partial, Vec3f.ZERO);
            ms.mulPose(Axis.ZP.rotation(rot.getZ()));
            ms.mulPose(Axis.YP.rotation(rot.getY()));
            ms.mulPose(Axis.XP.rotation(rot.getX()));
            ms.translate(0.0, -0.7, 0.0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean applyEmote(CompanionEntity npc, ModelPart head, ModelPart body, ModelPart rightArm,
                              ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        ModifierLayer<IAnimation> layer = layers.get(npc.getId());
        if (layer == null) {
            return false;
        }
        try {
            if (!layer.isActive()) {
                return false;
            }
            float partial = Minecraft.getInstance().getFrameTime();

            layer.setupAnim(partial);
            applyBone(head, PA_BONES[0], layer, partial);
            applyBone(body, PA_BONES[1], layer, partial);
            applyBone(rightArm, PA_BONES[2], layer, partial);
            applyBone(leftArm, PA_BONES[3], layer, partial);
            applyBone(rightLeg, PA_BONES[4], layer, partial);
            applyBone(leftLeg, PA_BONES[5], layer, partial);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void applyBone(ModelPart part, String bone, ModifierLayer<IAnimation> layer, float partial) {
        Vec3f pos = layer.get3DTransform(bone, TransformType.POSITION, partial,
                new Vec3f(part.x, part.y, part.z));
        part.x = pos.getX();
        part.y = pos.getY();
        part.z = pos.getZ();
        Vec3f rot = layer.get3DTransform(bone, TransformType.ROTATION, partial,
                new Vec3f(clampRad(part.xRot), clampRad(part.yRot), clampRad(part.zRot)));
        part.setRotation(rot.getX(), rot.getY(), rot.getZ());
    }

    private static float clampRad(float f) {
        double b = ((double) f + Math.PI) % (Math.PI * 2);
        if (b < 0.0) {
            b += Math.PI * 2;
        }
        return (float) (b - Math.PI);
    }

    private void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || layers.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, ModifierLayer<IAnimation>>> it = layers.entrySet().iterator();
        while (it.hasNext()) {
            ModifierLayer<IAnimation> layer = it.next().getValue();
            try {
                layer.tick();
                if (!layer.isActive()) {
                    it.remove();
                }
            } catch (Throwable t) {
                it.remove();
            }
        }
    }

    private KeyframeAnimation loadEmote(String emoteId) {
        if (cache.containsKey(emoteId)) {
            return cache.get(emoteId);
        }
        KeyframeAnimation anim = null;
        try {
            String json = Files.readString(emotesDir().resolve(emoteId), StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(json);

            List<KeyframeAnimation> list = gson.fromJson(root, EMOTE_LIST_TYPE);
            anim = (list == null || list.isEmpty()) ? null : list.get(0);
        } catch (Throwable t) {
            WHCompanions.LOGGER.warn("Emotecraft: failed to parse emote '{}': {}", emoteId, t.toString());
        }
        cache.put(emoteId, anim);
        return anim;
    }

    private static Path emotesDir() {
        return FMLPaths.GAMEDIR.get().resolve("emotes");
    }

    @javax.annotation.Nullable
    private static JsonObject readMeta(Path p) {
        try {
            JsonElement root = JsonParser.parseReader(Files.newBufferedReader(p, StandardCharsets.UTF_8));
            if (root.isJsonObject()) {
                return root.getAsJsonObject();
            }
            if (root.isJsonArray() && !root.getAsJsonArray().isEmpty()
                    && root.getAsJsonArray().get(0).isJsonObject()) {
                return root.getAsJsonArray().get(0).getAsJsonObject();
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    private static String readMetaName(@javax.annotation.Nullable JsonObject o, String fallback) {
        String v = readStringOrText(o, "name");
        return v.isEmpty() ? fallback : v;
    }

    private static String readMetaAuthor(@javax.annotation.Nullable JsonObject o) {
        return readStringOrText(o, "author");
    }

    private static String readStringOrText(@javax.annotation.Nullable JsonObject o, String key) {
        if (o == null || !o.has(key)) {
            return "";
        }
        JsonElement el = o.get(key);
        if (el.isJsonPrimitive()) {
            return el.getAsString();
        }
        if (el.isJsonObject() && el.getAsJsonObject().has("text")) {
            return el.getAsJsonObject().get("text").getAsString();
        }
        return "";
    }
}
