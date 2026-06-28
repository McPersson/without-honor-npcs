package com.withouthonor.npcs.common.entity.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.PoseJson;
import com.withouthonor.npcs.common.profile.ScheduleEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class ScheduleGoal extends Goal {

    private final CompanionEntity npc;
    private final PathNavigation navigation;
    private int recalcPath;
    private int wanderTimer;

    private String appliedSnapshot = "";

    public ScheduleGoal(CompanionEntity npc) {
        this.npc = npc;
        this.navigation = npc.getNavigation();

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return npc.scheduleActive() && activeEntry() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return npc.scheduleActive() && activeEntry() != null;
    }

    @Override
    public void start() {
        this.recalcPath = 0;
    }

    @Override
    public void stop() {
        this.navigation.stop();
        resetPose();
        npc.setScheduleEmote("");
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ScheduleEntry e = activeEntry();
        if (e == null) {
            return;
        }
        BlockPos target = e.pos();
        double cx = target.getX() + 0.5;
        double cz = target.getZ() + 0.5;

        int reach = "wander".equals(e.pose()) ? Math.max(2, e.radius()) : 2;
        boolean atPoint = npc.distanceToSqr(cx, target.getY(), cz) <= (double) (reach * reach);
        if (!atPoint) {
            resetPose();
            npc.setScheduleEmote("");
            if (--this.recalcPath <= 0) {
                this.recalcPath = adjustedTickDelay(10);
                navigation.moveTo(cx, target.getY(), cz, 1.0D);
            }
            return;
        }
        applyPose(e, target, cx, cz);
    }

    private void applyPose(ScheduleEntry e, BlockPos target, double cx, double cz) {
        npc.setScheduleEmote(e.emoteId(), e.emoteName(), e.emoteAuthor());
        if (e.isCustomPose()) {
            navigation.stop();
            if (npc.isSleeping()) {
                npc.stopSleeping();
            }
            npc.setSitting(false);
            applyCustomPose(e);
            return;
        }
        revertCustom();
        switch (e.pose()) {
            case "wander" -> {
                resetPose();
                if (--this.wanderTimer <= 0) {
                    this.wanderTimer = 60 + npc.getRandom().nextInt(80);
                    int r = Math.max(1, e.radius());
                    double wx = cx + npc.getRandom().nextInt(2 * r + 1) - r;
                    double wz = cz + npc.getRandom().nextInt(2 * r + 1) - r;
                    navigation.moveTo(wx, target.getY(), wz, 0.7D);
                }
            }
            case "sit" -> {
                navigation.stop();
                if (npc.isSleeping()) {
                    npc.stopSleeping();
                }
                npc.setSitting(true);
            }
            case "sleep" -> {
                navigation.stop();
                npc.setSitting(false);
                if (!npc.isSleeping()) {
                    npc.startSleeping(target);
                }
            }
            default -> {
                navigation.stop();
                resetPose();
            }
        }
    }

    private void resetPose() {
        revertCustom();
        if (npc.isSleeping()) {
            npc.stopSleeping();
        }
        npc.setSitting(false);
    }

    private void applyCustomPose(ScheduleEntry e) {
        if (e.poseSnapshot().equals(appliedSnapshot)) {
            return;
        }
        PoseJson.Pose pose = new PoseJson.Pose();
        float[] tf = {0F, 0F, 0F, 0F, 0F, 0F, 1F, 1F, 1F};
        parseSnapshot(e.poseSnapshot(), pose, tf);
        npc.applyScheduledVisuals(pose, tf);
        appliedSnapshot = e.poseSnapshot();
    }

    private void revertCustom() {
        if (!appliedSnapshot.isEmpty()) {
            npc.revertScheduledVisuals();
            appliedSnapshot = "";
        }
    }

    private static void parseSnapshot(String snapshot, PoseJson.Pose poseOut, float[] tf) {
        poseOut.clear();
        try {
            JsonObject o = JsonParser.parseString(snapshot).getAsJsonObject();
            if (o.has("pose")) {
                JsonObject wrapper = new JsonObject();
                wrapper.add("pose", o.get("pose"));
                PoseJson.read(wrapper, poseOut);
            }
            if (o.has("transform")) {
                JsonObject t = o.getAsJsonObject("transform");
                tf[0] = tf(t, "rot_x", 0F);
                tf[1] = tf(t, "rot_y", 0F);
                tf[2] = tf(t, "rot_z", 0F);
                tf[3] = tf(t, "pos_x", 0F);
                tf[4] = tf(t, "pos_y", 0F);
                tf[5] = tf(t, "pos_z", 0F);
                tf[6] = tf(t, "scale_x", 1F);
                tf[7] = tf(t, "scale_y", 1F);
                tf[8] = tf(t, "scale_z", 1F);
            }
        } catch (Exception ignored) {

        }
    }

    private static float tf(JsonObject t, String key, float def) {
        return t.has(key) && !t.get(key).isJsonNull() ? t.get(key).getAsFloat() : def;
    }

    @Nullable
    private ScheduleEntry activeEntry() {
        List<ScheduleEntry> schedule = npc.getSchedule();
        if (schedule.isEmpty()) {
            return null;
        }
        int now = minutesOfDay(npc.level().getDayTime());
        ScheduleEntry best = null;
        int bestTime = -1;
        ScheduleEntry latest = null;
        int latestTime = -1;
        for (ScheduleEntry e : schedule) {
            if (e.time() > latestTime) {
                latestTime = e.time();
                latest = e;
            }
            if (e.time() <= now && e.time() > bestTime) {
                bestTime = e.time();
                best = e;
            }
        }
        return best != null ? best : latest;
    }

    public static int minutesOfDay(long dayTime) {
        long t = ((dayTime % 24000L) + 24000L) % 24000L;
        int hour = (int) ((t / 1000L + 6L) % 24L);
        int minute = (int) ((t % 1000L) * 60L / 1000L);
        return hour * 60 + minute;
    }
}
