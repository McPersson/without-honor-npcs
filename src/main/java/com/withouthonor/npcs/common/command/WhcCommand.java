package com.withouthonor.npcs.common.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.withouthonor.npcs.common.dialogue.DialogueRuntime;
import com.withouthonor.npcs.common.dialogue.EntryPoint;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.profile.ProfileSync;
import com.withouthonor.npcs.network.EditorDataPacket;
import com.withouthonor.npcs.common.registry.ModEntities;
import com.withouthonor.npcs.common.skin.SkinService;
import com.withouthonor.npcs.common.storage.CompanionIndex;
import com.withouthonor.npcs.common.storage.DialogueManager;
import com.withouthonor.npcs.common.storage.PlayerStateManager;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class WhcCommand {

    private static final int PAGE_SIZE = 8;

    private static final Gson EXPORT_GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final DynamicCommandExceptionType NOT_FOUND =
            new DynamicCommandExceptionType(q -> Component.translatable("wh_npcs.msg.command.not_found", q));
    private static final DynamicCommandExceptionType NOT_LOADED =
            new DynamicCommandExceptionType(q -> Component.translatable("wh_npcs.msg.command.not_loaded", q));

    private static final SuggestionProvider<CommandSourceStack> DIALOGUE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(DialogueManager.get().ids(), builder);

    private static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    com.withouthonor.npcs.common.reputation.FactionRegistry.get().ids(), builder);

    private static final SuggestionProvider<CommandSourceStack> GRAVE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    com.withouthonor.npcs.common.storage.Graveyard
                            .get(ctx.getSource().getServer()).names(), builder);

    private static final SuggestionProvider<CommandSourceStack> NAME_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    CompanionIndex.get(ctx.getSource().getServer()).all().stream()
                            .map(e -> e.name().contains(" ") ? "\"" + e.name() + "\"" : e.name())
                            .distinct(),
                    builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whc")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", NameArgument.name())
                                .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> list(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                .then(Commands.literal("find")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .executes(ctx -> find(ctx))))
                .then(Commands.literal("tp")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .executes(ctx -> tp(ctx))))
                .then(Commands.literal("bring")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .executes(ctx -> bring(ctx))))
                .then(Commands.literal("move")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> move(ctx, null))
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(ctx -> move(ctx, DimensionArgument.getDimension(ctx, "dimension")))))))
                .then(Commands.literal("clone")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .executes(ctx -> clone(ctx))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .executes(ctx -> removeAskConfirm(ctx))
                                .then(Commands.literal("confirm")
                                        .executes(ctx -> remove(ctx)))))
                .then(Commands.literal("edit")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .executes(ctx -> edit(ctx))))
                .then(Commands.literal("dialogue")
                        .then(Commands.literal("start")
                                .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("dialogue", StringArgumentType.string()).suggests(DIALOGUE_SUGGESTIONS)
                                                        .executes(ctx -> dialogueStart(ctx, null))
                                                        .then(Commands.argument("node", StringArgumentType.string())
                                                                .executes(ctx -> dialogueStart(ctx,
                                                                        StringArgumentType.getString(ctx, "node")))))))))
                .then(Commands.literal("rep")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction", StringArgumentType.string()).suggests(FACTION_SUGGESTIONS)
                                                .executes(ctx -> repGet(ctx)))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction", StringArgumentType.string()).suggests(FACTION_SUGGESTIONS)
                                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                                        .executes(ctx -> repSet(ctx, false))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction", StringArgumentType.string()).suggests(FACTION_SUGGESTIONS)
                                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                                        .executes(ctx -> repSet(ctx, true)))))))
                .then(Commands.literal("revive")
                        .then(Commands.argument("name", NameArgument.name()).suggests(GRAVE_SUGGESTIONS)
                                .executes(ctx -> revive(ctx))))
                .then(Commands.literal("restock")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .executes(ctx -> restock(ctx))))
                .then(Commands.literal("faction")
                        .then(Commands.literal("list")
                                .executes(ctx -> factionList(ctx)))
                        .then(Commands.literal("reload")
                                .executes(ctx -> factionReload(ctx))))
                .then(Commands.literal("entry")
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                        .then(Commands.argument("dialogue", StringArgumentType.string()).suggests(DIALOGUE_SUGGESTIONS)
                                                .executes(ctx -> entryAdd(ctx)))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                        .executes(ctx -> entryList(ctx))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                        .executes(ctx -> entryClear(ctx)))))
                .then(Commands.literal("flags")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> flagsGet(ctx))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("flag", StringArgumentType.string())
                                                .executes(ctx -> flagsSet(ctx, true)))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> flagsClearAll(ctx))
                                        .then(Commands.argument("flag", StringArgumentType.string())
                                                .executes(ctx -> flagsSet(ctx, false))))))
                .then(Commands.literal("disguise")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .then(Commands.literal("default")
                                        .executes(ctx -> setDisguise(ctx, null)))
                                .then(Commands.argument("type", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKeys(), builder))
                                        .executes(ctx -> setDisguise(ctx,
                                                net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "type").toString())))))
                .then(Commands.literal("skin")
                        .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                .then(Commands.literal("default")
                                        .executes(ctx -> setSkin(ctx, null)))
                                .then(Commands.argument("nick", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                ctx.getSource().getServer().getPlayerNames(), builder))
                                        .executes(ctx -> setSkin(ctx, StringArgumentType.getString(ctx, "nick"))))))
                .then(Commands.literal("profile")
                        .then(Commands.literal("export")
                                .then(Commands.argument("name", NameArgument.name()).suggests(NAME_SUGGESTIONS)
                                        .then(Commands.argument("file", StringArgumentType.string())
                                                .executes(ctx -> profileExport(ctx)))))
                        .then(Commands.literal("import")
                                .then(Commands.argument("file", StringArgumentType.string())
                                        .executes(ctx -> profileImport(ctx))))));
    }

    private static int create(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();

        CompanionProfile profile = ProfileManager.get().create(name);

        CompanionEntity npc = ModEntities.COMPANION.get().create(level);
        if (npc == null) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.create_fail"));
            return 0;
        }
        HitResult hit = player.pick(5.0D, 1.0F, false);
        Vec3 pos = hit.getType() == HitResult.Type.MISS ? player.position() : hit.getLocation();
        float yaw = player.getYRot() + 180.0F;
        npc.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        npc.setYBodyRot(yaw);
        npc.setYHeadRot(yaw);
        npc.setProfileId(profile.getId());
        npc.setCustomName(Component.literal(name));
        level.addFreshEntity(npc);

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.create_ok", name, shortId(profile.getId())), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx, int page) {
        List<CompanionIndex.Entry> all = new ArrayList<>(CompanionIndex.get(ctx.getSource().getServer()).all());
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.list_empty"), false);
            return 0;
        }
        all.sort(Comparator.comparing(CompanionIndex.Entry::name, String.CASE_INSENSITIVE_ORDER));
        int pages = (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int p = Math.min(page, pages);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.list_header", all.size(), p, pages), false);
        for (CompanionIndex.Entry e : all.subList((p - 1) * PAGE_SIZE, Math.min(p * PAGE_SIZE, all.size()))) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "wh_npcs.msg.command.list_entry", e.name(), shortId(e.id()), formatPos(e)), false);
        }
        return all.size();
    }

    private static int find(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.find", e.name(), formatPos(e)), false);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getServer().getLevel(e.dimension());
        if (level == null) {
            throw NOT_FOUND.create(e.name());
        }
        player.teleportTo(level, e.pos().getX() + 0.5D, e.pos().getY(), e.pos().getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.tp", e.name()), true);
        return 1;
    }

    private static int bring(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompanionEntity npc = loadEntity(ctx.getSource().getServer(), e);
        if (npc == null) {
            throw NOT_LOADED.create(e.name());
        }
        teleportNpc(npc, player.serverLevel(), player.position());
        ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.bring", e.name()), true);
        return 1;
    }

    private static int move(CommandContext<CommandSourceStack> ctx, @Nullable ServerLevel dim) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        CompanionEntity npc = loadEntity(ctx.getSource().getServer(), e);
        if (npc == null) {
            throw NOT_LOADED.create(e.name());
        }
        ServerLevel target = dim != null ? dim : (ServerLevel) npc.level();
        teleportNpc(npc, target, pos);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.move", e.name(), (int) pos.x + " " + (int) pos.y + " " + (int) pos.z,
                target.dimension().location()), true);
        return 1;
    }

    private static int clone(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        CompanionEntity npc = ModEntities.COMPANION.get().create(level);
        if (npc == null) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.create_fail"));
            return 0;
        }
        npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot() + 180.0F, 0.0F);
        npc.setProfileId(e.profileId());
        CompanionProfile cloneProfile = e.profileId() != null ? ProfileManager.get().get(e.profileId()) : null;
        npc.setCustomName(cloneProfile != null
                ? ProfileSync.coloredName(cloneProfile)
                : Component.literal(e.name()));
        level.addFreshEntity(npc);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.clone", e.name(),
                (e.profileId() != null ? shortId(e.profileId()) : "—")), true);
        return 1;
    }

    private static int removeAskConfirm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        String query = StringArgumentType.getString(ctx, "name");
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.remove_confirm", e.name(), shortId(e.id()),
                (query.contains(" ") ? "\"" + query + "\"" : query)), false);
        return 0;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        MinecraftServer server = ctx.getSource().getServer();
        CompanionEntity npc = loadEntity(server, e);
        if (npc == null) {
            throw NOT_LOADED.create(e.name());
        }
        npc.discard();

        ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.remove_ok", e.name()), true);
        return 1;
    }

    private static int setSkin(CommandContext<CommandSourceStack> ctx, @Nullable String nick) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        if (e.profileId() == null) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.no_profile", e.name()));
            return 0;
        }
        CompanionProfile profile = ProfileManager.get().get(e.profileId());
        if (profile == null) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.profile_missing", shortId(e.profileId())));
            return 0;
        }
        profile.setSkinPlayerName(nick);
        ProfileManager.get().save(profile);
        MinecraftServer server = ctx.getSource().getServer();
        ProfileSync.applyToLoadedEntities(server, profile);
        if (nick != null) {

            SkinService.get().fetch(nick).whenComplete((data, err) -> server.execute(() -> {
                if (err != null) {
                    ctx.getSource().sendSuccess(() -> Component.translatable(
                            "wh_npcs.msg.command.skin_fetch_fail", nick, err.getMessage()), false);
                }
            }));
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "wh_npcs.msg.command.skin_set", nick, e.name()), true);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "wh_npcs.msg.command.skin_default", e.name()), true);
        }
        return 1;
    }

    @Nullable
    private static com.withouthonor.npcs.common.reputation.Faction faction(
            CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "faction");
        var faction = com.withouthonor.npcs.common.reputation.FactionRegistry.get().byId(id);
        if (faction == null) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.faction_missing", id));
        }
        return faction;
    }

    private static int repGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var faction = faction(ctx);
        if (faction == null) {
            return 0;
        }
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int value = PlayerStateManager.get(ctx.getSource().getServer())
                .getReputation(player.getUUID(), faction.getId());
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.rep_get", player.getGameProfile().getName(), faction.getName(),
                value, faction.tierFor(value).name()), false);
        return 1;
    }

    private static int repSet(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        var faction = faction(ctx);
        if (faction == null) {
            return 0;
        }
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "value");
        PlayerStateManager state = PlayerStateManager.get(ctx.getSource().getServer());
        int value = add
                ? state.addReputation(player.getUUID(), faction.getId(), amount)
                : setRep(state, player, faction.getId(), amount);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.rep_set", player.getGameProfile().getName(), faction.getName(),
                value, faction.tierFor(value).name()), true);
        return 1;
    }

    private static int setRep(PlayerStateManager state, ServerPlayer player, String faction, int value) {
        state.setReputation(player.getUUID(), faction, value);
        return value;
    }

    private static int revive(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        boolean revived = com.withouthonor.npcs.common.storage.Graveyard
                .get(ctx.getSource().getServer()).revive(ctx.getSource().getServer(), name);
        if (revived) {
            ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.revive_ok", name), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.revive_fail", name));
        return 0;
    }

    private static int restock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionProfile profile = resolveProfile(ctx);
        PlayerStateManager.get(ctx.getSource().getServer()).resetTradesForAll(profile.getId().toString());
        ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.restock", profile.getName()), true);
        return 1;
    }

    private static int factionList(CommandContext<CommandSourceStack> ctx) {
        var factions = com.withouthonor.npcs.common.reputation.FactionRegistry.get().all();
        if (factions.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.faction_list_empty"), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.faction_list_header", factions.size()), false);
        for (var faction : factions) {
            StringBuilder tiers = new StringBuilder();
            for (var tier : faction.getTiers()) {
                if (!tiers.isEmpty()) {
                    tiers.append(" / ");
                }
                tiers.append(tier.name());
                if (tier.min() != Integer.MIN_VALUE) {
                    tiers.append(" ≥").append(tier.min());
                }
            }
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "wh_npcs.msg.command.faction_list_entry", faction.getId(), faction.getName(), tiers.toString()), false);
        }
        return factions.size();
    }

    private static int factionReload(CommandContext<CommandSourceStack> ctx) {
        String error = com.withouthonor.npcs.common.reputation.FactionRegistry.get().load();
        if (error != null) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.faction_reload_err", error));
            return 0;
        }
        int count = com.withouthonor.npcs.common.reputation.FactionRegistry.get().all().size();
        ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.faction_reload_ok", count), true);
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionProfile profile = resolveProfile(ctx);

        CompanionEntity npc = loadEntity(ctx.getSource().getServer(), resolve(ctx));
        EditorDataPacket.send(ctx.getSource().getPlayerOrException(), profile, npc != null ? npc.getId() : -1);
        return 1;
    }

    private static int dialogueStart(CommandContext<CommandSourceStack> ctx, @Nullable String node) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String dialogueId = StringArgumentType.getString(ctx, "dialogue");
        CompanionEntity npc = loadEntity(ctx.getSource().getServer(), e);
        if (npc == null) {
            throw NOT_LOADED.create(e.name());
        }
        if (DialogueRuntime.open(target, npc, dialogueId, node)) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "wh_npcs.msg.command.dialogue_ok", dialogueId, target.getGameProfile().getName()), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.dialogue_fail", dialogueId));
        return 0;
    }

    private static int entryAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionProfile profile = resolveProfile(ctx);
        String dialogueId = StringArgumentType.getString(ctx, "dialogue");
        if (DialogueManager.get().get(dialogueId) == null) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.dialogue_missing", dialogueId));
            return 0;
        }
        profile.getEntryPoints().add(new EntryPoint(dialogueId));
        ProfileManager.get().save(profile);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.entry_add", dialogueId, profile.getEntryPoints().size()), true);
        return 1;
    }

    private static int entryList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionProfile profile = resolveProfile(ctx);
        List<EntryPoint> entries = profile.getEntryPoints();
        if (entries.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.entry_list_empty"), false);
            return 0;
        }
        for (int i = 0; i < entries.size(); i++) {
            EntryPoint entry = entries.get(i);
            int n = i + 1;
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "wh_npcs.msg.command.entry_list_entry", n, entry.getDialogueId(), entry.getConditions().size()), false);
        }
        return entries.size();
    }

    private static int entryClear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionProfile profile = resolveProfile(ctx);
        profile.getEntryPoints().clear();
        ProfileManager.get().save(profile);
        ctx.getSource().sendSuccess(() -> Component.translatable("wh_npcs.msg.command.entry_clear"), true);
        return 1;
    }

    private static CompanionProfile resolveProfile(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionIndex.Entry e = resolve(ctx);
        CompanionProfile profile = e.profileId() != null ? ProfileManager.get().get(e.profileId()) : null;
        if (profile == null) {
            throw NOT_FOUND.create(e.name());
        }
        return profile;
    }

    private static int profileExport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CompanionProfile profile = resolveProfile(ctx);
        String file = sanitizeFileName(StringArgumentType.getString(ctx, "file"));
        if (file.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.bad_filename"));
            return 0;
        }
        Path dir = ctx.getSource().getServer().getWorldPath(LevelResource.ROOT)
                .resolve("wh_npcs").resolve("exports");
        Path target = dir.resolve(file + ".json");
        Path tmp = dir.resolve(file + ".json.tmp");
        try {
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                EXPORT_GSON.toJson(profile.toJson(), w);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.export_write_err", e.getMessage()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.export_ok", profile.getName(), file), true);
        return 1;
    }

    private static int profileImport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String file = sanitizeFileName(StringArgumentType.getString(ctx, "file"));
        if (file.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.bad_filename"));
            return 0;
        }
        Path path = ctx.getSource().getServer().getWorldPath(LevelResource.ROOT)
                .resolve("wh_npcs").resolve("exports").resolve(file + ".json");
        if (!Files.isRegularFile(path)) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.import_not_found", file));
            return 0;
        }
        CompanionProfile profile;
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = EXPORT_GSON.fromJson(r, JsonObject.class);
            json.addProperty("id", UUID.randomUUID().toString());
            profile = CompanionProfile.fromJson(json);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.import_read_err", e.getMessage()));
            return 0;
        }
        ProfileManager.get().save(profile);

        ServerLevel level = player.serverLevel();
        CompanionEntity npc = ModEntities.COMPANION.get().create(level);
        if (npc == null) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.import_entity_fail"));
            return 0;
        }
        HitResult hit = player.pick(5.0D, 1.0F, false);
        Vec3 pos = hit.getType() == HitResult.Type.MISS ? player.position() : hit.getLocation();
        float yaw = player.getYRot() + 180.0F;
        npc.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        npc.setYBodyRot(yaw);
        npc.setYHeadRot(yaw);
        npc.setProfileId(profile.getId());
        npc.setCustomName(Component.literal(profile.getName()));
        level.addFreshEntity(npc);

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.import_ok", profile.getName(), shortId(profile.getId())), true);
        return 1;
    }

    private static String sanitizeFileName(String raw) {
        String s = raw.replaceAll("[^A-Za-z0-9._-]", "");
        while (s.startsWith(".")) {
            s = s.substring(1);
        }
        if (s.toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
            s = s.substring(0, s.length() - 5);
        }
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    private static int setDisguise(CommandContext<CommandSourceStack> ctx, @Nullable String type) throws CommandSyntaxException {
        CompanionProfile profile = resolveProfile(ctx);
        if (type != null && net.minecraft.world.entity.EntityType.byString(type).isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("wh_npcs.msg.command.disguise_unknown", type));
            return 0;
        }
        profile.setDisguise(type);
        ProfileManager.get().save(profile);
        ProfileSync.applyToLoadedEntities(ctx.getSource().getServer(), profile);
        ctx.getSource().sendSuccess(() -> type != null
                ? Component.translatable("wh_npcs.msg.command.disguise_set", profile.getName(), type)
                : Component.translatable("wh_npcs.msg.command.disguise_clear", profile.getName()), true);
        return 1;
    }

    private static int flagsGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        var flags = PlayerStateManager.get(ctx.getSource().getServer()).flagsOf(player.getUUID());
        if (flags.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "wh_npcs.msg.command.flags_empty", player.getGameProfile().getName()), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "wh_npcs.msg.command.flags_list", player.getGameProfile().getName(), flags.size(),
                    String.join(", ", flags)), false);
        }
        return flags.size();
    }

    private static int flagsSet(CommandContext<CommandSourceStack> ctx, boolean value) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String flag = StringArgumentType.getString(ctx, "flag");
        PlayerStateManager.get(ctx.getSource().getServer()).setFlag(player.getUUID(), flag, value);
        ctx.getSource().sendSuccess(() -> value
                ? Component.translatable("wh_npcs.msg.command.flag_set", flag, player.getGameProfile().getName())
                : Component.translatable("wh_npcs.msg.command.flag_unset", flag, player.getGameProfile().getName()), true);
        return 1;
    }

    private static int flagsClearAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerStateManager.get(ctx.getSource().getServer()).clearFlags(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "wh_npcs.msg.command.flags_clear", player.getGameProfile().getName()), true);
        return 1;
    }

    private static CompanionIndex.Entry resolve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String query = StringArgumentType.getString(ctx, "name");
        CompanionIndex index = CompanionIndex.get(ctx.getSource().getServer());
        try {
            CompanionIndex.Entry byId = index.byId(UUID.fromString(query));
            if (byId != null) {
                return byId;
            }
        } catch (IllegalArgumentException ignored) {

        }
        CompanionIndex.Entry byName = index.byName(query);
        if (byName == null) {
            throw NOT_FOUND.create(query);
        }
        return byName;
    }

    @Nullable
    private static CompanionEntity loadEntity(MinecraftServer server, CompanionIndex.Entry e) {
        ServerLevel level = server.getLevel(e.dimension());
        if (level == null) {
            return null;
        }
        level.getChunk(SectionPos.blockToSectionCoord(e.pos().getX()), SectionPos.blockToSectionCoord(e.pos().getZ()));
        Entity entity = level.getEntity(e.id());
        return entity instanceof CompanionEntity companion ? companion : null;
    }

    private static void teleportNpc(CompanionEntity npc, ServerLevel target, Vec3 pos) {
        if (npc.level() != target) {
            Entity moved = npc.changeDimension(target, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld,
                                          float yaw, Function<Boolean, Entity> repositionEntity) {
                    Entity placed = repositionEntity.apply(false);
                    placed.moveTo(pos.x, pos.y, pos.z, placed.getYRot(), placed.getXRot());
                    return placed;
                }
            });
            if (moved instanceof CompanionEntity movedNpc) {
                movedNpc.updateIndex();
            }
            return;
        }
        npc.teleportTo(pos.x, pos.y, pos.z);
        npc.updateIndex();
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static String formatPos(CompanionIndex.Entry e) {
        return e.pos().getX() + " " + e.pos().getY() + " " + e.pos().getZ() + " (" + e.dimension().location() + ")";
    }
}
