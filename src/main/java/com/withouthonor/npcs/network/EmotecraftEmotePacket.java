package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EmotecraftEmotePacket {

    private final int entityId;
    private final String emoteId;

    public EmotecraftEmotePacket(int entityId, String emoteId) {
        this.entityId = entityId;
        this.emoteId = emoteId == null ? "" : emoteId;
    }

    public static void encode(EmotecraftEmotePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeUtf(packet.emoteId);
    }

    public static EmotecraftEmotePacket decode(FriendlyByteBuf buf) {
        return new EmotecraftEmotePacket(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(EmotecraftEmotePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.ClientEmoteAnim.accept(packet.entityId, packet.emoteId)));
        ctx.get().setPacketHandled(true);
    }
}
