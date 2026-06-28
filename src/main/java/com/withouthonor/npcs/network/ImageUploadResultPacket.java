package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ImageUploadResultPacket {

    private final String name;

    public ImageUploadResultPacket(String name) {
        this.name = name;
    }

    public static void encode(ImageUploadResultPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.name, 80);
    }

    public static ImageUploadResultPacket decode(FriendlyByteBuf buf) {
        return new ImageUploadResultPacket(buf.readUtf(80));
    }

    public static void handle(ImageUploadResultPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> NetworkHandler.sendToServer(
                new RequestImageListPacket(p.name.startsWith(
                        com.withouthonor.npcs.common.storage.ImageStore.AVATAR_PREFIX))));
        ctx.get().setPacketHandled(true);
    }
}
