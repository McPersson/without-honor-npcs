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

    public record Weather(String state) implements DialogueCondition {

        @Override
        public String type() {
            return "weather";
        }

        @Override
        public boolean test(Context ctx) {
            net.minecraft.world.level.Level lvl = ctx.player().level();
            boolean thunder = lvl.isThundering();
            boolean rain = lvl.isRaining();
            return switch (state) {
                case "thunder" -> thunder;
                case "rain" -> rain && !thunder;
                default -> !rain;
            };
        }

        public static Weather fromJson(JsonObject json) {
            return new Weather(json.has("state") ? json.get("state").getAsString() : "clear");
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("state", state);
            return json;
        }
    }

    public record Time(int minTicks, int maxTicks) implements DialogueCondition {

        @Override
        public String type() {
            return "time";
        }

        @Override
        public boolean test(Context ctx) {
            long dt = ((ctx.player().level().getDayTime() % 24000L) + 24000L) % 24000L;
            int t = (int) dt;
            if (minTicks <= maxTicks) {
                return t >= minTicks && t <= maxTicks;
            }
            return t >= minTicks || t <= maxTicks;
        }

        public static Time fromJson(JsonObject json) {
            return new Time(json.has("min") ? json.get("min").getAsInt() : 0,
                    json.has("max") ? json.get("max").getAsInt() : 24000);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("min", minTicks);
            json.addProperty("max", maxTicks);
            return json;
        }
    }

    public record PlayerState(Integer hpMin, Integer hpMax, Integer foodMin, Integer foodMax,
                              java.util.List<ResourceLocation> effects, String effectMode)
            implements DialogueCondition {

        @Override
        public String type() {
            return "player_state";
        }

        @Override
        public boolean test(Context ctx) {
            net.minecraft.server.level.ServerPlayer p = ctx.player();
            float hp = p.getHealth();
            if (hpMin != null && hp < hpMin) {
                return false;
            }
            if (hpMax != null && hp > hpMax) {
                return false;
            }
            int food = p.getFoodData().getFoodLevel();
            if (foodMin != null && food < foodMin) {
                return false;
            }
            if (foodMax != null && food > foodMax) {
                return false;
            }
            switch (effectMode) {
                case "any" -> {
                    for (ResourceLocation rl : effects) {
                        net.minecraft.world.effect.MobEffect e = ForgeRegistries.MOB_EFFECTS.getValue(rl);
                        if (e != null && p.hasEffect(e)) {
                            return true;
                        }
                    }
                    return effects.isEmpty();
                }
                case "all" -> {
                    for (ResourceLocation rl : effects) {
                        net.minecraft.world.effect.MobEffect e = ForgeRegistries.MOB_EFFECTS.getValue(rl);
                        if (e == null || !p.hasEffect(e)) {
                            return false;
                        }
                    }
                }
                case "any_effect" -> {
                    return !p.getActiveEffects().isEmpty();
                }
                case "no_effect" -> {
                    return p.getActiveEffects().isEmpty();
                }
                default -> {
                }
            }
            return true;
        }

        public static PlayerState fromJson(JsonObject json) {
            java.util.List<ResourceLocation> effects = new java.util.ArrayList<>();
            if (json.has("effects")) {
                json.getAsJsonArray("effects").forEach(e -> {
                    ResourceLocation rl = ResourceLocation.tryParse(e.getAsString());
                    if (rl != null) {
                        effects.add(rl);
                    }
                });
            }
            return new PlayerState(
                    json.has("hp_min") ? json.get("hp_min").getAsInt() : null,
                    json.has("hp_max") ? json.get("hp_max").getAsInt() : null,
                    json.has("food_min") ? json.get("food_min").getAsInt() : null,
                    json.has("food_max") ? json.get("food_max").getAsInt() : null,
                    effects,
                    json.has("effect_mode") ? json.get("effect_mode").getAsString() : "off");
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            if (hpMin != null) {
                json.addProperty("hp_min", hpMin);
            }
            if (hpMax != null) {
                json.addProperty("hp_max", hpMax);
            }
            if (foodMin != null) {
                json.addProperty("food_min", foodMin);
            }
            if (foodMax != null) {
                json.addProperty("food_max", foodMax);
            }
            if (!"off".equals(effectMode)) {
                json.addProperty("effect_mode", effectMode);
                JsonArray arr = new JsonArray();
                effects.forEach(rl -> arr.add(rl.toString()));
                json.add("effects", arr);
            }
            return json;
        }
    }

    public record NpcHealth(Integer minPct, Integer maxPct) implements DialogueCondition {

        @Override
        public String type() {
            return "npc_health";
        }

        @Override
        public boolean test(Context ctx) {
            if (ctx.npc() == null) {
                return false;
            }
            float max = ctx.npc().getMaxHealth();
            if (max <= 0.0F) {
                return false;
            }
            // Проценты включительно; null-граница не проверяется.
            int pct = Math.round(ctx.npc().getHealth() / max * 100.0F);
            return (minPct == null || pct >= minPct) && (maxPct == null || pct <= maxPct);
        }

        public static NpcHealth fromJson(JsonObject json) {
            return new NpcHealth(
                    json.has("min") ? json.get("min").getAsInt() : null,
                    json.has("max") ? json.get("max").getAsInt() : null);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            if (minPct != null) {
                json.addProperty("min", minPct);
            }
            if (maxPct != null) {
                json.addProperty("max", maxPct);
            }
            return json;
        }
    }

    public record PlayerLevel(Integer min, Integer max) implements DialogueCondition {

        @Override
        public String type() {
            return "player_level";
        }

        @Override
        public boolean test(Context ctx) {
            // Уровни опыта включительно; null-граница не проверяется.
            int level = ctx.player().experienceLevel;
            return (min == null || level >= min) && (max == null || level <= max);
        }

        public static PlayerLevel fromJson(JsonObject json) {
            return new PlayerLevel(
                    json.has("min") ? json.get("min").getAsInt() : null,
                    json.has("max") ? json.get("max").getAsInt() : null);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            if (min != null) {
                json.addProperty("min", min);
            }
            if (max != null) {
                json.addProperty("max", max);
            }
            return json;
        }
    }

    public record Inverted(DialogueCondition inner) implements DialogueCondition {

        @Override
        public String type() {
            return inner.type();
        }

        @Override
        public boolean test(Context ctx) {
            return !inner.test(ctx);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = inner.toJson();
            json.addProperty("invert", true);
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
