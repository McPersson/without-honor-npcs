package com.withouthonor.npcs.common.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class PoseJson {

    public static final String[] PART_KEYS = {"head", "body", "arm_r", "arm_l", "leg_r", "leg_l"};

    private PoseJson() {
    }

    public static final class Pose {
        public final float[] angles = new float[18];
        public boolean freeze;
        public final boolean[] hidden = new boolean[6];
        public final float[] bb = new float[2];

        public void clear() {
            for (int i = 0; i < 18; i++) {
                angles[i] = 0F;
            }
            freeze = false;
            for (int i = 0; i < 6; i++) {
                hidden[i] = false;
            }
            bb[0] = 0F;
            bb[1] = 0F;
        }

        public void copyFrom(Pose o) {
            System.arraycopy(o.angles, 0, angles, 0, 18);
            freeze = o.freeze;
            System.arraycopy(o.hidden, 0, hidden, 0, 6);
            bb[0] = o.bb[0];
            bb[1] = o.bb[1];
        }

        public boolean isEmpty() {
            if (freeze || bb[0] > 0F) {
                return false;
            }
            for (boolean h : hidden) {
                if (h) {
                    return false;
                }
            }
            for (float a : angles) {
                if (a != 0F) {
                    return false;
                }
            }
            return true;
        }

        public boolean hasAngles() {
            for (float a : angles) {
                if (a != 0F) {
                    return true;
                }
            }
            return false;
        }
    }

    private static int partIndex(String key) {
        for (int i = 0; i < 6; i++) {
            if (PART_KEYS[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    public static void read(JsonObject profileJson, Pose out) {
        out.clear();
        if (!profileJson.has("pose")) {
            return;
        }
        JsonObject po = profileJson.getAsJsonObject("pose");
        for (int pi = 0; pi < 6; pi++) {
            if (po.has(PART_KEYS[pi])) {
                JsonArray a = po.getAsJsonArray(PART_KEYS[pi]);
                for (int k = 0; k < 3 && k < a.size(); k++) {
                    out.angles[pi * 3 + k] = a.get(k).getAsFloat();
                }
            }
        }
        out.freeze = po.has("freeze") && po.get("freeze").getAsBoolean();
        if (po.has("hide")) {
            for (var el : po.getAsJsonArray("hide")) {
                int idx = partIndex(el.getAsString());
                if (idx >= 0) {
                    out.hidden[idx] = true;
                }
            }
        }
        if (po.has("bb")) {
            JsonArray a = po.getAsJsonArray("bb");
            if (a.size() >= 2) {
                out.bb[0] = a.get(0).getAsFloat();
                out.bb[1] = a.get(1).getAsFloat();
            }
        }
    }

    public static void write(JsonObject profileJson, Pose pose) {
        if (pose.isEmpty()) {
            profileJson.remove("pose");
            return;
        }
        JsonObject po = new JsonObject();
        for (int pi = 0; pi < 6; pi++) {
            int b = pi * 3;
            if (pose.angles[b] != 0F || pose.angles[b + 1] != 0F || pose.angles[b + 2] != 0F) {
                JsonArray arr = new JsonArray();
                arr.add(pose.angles[b]);
                arr.add(pose.angles[b + 1]);
                arr.add(pose.angles[b + 2]);
                po.add(PART_KEYS[pi], arr);
            }
        }
        if (pose.freeze) {
            po.addProperty("freeze", true);
        }
        JsonArray hide = new JsonArray();
        for (int i = 0; i < 6; i++) {
            if (pose.hidden[i]) {
                hide.add(PART_KEYS[i]);
            }
        }
        if (!hide.isEmpty()) {
            po.add("hide", hide);
        }
        if (pose.bb[0] > 0F) {
            JsonArray bb = new JsonArray();
            bb.add(pose.bb[0]);
            bb.add(pose.bb[1]);
            po.add("bb", bb);
        }
        profileJson.add("pose", po);
    }

    public static JsonObject toPoseObject(Pose pose) {
        JsonObject wrapper = new JsonObject();
        write(wrapper, pose);
        return wrapper.has("pose") ? wrapper.getAsJsonObject("pose") : new JsonObject();
    }
}
