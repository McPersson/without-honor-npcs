package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.PlayerStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class FlagPackets {

    private FlagPackets() {
    }

    public record PlayerRef(String uuid, String name, int count) {
    }

    public record FlagRef(String name, String desc, String source, long time) {
    }

    private static String resolveName(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        if (server.getProfileCache() != null) {
            return server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName).orElse(id.toString());
        }
        return id.toString();
    }

    static void sendFlags(ServerPlayer s, String uuidStr) {
        UUID id;
        try {
            id = UUID.fromString(uuidStr);
        } catch (Exception e) {
            return;
        }
        PlayerStateManager states = PlayerStateManager.get(s.server);
        List<FlagRef> out = new ArrayList<>();
        for (PlayerStateManager.FlagInfo fi : states.flagInfosOf(id)) {
            out.add(new FlagRef(fi.name(), states.getFlagDescription(fi.name()), fi.source(), fi.time()));
        }
        NetworkHandler.sendToPlayer(new FlagsResult(uuidStr, out), s);
    }

    public static final class RequestPlayers {

        public RequestPlayers() {
        }

        public static void encode(RequestPlayers p, FriendlyByteBuf buf) {
        }

        public static RequestPlayers decode(FriendlyByteBuf buf) {
            return new RequestPlayers();
        }

        public static void handle(RequestPlayers p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer s = ctx.get().getSender();
            if (s != null && s.hasPermissions(2)) {
                PlayerStateManager states = PlayerStateManager.get(s.server);
                Map<UUID, Integer> counts = states.playersWithFlags();
                java.util.LinkedHashSet<UUID> ids = new java.util.LinkedHashSet<>();
                for (ServerPlayer sp : s.server.getPlayerList().getPlayers()) {
                    ids.add(sp.getUUID());
                }
                ids.addAll(playerDataUuids(s.server));
                ids.addAll(counts.keySet());
                List<PlayerRef> out = new ArrayList<>();
                for (UUID id : ids) {
                    out.add(new PlayerRef(id.toString(), resolveName(s.server, id), counts.getOrDefault(id, 0)));
                }
                out.sort(Comparator.comparing(PlayerRef::name, String.CASE_INSENSITIVE_ORDER));
                NetworkHandler.sendToPlayer(new PlayersResult(out), s);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    private static List<UUID> playerDataUuids(MinecraftServer server) {
        List<UUID> out = new ArrayList<>();
        try {
            java.nio.file.Path dir = server.getWorldPath(
                    net.minecraft.world.level.storage.LevelResource.ROOT).resolve("playerdata");
            if (java.nio.file.Files.isDirectory(dir)) {
                try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
                    files.filter(f -> f.getFileName().toString().endsWith(".dat")).forEach(f -> {
                        String n = f.getFileName().toString();
                        try {
                            out.add(UUID.fromString(n.substring(0, n.length() - 4)));
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static final class PlayersResult {

        private final List<PlayerRef> players;

        public PlayersResult(List<PlayerRef> players) {
            this.players = players;
        }

        public static void encode(PlayersResult p, FriendlyByteBuf buf) {
            buf.writeCollection(p.players, (b, r) -> {
                b.writeUtf(r.uuid(), 64);
                b.writeUtf(r.name(), 64);
                b.writeVarInt(r.count());
            });
        }

        public static PlayersResult decode(FriendlyByteBuf buf) {
            return new PlayersResult(buf.readCollection(ArrayList::new,
                    b -> new PlayerRef(b.readUtf(64), b.readUtf(64), b.readVarInt())));
        }

        public static void handle(PlayersResult p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.withouthonor.npcs.client.gui.editor.FlagsScreen.acceptPlayers(p.players)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class RequestFlags {

        private final String uuid;

        public RequestFlags(String uuid) {
            this.uuid = uuid;
        }

        public static void encode(RequestFlags p, FriendlyByteBuf buf) {
            buf.writeUtf(p.uuid, 64);
        }

        public static RequestFlags decode(FriendlyByteBuf buf) {
            return new RequestFlags(buf.readUtf(64));
        }

        public static void handle(RequestFlags p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer s = ctx.get().getSender();
            if (s != null && s.hasPermissions(2)) {
                sendFlags(s, p.uuid);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class FlagsResult {

        private final String uuid;
        private final List<FlagRef> flags;

        public FlagsResult(String uuid, List<FlagRef> flags) {
            this.uuid = uuid;
            this.flags = flags;
        }

        public static void encode(FlagsResult p, FriendlyByteBuf buf) {
            buf.writeUtf(p.uuid, 64);
            buf.writeCollection(p.flags, (b, r) -> {
                b.writeUtf(r.name(), 96);
                b.writeUtf(r.desc(), 256);
                b.writeUtf(r.source(), 64);
                b.writeVarLong(r.time());
            });
        }

        public static FlagsResult decode(FriendlyByteBuf buf) {
            String uuid = buf.readUtf(64);
            return new FlagsResult(uuid, buf.readCollection(ArrayList::new,
                    b -> new FlagRef(b.readUtf(96), b.readUtf(256), b.readUtf(64), b.readVarLong())));
        }

        public static void handle(FlagsResult p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.withouthonor.npcs.client.gui.editor.FlagsScreen.acceptFlags(p.uuid, p.flags)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class SetDescription {

        private final String flag;
        private final String desc;

        public SetDescription(String flag, String desc) {
            this.flag = flag;
            this.desc = desc;
        }

        public static void encode(SetDescription p, FriendlyByteBuf buf) {
            buf.writeUtf(p.flag, 96);
            buf.writeUtf(p.desc, 256);
        }

        public static SetDescription decode(FriendlyByteBuf buf) {
            return new SetDescription(buf.readUtf(96), buf.readUtf(256));
        }

        public static void handle(SetDescription p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer s = ctx.get().getSender();
            if (s != null && s.hasPermissions(2)) {
                PlayerStateManager.get(s.server).setFlagDescription(p.flag, p.desc);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class AddFlag {

        private final String uuid;
        private final String flag;

        public AddFlag(String uuid, String flag) {
            this.uuid = uuid;
            this.flag = flag;
        }

        public static void encode(AddFlag p, FriendlyByteBuf buf) {
            buf.writeUtf(p.uuid, 64);
            buf.writeUtf(p.flag, 96);
        }

        public static AddFlag decode(FriendlyByteBuf buf) {
            return new AddFlag(buf.readUtf(64), buf.readUtf(96));
        }

        public static void handle(AddFlag p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer s = ctx.get().getSender();
            if (s != null && s.hasPermissions(2)) {
                String flag = p.flag == null ? "" : p.flag.trim();
                if (!flag.isEmpty()) {
                    try {
                        PlayerStateManager.get(s.server).setFlag(UUID.fromString(p.uuid), flag, true,
                                s.getGameProfile().getName());
                    } catch (Exception ignored) {
                    }
                }
                sendFlags(s, p.uuid);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class RemoveFlag {

        private final String uuid;
        private final String flag;

        public RemoveFlag(String uuid, String flag) {
            this.uuid = uuid;
            this.flag = flag;
        }

        public static void encode(RemoveFlag p, FriendlyByteBuf buf) {
            buf.writeUtf(p.uuid, 64);
            buf.writeUtf(p.flag, 96);
        }

        public static RemoveFlag decode(FriendlyByteBuf buf) {
            return new RemoveFlag(buf.readUtf(64), buf.readUtf(96));
        }

        public static void handle(RemoveFlag p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer s = ctx.get().getSender();
            if (s != null && s.hasPermissions(2)) {
                try {
                    PlayerStateManager.get(s.server).removeFlag(UUID.fromString(p.uuid), p.flag);
                } catch (Exception ignored) {
                }
                sendFlags(s, p.uuid);
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
