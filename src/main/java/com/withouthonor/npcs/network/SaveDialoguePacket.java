package com.withouthonor.npcs.network;

import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import com.withouthonor.npcs.common.storage.DialogueManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SaveDialoguePacket {

    private final byte[] json;

    public SaveDialoguePacket(byte[] json) {
        this.json = json;
    }

    public static void encode(SaveDialoguePacket packet, FriendlyByteBuf buf) {
        buf.writeByteArray(packet.json);
    }

    public static SaveDialoguePacket decode(FriendlyByteBuf buf) {
        return new SaveDialoguePacket(buf.readByteArray(EditorDataPacket.MAX_JSON_BYTES));
    }

    public static void handle(SaveDialoguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            handleOnServer(packet, sender);
        }
        ctx.get().setPacketHandled(true);
    }

    private static void handleOnServer(SaveDialoguePacket packet, ServerPlayer sender) {
        if (!sender.hasPermissions(2)) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.no_permission")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        try {
            DialogueGraph graph = DialogueGraph.fromJson(
                    JsonParser.parseString(new String(packet.json, StandardCharsets.UTF_8)).getAsJsonObject());
            if (!DialogueManager.isValidId(graph.getId())) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.bad_id")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            DialogueGraph existing = DialogueManager.get().get(graph.getId());
            graph.setAuthor(existing != null && !existing.getAuthor().isEmpty()
                    ? existing.getAuthor()
                    : sender.getGameProfile().getName());
            DialogueManager.get().save(graph);
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.saved", graph.getId())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Bad dialogue from {}: {}", sender.getGameProfile().getName(), e.getMessage());
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.save_err", e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }
}
