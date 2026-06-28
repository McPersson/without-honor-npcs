package com.withouthonor.npcs.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
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

public final class PoseLibraryPackets {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private PoseLibraryPackets() {
    }

    public record PoseEntry(String name, String author, long mtime, int sizeKb, String pose, String transform) {

        public static void write(FriendlyByteBuf buf, PoseEntry e) {
            buf.writeUtf(e.name(), 80);
            buf.writeUtf(e.author(), 32);
            buf.writeVarLong(e.mtime());
            buf.writeVarInt(e.sizeKb());
            buf.writeUtf(e.pose(), 1024);
            buf.writeUtf(e.transform(), 512);
        }

        public static PoseEntry read(FriendlyByteBuf buf) {
            return new PoseEntry(buf.readUtf(80), buf.readUtf(32), buf.readVarLong(),
                    buf.readVarInt(), buf.readUtf(1024), buf.readUtf(512));
        }
    }

    private static Path posesDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("poses");
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
            s = "pose";
        }
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    private static List<PoseEntry> listEntries(MinecraftServer server) {
        List<PoseEntry> out = new ArrayList<>();
        Path dir = posesDir(server);
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
                String pose = "{}";
                String transform = "{}";
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                    if (json.has("name") && !json.get("name").isJsonNull()) {
                        name = json.get("name").getAsString();
                    }
                    if (json.has("exported_by")) {
                        author = json.get("exported_by").getAsString();
                    }
                    if (json.has("pose") && json.get("pose").isJsonObject()) {
                        pose = json.getAsJsonObject("pose").toString();
                    }
                    if (json.has("transform") && json.get("transform").isJsonObject()) {
                        transform = json.getAsJsonObject("transform").toString();
                    }
                } catch (Exception ignored) {

                }
                int sizeKb = (int) Math.max(1, Files.size(p) / 1024);
                long mtime = Files.getLastModifiedTime(p).toMillis();
                out.add(new PoseEntry(name, author, mtime, sizeKb, pose, transform));
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

    public static final class Save {

        private final String name;
        private final String poseJson;
        private final String transformJson;

        public Save(String name, String poseJson, String transformJson) {
            this.name = name;
            this.poseJson = poseJson;
            this.transformJson = transformJson;
        }

        public static void encode(Save p, FriendlyByteBuf buf) {
            buf.writeUtf(p.name, 80);
            buf.writeUtf(p.poseJson, 1024);
            buf.writeUtf(p.transformJson, 512);
        }

        public static Save decode(FriendlyByteBuf buf) {
            return new Save(buf.readUtf(80), buf.readUtf(1024), buf.readUtf(512));
        }

        public static void handle(Save p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                server(p, sender);
            }
            ctx.get().setPacketHandled(true);
        }

        private static void server(Save p, ServerPlayer sender) {
            if (!sender.hasPermissions(2)) {
                deny(sender);
                return;
            }
            try {
                String display = p.name == null ? "" : p.name.trim();
                if (display.isEmpty()) {
                    display = "Pose";
                }
                JsonObject pose = parseObjOrEmpty(p.poseJson);
                JsonObject transform = parseObjOrEmpty(p.transformJson);
                JsonObject json = new JsonObject();
                json.addProperty("name", display);
                json.addProperty("exported_by", sender.getGameProfile().getName());
                json.add("pose", pose);
                json.add("transform", transform);

                String file = sanitize(display);
                Path dir = posesDir(sender.server);
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
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.pose.saved", file));

                NetworkHandler.sendToPlayer(new ListResult(listEntries(sender.server)), sender);
            } catch (Exception e) {
                WHCompanions.LOGGER.warn("Pose save failed for {}: {}", sender.getGameProfile().getName(), e.getMessage());
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.pose.save_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        }

        private static JsonObject parseObjOrEmpty(String s) {
            try {
                if (s != null && !s.isBlank()) {
                    var el = JsonParser.parseString(s);
                    if (el.isJsonObject()) {
                        return el.getAsJsonObject();
                    }
                }
            } catch (Exception ignored) {

            }
            return new JsonObject();
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

        private final List<PoseEntry> poses;

        public ListResult(List<PoseEntry> poses) {
            this.poses = poses;
        }

        public static void encode(ListResult p, FriendlyByteBuf buf) {
            buf.writeCollection(p.poses, PoseEntry::write);
        }

        public static ListResult decode(FriendlyByteBuf buf) {
            return new ListResult(buf.readCollection(ArrayList::new, PoseEntry::read));
        }

        public static void handle(ListResult p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> accept(p.poses)));
            ctx.get().setPacketHandled(true);
        }

        private static void accept(List<PoseEntry> poses) {
            com.withouthonor.npcs.client.ClientPoseLibrary.set(poses);
            com.withouthonor.npcs.client.gui.editor.PoseLibraryScreen.acceptList(poses);
        }
    }

    public static final class Delete {

        private final String name;

        public Delete(String name) {
            this.name = name;
        }

        public static void encode(Delete p, FriendlyByteBuf buf) {
            buf.writeUtf(p.name, 80);
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
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.pose.delete_denied")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            String file = sanitize(p.name);
            try {
                Files.deleteIfExists(posesDir(sender.server).resolve(file + ".json"));
            } catch (IOException e) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.delete_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }

            NetworkHandler.sendToPlayer(new ListResult(listEntries(sender.server)), sender);
        }
    }
}
