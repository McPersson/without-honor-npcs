package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import com.withouthonor.npcs.common.storage.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RequestDialogueBundle {

    private static final int MAX_IDS = 64;

    private final List<String> ids;

    public RequestDialogueBundle(List<String> ids) {
        this.ids = ids;
    }

    public static void encode(RequestDialogueBundle p, FriendlyByteBuf buf) {
        buf.writeCollection(p.ids, (b, s) -> b.writeUtf(s, 64));
    }

    public static RequestDialogueBundle decode(FriendlyByteBuf buf) {
        return new RequestDialogueBundle(buf.readCollection(ArrayList::new, b -> b.readUtf(64)));
    }

    public static void handle(RequestDialogueBundle p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            ctx.get().enqueueWork(() -> {
                List<DialogueBundlePacket.BundleEntry> out = new ArrayList<>();
                for (String id : p.ids) {
                    if (out.size() >= MAX_IDS) {
                        break;
                    }
                    DialogueGraph graph = DialogueManager.get().get(id);
                    if (graph != null) {
                        out.add(new DialogueBundlePacket.BundleEntry(id, graph.toJson().toString()));
                    }
                }
                if (!out.isEmpty()) {
                    NetworkHandler.sendToPlayer(new DialogueBundlePacket(out), sender);
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
