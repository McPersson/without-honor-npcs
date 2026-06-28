package com.withouthonor.npcs.client;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.editor.RespawnScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public final class RespawnHomePicker {

    @Nullable
    private static Screen returnParent;
    @Nullable
    private static JsonObject profileJson;
    private static boolean pending;

    private RespawnHomePicker() {
    }

    public static void begin(@Nullable Screen parent, JsonObject profileJson) {
        RespawnHomePicker.returnParent = parent;
        RespawnHomePicker.profileJson = profileJson;
        RespawnHomePicker.pending = true;
    }

    public static boolean isPending() {
        return pending && profileJson != null;
    }

    public static void cancel() {
        returnParent = null;
        profileJson = null;
        pending = false;
    }

    public static void complete(BlockPos pos) {
        if (!isPending()) {
            return;
        }
        JsonObject home = new JsonObject();
        home.addProperty("x", pos.getX());
        home.addProperty("y", pos.getY());
        home.addProperty("z", pos.getZ());
        home.addProperty("dim", Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.dimension().location().toString() : "minecraft:overworld");
        profileJson.add("respawn_home", home);
        profileJson.addProperty("respawn_location", "home");
        Screen parent = returnParent;
        JsonObject pj = profileJson;
        cancel();
        Minecraft.getInstance().setScreen(new RespawnScreen(parent, pj));
    }
}
