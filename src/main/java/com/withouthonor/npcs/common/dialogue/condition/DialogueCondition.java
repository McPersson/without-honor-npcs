package com.withouthonor.npcs.common.dialogue.condition;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public interface DialogueCondition {

    record Context(ServerPlayer player, @Nullable CompanionEntity npc) {
    }

    String type();

    boolean test(Context ctx);

    JsonObject toJson();

    static boolean testAll(Iterable<DialogueCondition> conditions, Context ctx) {
        for (DialogueCondition condition : conditions) {
            if (!condition.test(ctx)) {
                return false;
            }
        }
        return true;
    }
}
