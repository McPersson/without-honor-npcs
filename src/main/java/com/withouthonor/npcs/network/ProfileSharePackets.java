package com.withouthonor.npcs.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.profile.ProfileSync;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class ProfileSharePackets {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ProfileSharePackets() {
    }

    public record FileEntry(String name, String author, String skin, int sizeKb, long mtime) {

        public static void write(FriendlyByteBuf buf, FileEntry e) {
            buf.writeUtf(e.name(), 80);
            buf.writeUtf(e.author(), 32);
            buf.writeUtf(e.skin(), 64);
            buf.writeVarInt(e.sizeKb());
            buf.writeVarLong(e.mtime());
        }

        public static FileEntry read(FriendlyByteBuf buf) {
            return new FileEntry(buf.readUtf(80), buf.readUtf(32), buf.readUtf(64),
                    buf.readVarInt(), buf.readVarLong());
        }
    }

    private static Path exportsDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("exports");
    }

    private static String sanitize(String raw) {
        String s = raw.replaceAll("[^A-Za-z0-9._-]", "");
        while (s.startsWith(".")) {
            s = s.substring(1);
        }
        if (s.toLowerCase(Locale.ROOT).endsWith(".json")) {
            s = s.substring(0, s.length() - 5);
        }
        if (s.isEmpty()) {
            s = "npc";
        }
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    private static final java.util.Map<String, long[]> PRESET_MTIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, CompanionProfile> PRESET_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @javax.annotation.Nullable
    public static CompanionProfile loadExportProfile(MinecraftServer server, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String file = sanitize(name);
        Path path = exportsDir(server).resolve(file + ".json");
        try {
            if (!Files.isRegularFile(path)) {
                PRESET_CACHE.remove(file);
                PRESET_MTIME.remove(file);
                return null;
            }
            long mtime = Files.getLastModifiedTime(path).toMillis();
            long[] cached = PRESET_MTIME.get(file);
            if (cached != null && cached[0] == mtime && PRESET_CACHE.containsKey(file)) {
                return PRESET_CACHE.get(file);
            }
            JsonObject json;
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(r).getAsJsonObject();
            }
            CompanionProfile profile = CompanionProfile.fromJson(json);
            PRESET_CACHE.put(file, profile);
            PRESET_MTIME.put(file, new long[]{mtime});
            return profile;
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to load second-char preset '{}': {}", file, e.toString());
            return null;
        }
    }

    private static List<FileEntry> listEntries(MinecraftServer server) {
        List<FileEntry> out = new ArrayList<>();
        Path dir = exportsDir(server);
        if (!Files.isDirectory(dir)) {
            return out;
        }
        List<Path> paths = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().forEach(paths::add);
        } catch (IOException ignored) {
            return out;
        }
        for (Path p : paths) {
            try {
                String fn = p.getFileName().toString();
                String name = fn.substring(0, fn.length() - 5);
                String author = "";
                String skin = "";
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                    if (json.has("exported_by")) {
                        author = json.get("exported_by").getAsString();
                    }
                    if (json.has("skin_player_name") && !json.get("skin_player_name").isJsonNull()) {
                        skin = json.get("skin_player_name").getAsString();
                    }
                } catch (Exception ignored) {

                }
                int sizeKb = (int) Math.max(1, Files.size(p) / 1024);
                long mtime = Files.getLastModifiedTime(p).toMillis();
                out.add(new FileEntry(name, author, skin, sizeKb, mtime));
            } catch (IOException ignored) {

            }
        }
        return out;
    }

    private static boolean canDelete(ServerPlayer sender) {
        if (sender.hasPermissions(3)) {
            return true;
        }
        MinecraftServer server = sender.server;
        return server.isSingleplayer()
                && server.getSingleplayerProfile() != null
                && server.getSingleplayerProfile().getId().equals(sender.getUUID());
    }

    private static void deny(ServerPlayer sender) {
        sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.no_permission").withStyle(ChatFormatting.RED));
    }

    public static final class Export {

        private final byte[] json;

        public Export(byte[] json) {
            this.json = json;
        }

        public Export(JsonObject json) {
            this(json.toString().getBytes(StandardCharsets.UTF_8));
        }

        public static void encode(Export p, FriendlyByteBuf buf) {
            buf.writeByteArray(p.json);
        }

        public static Export decode(FriendlyByteBuf buf) {
            return new Export(buf.readByteArray(EditorDataPacket.MAX_JSON_BYTES));
        }

        public static void handle(Export p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                server(p, sender);
            }
            ctx.get().setPacketHandled(true);
        }

        private static void server(Export p, ServerPlayer sender) {
            if (!sender.hasPermissions(2)) {
                deny(sender);
                return;
            }
            try {
                JsonObject json = JsonParser.parseString(
                        new String(p.json, StandardCharsets.UTF_8)).getAsJsonObject();
                json.addProperty("exported_by", sender.getGameProfile().getName());
                String name = json.has("name") ? json.get("name").getAsString() : "npc";
                String id = json.has("id") ? json.get("id").getAsString() : "";
                String file = sanitize(name) + (id.length() >= 8 ? "_" + id.substring(0, 8) : "");
                Path dir = exportsDir(sender.server);
                Path target = dir.resolve(file + ".json");
                Path tmp = dir.resolve(file + ".json.tmp");
                Files.createDirectories(dir);
                try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                    GSON.toJson(json, w);
                }
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.export_ok", file));
            } catch (Exception e) {
                WHCompanions.LOGGER.warn("Export failed for {}: {}", sender.getGameProfile().getName(), e.getMessage());
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.export_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    public static final class RequestList {

        public RequestList() {
        }

        public static void encode(RequestList p, FriendlyByteBuf buf) {
        }

        public static RequestList decode(FriendlyByteBuf buf) {
            return new RequestList();
        }

        public static void handle(RequestList p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                NetworkHandler.sendToPlayer(new ListResult(listEntries(sender.server)), sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class ListResult {

        private final List<FileEntry> files;
        private final boolean forSpawn;

        public ListResult(List<FileEntry> files) {
            this(files, false);
        }

        public ListResult(List<FileEntry> files, boolean forSpawn) {
            this.files = files;
            this.forSpawn = forSpawn;
        }

        public static void encode(ListResult p, FriendlyByteBuf buf) {
            buf.writeCollection(p.files, FileEntry::write);
            buf.writeBoolean(p.forSpawn);
        }

        public static ListResult decode(FriendlyByteBuf buf) {
            return new ListResult(buf.readCollection(ArrayList::new, FileEntry::read), buf.readBoolean());
        }

        public static void handle(ListResult p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                            com.withouthonor.npcs.client.ClientNetHandlers.openImport(p.files, p.forSpawn)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class Import {

        private final int entityId;
        private final String file;

        public Import(int entityId, String file) {
            this.entityId = entityId;
            this.file = file;
        }

        public static void encode(Import p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.entityId);
            buf.writeUtf(p.file, 80);
        }

        public static Import decode(FriendlyByteBuf buf) {
            return new Import(buf.readVarInt(), buf.readUtf(80));
        }

        public static void handle(Import p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                server(p, sender);
            }
            ctx.get().setPacketHandled(true);
        }

        private static void server(Import p, ServerPlayer sender) {
            if (!sender.hasPermissions(2)) {
                deny(sender);
                return;
            }
            try {
                if (!(sender.serverLevel().getEntity(p.entityId) instanceof CompanionEntity npc)
                        || npc.getProfileId() == null) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.npc_reopen")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                String file = sanitize(p.file);
                Path path = exportsDir(sender.server).resolve(file + ".json");
                if (!Files.isRegularFile(path)) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.file_not_found", file)
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                JsonObject json;
                try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(r).getAsJsonObject();
                }

                json.addProperty("id", npc.getProfileId().toString());
                CompanionProfile profile = CompanionProfile.fromJson(json);
                ProfileManager.get().save(profile);
                ProfileSync.applyToLoadedEntities(sender.server, profile);
                EditorDataPacket.send(sender, profile, p.entityId);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.import_ok", file));
            } catch (Exception e) {
                WHCompanions.LOGGER.warn("Import failed for {}: {}", sender.getGameProfile().getName(), e.getMessage());
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.import_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    public static final class Delete {

        private final String file;

        public Delete(String file) {
            this.file = file;
        }

        public static void encode(Delete p, FriendlyByteBuf buf) {
            buf.writeUtf(p.file, 80);
        }

        public static Delete decode(FriendlyByteBuf buf) {
            return new Delete(buf.readUtf(80));
        }

        public static void handle(Delete p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                server(p, sender);
            }
            ctx.get().setPacketHandled(true);
        }

        private static void server(Delete p, ServerPlayer sender) {
            if (!canDelete(sender)) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.delete_denied")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            String file = sanitize(p.file);
            try {
                Files.deleteIfExists(exportsDir(sender.server).resolve(file + ".json"));
            } catch (IOException e) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.delete_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }

            NetworkHandler.sendToPlayer(new ListResult(listEntries(sender.server)), sender);
        }
    }

    public static final class Rename {

        private final String file;
        private final String newName;

        public Rename(String file, String newName) {
            this.file = file;
            this.newName = newName;
        }

        public static void encode(Rename p, FriendlyByteBuf buf) {
            buf.writeUtf(p.file, 80);
            buf.writeUtf(p.newName, 80);
        }

        public static Rename decode(FriendlyByteBuf buf) {
            return new Rename(buf.readUtf(80), buf.readUtf(80));
        }

        public static void handle(Rename p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                server(p, sender);
            }
            ctx.get().setPacketHandled(true);
        }

        private static void server(Rename p, ServerPlayer sender) {
            if (!sender.hasPermissions(2)) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.share.no_permission")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            String old = sanitize(p.file);
            String nw = sanitize(p.newName);
            Path dir = exportsDir(sender.server);
            try {
                if (!old.equals(nw) && Files.isRegularFile(dir.resolve(old + ".json"))) {
                    Path dst = dir.resolve(nw + ".json");
                    if (Files.exists(dst)) {
                        sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.rename_taken", nw)
                                .withStyle(ChatFormatting.RED));
                        return;
                    }
                    Files.move(dir.resolve(old + ".json"), dst);
                }
            } catch (IOException e) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.rename_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
            NetworkHandler.sendToPlayer(new ListResult(listEntries(sender.server)), sender);
        }
    }

    public static final class BookRequest {

        public BookRequest() {
        }

        public static void encode(BookRequest p, FriendlyByteBuf buf) {
        }

        public static BookRequest decode(FriendlyByteBuf buf) {
            return new BookRequest();
        }

        public static void handle(BookRequest p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                if (sender.hasPermissions(2)) {
                    NetworkHandler.sendToPlayer(new ListResult(listEntries(sender.server), true), sender);
                } else {
                    deny(sender);
                }
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class SpawnFromFile {

        private final String file;

        public SpawnFromFile(String file) {
            this.file = file;
        }

        public static void encode(SpawnFromFile p, FriendlyByteBuf buf) {
            buf.writeUtf(p.file, 80);
        }

        public static SpawnFromFile decode(FriendlyByteBuf buf) {
            return new SpawnFromFile(buf.readUtf(80));
        }

        public static void handle(SpawnFromFile p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                server(p, sender);
            }
            ctx.get().setPacketHandled(true);
        }

        private static void server(SpawnFromFile p, ServerPlayer sender) {
            if (!sender.hasPermissions(2)) {
                deny(sender);
                return;
            }
            try {
                String file = sanitize(p.file);
                Path path = exportsDir(sender.server).resolve(file + ".json");
                if (!Files.isRegularFile(path)) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.file_not_found", file)
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                JsonObject json;
                try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(r).getAsJsonObject();
                }
                json.addProperty("id", java.util.UUID.randomUUID().toString());
                CompanionProfile profile = CompanionProfile.fromJson(json);
                ProfileManager.get().save(profile);

                net.minecraft.server.level.ServerLevel level = sender.serverLevel();
                CompanionEntity npc = com.withouthonor.npcs.common.registry.ModEntities.COMPANION.get()
                        .create(level);
                if (npc == null) {
                    return;
                }
                net.minecraft.world.phys.Vec3 loc = sender.pick(6.0, 1.0F, false).getLocation();
                float yaw = sender.getYRot() + 180.0F;
                npc.moveTo(loc.x, loc.y, loc.z, yaw, 0.0F);
                npc.setYBodyRot(yaw);
                npc.setYHeadRot(yaw);
                npc.setProfileId(profile.getId());
                String name = json.has("name") ? json.get("name").getAsString() : "NPC";
                npc.setCustomName(Component.literal(name));
                level.addFreshEntity(npc);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.spawned", file));
            } catch (Exception e) {
                WHCompanions.LOGGER.warn("Spawn-from-file failed for {}: {}",
                        sender.getGameProfile().getName(), e.getMessage());
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.spawn_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    public static final class SpawnFromClient {

        private final byte[] json;

        public SpawnFromClient(byte[] json) {
            this.json = json;
        }

        public static void encode(SpawnFromClient p, FriendlyByteBuf buf) {
            buf.writeByteArray(p.json);
        }

        public static SpawnFromClient decode(FriendlyByteBuf buf) {
            return new SpawnFromClient(buf.readByteArray(EditorDataPacket.MAX_JSON_BYTES));
        }

        public static void handle(SpawnFromClient p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                server(p, sender);
            }
            ctx.get().setPacketHandled(true);
        }

        private static void server(SpawnFromClient p, ServerPlayer sender) {
            if (!sender.hasPermissions(2)) {
                deny(sender);
                return;
            }
            if (!com.withouthonor.npcs.common.config.WhConfig.allowClientImport()) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.share.disabled")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            try {
                JsonObject json = JsonParser.parseString(
                        new String(p.json, StandardCharsets.UTF_8)).getAsJsonObject();
                json.addProperty("id", java.util.UUID.randomUUID().toString());
                CompanionProfile profile = CompanionProfile.fromJson(json);
                ProfileManager.get().save(profile);

                net.minecraft.server.level.ServerLevel level = sender.serverLevel();
                CompanionEntity npc = com.withouthonor.npcs.common.registry.ModEntities.COMPANION.get()
                        .create(level);
                if (npc == null) {
                    return;
                }
                net.minecraft.world.phys.Vec3 loc = sender.pick(6.0, 1.0F, false).getLocation();
                float yaw = sender.getYRot() + 180.0F;
                npc.moveTo(loc.x, loc.y, loc.z, yaw, 0.0F);
                npc.setYBodyRot(yaw);
                npc.setYHeadRot(yaw);
                npc.setProfileId(profile.getId());
                String name = json.has("name") ? json.get("name").getAsString() : "NPC";
                npc.setCustomName(Component.literal(name));
                level.addFreshEntity(npc);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.spawned", name));
            } catch (Exception e) {
                WHCompanions.LOGGER.warn("Spawn-from-client failed for {}: {}",
                        sender.getGameProfile().getName(), e.getMessage());
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.spawn_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        }
    }
}
