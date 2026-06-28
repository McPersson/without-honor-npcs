package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopMusicPacket {

    public static void encode(StopMusicPacket packet, FriendlyByteBuf buf) {
    }

    public static StopMusicPacket decode(FriendlyByteBuf buf) {
        return new StopMusicPacket();
    }

    public static void handle(StopMusicPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.audio.ClientNpcAudio.fadeMusic()));
        ctx.get().setPacketHandled(true);
    }
}
