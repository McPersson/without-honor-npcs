package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RefreshDialoguesPacket {

    public RefreshDialoguesPacket() {
    }

    public static void encode(RefreshDialoguesPacket packet, FriendlyByteBuf buf) {
    }

    public static RefreshDialoguesPacket decode(FriendlyByteBuf buf) {
        return new RefreshDialoguesPacket();
    }

    public static void handle(RefreshDialoguesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            DialogueManager.get().reload();
            NetworkHandler.sendToPlayer(
                    new DialogueListPacket(EditorDataPacket.buildSummaries()), sender);
        }
        ctx.get().setPacketHandled(true);
    }
}
