package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.ImageStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.function.Supplier;

public class RequestImagePacket {

    private final String name;

    public RequestImagePacket(String name) {
        this.name = name;
    }

    public static void encode(RequestImagePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.name, 80);
    }

    public static RequestImagePacket decode(FriendlyByteBuf buf) {
        return new RequestImagePacket(buf.readUtf(80));
    }

    public static void handle(RequestImagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            byte[] data = ImageStore.get().read(packet.name);
            if (data == null) {

                NetworkHandler.sendToPlayer(new ImageChunkPacket(packet.name, 0, 0, new byte[0]), sender);
            } else {
                int total = (data.length + ImageStore.CHUNK_SIZE - 1) / ImageStore.CHUNK_SIZE;
                for (int i = 0; i < total; i++) {
                    int from = i * ImageStore.CHUNK_SIZE;
                    int to = Math.min(data.length, from + ImageStore.CHUNK_SIZE);
                    NetworkHandler.sendToPlayer(new ImageChunkPacket(packet.name, i, total,
                            Arrays.copyOfRange(data, from, to)), sender);
                }
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
