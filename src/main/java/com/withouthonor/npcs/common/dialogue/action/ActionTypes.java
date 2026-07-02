package com.withouthonor.npcs.common.dialogue.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ActionTypes {

    private static final Map<String, Function<JsonObject, DialogueAction>> PARSERS = new HashMap<>();

    static {
        register("give_item", Actions.GiveItem::fromJson);
        register("take_item", Actions.TakeItem::fromJson);
        register("set_flag", Actions.SetFlag::fromJson);
        register("run_command", Actions.RunCommand::fromJson);
        register("sound", Actions.Sound::fromJson);
        register("title", Actions.Title::fromJson);
        register("reputation", Actions.Reputation::fromJson);
        register("open_trade", Actions.OpenTrade::fromJson);
        register("say", Actions.Say::fromJson);
        register("emote", Actions.Emote::fromJson);
        register("emotecraft_emote", Actions.EmotecraftEmote::fromJson);
        register("stop_emotecraft_emote", Actions.StopEmotecraftEmote::fromJson);
        register("monologue", Actions.Monologue::fromJson);
        register("stop_music", Actions.StopMusic::fromJson);
        register("follow", Actions.Follow::fromJson);
        register("stop_follow", Actions.StopFollow::fromJson);
        register("follow_wait", Actions.FollowWait::fromJson);
        register("effect", Actions.Effect::fromJson);
    }

    private ActionTypes() {
    }

    public static void register(String type, Function<JsonObject, DialogueAction> parser) {
        if (PARSERS.putIfAbsent(type, parser) != null) {
            throw new IllegalArgumentException("Action type already registered: " + type);
        }
    }

    public static DialogueAction parse(JsonObject json) {
        if (!json.has("type")) {
            throw new JsonParseException("Action without 'type': " + json);
        }
        String type = json.get("type").getAsString();
        Function<JsonObject, DialogueAction> parser = PARSERS.get(type);
        if (parser == null) {
            throw new JsonParseException("Unknown action type: " + type);
        }
        return parser.apply(json);
    }

    public static List<DialogueAction> parseList(JsonArray array) {
        List<DialogueAction> list = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            list.add(parse(element.getAsJsonObject()));
        }
        return list;
    }
}
