package com.withouthonor.npcs.common.dialogue.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.dialogue.Placeholders;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;
import com.withouthonor.npcs.common.dialogue.condition.ItemsCondition;
import com.withouthonor.npcs.common.storage.PlayerStateManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Actions {

    private Actions() {
    }

    public record ItemSpec(ResourceLocation itemId, int count, @Nullable CompoundTag nbt) {

        public static ItemSpec fromJson(JsonObject json) {
            try {
                return new ItemSpec(
                        ResourceLocation.parse(json.get("item").getAsString()),
                        json.has("count") ? json.get("count").getAsInt() : 1,
                        json.has("nbt") ? TagParser.parseTag(json.get("nbt").getAsString()) : null);
            } catch (Exception e) {
                throw new JsonParseException("Bad item spec: " + json, e);
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("item", itemId.toString());
            json.addProperty("count", count);
            if (nbt != null) {
                json.addProperty("nbt", nbt.toString());
            }
            return json;
        }

        public ItemStack toStack() {
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(item, count);
            if (nbt != null) {
                stack.setTag(nbt.copy());
            }
            return stack;
        }
    }

    public record EffectSpec(ResourceLocation id, int durationTicks, int amplifier) {

        public static EffectSpec fromJson(JsonObject json) {
            return new EffectSpec(
                    ResourceLocation.parse(json.get("id").getAsString()),
                    json.has("duration") ? json.get("duration").getAsInt() : 600,
                    json.has("amplifier") ? json.get("amplifier").getAsInt() : 0);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("id", id.toString());
            json.addProperty("duration", durationTicks);
            json.addProperty("amplifier", amplifier);
            return json;
        }
    }

    public record Effect(String mode, List<EffectSpec> effects, boolean removeAll) implements DialogueAction {

        @Override
        public String type() {
            return "effect";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            ServerPlayer p = ctx.player();
            if ("remove".equals(mode)) {
                if (removeAll) {
                    p.removeAllEffects();
                    return;
                }
                for (EffectSpec s : effects) {
                    net.minecraft.world.effect.MobEffect e = ForgeRegistries.MOB_EFFECTS.getValue(s.id());
                    if (e != null) {
                        p.removeEffect(e);
                    }
                }
            } else {
                for (EffectSpec s : effects) {
                    net.minecraft.world.effect.MobEffect e = ForgeRegistries.MOB_EFFECTS.getValue(s.id());
                    if (e != null) {
                        int dur = s.durationTicks() <= 0 ? -1 : s.durationTicks();
                        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                e, dur, Math.max(0, s.amplifier())));
                    }
                }
            }
        }

        public static Effect fromJson(JsonObject json) {
            List<EffectSpec> effects = new ArrayList<>();
            if (json.has("effects")) {
                for (JsonElement e : json.getAsJsonArray("effects")) {
                    try {
                        effects.add(EffectSpec.fromJson(e.getAsJsonObject()));
                    } catch (Exception ignored) {
                    }
                }
            }
            return new Effect(
                    json.has("mode") ? json.get("mode").getAsString() : "apply",
                    effects,
                    json.has("remove_all") && json.get("remove_all").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("mode", mode);
            if (removeAll) {
                json.addProperty("remove_all", true);
            }
            JsonArray arr = new JsonArray();
            for (EffectSpec s : effects) {
                arr.add(s.toJson());
            }
            json.add("effects", arr);
            return json;
        }
    }

    public record GiveItem(List<ItemSpec> items) implements DialogueAction {

        @Override
        public String type() {
            return "give_item";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            for (ItemSpec spec : items) {
                Item item = ForgeRegistries.ITEMS.getValue(spec.itemId());
                if (item == null) {
                    WHCompanions.LOGGER.warn("give_item: unknown item {}", spec.itemId());
                    continue;
                }
                int remaining = spec.count();
                while (remaining > 0) {
                    ItemStack stack = new ItemStack(item, Math.min(remaining, item.getMaxStackSize()));
                    if (spec.nbt() != null) {
                        stack.setTag(spec.nbt().copy());
                    }
                    remaining -= stack.getCount();
                    if (!ctx.player().getInventory().add(stack)) {
                        ctx.player().drop(stack, false);
                    }
                }
            }
        }

        public static GiveItem fromJson(JsonObject json) {
            List<ItemSpec> items = new ArrayList<>();
            for (JsonElement e : json.getAsJsonArray("items")) {
                items.add(ItemSpec.fromJson(e.getAsJsonObject()));
            }
            return new GiveItem(List.copyOf(items));
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            JsonArray itemsJson = new JsonArray();
            items.forEach(i -> itemsJson.add(i.toJson()));
            json.add("items", itemsJson);
            return json;
        }
    }

    public record TakeItem(List<ItemsCondition.Slot> slots) implements DialogueAction {

        @Override
        public String type() {
            return "take_item";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            Inventory inventory = ctx.player().getInventory();
            for (ItemsCondition.Slot slot : slots) {
                if (ItemsCondition.countOf(inventory, slot) < slot.count()) {
                    return;
                }
            }
            slots.forEach(slot -> ItemsCondition.removeCount(inventory, slot));
        }

        public static TakeItem fromJson(JsonObject json) {

            ItemsCondition parsed = ItemsCondition.fromJson(withType(json));
            return new TakeItem(parsed.getSlots());
        }

        private static JsonObject withType(JsonObject json) {
            JsonObject copy = json.deepCopy();
            copy.addProperty("type", "items");
            return copy;
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new ItemsCondition(slots, true, true).toJson();
            json.addProperty("type", type());
            json.remove("mode");
            json.remove("consume");
            return json;
        }
    }

    public record Say(String text, boolean toChat, String cdScope, int cooldownSec) implements DialogueAction {

        public Say(String text) {
            this(text, false, "npc", 0);
        }

        @Override
        public String type() {
            return "say";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            if (ctx.npc() == null || text.isBlank()) {
                return;
            }
            if (cooldownSec > 0) {
                // КД: "npc" — один на всех, "player" — у каждого игрока свой ключ.
                String key = ("player".equals(cdScope) ? ctx.player().getUUID() + "|" : "")
                        + Integer.toHexString(text.hashCode());
                if (!ctx.npc().trySayCooldown(key, cooldownSec * 1000L)) {
                    return;
                }
            }
            String resolved = Placeholders.apply(text, ctx);
            ctx.npc().sendBubble(resolved);
            if (toChat) {
                ctx.npc().broadcastPhraseChat(resolved);
            }
        }

        public static Say fromJson(JsonObject json) {
            // Отсутствующие поля = старое поведение: только бабл, без КД.
            return new Say(json.get("text").getAsString(),
                    json.has("to_chat") && json.get("to_chat").getAsBoolean(),
                    json.has("cd_scope") ? json.get("cd_scope").getAsString() : "npc",
                    json.has("cooldown") ? json.get("cooldown").getAsInt() : 0);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("text", text);
            if (toChat) {
                json.addProperty("to_chat", true);
            }
            if (cooldownSec > 0) {
                json.addProperty("cooldown", cooldownSec);
                json.addProperty("cd_scope", "player".equals(cdScope) ? "player" : "npc");
            }
            return json;
        }
    }

    public record Emote(com.withouthonor.npcs.common.dialogue.EmoteIcon icon) implements DialogueAction {

        @Override
        public String type() {
            return "emote";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            if (ctx.npc() != null) {
                ctx.npc().sendEmote(icon);
            }
        }

        public static Emote fromJson(JsonObject json) {
            return new Emote(com.withouthonor.npcs.common.dialogue.EmoteIcon.byId(
                    json.get("emote").getAsString()));
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("emote", icon.id());
            return json;
        }
    }

    public record EmotecraftEmote(String emoteId, String emoteName, String emoteAuthor,
                                  String mode, int cooldownSec) implements DialogueAction {

        public EmotecraftEmote(String emoteId) {
            this(emoteId, "", "");
        }

        public EmotecraftEmote(String emoteId, String emoteName, String emoteAuthor) {
            // "once" = старое поведение: повтор той же эмоции не перезапускается.
            this(emoteId, emoteName, emoteAuthor, "once", 60);
        }

        @Override
        public String type() {
            return "emotecraft_emote";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            if (ctx.npc() == null) {
                return;
            }
            switch (mode) {
                case "always" -> ctx.npc().sendEmotecraftEmote(emoteId, emoteName, emoteAuthor,
                        ctx.npc().nextDialogueEmoteNonce());
                case "cooldown" -> {
                    if (ctx.npc().trySayCooldown("emote|" + emoteId,
                            Math.max(1, cooldownSec) * 1000L)) {
                        ctx.npc().sendEmotecraftEmote(emoteId, emoteName, emoteAuthor,
                                ctx.npc().nextDialogueEmoteNonce());
                    }
                }
                default -> ctx.npc().sendEmotecraftEmote(emoteId, emoteName, emoteAuthor);
            }
        }

        public static EmotecraftEmote fromJson(JsonObject json) {
            // Отсутствующие поля = старое поведение ("once").
            return new EmotecraftEmote(
                    json.has("emote_id") ? json.get("emote_id").getAsString() : "",
                    json.has("emote_name") ? json.get("emote_name").getAsString() : "",
                    json.has("emote_author") ? json.get("emote_author").getAsString() : "",
                    json.has("mode") ? json.get("mode").getAsString() : "once",
                    json.has("cooldown") ? json.get("cooldown").getAsInt() : 60);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("emote_id", emoteId);
            if (emoteName != null && !emoteName.isEmpty()) {
                json.addProperty("emote_name", emoteName);
            }
            if (emoteAuthor != null && !emoteAuthor.isEmpty()) {
                json.addProperty("emote_author", emoteAuthor);
            }
            if (!"once".equals(mode)) {
                json.addProperty("mode", mode);
                if ("cooldown".equals(mode)) {
                    json.addProperty("cooldown", cooldownSec);
                }
            }
            return json;
        }
    }

    public record StopEmotecraftEmote() implements DialogueAction {

        @Override
        public String type() {
            return "stop_emotecraft_emote";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            if (ctx.npc() != null) {
                ctx.npc().sendEmotecraftEmote("");
            }
        }

        public static StopEmotecraftEmote fromJson(JsonObject json) {
            return new StopEmotecraftEmote();
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            return json;
        }
    }

    public record Monologue(List<MonologueLine> lines, boolean lockControl)
            implements DialogueAction {

        @Override
        public String type() {
            return "monologue";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            List<MonologueLine> resolved = new ArrayList<>();
            for (MonologueLine l : lines) {
                resolved.add(new MonologueLine(
                        Placeholders.apply(l.name(), ctx),
                        Placeholders.apply(l.portrait(), ctx),
                        Placeholders.apply(l.text(), ctx)));
            }
            com.withouthonor.npcs.network.NetworkHandler.sendToPlayer(
                    new com.withouthonor.npcs.network.MonologuePacket(resolved, lockControl),
                    ctx.player());
        }

        public static Monologue fromJson(JsonObject json) {
            List<MonologueLine> lines = new ArrayList<>();
            if (json.has("lines")) {
                for (JsonElement e : json.getAsJsonArray("lines")) {
                    JsonObject o = e.getAsJsonObject();
                    lines.add(new MonologueLine(
                            o.has("name") ? o.get("name").getAsString() : "",
                            o.has("portrait") ? o.get("portrait").getAsString() : "",
                            o.has("text") ? o.get("text").getAsString() : ""));
                }
            } else if (json.has("pages")) {

                String name = json.has("name") ? json.get("name").getAsString() : "";
                String portrait = json.has("portrait") ? json.get("portrait").getAsString() : "";
                for (JsonElement e : json.getAsJsonArray("pages")) {
                    lines.add(new MonologueLine(name, portrait, e.getAsString()));
                }
            }
            return new Monologue(lines, json.has("lock") && json.get("lock").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            JsonArray arr = new JsonArray();
            for (MonologueLine l : lines) {
                JsonObject o = new JsonObject();
                o.addProperty("name", l.name());
                if (!l.portrait().isEmpty()) {
                    o.addProperty("portrait", l.portrait());
                }
                o.addProperty("text", l.text());
                arr.add(o);
            }
            json.add("lines", arr);
            if (lockControl) {
                json.addProperty("lock", true);
            }
            return json;
        }
    }

    public record OpenTrade() implements DialogueAction {

        @Override
        public String type() {
            return "open_trade";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            if (ctx.npc() == null || !ctx.npc().openTrade(ctx.player())) {
                ctx.player().connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("wh_npcs.msg.trade.none")
                                .withStyle(net.minecraft.ChatFormatting.GRAY)));
            }
        }

        public static OpenTrade fromJson(JsonObject json) {
            return new OpenTrade();
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            return json;
        }
    }

    public record Reputation(String faction, int delta) implements DialogueAction {

        @Override
        public String type() {
            return "reputation";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            var factionDef = com.withouthonor.npcs.common.reputation.FactionRegistry.get().byId(faction);
            if (factionDef == null) {
                WHCompanions.LOGGER.warn("reputation action: unknown faction '{}'", faction);
                return;
            }
            int value = PlayerStateManager.get(ctx.player().server)
                    .addReputation(ctx.player().getUUID(), faction, delta);
            String sign = delta > 0 ? "+" : "−";
            ctx.player().connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                    "wh_npcs.msg.reputation_change", factionDef.getName(),
                    sign + Math.abs(delta) + " (" + factionDef.tierFor(value).name() + ")")
                    .withStyle(delta > 0
                            ? net.minecraft.ChatFormatting.GREEN
                            : net.minecraft.ChatFormatting.RED)));
        }

        public static Reputation fromJson(JsonObject json) {
            return new Reputation(json.get("faction").getAsString(), json.get("delta").getAsInt());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("faction", faction);
            json.addProperty("delta", delta);
            return json;
        }
    }

    public record SetFlag(String flag, boolean value) implements DialogueAction {

        @Override
        public String type() {
            return "set_flag";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            String source = ctx.npc() != null
                    ? net.minecraft.ChatFormatting.stripFormatting(ctx.npc().getName().getString())
                    : "";
            PlayerStateManager.get(ctx.player().server).setFlag(
                    ctx.player().getUUID(), flag, value, source == null ? "" : source);
        }

        public static SetFlag fromJson(JsonObject json) {
            return new SetFlag(json.get("flag").getAsString(),
                    !json.has("value") || json.get("value").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("flag", flag);
            json.addProperty("value", value);
            return json;
        }
    }

    public record RunCommand(String command) implements DialogueAction {

        private static final Set<String> BLOCKED =
                Set.of("op", "deop", "stop", "whitelist", "ban", "ban-ip", "pardon", "pardon-ip");

        @Override
        public String type() {
            return "run_command";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            String resolved = Placeholders.apply(command, ctx);
            // Блоклист совещательный (автор диалога и так OP-2): ловим корень
            // и вложенные команды после "run" в цепочках /execute.
            String[] tokens = resolved.strip().split("\\s+");
            boolean blocked = tokens.length > 0 && BLOCKED.contains(stripSlash(tokens[0]));
            for (int i = 1; !blocked && i < tokens.length - 1; i++) {
                if ("run".equalsIgnoreCase(tokens[i]) && BLOCKED.contains(stripSlash(tokens[i + 1]))) {
                    blocked = true;
                }
            }
            if (blocked) {
                WHCompanions.LOGGER.warn("run_command blocked: '{}' (player {})",
                        resolved, ctx.player().getGameProfile().getName());
                return;
            }
            ServerPlayer player = ctx.player();
            player.server.getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(2).withSuppressedOutput(),
                    resolved);
        }

        private static String stripSlash(String token) {
            String t = token.startsWith("/") ? token.substring(1) : token;
            return t.toLowerCase(Locale.ROOT);
        }

        public static RunCommand fromJson(JsonObject json) {
            return new RunCommand(json.get("command").getAsString());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("command", command);
            return json;
        }
    }

    public record Sound(ResourceLocation soundId, float volume, float pitch) implements DialogueAction {

        @Override
        public String type() {
            return "sound";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
            if (sound == null) {
                WHCompanions.LOGGER.warn("sound: unknown sound {}", soundId);
                return;
            }
            ServerPlayer player = ctx.player();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.NEUTRAL, volume, pitch);
        }

        public static Sound fromJson(JsonObject json) {
            return new Sound(ResourceLocation.parse(json.get("sound").getAsString()),
                    json.has("volume") ? json.get("volume").getAsFloat() : 1.0F,
                    json.has("pitch") ? json.get("pitch").getAsFloat() : 1.0F);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("sound", soundId.toString());
            json.addProperty("volume", volume);
            json.addProperty("pitch", pitch);
            return json;
        }
    }

    public record Title(@Nullable String title, @Nullable String subtitle, @Nullable String actionbar)
            implements DialogueAction {

        @Override
        public String type() {
            return "title";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            ServerPlayer player = ctx.player();
            if (actionbar != null) {
                player.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.literal(Placeholders.apply(actionbar, ctx))));
            }
            if (title != null || subtitle != null) {

                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal(title != null ? Placeholders.apply(title, ctx) : "")));
                if (subtitle != null) {
                    player.connection.send(new ClientboundSetSubtitleTextPacket(
                            Component.literal(Placeholders.apply(subtitle, ctx))));
                }
            }
        }

        public static Title fromJson(JsonObject json) {
            return new Title(
                    json.has("title") ? json.get("title").getAsString() : null,
                    json.has("subtitle") ? json.get("subtitle").getAsString() : null,
                    json.has("actionbar") ? json.get("actionbar").getAsString() : null);
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            if (title != null) {
                json.addProperty("title", title);
            }
            if (subtitle != null) {
                json.addProperty("subtitle", subtitle);
            }
            if (actionbar != null) {
                json.addProperty("actionbar", actionbar);
            }
            return json;
        }
    }

    public record StopMusic() implements DialogueAction {

        @Override
        public String type() {
            return "stop_music";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            com.withouthonor.npcs.network.NetworkHandler.sendToPlayer(
                    new com.withouthonor.npcs.network.StopMusicPacket(), ctx.player());
        }

        public static StopMusic fromJson(JsonObject json) {
            return new StopMusic();
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            return json;
        }
    }

    public record Follow() implements DialogueAction {

        @Override
        public String type() {
            return "follow";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            if (ctx.npc() != null) {
                ctx.npc().startFollowing(ctx.player());
            }
        }

        public static Follow fromJson(JsonObject json) {
            return new Follow();
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            return json;
        }
    }

    public record StopFollow() implements DialogueAction {

        @Override
        public String type() {
            return "stop_follow";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            if (ctx.npc() != null) {
                ctx.npc().waitHere();
            }
        }

        public static StopFollow fromJson(JsonObject json) {
            return new StopFollow();
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            return json;
        }
    }

    public record Transform(String file, boolean equipment) implements DialogueAction {

        @Override
        public String type() {
            return "transform";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            com.withouthonor.npcs.common.entity.CompanionEntity npc = ctx.npc();
            if (npc == null || npc.getProfileId() == null || file.isBlank()) {
                return;
            }
            net.minecraft.server.MinecraftServer server = ctx.player().server;
            if (server == null) {
                return;
            }
            com.withouthonor.npcs.network.ProfileSharePackets.transformFromExport(
                    server, npc, file, equipment);
        }

        public static Transform fromJson(JsonObject json) {
            return new Transform(
                    json.has("file") ? json.get("file").getAsString() : "",
                    json.has("equipment") && json.get("equipment").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("file", file);
            if (equipment) {
                json.addProperty("equipment", true);
            }
            return json;
        }
    }

    public record AttackPlayer(boolean allies, int alliesRadius, boolean allPlayers) implements DialogueAction {

        @Override
        public String type() {
            return "attack_player";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            com.withouthonor.npcs.common.entity.CompanionEntity npc = ctx.npc();
            if (npc == null || !npc.isAlive()) {
                return;
            }
            ServerPlayer player = ctx.player();
            if (npc.getProfileId() != null) {
                var profile = com.withouthonor.npcs.common.storage.ProfileManager.get().get(npc.getProfileId());
                if (profile != null && "passive".equals(profile.getCombatPreset())) {
                    // Мирный пресет не имеет боевых целей — target взводится впустую.
                    WHCompanions.LOGGER.warn(
                            "attack_player: NPC '{}' has passive combat preset, attack will not start"
                                    + " (change preset via combat_stats/transform first)", npc.getName().getString());
                }
            }
            // Радиус общий: и для поиска игроков (all_players), и для созыва союзников.
            int radius = Math.max(4, Math.min(48, alliesRadius));
            List<ServerPlayer> targets = new ArrayList<>();
            if (allPlayers) {
                for (ServerPlayer sp : player.serverLevel().getEntitiesOfClass(
                        ServerPlayer.class, player.getBoundingBox().inflate(radius))) {
                    if (sp.isAlive() && !sp.isCreative() && !sp.isSpectator()) {
                        targets.add(sp);
                    }
                }
            }
            if (!targets.contains(player) && player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                targets.add(player);
            }
            if (targets.isEmpty()) {
                // Атаковать некого (креатив/наблюдатель) — не взводим target впустую.
                return;
            }
            // setTarget сам поднимет setAggressive(true) в переопределении CompanionEntity.
            npc.setTarget(nearestTo(npc, targets));
            if (!allies) {
                return;
            }
            String faction = factionOf(npc);
            if (faction == null || faction.isBlank()) {
                return;
            }
            var box = npc.getBoundingBox().inflate(radius);
            for (var ally : npc.level().getEntitiesOfClass(
                    com.withouthonor.npcs.common.entity.CompanionEntity.class, box)) {
                if (ally == npc || !ally.isAlive() || ally.getTarget() != null) {
                    continue;
                }
                if (faction.equals(factionOf(ally))) {
                    ally.setTarget(nearestTo(ally, targets));
                }
            }
        }

        private static ServerPlayer nearestTo(com.withouthonor.npcs.common.entity.CompanionEntity npc,
                                              List<ServerPlayer> targets) {
            ServerPlayer best = targets.get(0);
            double bestDist = npc.distanceToSqr(best);
            for (int i = 1; i < targets.size(); i++) {
                double d = npc.distanceToSqr(targets.get(i));
                if (d < bestDist) {
                    bestDist = d;
                    best = targets.get(i);
                }
            }
            return best;
        }

        @Nullable
        private static String factionOf(com.withouthonor.npcs.common.entity.CompanionEntity npc) {
            if (npc.getProfileId() == null) {
                return null;
            }
            var profile = com.withouthonor.npcs.common.storage.ProfileManager.get().get(npc.getProfileId());
            return profile == null ? null : profile.getFaction();
        }

        public static AttackPlayer fromJson(JsonObject json) {
            return new AttackPlayer(
                    json.has("allies") && json.get("allies").getAsBoolean(),
                    json.has("allies_radius") ? json.get("allies_radius").getAsInt() : 16,
                    json.has("all_players") && json.get("all_players").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            if (allies) {
                json.addProperty("allies", true);
            }
            if (allPlayers) {
                json.addProperty("all_players", true);
            }
            if (allies || allPlayers) {
                // Радиус общий: поиск игроков и созыв союзников.
                json.addProperty("allies_radius", alliesRadius);
            }
            return json;
        }
    }

    public record EditProfile(String name, String title, String skin, String faction)
            implements DialogueAction {

        @Override
        public String type() {
            return "edit_profile";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            com.withouthonor.npcs.common.entity.CompanionEntity npc = ctx.npc();
            if (npc == null || npc.getProfileId() == null) {
                return;
            }
            var profile = com.withouthonor.npcs.common.storage.ProfileManager.get().get(npc.getProfileId());
            if (profile == null) {
                return;
            }
            // Пустая строка = «не менять»; имя может содержать §-коды — храним как есть.
            boolean changed = false;
            if (!name.isBlank()) {
                profile.setName(name);
                changed = true;
            }
            if (!title.isBlank()) {
                profile.setTitle(title);
                changed = true;
            }
            if (!skin.isBlank()) {
                profile.setSkinPlayerName(skin);
                changed = true;
            }
            if (!faction.isBlank()) {
                profile.setFaction(faction);
                changed = true;
            }
            if (changed) {
                com.withouthonor.npcs.common.storage.ProfileManager.get().save(profile);
                com.withouthonor.npcs.common.profile.ProfileSync
                        .applyToLoadedEntities(ctx.player().server, profile);
            }
        }

        public static EditProfile fromJson(JsonObject json) {
            return new EditProfile(
                    json.has("name") ? json.get("name").getAsString() : "",
                    json.has("title") ? json.get("title").getAsString() : "",
                    json.has("skin") ? json.get("skin").getAsString() : "",
                    json.has("faction") ? json.get("faction").getAsString() : "");
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            if (!name.isBlank()) {
                json.addProperty("name", name);
            }
            if (!title.isBlank()) {
                json.addProperty("title", title);
            }
            if (!skin.isBlank()) {
                json.addProperty("skin", skin);
            }
            if (!faction.isBlank()) {
                json.addProperty("faction", faction);
            }
            return json;
        }
    }

    public record CombatStats(String preset, String aggroTargets, @Nullable Float hp,
                              @Nullable Float damage, @Nullable Float armor, boolean heal)
            implements DialogueAction {

        private static final Set<String> PRESETS = Set.of("passive", "melee", "shield", "bow", "potion", "mage");

        @Override
        public String type() {
            return "combat_stats";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            com.withouthonor.npcs.common.entity.CompanionEntity npc = ctx.npc();
            if (npc == null || npc.getProfileId() == null) {
                return;
            }
            var profile = com.withouthonor.npcs.common.storage.ProfileManager.get().get(npc.getProfileId());
            if (profile == null) {
                return;
            }
            // Пустой пресет/цели и null-числа = «не менять»; клампы живут в сеттерах профиля.
            boolean changed = false;
            if (!preset.isBlank()) {
                if (PRESETS.contains(preset)) {
                    profile.setCombatPreset(preset);
                    changed = true;
                } else {
                    WHCompanions.LOGGER.warn("combat_stats: unknown preset '{}'", preset);
                }
            }
            if (!aggroTargets.isBlank()) {
                profile.setAggressorTargets(aggroTargets);
                changed = true;
            }
            if (hp != null) {
                profile.setMaxHealth(hp);
                changed = true;
            }
            if (damage != null) {
                profile.setAttackDamage(damage);
                changed = true;
            }
            if (armor != null) {
                profile.setArmor(armor);
                changed = true;
            }
            if (changed) {
                com.withouthonor.npcs.common.storage.ProfileManager.get().save(profile);
                // applyToLoadedEntities → applyCombatProfile уже применит новый MAX_HEALTH к сущности.
                com.withouthonor.npcs.common.profile.ProfileSync
                        .applyToLoadedEntities(ctx.player().server, profile);
            }
            if (heal) {
                npc.setHealth(npc.getMaxHealth());
            }
        }

        public static CombatStats fromJson(JsonObject json) {
            return new CombatStats(
                    json.has("preset") ? json.get("preset").getAsString() : "",
                    json.has("aggro_targets") ? json.get("aggro_targets").getAsString() : "",
                    json.has("hp") ? json.get("hp").getAsFloat() : null,
                    json.has("damage") ? json.get("damage").getAsFloat() : null,
                    json.has("armor") ? json.get("armor").getAsFloat() : null,
                    json.has("heal") && json.get("heal").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            if (!preset.isBlank()) {
                json.addProperty("preset", preset);
            }
            if (!aggroTargets.isBlank()) {
                json.addProperty("aggro_targets", aggroTargets);
            }
            if (hp != null) {
                json.addProperty("hp", hp);
            }
            if (damage != null) {
                json.addProperty("damage", damage);
            }
            if (armor != null) {
                json.addProperty("armor", armor);
            }
            if (heal) {
                json.addProperty("heal", true);
            }
            return json;
        }
    }

    public record FollowWait() implements DialogueAction {

        @Override
        public String type() {
            return "follow_wait";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            if (ctx.npc() != null) {
                ctx.npc().waitHere();
            }
        }

        public static FollowWait fromJson(JsonObject json) {
            return new FollowWait();
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            return json;
        }
    }

    /**
     * Пауза перед следующими действиями списка (#2). НЕ блокирует: DialogueAction.executeFrom при
     * встрече Wait кладёт остаток в отложенную очередь NPC. Переход на след. узел диалога происходит
     * сразу; wait откладывает лишь действия ПОСЛЕ себя. Не переживает рестарт мира и смерть NPC.
     */
    public record Wait(float seconds) implements DialogueAction {

        @Override
        public String type() {
            return "wait";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            // Само ожидание реализовано в DialogueAction.executeFrom (постановка в очередь NPC).
            // execute() зовётся, только если Wait исполняют «в лоб» (без staged-пути) — тогда no-op.
        }

        public static Wait fromJson(JsonObject json) {
            float s = json.has("seconds") ? json.get("seconds").getAsFloat() : 1.0F;
            return new Wait(Math.max(0.05F, Math.min(600.0F, s)));
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("seconds", seconds);
            return json;
        }
    }

    /**
     * Кастовать заклинание Iron's Spells по реплике (сценарный каст, не боевой ИИ).
     * Драйвер каста тикается при загруженном ISS независимо от пресета, поэтому работает у любого NPC.
     * targetSelf=false → целимся в собеседника (снаряды летят в игрока; NPC берёт его целью
     * на время каста, по завершении цель снимается — если её поставило само действие);
     * targetSelf=true → каст на себя (баффы/лечение/призыв/AoE вокруг). Без ISS — тихо ничего.
     */
    public record CastSpell(String spellId, int level, boolean targetSelf) implements DialogueAction {

        @Override
        public String type() {
            return "cast_spell";
        }

        @Override
        public void execute(DialogueCondition.Context ctx) {
            com.withouthonor.npcs.common.entity.CompanionEntity npc = ctx.npc();
            if (npc == null || spellId == null || spellId.isBlank()
                    || !com.withouthonor.npcs.compat.Compat.ironsSpellsLoaded()) {
                return;
            }
            if (targetSelf) {
                com.withouthonor.npcs.compat.Compat.ironsSpells().castSpell(npc, spellId, level);
            } else {
                // Прицел = собеседник; мост сам снимет цель по завершении каста, если её поставил он
                com.withouthonor.npcs.compat.Compat.ironsSpells().castSpellAt(npc, spellId, level, ctx.player());
            }
        }

        public static CastSpell fromJson(JsonObject json) {
            return new CastSpell(
                    json.has("spell_id") ? json.get("spell_id").getAsString() : "",
                    json.has("level") ? Math.max(1, Math.min(10, json.get("level").getAsInt())) : 1,
                    json.has("target_self") && json.get("target_self").getAsBoolean());
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type());
            json.addProperty("spell_id", spellId);
            json.addProperty("level", level);
            if (targetSelf) {
                json.addProperty("target_self", true);
            }
            return json;
        }
    }
}
