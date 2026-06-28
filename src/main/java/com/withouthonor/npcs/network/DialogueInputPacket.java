package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.dialogue.DialogueRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DialogueInputPacket {

    private final String dialogueId;
    private final String nodeId;
    private final String text;

    public DialogueInputPacket(String dialogueId, String nodeId, String text) {
        this.dialogueId = dialogueId;
        this.nodeId = nodeId;
        this.text = text;
    }

    public static void encode(DialogueInputPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.dialogueId);
        buf.writeUtf(packet.nodeId);
        buf.writeUtf(packet.text, 256);
    }

    public static DialogueInputPacket decode(FriendlyByteBuf buf) {
        return new DialogueInputPacket(buf.readUtf(), buf.readUtf(), buf.readUtf(256));
    }

    public static void handle(DialogueInputPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            DialogueRuntime.handleInput(sender, packet.dialogueId, packet.nodeId, packet.text);
        }
        ctx.get().setPacketHandled(true);
    }
}
