package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import com.withouthonor.npcs.common.storage.DialogueManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteDialoguePacket {

    private final String id;

    public DeleteDialoguePacket(String id) {
        this.id = id;
    }

    public static void encode(DeleteDialoguePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.id, 64);
    }

    public static DeleteDialoguePacket decode(FriendlyByteBuf buf) {
        return new DeleteDialoguePacket(buf.readUtf(64));
    }

    public static void handle(DeleteDialoguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            handleOnServer(packet.id, sender);
        }
        ctx.get().setPacketHandled(true);
    }

    private static void handleOnServer(String id, ServerPlayer sender) {
        DialogueGraph graph = DialogueManager.get().get(id);
        if (graph == null) {
            return;
        }
        if (DialogueManager.get().delete(id)) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.deleted", id)
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
