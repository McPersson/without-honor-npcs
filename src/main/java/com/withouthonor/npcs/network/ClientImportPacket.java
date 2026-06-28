package com.withouthonor.npcs.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.profile.ProfileSync;
import com.withouthonor.npcs.common.storage.DialogueManager;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class ClientImportPacket {

    public static final int KIND_PROFILE = 0;
    public static final int KIND_DIALOGUE = 1;

    private final int kind;
    private final int entityId;
    private final byte[] json;

    public ClientImportPacket(int kind, int entityId, byte[] json) {
        this.kind = kind;
        this.entityId = entityId;
        this.json = json;
    }

    public static void encode(ClientImportPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.kind);
        buf.writeVarInt(p.entityId);
        buf.writeByteArray(p.json);
    }

    public static ClientImportPacket decode(FriendlyByteBuf buf) {
        return new ClientImportPacket(buf.readVarInt(), buf.readVarInt(),
                buf.readByteArray(EditorDataPacket.MAX_JSON_BYTES));
    }

    public static void handle(ClientImportPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            ctx.get().enqueueWork(() -> apply(p, sender));
        }
        ctx.get().setPacketHandled(true);
    }

    private static void apply(ClientImportPacket p, ServerPlayer sender) {
        if (!com.withouthonor.npcs.common.config.WhConfig.allowClientImport()) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.share.disabled")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!sender.hasPermissions(2)) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.share.no_permission")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(
                    new String(p.json, StandardCharsets.UTF_8)).getAsJsonObject();
            if (p.kind == KIND_DIALOGUE) {
                DialogueGraph graph = DialogueGraph.fromJson(json);
                if (!DialogueManager.isValidId(graph.getId())) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.bad_id")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                DialogueManager.get().save(graph);
                NetworkHandler.sendToPlayer(
                        new DialogueListPacket(EditorDataPacket.buildSummaries()), sender);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.saved", graph.getId()));
            } else {
                if (!(sender.serverLevel().getEntity(p.entityId) instanceof CompanionEntity npc)
                        || npc.getProfileId() == null) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.npc_reopen")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                json.addProperty("id", npc.getProfileId().toString());
                CompanionProfile profile = CompanionProfile.fromJson(json);
                ProfileManager.get().save(profile);
                ProfileSync.applyToLoadedEntities(sender.server, profile);
                EditorDataPacket.send(sender, profile, p.entityId);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.import_ok", profile.getName()));
            }
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Client import failed for {}: {}",
                    sender.getGameProfile().getName(), e.getMessage());
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.import_err", e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }
}
