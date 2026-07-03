package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.CompanionIndex;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Общий список NPC сервера для клиентских пикеров (выбор привязанного NPC и т.п.).
 */
public final class NpcListPackets {

    private NpcListPackets() {
    }

    /** dist = -1, если NPC в другом измерении. */
    public record NpcEntry(UUID uuid, String name, int dist, boolean loaded) {

        public static void write(NpcEntry e, FriendlyByteBuf buf) {
            buf.writeUUID(e.uuid());
            buf.writeUtf(e.name(), 48);
            buf.writeVarInt(e.dist());
            buf.writeBoolean(e.loaded());
        }

        public static NpcEntry read(FriendlyByteBuf buf) {
            return new NpcEntry(buf.readUUID(), buf.readUtf(48), buf.readVarInt(), buf.readBoolean());
        }
    }

    public static final class Request {

        public Request() {
        }

        public static void encode(Request p, FriendlyByteBuf buf) {
        }

        public static Request decode(FriendlyByteBuf buf) {
            return new Request();
        }

        public static void handle(Request p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer s = ctx.get().getSender();
            if (s != null && s.hasPermissions(2)) {
                List<NpcEntry> out = new ArrayList<>();
                for (CompanionIndex.Entry e : CompanionIndex.get(s.server).all()) {
                    boolean sameDim = e.dimension().equals(s.level().dimension());
                    int dist = sameDim ? (int) Math.sqrt(e.pos().distToCenterSqr(s.position())) : -1;
                    boolean loaded = sameDim && s.serverLevel().getEntity(e.id()) != null;
                    String name = e.name().length() > 48 ? e.name().substring(0, 48) : e.name();
                    out.add(new NpcEntry(e.id(), name, dist, loaded));
                }
                // Сначала своё измерение по близости, чужие измерения — в конец.
                out.sort(Comparator.comparingInt((NpcEntry e) -> e.dist() < 0 ? Integer.MAX_VALUE : e.dist()));
                if (out.size() > 64) {
                    out = new ArrayList<>(out.subList(0, 64));
                }
                NetworkHandler.sendToPlayer(new Result(out), s);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class Result {

        private final List<NpcEntry> entries;

        public Result(List<NpcEntry> entries) {
            this.entries = entries;
        }

        public static void encode(Result p, FriendlyByteBuf buf) {
            buf.writeCollection(p.entries, (b, e) -> NpcEntry.write(e, b));
        }

        public static Result decode(FriendlyByteBuf buf) {
            return new Result(buf.readCollection(ArrayList::new, NpcEntry::read));
        }

        public static void handle(Result p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.withouthonor.npcs.client.ClientNetHandlers.openNpcPick(p.entries)));
            ctx.get().setPacketHandled(true);
        }
    }
}
