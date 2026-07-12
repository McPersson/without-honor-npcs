package com.withouthonor.npcs.common.reputation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Faction {

    public record Tier(String name, int min, int color, float priceMult) {
    }

    public static final List<Tier> DEFAULT_TIERS = List.of(
            new Tier("Hostile", Integer.MIN_VALUE, 0xFFFF5555, 1.0F),
            new Tier("Neutral", -20, 0xFFAAAAAA, 1.0F),
            new Tier("Friendly", 20, 0xFF55FF55, 1.0F),
            new Tier("Honored", 60, 0xFFFFAA00, 1.0F));

    private final String id;
    private final String name;

    private final int color;

    private final int killPenalty;

    private final List<Tier> tiers;

    private final List<String> hostileTo;

    /** false (деф.) — свои НЕ бьют своих (урон по своей фракции отменяется); true — разрешён. */
    private final boolean friendlyFire;

    public Faction(String id, String name, int color, int killPenalty, List<Tier> tiers) {
        this(id, name, color, killPenalty, tiers, List.of(), false);
    }

    public Faction(String id, String name, int color, int killPenalty, List<Tier> tiers,
                   List<String> hostileTo) {
        this(id, name, color, killPenalty, tiers, hostileTo, false);
    }

    public Faction(String id, String name, int color, int killPenalty, List<Tier> tiers,
                   List<String> hostileTo, boolean friendlyFire) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.killPenalty = killPenalty;
        List<Tier> sorted = new ArrayList<>(tiers.isEmpty() ? DEFAULT_TIERS : tiers);
        sorted.sort(Comparator.comparingInt(Tier::min));
        this.tiers = List.copyOf(sorted);
        this.hostileTo = List.copyOf(hostileTo);
        this.friendlyFire = friendlyFire;
    }

    /** true — разрешён урон по своей фракции; false (деф.) — свои защищены. */
    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public int getKillPenalty() {
        return killPenalty;
    }

    public List<Tier> getTiers() {
        return tiers;
    }

    public List<String> getHostileTo() {
        return hostileTo;
    }

    public boolean isHostileValue(int value) {
        return tiers.size() > 1 && tierFor(value) == tiers.get(0);
    }

    public Tier tierFor(int value) {
        Tier result = tiers.get(0);
        for (Tier tier : tiers) {
            if (value >= tier.min()) {
                result = tier;
            }
        }
        return result;
    }

    public static Faction fromJson(JsonObject json) {
        if (!json.has("id")) {
            throw new JsonParseException("Faction without 'id'");
        }
        String id = json.get("id").getAsString();
        if (!id.matches("[a-z0-9_]{1,32}")) {
            throw new JsonParseException("Bad faction id '" + id + "' (a-z, 0-9, _)");
        }
        List<Tier> tiers = new ArrayList<>();
        if (json.has("tiers")) {
            for (JsonElement e : json.getAsJsonArray("tiers")) {
                JsonObject t = e.getAsJsonObject();
                tiers.add(new Tier(
                        t.get("name").getAsString(),
                        t.has("min") ? t.get("min").getAsInt() : Integer.MIN_VALUE,
                        t.has("color") ? parseColor(t.get("color").getAsString()) : 0xFFAAAAAA,
                        t.has("price") ? t.get("price").getAsFloat() : 1.0F));
            }
        }
        List<String> hostileTo = new ArrayList<>();
        if (json.has("hostile_to")) {
            json.getAsJsonArray("hostile_to").forEach(e -> hostileTo.add(e.getAsString()));
        }
        return new Faction(id,
                json.has("name") ? json.get("name").getAsString() : id,
                json.has("color") ? parseColor(json.get("color").getAsString()) : 0xFF55FFFF,
                json.has("kill_penalty") ? json.get("kill_penalty").getAsInt() : 0,
                tiers, hostileTo,
                json.has("friendly_fire") && json.get("friendly_fire").getAsBoolean());
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("color", formatColor(color));
        if (killPenalty != 0) {
            json.addProperty("kill_penalty", killPenalty);
        }
        if (friendlyFire) {
            json.addProperty("friendly_fire", true);
        }
        if (!hostileTo.isEmpty()) {
            JsonArray hostile = new JsonArray();
            hostileTo.forEach(hostile::add);
            json.add("hostile_to", hostile);
        }
        JsonArray tiersJson = new JsonArray();
        for (Tier tier : tiers) {
            JsonObject t = new JsonObject();
            t.addProperty("name", tier.name());
            if (tier.min() != Integer.MIN_VALUE) {
                t.addProperty("min", tier.min());
            }
            t.addProperty("color", formatColor(tier.color()));
            if (tier.priceMult() != 1.0F) {
                t.addProperty("price", tier.priceMult());
            }
            tiersJson.add(t);
        }
        json.add("tiers", tiersJson);
        return json;
    }

    public static int parseColor(String hex) {
        try {
            return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            throw new JsonParseException("Bad color '" + hex + "', expected #RRGGBB");
        }
    }

    public static String formatColor(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }
}
