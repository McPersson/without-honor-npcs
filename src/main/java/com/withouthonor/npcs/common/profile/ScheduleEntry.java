package com.withouthonor.npcs.common.profile;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

public record ScheduleEntry(int time, int x, int y, int z, String pose, int radius,
                            String poseName, String poseSnapshot, String emoteId,
                            String emoteName, String emoteAuthor) {

    public ScheduleEntry(int time, int x, int y, int z, String pose, int radius,
                         String poseName, String poseSnapshot, String emoteId) {
        this(time, x, y, z, pose, radius, poseName, poseSnapshot, emoteId, "", "");
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    public boolean isCustomPose() {
        return poseSnapshot != null && !poseSnapshot.isEmpty();
    }

    public static ScheduleEntry fromJson(JsonObject o) {
        return new ScheduleEntry(
                o.has("time") ? Math.max(0, Math.min(1439, o.get("time").getAsInt())) : 0,
                o.has("x") ? o.get("x").getAsInt() : 0,
                o.has("y") ? o.get("y").getAsInt() : 64,
                o.has("z") ? o.get("z").getAsInt() : 0,
                o.has("pose") ? o.get("pose").getAsString() : "stand",
                o.has("radius") ? Math.max(0, Math.min(32, o.get("radius").getAsInt())) : 4,
                o.has("pose_name") ? o.get("pose_name").getAsString() : "",
                o.has("pose_snapshot") ? o.get("pose_snapshot").toString() : "",
                o.has("emote_id") ? o.get("emote_id").getAsString() : "",
                o.has("emote_name") ? o.get("emote_name").getAsString() : "",
                o.has("emote_author") ? o.get("emote_author").getAsString() : "");
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("time", time);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        o.addProperty("pose", pose);
        o.addProperty("radius", radius);
        if (poseName != null && !poseName.isEmpty()) {
            o.addProperty("pose_name", poseName);
        }
        if (poseSnapshot != null && !poseSnapshot.isEmpty()) {
            o.add("pose_snapshot", com.google.gson.JsonParser.parseString(poseSnapshot));
        }
        if (emoteId != null && !emoteId.isEmpty()) {
            o.addProperty("emote_id", emoteId);
        }
        if (emoteName != null && !emoteName.isEmpty()) {
            o.addProperty("emote_name", emoteName);
        }
        if (emoteAuthor != null && !emoteAuthor.isEmpty()) {
            o.addProperty("emote_author", emoteAuthor);
        }
        return o;
    }
}
