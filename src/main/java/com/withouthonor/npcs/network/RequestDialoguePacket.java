package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.DialogueManager;
import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class RequestDialoguePacket {

    private final String dialogueId;

    public RequestDialoguePacket(String dialogueId) {
        this.dialogueId = dialogueId;
    }

    public static void encode(RequestDialoguePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.dialogueId, 64);
    }

    public static RequestDialoguePacket decode(FriendlyByteBuf buf) {
        return new RequestDialoguePacket(buf.readUtf(64));
    }

    public static void handle(RequestDialoguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            DialogueGraph graph = DialogueManager.get().get(packet.dialogueId);
            if (graph == null) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.not_found", packet.dialogueId)
                        .withStyle(ChatFormatting.RED));
            } else {
                NetworkHandler.sendToPlayer(new DialogueDataPacket(packet.dialogueId,
                        graph.toJson().toString().getBytes(StandardCharsets.UTF_8)), sender);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
