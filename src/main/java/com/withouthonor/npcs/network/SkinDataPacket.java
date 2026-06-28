package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SkinDataPacket {

    private final String skinName;
    private final boolean slim;
    private final byte[] data;

    public SkinDataPacket(String skinName, boolean slim, byte[] data) {
        this.skinName = skinName;
        this.slim = slim;
        this.data = data;
    }

    public static void encode(SkinDataPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.skinName, 256);
        buf.writeBoolean(packet.slim);
        buf.writeByteArray(packet.data);
    }

    public static SkinDataPacket decode(FriendlyByteBuf buf) {
        return new SkinDataPacket(buf.readUtf(256), buf.readBoolean(), buf.readByteArray(262144));
    }

    public static void handle(SkinDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.cache.ClientSkinCache.getInstance()
                        .accept(packet.skinName, packet.slim, packet.data)));
        ctx.get().setPacketHandled(true);
    }
}
