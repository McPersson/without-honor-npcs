package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import com.withouthonor.npcs.common.dialogue.EntryPoint;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.storage.DialogueManager;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EditorDataPacket {

    public static final int MAX_JSON_BYTES = 262144;

    private static final int MAX_USED_BY = 5;

    public record DialogueSummary(String id, String author, int nodes, List<String> usedBy) {

        public static void write(FriendlyByteBuf buf, DialogueSummary summary) {
            buf.writeUtf(summary.id(), 64);
            buf.writeUtf(summary.author(), 32);
            buf.writeVarInt(summary.nodes());
            buf.writeCollection(summary.usedBy(), FriendlyByteBuf::writeUtf);
        }

        public static DialogueSummary read(FriendlyByteBuf buf) {
            return new DialogueSummary(buf.readUtf(64), buf.readUtf(32), buf.readVarInt(),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf));
        }
    }

    public record FactionInfo(String id, String name, int color) {
    }

    private final byte[] profileJson;
    private final List<DialogueSummary> dialogues;

    private final int entityId;

    private final List<FactionInfo> factions;

    private final int siblingCount;

    public EditorDataPacket(byte[] profileJson, List<DialogueSummary> dialogues, int entityId,
                            List<FactionInfo> factions,
                            int siblingCount) {
        this.profileJson = profileJson;
        this.dialogues = dialogues;
        this.entityId = entityId;
        this.factions = factions;
        this.siblingCount = siblingCount;
    }

    public static List<DialogueSummary> buildSummaries(net.minecraft.server.MinecraftServer server) {
        // «Использовано» считаем только по NPC, реально стоящим в мире (индекс), а не по всем профилям
        java.util.Set<java.util.UUID> worldProfileIds = new java.util.HashSet<>();
        if (server != null) {
            for (var e : com.withouthonor.npcs.common.storage.CompanionIndex.get(server).all()) {
                if (e.profileId() != null) {
                    worldProfileIds.add(e.profileId());
                }
            }
        }
        List<DialogueSummary> summaries = new ArrayList<>();
        for (String id : DialogueManager.get().ids().stream().sorted().toList()) {
            DialogueGraph graph = DialogueManager.get().get(id);
            if (graph == null) {
                continue;
            }
            List<String> usedBy = new ArrayList<>();
            int extra = 0;
            for (CompanionProfile p : ProfileManager.get().all()) {
                if (!worldProfileIds.contains(p.getId())) {
                    continue;
                }
                boolean uses = false;
                for (EntryPoint entry : p.getEntryPoints()) {
                    if (id.equals(entry.getDialogueId())) {
                        uses = true;
                        break;
                    }
                }
                if (uses) {
                    if (usedBy.size() < MAX_USED_BY) {
                        usedBy.add(p.getName());
                    } else {
                        extra++;
                    }
                }
            }
            if (extra > 0) {
                usedBy.add("+" + extra + " more");
            }
            summaries.add(new DialogueSummary(id, graph.getAuthor(), graph.getNodes().size(), usedBy));
        }
        return summaries;
    }

    public static void send(ServerPlayer player, CompanionProfile profile, int entityId) {
        DialogueManager.get().loadMissing();
        byte[] json = profile.toJson().toString().getBytes(StandardCharsets.UTF_8);
        List<DialogueSummary> summaries = buildSummaries(player.server);
        List<FactionInfo> factions = new ArrayList<>();
        for (var faction : com.withouthonor.npcs.common.reputation.FactionRegistry.get().all()) {
            factions.add(new FactionInfo(faction.getId(), faction.getName(), faction.getColor()));
        }

        int total = 0;
        if (player.server != null) {
            for (var e : com.withouthonor.npcs.common.storage.CompanionIndex.get(player.server).all()) {
                if (profile.getId().equals(e.profileId())) {
                    total++;
                }
            }
        }
        int siblingCount = Math.max(0, total - 1);
        NetworkHandler.sendToPlayer(
                new EditorDataPacket(json, summaries, entityId, factions, siblingCount), player);
    }

    public static void encode(EditorDataPacket packet, FriendlyByteBuf buf) {
        buf.writeByteArray(packet.profileJson);
        buf.writeCollection(packet.dialogues, DialogueSummary::write);
        buf.writeVarInt(packet.entityId);
        buf.writeCollection(packet.factions, (b, f) -> {
            b.writeUtf(f.id(), 32);
            b.writeUtf(f.name(), 48);
            b.writeInt(f.color());
        });
        buf.writeVarInt(packet.siblingCount);
    }

    public static EditorDataPacket decode(FriendlyByteBuf buf) {
        return new EditorDataPacket(
                buf.readByteArray(MAX_JSON_BYTES),
                buf.readCollection(ArrayList::new, DialogueSummary::read),
                buf.readVarInt(),
                buf.readCollection(ArrayList::new, b -> new FactionInfo(
                        b.readUtf(32), b.readUtf(48), b.readInt())),
                buf.readVarInt());
    }

    public static void handle(EditorDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                    List<com.withouthonor.npcs.client.ClientFactions.Info> infos = new ArrayList<>();
                    for (FactionInfo f : packet.factions) {
                        infos.add(new com.withouthonor.npcs.client.ClientFactions.Info(f.id(), f.name(), f.color()));
                    }
                    com.withouthonor.npcs.client.ClientFactions.set(infos);
                    com.withouthonor.npcs.client.gui.editor.NpcEditorScreen.open(
                            new String(packet.profileJson, StandardCharsets.UTF_8), packet.dialogues,
                            packet.entityId, packet.siblingCount);
                }));
        ctx.get().setPacketHandled(true);
    }
}
