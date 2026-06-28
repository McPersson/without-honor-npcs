package com.withouthonor.npcs.common.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.common.dialogue.action.Actions;
import com.withouthonor.npcs.common.dialogue.condition.ConditionTypes;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import javax.annotation.Nullable;
import java.util.List;

public record TradeOffer(Actions.ItemSpec costA, @Nullable Actions.ItemSpec costB,
                         Actions.ItemSpec result, int maxUses, boolean sharedLimit, int playerXp,
                         List<DialogueCondition> conditions) {

    public static TradeOffer fromJson(JsonObject json) {
        return new TradeOffer(
                Actions.ItemSpec.fromJson(json.getAsJsonObject("buy")),
                json.has("buy_b") ? Actions.ItemSpec.fromJson(json.getAsJsonObject("buy_b")) : null,
                Actions.ItemSpec.fromJson(json.getAsJsonObject("sell")),
                json.has("max_uses") ? json.get("max_uses").getAsInt() : 0,
                json.has("shared_limit") && json.get("shared_limit").getAsBoolean(),
                json.has("xp") ? json.get("xp").getAsInt() : 0,
                json.has("conditions")
                        ? ConditionTypes.parseList(json.getAsJsonArray("conditions"))
                        : List.of());
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.add("buy", costA.toJson());
        if (costB != null) {
            json.add("buy_b", costB.toJson());
        }
        json.add("sell", result.toJson());
        if (maxUses > 0) {
            json.addProperty("max_uses", maxUses);
        }
        if (sharedLimit) {
            json.addProperty("shared_limit", true);
        }
        if (playerXp > 0) {
            json.addProperty("xp", playerXp);
        }
        if (!conditions.isEmpty()) {
            JsonArray array = new JsonArray();
            conditions.forEach(c -> array.add(c.toJson()));
            json.add("conditions", array);
        }
        return json;
    }

    @Nullable
    public MerchantOffer toMerchantOffer(int uses) {
        ItemStack a = costA.toStack();
        ItemStack r = result.toStack();
        if (a.isEmpty() || r.isEmpty()) {
            return null;
        }
        ItemStack b = costB != null ? costB.toStack() : ItemStack.EMPTY;
        return new MerchantOffer(a, b, r, uses,
                maxUses > 0 ? maxUses : Integer.MAX_VALUE, playerXp, 1.0F);
    }
}
