package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class IndicatorPacket {

    private final int entityId;
    private final int indicator;

    public IndicatorPacket(int entityId, int indicator) {
        this.entityId = entityId;
        this.indicator = indicator;
    }

    public static void encode(IndicatorPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeByte(packet.indicator);
    }

    public static IndicatorPacket decode(FriendlyByteBuf buf) {
        return new IndicatorPacket(buf.readVarInt(), buf.readByte());
    }

    public static void handle(IndicatorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.ClientIndicators.accept(packet.entityId, packet.indicator)));
        ctx.get().setPacketHandled(true);
    }
}
