package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.dialogue.DialogueSessions;
import com.withouthonor.npcs.common.storage.PlayerStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RenameNodePacket {

    public record Rename(String oldId, String newId) {
    }

    private final String dialogueId;
    private final List<Rename> renames;

    public RenameNodePacket(String dialogueId, List<Rename> renames) {
        this.dialogueId = dialogueId;
        this.renames = renames;
    }

    public static void encode(RenameNodePacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.dialogueId, 64);
        buf.writeVarInt(p.renames.size());
        for (Rename r : p.renames) {
            buf.writeUtf(r.oldId(), 64);
            buf.writeUtf(r.newId(), 64);
        }
    }

    public static RenameNodePacket decode(FriendlyByteBuf buf) {
        String dialogueId = buf.readUtf(64);
        int n = Math.min(buf.readVarInt(), 512);
        List<Rename> renames = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            renames.add(new Rename(buf.readUtf(64), buf.readUtf(64)));
        }
        return new RenameNodePacket(dialogueId, renames);
    }

    public static void handle(RenameNodePacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            for (Rename r : p.renames) {
                PlayerStateManager.get(sender.server).renameNode(p.dialogueId, r.oldId(), r.newId());
                DialogueSessions.renameNode(p.dialogueId, r.oldId(), r.newId());
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
