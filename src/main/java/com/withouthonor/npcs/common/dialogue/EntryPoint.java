package com.withouthonor.npcs.common.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.common.dialogue.condition.ConditionTypes;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EntryPoint {

    private final List<DialogueCondition> conditions = new ArrayList<>();
    private String dialogueId;

    @Nullable
    private EmoteIcon indicator;

    public EntryPoint(String dialogueId) {
        this.dialogueId = dialogueId;
    }

    @Nullable
    public EmoteIcon getIndicator() {
        return indicator;
    }

    public void setIndicator(@Nullable EmoteIcon indicator) {
        this.indicator = indicator;
    }

    public List<DialogueCondition> getConditions() {
        return conditions;
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public void setDialogueId(String dialogueId) {
        this.dialogueId = dialogueId;
    }

    public boolean matches(DialogueCondition.Context ctx) {
        return DialogueCondition.testAll(conditions, ctx);
    }

    public static EntryPoint fromJson(JsonObject json) {
        EntryPoint entry = new EntryPoint(json.get("dialogue").getAsString());
        if (json.has("conditions")) {
            entry.conditions.addAll(ConditionTypes.parseList(json.getAsJsonArray("conditions")));
        }
        if (json.has("indicator")) {
            String id = json.get("indicator").getAsString();
            if (!id.isEmpty() && !id.equals("none")) {
                entry.indicator = EmoteIcon.byId(id);
            }
        }
        return entry;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("dialogue", dialogueId);
        if (!conditions.isEmpty()) {
            JsonArray conditionsJson = new JsonArray();
            conditions.forEach(c -> conditionsJson.add(c.toJson()));
            json.add("conditions", conditionsJson);
        }
        if (indicator != null) {
            json.addProperty("indicator", indicator.id());
        }
        return json;
    }
}
