package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.dialogue.DialogueRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DialogueChoicePacket {

    private final String dialogueId;
    private final String nodeId;
    private final int choiceIndex;

    public DialogueChoicePacket(String dialogueId, String nodeId, int choiceIndex) {
        this.dialogueId = dialogueId;
        this.nodeId = nodeId;
        this.choiceIndex = choiceIndex;
    }

    public static void encode(DialogueChoicePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.dialogueId);
        buf.writeUtf(packet.nodeId);
        buf.writeVarInt(packet.choiceIndex);
    }

    public static DialogueChoicePacket decode(FriendlyByteBuf buf) {
        return new DialogueChoicePacket(buf.readUtf(), buf.readUtf(), buf.readVarInt());
    }

    public static void handle(DialogueChoicePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            DialogueRuntime.handleChoice(sender, packet.dialogueId, packet.nodeId, packet.choiceIndex);
        }
        ctx.get().setPacketHandled(true);
    }
}
