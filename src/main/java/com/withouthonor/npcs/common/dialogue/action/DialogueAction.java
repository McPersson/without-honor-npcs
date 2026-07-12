package com.withouthonor.npcs.common.dialogue.action;

import com.google.gson.JsonObject;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;

public interface DialogueAction {

    String type();

    void execute(DialogueCondition.Context ctx);

    JsonObject toJson();

    static void executeAll(Iterable<DialogueAction> actions, DialogueCondition.Context ctx) {
        java.util.List<DialogueAction> list = new java.util.ArrayList<>();
        actions.forEach(list::add);
        executeFrom(list, 0, ctx);
    }

    /**
     * Выполняет список с позиции {@code start}. Встретив {@link Actions.Wait} — останавливается и
     * кладёт остаток в отложенную очередь NPC (продолжит через wait.seconds). Без NPC в контексте
     * wait просто игнорируется (очереди негде жить), остальные действия идут дальше.
     */
    static void executeFrom(java.util.List<DialogueAction> actions, int start, DialogueCondition.Context ctx) {
        for (int i = start; i < actions.size(); i++) {
            DialogueAction action = actions.get(i);
            if (action instanceof Actions.Wait wait) {
                com.withouthonor.npcs.common.entity.CompanionEntity npc = ctx.npc();
                if (npc == null) {
                    // Негде хранить очередь — пауза игнорируется, действия продолжаются.
                    com.withouthonor.npcs.WHCompanions.LOGGER.warn(
                            "Действие wait вне контекста NPC игнорировано — пауза пропущена");
                    continue;
                }
                java.util.List<DialogueAction> rest =
                        new java.util.ArrayList<>(actions.subList(i + 1, actions.size()));
                if (!rest.isEmpty() && ctx.player() != null) {
                    npc.queueDelayedActions(rest, ctx.player().getUUID(),
                            Math.max(1, Math.round(wait.seconds() * 20.0F)));
                }
                return; // остаток исполнит тик NPC
            }
            action.execute(ctx);
        }
    }
}
