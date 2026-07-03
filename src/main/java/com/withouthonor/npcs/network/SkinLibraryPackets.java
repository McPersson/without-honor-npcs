package com.withouthonor.npcs.network;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.skin.UrlSkinRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.withouthonor.npcs.common.skin.SkinService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class SkinLibraryPackets {

    private SkinLibraryPackets() {
    }

    public record FileEntry(String name, int sizeKb) {
    }

    private static List<FileEntry> listSkinFiles(MinecraftServer server) {
        List<FileEntry> files = new ArrayList<>();
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("skins");
        if (!Files.isDirectory(dir)) {
            return files;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".png") && !n.startsWith("url_"))
                    .sorted()
                    .forEach(n -> {
                        try {
                            files.add(new FileEntry(n, (int) (Files.size(dir.resolve(n)) / 1024)));
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to list skin files", e);
        }
        return files;
    }

    private static void sendLibrary(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new Library(
                listSkinFiles(player.server), UrlSkinRegistry.get().all()), player);
    }

    public static class Request {

        public static void encode(Request packet, FriendlyByteBuf buf) {
        }

        public static Request decode(FriendlyByteBuf buf) {
            return new Request();
        }

        public static void handle(Request packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                sendLibrary(sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static class Library {

        private final List<FileEntry> files;
        private final List<UrlSkinRegistry.UrlSkin> urls;

        public Library(List<FileEntry> files, List<UrlSkinRegistry.UrlSkin> urls) {
            this.files = files;
            this.urls = urls;
        }

        public static void encode(Library packet, FriendlyByteBuf buf) {
            buf.writeCollection(packet.files, (b, f) -> {
                b.writeUtf(f.name(), 80);
                b.writeVarInt(f.sizeKb());
            });
            buf.writeCollection(packet.urls, (b, u) -> {
                b.writeUtf(u.url(), 256);
                b.writeUtf(u.addedBy(), 32);
                b.writeUtf(u.name(), 48);
            });
        }

        public static Library decode(FriendlyByteBuf buf) {
            return new Library(
                    buf.readCollection(ArrayList::new, b -> new FileEntry(b.readUtf(80), b.readVarInt())),
                    buf.readCollection(ArrayList::new,
                            b -> new UrlSkinRegistry.UrlSkin(b.readUtf(256), b.readUtf(32), b.readUtf(48))));
        }

        public static void handle(Library packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.withouthonor.npcs.client.gui.editor.SkinLibraryScreen
                            .acceptLibrary(packet.files, packet.urls)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class Rename {

        private final String url;
        private final String name;

        public Rename(String url, String name) {
            this.url = url;
            this.name = name;
        }

        public static void encode(Rename packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.url, 256);
            buf.writeUtf(packet.name, 48);
        }

        public static Rename decode(FriendlyByteBuf buf) {
            return new Rename(buf.readUtf(256), buf.readUtf(48));
        }

        public static void handle(Rename packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                UrlSkinRegistry.get().rename(packet.url, packet.name);
                sendLibrary(sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static class Delete {

        private final boolean isFile;

        private final String value;

        public Delete(boolean isFile, String value) {
            this.isFile = isFile;
            this.value = value;
        }

        public static void encode(Delete packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.isFile);
            buf.writeUtf(packet.value, 256);
        }

        public static Delete decode(FriendlyByteBuf buf) {
            return new Delete(buf.readBoolean(), buf.readUtf(256));
        }

        public static void handle(Delete packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                if (packet.isFile) {
                    deleteFile(packet.value, sender);
                } else {
                    deleteUrl(packet.value, sender);
                }
                sendLibrary(sender);
            }
            ctx.get().setPacketHandled(true);
        }

        private static void deleteFile(String name, ServerPlayer sender) {
            if (!sender.hasPermissions(3)) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.delete_file_denied")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            if (!name.endsWith(".png") || name.startsWith("url_")
                    || name.contains("/") || name.contains("\\") || name.contains("..")) {
                return;
            }
            Path file = sender.server.getWorldPath(LevelResource.ROOT)
                    .resolve("wh_npcs").resolve("skins").resolve(name);
            try {
                if (Files.deleteIfExists(file)) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.file_deleted", name)
                            .withStyle(ChatFormatting.GREEN));
                }
            } catch (IOException e) {
                WHCompanions.LOGGER.warn("Failed to delete skin file {}", name, e);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.file_delete_err", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        }

        private static void deleteUrl(String url, ServerPlayer sender) {
            UrlSkinRegistry.UrlSkin existing = null;
            for (UrlSkinRegistry.UrlSkin skin : UrlSkinRegistry.get().all()) {
                if (skin.url().equalsIgnoreCase(url)) {
                    existing = skin;
                    break;
                }
            }
            if (existing == null) {
                return;
            }
            boolean own = existing.addedBy().equalsIgnoreCase(sender.getGameProfile().getName());
            if (!own && !sender.hasPermissions(3)) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.delete_url_denied", existing.addedBy())
                        .withStyle(ChatFormatting.RED));
                return;
            }
            UrlSkinRegistry.get().remove(url);
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.url_deleted")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    public static class AddUrl {

        private final String url;

        public AddUrl(String url) {
            this.url = url;
        }

        public static void encode(AddUrl packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.url, 256);
        }

        public static AddUrl decode(FriendlyByteBuf buf) {
            return new AddUrl(buf.readUtf(256));
        }

        public static void handle(AddUrl packet, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                String url = packet.url.trim();
                String lower = url.toLowerCase(java.util.Locale.ROOT);
                if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.not_url").withStyle(ChatFormatting.RED));
                } else {
                    if (!UrlSkinRegistry.get().add(url, sender.getGameProfile().getName())) {
                        sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.url_exists")
                                .withStyle(ChatFormatting.YELLOW));
                    }
                    sendLibrary(sender);
                }
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static class Upload {

        private static final class Buf {
            String name;
            int total;
            int size;
            final List<byte[]> chunks = new ArrayList<>();
        }

        private static final Map<UUID, Buf> BUFFERS = new HashMap<>();

        public static void onLogout(UUID playerId) {
            BUFFERS.remove(playerId);
        }

        private final String name;
        private final int index;
        private final int total;
        private final byte[] chunk;

        public Upload(String name, int index, int total, byte[] chunk) {
            this.name = name;
            this.index = index;
            this.total = total;
            this.chunk = chunk;
        }

        public static void encode(Upload p, FriendlyByteBuf buf) {
            buf.writeUtf(p.name, 80);
            buf.writeVarInt(p.index);
            buf.writeVarInt(p.total);
            buf.writeByteArray(p.chunk);
        }

        public static Upload decode(FriendlyByteBuf buf) {
            return new Upload(buf.readUtf(80), buf.readVarInt(), buf.readVarInt(),
                    buf.readByteArray(com.withouthonor.npcs.common.storage.ImageStore.CHUNK_SIZE + 1024));
        }

        public static void handle(Upload p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                ctx.get().enqueueWork(() -> apply(p, sender));
            }
            ctx.get().setPacketHandled(true);
        }

        private static void apply(Upload p, ServerPlayer sender) {
            UUID id = sender.getUUID();
            if (!com.withouthonor.npcs.common.config.WhConfig.allowClientImport()) {
                BUFFERS.remove(id);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.share.disabled")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            if (!sender.hasPermissions(2)) {
                BUFFERS.remove(id);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.share.no_permission")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            Buf buf;
            if (p.index == 0) {
                buf = new Buf();
                buf.name = p.name;
                buf.total = p.total;
                BUFFERS.put(id, buf);
            } else {
                buf = BUFFERS.get(id);
                if (buf == null || p.index != buf.chunks.size()) {
                    BUFFERS.remove(id);
                    return;
                }
            }
            buf.chunks.add(p.chunk);
            buf.size += p.chunk.length;
            if (buf.size > SkinService.MAX_SKIN_BYTES) {
                BUFFERS.remove(id);
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.too_big")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            if (buf.chunks.size() < buf.total) {
                return;
            }
            BUFFERS.remove(id);
            byte[] all = new byte[buf.size];
            int pos = 0;
            for (byte[] c : buf.chunks) {
                System.arraycopy(c, 0, all, pos, c.length);
                pos += c.length;
            }
            SkinService.SkinSaveResult res = SkinService.get().saveSkinFile(buf.name, all);
            switch (res.status()) {
                case OK -> {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.uploaded", res.name()));
                    sendLibrary(sender);
                }
                case REPLACED -> {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.replaced", res.name()));
                    sendLibrary(sender);
                }
                case TOO_BIG -> sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.too_big")
                        .withStyle(ChatFormatting.RED));
                case BAD_SIZE -> sender.sendSystemMessage(Component.translatable("wh_npcs.msg.skin.bad_size")
                        .withStyle(ChatFormatting.RED));
                case BAD_FORMAT -> sender.sendSystemMessage(Component.translatable("wh_npcs.msg.image.bad_format")
                        .withStyle(ChatFormatting.RED));
                default -> sender.sendSystemMessage(Component.translatable("wh_npcs.msg.image.error")
                        .withStyle(ChatFormatting.RED));
            }
        }
    }
}
