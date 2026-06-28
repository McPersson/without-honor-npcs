package com.withouthonor.npcs.common.dialogue.action;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;

public interface DialogueAction {

    String type();

    void execute(DialogueCondition.Context ctx);

    JsonObject toJson();

    static void executeAll(Iterable<DialogueAction> actions, DialogueCondition.Context ctx) {
        for (DialogueAction action : actions) {
            action.execute(ctx);
        }
    }
}
