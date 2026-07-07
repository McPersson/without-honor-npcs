package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.dialogue.DialogueSessions;
import com.withouthonor.npcs.common.dialogue.EntryPoint;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.storage.DialogueManager;
import com.withouthonor.npcs.common.storage.PlayerStateManager;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RenameDialoguePacket {

    private final String oldId;
    private final String newId;

    public RenameDialoguePacket(String oldId, String newId) {
        this.oldId = oldId;
        this.newId = newId;
    }

    public static void encode(RenameDialoguePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.oldId, 64);
        buf.writeUtf(packet.newId, 64);
    }

    public static RenameDialoguePacket decode(FriendlyByteBuf buf) {
        return new RenameDialoguePacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(RenameDialoguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            handleOnServer(packet.oldId, packet.newId, sender);
        }
        ctx.get().setPacketHandled(true);
    }

    private static void handleOnServer(String oldId, String newId, ServerPlayer sender) {
        DialogueManager dm = DialogueManager.get();
        if (dm.get(oldId) == null) {
            return;
        }
        if (!DialogueManager.isValidId(newId)) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.rename_invalid", newId)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (dm.get(newId) != null) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.rename_taken", newId)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!dm.rename(oldId, newId)) {
            return;
        }

        for (CompanionProfile profile : ProfileManager.get().all()) {
            boolean changed = false;
            for (EntryPoint entry : profile.getEntryPoints()) {
                if (entry.getDialogueId().equals(oldId)) {
                    entry.setDialogueId(newId);
                    changed = true;
                }
            }
            if (changed) {
                ProfileManager.get().save(profile);
            }
        }

        PlayerStateManager.get(sender.server).renameDialogue(oldId, newId);
        DialogueSessions.renameDialogue(oldId, newId);

        sender.sendSystemMessage(Component.translatable("wh_npcs.msg.dialogue.renamed", oldId, newId)
                .withStyle(ChatFormatting.GREEN));
        NetworkHandler.sendToPlayer(new DialogueListPacket(EditorDataPacket.buildSummaries(sender.server)), sender);
    }
}
