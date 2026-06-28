package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DialogueListPacket {

    private final List<EditorDataPacket.DialogueSummary> dialogues;

    public DialogueListPacket(List<EditorDataPacket.DialogueSummary> dialogues) {
        this.dialogues = dialogues;
    }

    public static void encode(DialogueListPacket packet, FriendlyByteBuf buf) {
        buf.writeCollection(packet.dialogues, EditorDataPacket.DialogueSummary::write);
    }

    public static DialogueListPacket decode(FriendlyByteBuf buf) {
        return new DialogueListPacket(
                buf.readCollection(ArrayList::new, EditorDataPacket.DialogueSummary::read));
    }

    public static void handle(DialogueListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.withouthonor.npcs.client.gui.editor.NpcEditorScreen.acceptDialogueList(
                                packet.dialogues)));
        ctx.get().setPacketHandled(true);
    }
}
