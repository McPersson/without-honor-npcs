package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EmotePacket {

    private final int entityId;
    private final int iconOrdinal;
    private final int durationTicks;

    public EmotePacket(int entityId, int iconOrdinal, int durationTicks) {
        this.entityId = entityId;
        this.iconOrdinal = iconOrdinal;
        this.durationTicks = durationTicks;
    }

    public static void encode(EmotePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeByte(packet.iconOrdinal);
        buf.writeVarInt(packet.durationTicks);
    }

    public static EmotePacket decode(FriendlyByteBuf buf) {
        return new EmotePacket(buf.readVarInt(), buf.readByte(), buf.readVarInt());
    }

    public static void handle(EmotePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.ClientEmotes.accept(
                        packet.entityId, packet.iconOrdinal, packet.durationTicks)));
        ctx.get().setPacketHandled(true);
    }
}
