package com.withouthonor.npcs.common.dialogue.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.common.storage.PlayerStateManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Conditions {

    private Conditions() {
    }

    public record HeldItem(ResourceLocation itemId) implements DialogueCondition {

        @Override
        public String type() {
            return "held_item";
        }

        @Override
        public boolean test(Context ctx) {
            ItemStack held = ctx.player().getMainHandItem();
            return !held.isEmpty() && itemId.equals(ForgeRegistries.ITEMS.getKey(held.getItem()));
        }

        public static HeldItem fromJson(JsonObject json) {
            return new HeldItem(ResourceLocation.parse(json.get("item").getAsString()));
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("item", itemId.toString());
            return json;
        }
    }

    public record PlayerName(Set<String> namesLower) implements DialogueCondition {

        @Override
        public String type() {
            return "player";
        }

        @Override
        public boolean test(Context ctx) {
            return namesLower.contains(ctx.player().getGameProfile().getName().toLowerCase(Locale.ROOT));
        }

        public static PlayerName fromJson(JsonObject json) {
            Set<String> names = new HashSet<>();
            json.getAsJsonArray("names").forEach(e -> names.add(e.getAsString().toLowerCase(Locale.ROOT)));
            return new PlayerName(names);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            JsonArray names = new JsonArray();
            namesLower.forEach(names::add);
            json.add("names", names);
            return json;
        }
    }

    public record Permission(int level) implements DialogueCondition {

        @Override
        public String type() {
            return "permission";
        }

        @Override
        public boolean test(Context ctx) {
            return ctx.player().hasPermissions(level);
        }

        public static Permission fromJson(JsonObject json) {
            return new Permission(json.get("level").getAsInt());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("level", level);
            return json;
        }
    }

    public record Random(int chancePercent) implements DialogueCondition {

        @Override
        public String type() {
            return "random";
        }

        @Override
        public boolean test(Context ctx) {
            return ctx.player().getRandom().nextInt(100) < chancePercent;
        }

        public static Random fromJson(JsonObject json) {
            return new Random(json.get("chance").getAsInt());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("chance", chancePercent);
            return json;
        }
    }

    public record Flag(String flag, boolean value) implements DialogueCondition {

        @Override
        public String type() {
            return "flag";
        }

        @Override
        public boolean test(Context ctx) {
            return PlayerStateManager.get(ctx.player().server).hasFlag(ctx.player().getUUID(), flag) == value;
        }

        public static Flag fromJson(JsonObject json) {
            return new Flag(json.get("flag").getAsString(),
                    !json.has("value") || json.get("value").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("flag", flag);
            json.addProperty("value", value);
            return json;
        }
    }

    public record VarEquals(String name, String value, boolean ignoreCase) implements DialogueCondition {

        @Override
        public String type() {
            return "var_equals";
        }

        @Override
        public boolean test(Context ctx) {
            String v = PlayerStateManager.get(ctx.player().server).getVar(ctx.player().getUUID(), name);
            return ignoreCase ? v.equalsIgnoreCase(value) : v.equals(value);
        }

        public static VarEquals fromJson(JsonObject json) {
            return new VarEquals(json.get("name").getAsString(),
                    json.has("value") ? json.get("value").getAsString() : "",
                    !json.has("ignore_case") || json.get("ignore_case").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("name", name);
            json.addProperty("value", value);
            if (!ignoreCase) {
                json.addProperty("ignore_case", false);
            }
            return json;
        }
    }

    public record Reputation(String faction, Integer min, Integer max) implements DialogueCondition {

        @Override
        public String type() {
            return "reputation";
        }

        @Override
        public boolean test(Context ctx) {
            int value = PlayerStateManager.get(ctx.player().server)
                    .getReputation(ctx.player().getUUID(), faction);
            return (min == null || value >= min) && (max == null || value <= max);
        }

        public static Reputation fromJson(JsonObject json) {
            return new Reputation(json.get("faction").getAsString(),
                    json.has("min") ? json.get("min").getAsInt() : null,
                    json.has("max") ? json.get("max").getAsInt() : null);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("faction", faction);
            if (min != null) {
                json.addProperty("min", min);
            }
            if (max != null) {
                json.addProperty("max", max);
            }
            return json;
        }
    }

    public record Score(String objective, Integer min, Integer max) implements DialogueCondition {

        @Override
        public String type() {
            return "score";
        }

        @Override
        public boolean test(Context ctx) {
            Scoreboard scoreboard = ctx.player().getScoreboard();
            Objective obj = scoreboard.getObjective(objective);
            if (obj == null || !scoreboard.hasPlayerScore(ctx.player().getScoreboardName(), obj)) {
                return false;
            }
            int score = scoreboard.getOrCreatePlayerScore(ctx.player().getScoreboardName(), obj).getScore();
            return (min == null || score >= min) && (max == null || score <= max);
        }

        public static Score fromJson(JsonObject json) {
            return new Score(json.get("objective").getAsString(),
                    json.has("min") ? json.get("min").getAsInt() : null,
                    json.has("max") ? json.get("max").getAsInt() : null);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("objective", objective);
            if (min != null) {
                json.addProperty("min", min);
            }
            if (max != null) {
                json.addProperty("max", max);
            }
            return json;
        }
    }
}
