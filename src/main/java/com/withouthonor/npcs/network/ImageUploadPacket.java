package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.ImageStore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ImageUploadPacket {

    private static final class Buf {
        boolean avatar;
        int total;
        int size;
        final List<byte[]> chunks = new ArrayList<>();
    }

    private static final Map<UUID, Buf> BUFFERS = new HashMap<>();

    public static void onLogout(UUID playerId) {
        BUFFERS.remove(playerId);
    }

    private final boolean avatar;
    private final int index;
    private final int total;
    private final byte[] chunk;

    public ImageUploadPacket(boolean avatar, int index, int total, byte[] chunk) {
        this.avatar = avatar;
        this.index = index;
        this.total = total;
        this.chunk = chunk;
    }

    public static void encode(ImageUploadPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.avatar);
        buf.writeVarInt(p.index);
        buf.writeVarInt(p.total);
        buf.writeByteArray(p.chunk);
    }

    public static ImageUploadPacket decode(FriendlyByteBuf buf) {
        return new ImageUploadPacket(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                buf.readByteArray(ImageStore.CHUNK_SIZE + 1024));
    }

    public static void handle(ImageUploadPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            ctx.get().enqueueWork(() -> apply(p, sender));
        }
        ctx.get().setPacketHandled(true);
    }

    private static void apply(ImageUploadPacket p, ServerPlayer sender) {
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
            buf.avatar = p.avatar;
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
        if (buf.size > ImageStore.MAX_BYTES) {
            BUFFERS.remove(id);
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.image.too_big")
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
        ImageStore.SaveResult res = ImageStore.get().saveUpload(all, buf.avatar);
        switch (res.status()) {
            case OK -> {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.image.uploaded", res.name()));
                NetworkHandler.sendToPlayer(new ImageUploadResultPacket(res.name()), sender);
            }
            case EXISTS -> {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.image.exists", res.name()));
                NetworkHandler.sendToPlayer(new ImageUploadResultPacket(res.name()), sender);
            }
            case TOO_BIG -> sender.sendSystemMessage(Component.translatable("wh_npcs.msg.image.too_big")
                    .withStyle(ChatFormatting.RED));
            case BAD_FORMAT -> sender.sendSystemMessage(Component.translatable("wh_npcs.msg.image.bad_format")
                    .withStyle(ChatFormatting.RED));
            default -> sender.sendSystemMessage(Component.translatable("wh_npcs.msg.image.error")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
