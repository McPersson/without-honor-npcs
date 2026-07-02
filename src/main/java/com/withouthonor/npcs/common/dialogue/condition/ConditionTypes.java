package com.withouthonor.npcs.common.dialogue.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ConditionTypes {

    private static final Map<String, Function<JsonObject, DialogueCondition>> PARSERS = new HashMap<>();

    static {
        register("items", ItemsCondition::fromJson);
        register("held_item", Conditions.HeldItem::fromJson);
        register("player", Conditions.PlayerName::fromJson);
        register("flag", Conditions.Flag::fromJson);
        register("permission", Conditions.Permission::fromJson);
        register("random", Conditions.Random::fromJson);
        register("score", Conditions.Score::fromJson);
        register("reputation", Conditions.Reputation::fromJson);
        register("var_equals", Conditions.VarEquals::fromJson);
        register("weather", Conditions.Weather::fromJson);
        register("time", Conditions.Time::fromJson);
        register("player_state", Conditions.PlayerState::fromJson);
    }

    private ConditionTypes() {
    }

    public static void register(String type, Function<JsonObject, DialogueCondition> parser) {
        if (PARSERS.putIfAbsent(type, parser) != null) {
            throw new IllegalArgumentException("Condition type already registered: " + type);
        }
    }

    public static DialogueCondition parse(JsonObject json) {
        if (!json.has("type")) {
            throw new JsonParseException("Condition without 'type': " + json);
        }
        String type = json.get("type").getAsString();
        Function<JsonObject, DialogueCondition> parser = PARSERS.get(type);
        if (parser == null) {
            throw new JsonParseException("Unknown condition type: " + type);
        }
        DialogueCondition condition = parser.apply(json);
        if (json.has("invert") && json.get("invert").getAsBoolean()) {
            return new Conditions.Inverted(condition);
        }
        return condition;
    }

    public static List<DialogueCondition> parseList(JsonArray array) {
        List<DialogueCondition> list = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            list.add(parse(element.getAsJsonObject()));
        }
        return list;
    }
}
