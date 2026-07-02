package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.GlobalScheduleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class GlobalSchedulePackets {

    public static final byte ACTION_TP = 0;
    public static final byte ACTION_DISABLE = 1;

    private GlobalSchedulePackets() {
    }

    public record Row(UUID uuid, String name, String dim, int x, int y, int z, boolean loaded) {
    }

    public static final class Request {
        public Request() {
        }

        public static void encode(Request packet, FriendlyByteBuf buf) {
        }

        public static Request decode(FriendlyByteBuf buf) {
            return new Request();
        }

        public static void handle(Request packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender != null && sender.hasPermissions(2)) {
                    sendList(sender);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class Data {
        private final List<Row> rows;

        public Data(List<Row> rows) {
            this.rows = rows;
        }

        public static void encode(Data packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.rows.size());
            for (Row r : packet.rows) {
                buf.writeUUID(r.uuid());
                buf.writeUtf(r.name(), 64);
                buf.writeUtf(r.dim(), 128);
                buf.writeVarInt(r.x());
                buf.writeVarInt(r.y());
                buf.writeVarInt(r.z());
                buf.writeBoolean(r.loaded());
            }
        }

        public static Data decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<Row> rows = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                rows.add(new Row(buf.readUUID(), buf.readUtf(64), buf.readUtf(128),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
            }
            return new Data(rows);
        }

        public static void handle(Data packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.withouthonor.npcs.client.ClientGlobalSchedule.set(packet.rows)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class Action {
        private final UUID uuid;
        private final byte action;

        public Action(UUID uuid, byte action) {
            this.uuid = uuid;
            this.action = action;
        }

        public static void encode(Action packet, FriendlyByteBuf buf) {
            buf.writeUUID(packet.uuid);
            buf.writeByte(packet.action);
        }

        public static Action decode(FriendlyByteBuf buf) {
            return new Action(buf.readUUID(), buf.readByte());
        }

        public static void handle(Action packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null || !sender.hasPermissions(2)) {
                    return;
                }
                MinecraftServer server = sender.server;
                GlobalScheduleManager mgr = GlobalScheduleManager.get(server);
                if (packet.action == ACTION_TP) {
                    GlobalScheduleManager.Info info = mgr.info(server, packet.uuid);
                    if (info != null) {
                        ServerLevel level = server.getLevel(info.dim());
                        if (level != null) {
                            BlockPos p = info.pos();
                            sender.teleportTo(level, p.getX() + 0.5, p.getY(), p.getZ() + 0.5,
                                    sender.getYRot(), sender.getXRot());
                        }
                    }
                } else if (packet.action == ACTION_DISABLE) {
                    mgr.disable(server, packet.uuid);
                }
                sendList(sender);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private static void sendList(ServerPlayer sender) {
        List<GlobalScheduleManager.Info> infos = GlobalScheduleManager.get(sender.server).list(sender.server);
        List<Row> rows = new ArrayList<>(infos.size());
        for (GlobalScheduleManager.Info i : infos) {
            rows.add(new Row(i.uuid(), i.name(), i.dim().location().toString(),
                    i.pos().getX(), i.pos().getY(), i.pos().getZ(), i.loaded()));
        }
        NetworkHandler.sendToPlayer(new Data(rows), sender);
    }
}
