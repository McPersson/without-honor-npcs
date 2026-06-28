package com.withouthonor.npcs.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.glossary.GlossaryManager;
import com.withouthonor.npcs.common.glossary.GlossaryTerm;
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

public final class GlossaryPackets {

    private static final int MAX_BYTES = 262144;

    private GlossaryPackets() {
    }

    private static void sendData(ServerPlayer player) {
        JsonArray array = new JsonArray();
        for (GlossaryTerm term : GlossaryManager.get().all()) {
            array.add(term.toJson());
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
                    List<GlossaryTerm> list = new ArrayList<>();
                    for (JsonElement e : JsonParser.parseString(
                            new String(packet.payload, StandardCharsets.UTF_8)).getAsJsonArray()) {
                        list.add(GlossaryTerm.fromJson(e.getAsJsonObject()));
                    }
                    com.withouthonor.npcs.client.ClientGlossary.set(list);
                } catch (Exception ex) {
                    WHCompanions.LOGGER.warn("Bad glossary data from server", ex);
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
                    GlossaryTerm term = GlossaryTerm.fromJson(JsonParser.parseString(
                            new String(packet.json, StandardCharsets.UTF_8)).getAsJsonObject());

                    GlossaryTerm existing = GlossaryManager.get().byId(term.getId());
                    if (existing != null && !existing.getAuthor().isEmpty()) {
                        term.setAuthor(existing.getAuthor());
                    } else if (term.getAuthor().isEmpty()) {
                        term.setAuthor(sender.getGameProfile().getName());
                    }
                    GlossaryManager.get().put(term);
                } catch (Exception e) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.glossary.save_err", e.getMessage())
                            .withStyle(ChatFormatting.RED));
                }
                sendData(sender);
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
            buf.writeUtf(packet.id, 64);
        }

        public static Delete decode(FriendlyByteBuf buf) {
            return new Delete(buf.readUtf(64));
        }

        public static void handle(Delete packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                GlossaryManager.get().delete(packet.id);
                sendData(sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
