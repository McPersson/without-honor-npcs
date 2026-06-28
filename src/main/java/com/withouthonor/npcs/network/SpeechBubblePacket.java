package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpeechBubblePacket {

    private final int entityId;
    private final String text;
    private final int durationTicks;

    public SpeechBubblePacket(int entityId, String text, int durationTicks) {
        this.entityId = entityId;
        this.text = text;
        this.durationTicks = durationTicks;
    }

    public static void encode(SpeechBubblePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeUtf(packet.text, 256);
        buf.writeVarInt(packet.durationTicks);
    }

    public static SpeechBubblePacket decode(FriendlyByteBuf buf) {
        return new SpeechBubblePacket(buf.readVarInt(), buf.readUtf(256), buf.readVarInt());
    }

    public static void handle(SpeechBubblePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.ClientBubbles.accept(
                        packet.entityId, packet.text, packet.durationTicks)));
        ctx.get().setPacketHandled(true);
    }
}
