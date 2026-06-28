package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.ImageStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ImageChunkPacket {

    private final String name;
    private final int index;
    private final int total;
    private final byte[] data;

    public ImageChunkPacket(String name, int index, int total, byte[] data) {
        this.name = name;
        this.index = index;
        this.total = total;
        this.data = data;
    }

    public static void encode(ImageChunkPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.name, 80);
        buf.writeVarInt(packet.index);
        buf.writeVarInt(packet.total);
        buf.writeByteArray(packet.data);
    }

    public static ImageChunkPacket decode(FriendlyByteBuf buf) {
        return new ImageChunkPacket(buf.readUtf(80), buf.readVarInt(), buf.readVarInt(),
                buf.readByteArray(ImageStore.CHUNK_SIZE + 1024));
    }

    public static void handle(ImageChunkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.cache.ClientImageCache.getInstance()
                        .receiveChunk(packet.name, packet.index, packet.total, packet.data)));
        ctx.get().setPacketHandled(true);
    }
}
