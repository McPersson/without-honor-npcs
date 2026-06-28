package com.withouthonor.npcs.common.dialogue.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ItemsCondition implements DialogueCondition {

    public static final int MAX_SLOTS = 4;

    public enum NbtMode {
        IGNORE, EXACT, TAG;

        static NbtMode fromJson(@Nullable JsonElement e) {
            return e == null ? IGNORE : valueOf(e.getAsString().toUpperCase(Locale.ROOT));
        }
    }

    public record Slot(@Nullable ResourceLocation itemId, @Nullable ResourceLocation tagId,
                       int count, NbtMode nbtMode, @Nullable CompoundTag nbt) {

        boolean matches(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (nbtMode == NbtMode.TAG) {
                return tagId != null && stack.is(TagKey.create(Registries.ITEM, tagId));
            }
            if (itemId == null || !itemId.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
                return false;
            }
            return nbtMode != NbtMode.EXACT || Objects.equals(stack.getTag(), nbt);
        }
    }

    private final List<Slot> slots;
    private final boolean requireAll;
    private final boolean consume;

    public ItemsCondition(List<Slot> slots, boolean requireAll, boolean consume) {
        if (slots.isEmpty() || slots.size() > MAX_SLOTS) {
            throw new IllegalArgumentException("items condition requires 1.." + MAX_SLOTS + " slots");
        }
        this.slots = List.copyOf(slots);
        this.requireAll = requireAll;
        this.consume = consume;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public boolean isRequireAll() {
        return requireAll;
    }

    public boolean isConsume() {
        return consume;
    }

    @Override
    public String type() {
        return "items";
    }

    @Override
    public boolean test(Context ctx) {
        Inventory inventory = ctx.player().getInventory();
        if (requireAll) {
            return slots.stream().allMatch(slot -> countOf(inventory, slot) >= slot.count());
        }
        return slots.stream().anyMatch(slot -> countOf(inventory, slot) >= slot.count());
    }

    public boolean consumeItems(Context ctx) {
        if (!test(ctx)) {
            return false;
        }
        Inventory inventory = ctx.player().getInventory();
        if (requireAll) {
            slots.forEach(slot -> removeCount(inventory, slot));
        } else {
            for (Slot slot : slots) {
                if (countOf(inventory, slot) >= slot.count()) {
                    removeCount(inventory, slot);
                    break;
                }
            }
        }
        return true;
    }

    public static int countOf(Inventory inventory, Slot slot) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (slot.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static void removeCount(Inventory inventory, Slot slot) {
        int remaining = slot.count();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (slot.matches(stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        inventory.setChanged();
    }

    public static ItemsCondition fromJson(JsonObject json) {
        List<Slot> slots = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            NbtMode mode = NbtMode.fromJson(s.get("nbt_match"));
            try {
                slots.add(new Slot(
                        s.has("item") ? ResourceLocation.parse(s.get("item").getAsString()) : null,
                        s.has("tag") ? ResourceLocation.parse(s.get("tag").getAsString()) : null,
                        s.has("count") ? s.get("count").getAsInt() : 1,
                        mode,
                        s.has("nbt") ? TagParser.parseTag(s.get("nbt").getAsString()) : null));
            } catch (Exception ex) {
                throw new JsonParseException("Bad items slot: " + s, ex);
            }
        }
        boolean all = !json.has("mode") || "all".equals(json.get("mode").getAsString());
        boolean consume = json.has("consume") && json.get("consume").getAsBoolean();
        return new ItemsCondition(slots, all, consume);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "items");
        json.addProperty("mode", requireAll ? "all" : "any");
        json.addProperty("consume", consume);
        JsonArray slotsJson = new JsonArray();
        for (Slot slot : slots) {
            JsonObject s = new JsonObject();
            if (slot.itemId() != null) {
                s.addProperty("item", slot.itemId().toString());
            }
            if (slot.tagId() != null) {
                s.addProperty("tag", slot.tagId().toString());
            }
            s.addProperty("count", slot.count());
            s.addProperty("nbt_match", slot.nbtMode().name().toLowerCase(Locale.ROOT));
            if (slot.nbt() != null) {
                s.addProperty("nbt", slot.nbt().toString());
            }
            slotsJson.add(s);
        }
        json.add("slots", slotsJson);
        return json;
    }
}
