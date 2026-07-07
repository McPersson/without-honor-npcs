package com.withouthonor.npcs.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.reputation.Faction;
import com.withouthonor.npcs.common.reputation.FactionRegistry;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FactionPackets {

    private static final int MAX_BYTES = 65536;

    private FactionPackets() {
    }

    private static void sendData(ServerPlayer player) {
        // Считаем NPC в мире (по индексу), а не профили: профиль без NPC не учитывается
        java.util.Map<String, Integer> factionCounts = new java.util.HashMap<>();
        if (player.server != null) {
            for (var e : com.withouthonor.npcs.common.storage.CompanionIndex.get(player.server).all()) {
                if (e.profileId() == null) {
                    continue;
                }
                CompanionProfile profile = ProfileManager.get().get(e.profileId());
                if (profile != null && profile.getFaction() != null && !profile.getFaction().isEmpty()) {
                    factionCounts.merge(profile.getFaction(), 1, Integer::sum);
                }
            }
        }
        JsonArray array = new JsonArray();
        for (Faction faction : FactionRegistry.get().all()) {
            JsonObject json = faction.toJson();
            json.addProperty("used_by", factionCounts.getOrDefault(faction.getId(), 0));
            array.add(json);
        }
        NetworkHandler.sendToPlayer(new Data(array.toString().getBytes(StandardCharsets.UTF_8)), player);
    }

    public static class Request {

        public static void encode(Request packet, FriendlyByteBuf buf) {
        }

        public static Request decode(FriendlyByteBuf buf) {
            return new Request();
        }

        public static void handle(Request packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                sendData(sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static class Data {

        private final byte[] payload;

        public Data(byte[] payload) {
            this.payload = payload;
        }

        public static void encode(Data packet, FriendlyByteBuf buf) {
            buf.writeByteArray(packet.payload);
        }

        public static Data decode(FriendlyByteBuf buf) {
            return new Data(buf.readByteArray(MAX_BYTES));
        }

        public static void handle(Data packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    List<com.withouthonor.npcs.client.ClientFactions.Full> list = new ArrayList<>();
                    for (JsonElement e : JsonParser.parseString(
                            new String(packet.payload, StandardCharsets.UTF_8)).getAsJsonArray()) {
                        JsonObject json = e.getAsJsonObject();
                        int usedBy = json.has("used_by") ? json.get("used_by").getAsInt() : 0;
                        json.remove("used_by");
                        list.add(new com.withouthonor.npcs.client.ClientFactions.Full(Faction.fromJson(json), usedBy));
                    }
                    com.withouthonor.npcs.client.ClientFactions.setFull(list);
                } catch (Exception ex) {
                    WHCompanions.LOGGER.warn("Bad factions data from server", ex);
                }
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class Save {

        private final byte[] json;

        public Save(JsonObject json) {
            this.json = json.toString().getBytes(StandardCharsets.UTF_8);
        }

        private Save(byte[] json) {
            this.json = json;
        }

        public static void encode(Save packet, FriendlyByteBuf buf) {
            buf.writeByteArray(packet.json);
        }

        public static Save decode(FriendlyByteBuf buf) {
            return new Save(buf.readByteArray(MAX_BYTES));
        }

        public static void handle(Save packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                try {
                    Faction faction = Faction.fromJson(JsonParser.parseString(
                            new String(packet.json, StandardCharsets.UTF_8)).getAsJsonObject());
                    FactionRegistry.get().put(faction);
                } catch (Exception e) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.faction.save_err", e.getMessage())
                            .withStyle(ChatFormatting.RED));
                }
                sendData(sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public record RepEntry(java.util.UUID id, String name, int value) {
    }

    private static void sendRepData(ServerPlayer player, String factionId) {
        var stored = com.withouthonor.npcs.common.storage.PlayerStateManager
                .get(player.server).reputationsFor(factionId);
        List<RepEntry> entries = new ArrayList<>();
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            seen.add(online.getUUID());
            entries.add(new RepEntry(online.getUUID(), online.getGameProfile().getName(),
                    stored.getOrDefault(online.getUUID(), 0)));
        }
        for (var entry : stored.entrySet()) {
            if (seen.add(entry.getKey())) {
                String name = player.server.getProfileCache() != null
                        ? player.server.getProfileCache().get(entry.getKey())
                                .map(com.mojang.authlib.GameProfile::getName)
                                .orElse(entry.getKey().toString().substring(0, 8))
                        : entry.getKey().toString().substring(0, 8);
                entries.add(new RepEntry(entry.getKey(), name, entry.getValue()));
            }
        }
        entries.sort(java.util.Comparator.comparing(RepEntry::name, String.CASE_INSENSITIVE_ORDER));
        NetworkHandler.sendToPlayer(new RepData(factionId, entries), player);
    }

    public static class RepRequest {

        private final String factionId;

        public RepRequest(String factionId) {
            this.factionId = factionId;
        }

        public static void encode(RepRequest packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.factionId, 32);
        }

        public static RepRequest decode(FriendlyByteBuf buf) {
            return new RepRequest(buf.readUtf(32));
        }

        public static void handle(RepRequest packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                sendRepData(sender, packet.factionId);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static class RepData {

        private final String factionId;
        private final List<RepEntry> entries;

        public RepData(String factionId, List<RepEntry> entries) {
            this.factionId = factionId;
            this.entries = entries;
        }

        public static void encode(RepData packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.factionId, 32);
            buf.writeCollection(packet.entries, (b, e) -> {
                b.writeUUID(e.id());
                b.writeUtf(e.name(), 32);
                b.writeVarInt(e.value());
            });
        }

        public static RepData decode(FriendlyByteBuf buf) {
            return new RepData(buf.readUtf(32), buf.readCollection(ArrayList::new,
                    b -> new RepEntry(b.readUUID(), b.readUtf(32), b.readVarInt())));
        }

        public static void handle(RepData packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.withouthonor.npcs.client.gui.editor.NpcEditorScreen
                            .acceptRepData(packet.factionId, packet.entries)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class RepSet {

        private final String factionId;
        private final java.util.UUID player;
        private final int value;

        public RepSet(String factionId, java.util.UUID player, int value) {
            this.factionId = factionId;
            this.player = player;
            this.value = value;
        }

        public static void encode(RepSet packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.factionId, 32);
            buf.writeUUID(packet.player);
            buf.writeVarInt(packet.value);
        }

        public static RepSet decode(FriendlyByteBuf buf) {
            return new RepSet(buf.readUtf(32), buf.readUUID(), buf.readVarInt());
        }

        public static void handle(RepSet packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                com.withouthonor.npcs.common.storage.PlayerStateManager.get(sender.server)
                        .setReputation(packet.player, packet.factionId, packet.value);
                sendRepData(sender, packet.factionId);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static class Delete {

        private final String id;

        public Delete(String id) {
            this.id = id;
        }

        public static void encode(Delete packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.id, 32);
        }

        public static Delete decode(FriendlyByteBuf buf) {
            return new Delete(buf.readUtf(32));
        }

        public static void handle(Delete packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                if (!sender.hasPermissions(3)) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.faction.delete_denied")
                            .withStyle(ChatFormatting.RED));
                } else if (FactionRegistry.get().delete(packet.id)) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.faction.deleted", packet.id)
                            .withStyle(ChatFormatting.GREEN));
                }
                sendData(sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
