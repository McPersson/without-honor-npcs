package com.withouthonor.npcs.common.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.common.dialogue.action.ActionTypes;
import com.withouthonor.npcs.common.dialogue.action.DialogueAction;
import com.withouthonor.npcs.common.dialogue.condition.ConditionTypes;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DialogueChoice {

    private String text;
    @Nullable
    private String next;
    private final List<DialogueCondition> conditions = new ArrayList<>();
    private final List<DialogueAction> actions = new ArrayList<>();

    @Nullable
    private String lockedHint;

    public DialogueChoice(String text, @Nullable String next) {
        this.text = text;
        this.next = next;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Nullable
    public String getNext() {
        return next;
    }

    public void setNext(@Nullable String next) {
        this.next = next;
    }

    public List<DialogueCondition> getConditions() {
        return conditions;
    }

    public List<DialogueAction> getActions() {
        return actions;
    }

    @Nullable
    public String getLockedHint() {
        return lockedHint;
    }

    public void setLockedHint(@Nullable String lockedHint) {
        this.lockedHint = lockedHint;
    }

    public static DialogueChoice fromJson(JsonObject json) {
        DialogueChoice choice = new DialogueChoice(
                json.has("text") ? json.get("text").getAsString() : "",
                json.has("next") && !json.get("next").isJsonNull() ? json.get("next").getAsString() : null);
        if (json.has("conditions")) {
            choice.conditions.addAll(ConditionTypes.parseList(json.getAsJsonArray("conditions")));
        }
        if (json.has("actions")) {
            choice.actions.addAll(ActionTypes.parseList(json.getAsJsonArray("actions")));
        }
        if (json.has("locked_hint")) {
            choice.lockedHint = json.get("locked_hint").getAsString();
        }
        return choice;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        if (next != null) {
            json.addProperty("next", next);
        }
        if (!conditions.isEmpty()) {
            JsonArray conditionsJson = new JsonArray();
            conditions.forEach(c -> conditionsJson.add(c.toJson()));
            json.add("conditions", conditionsJson);
        }
        if (!actions.isEmpty()) {
            JsonArray actionsJson = new JsonArray();
            actions.forEach(a -> actionsJson.add(a.toJson()));
            json.add("actions", actionsJson);
        }
        if (lockedHint != null) {
            json.addProperty("locked_hint", lockedHint);
        }
        return json;
    }
}
