package com.withouthonor.npcs.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.client.gui.editor.ScheduleScreen;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public final class SchedulePointPicker {

    @Nullable
    private static Screen returnParent;
    @Nullable
    private static JsonObject profileJson;
    @Nullable
    private static CompanionEntity npc;
    private static int index = -1;

    private SchedulePointPicker() {
    }

    public static void begin(@Nullable Screen parent, JsonObject profileJson,
                             @Nullable CompanionEntity npc, int index) {
        SchedulePointPicker.returnParent = parent;
        SchedulePointPicker.profileJson = profileJson;
        SchedulePointPicker.npc = npc;
        SchedulePointPicker.index = index;
    }

    public static boolean isPending() {
        return index >= 0 && profileJson != null;
    }

    public static void cancel() {
        returnParent = null;
        profileJson = null;
        npc = null;
        index = -1;
    }

    public static void complete(BlockPos pos) {
        if (!isPending()) {
            return;
        }
        if (profileJson.has("schedule")) {
            JsonArray arr = profileJson.getAsJsonArray("schedule");
            if (index < arr.size()) {
                JsonObject e = arr.get(index).getAsJsonObject();
                e.addProperty("x", pos.getX());
                e.addProperty("y", pos.getY());
                e.addProperty("z", pos.getZ());
            }
        }
        Screen parent = returnParent;
        JsonObject pj = profileJson;
        CompanionEntity n = npc;
        cancel();
        Minecraft.getInstance().setScreen(new ScheduleScreen(parent, pj, n));
    }
}
