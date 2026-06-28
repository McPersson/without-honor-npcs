package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.skin.SkinService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestSkinPacket {

    private final String skinName;

    public RequestSkinPacket(String skinName) {
        this.skinName = skinName;
    }

    public static void encode(RequestSkinPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.skinName, 256);
    }

    public static RequestSkinPacket decode(FriendlyByteBuf buf) {
        return new RequestSkinPacket(buf.readUtf(256));
    }

    public static void handle(RequestSkinPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            String name = packet.skinName;
            SkinService.get().forget(name);
            SkinService.get().fetch(name).whenComplete((data, err) -> sender.server.execute(() -> {
                if (err != null || data == null) {

                    NetworkHandler.sendToPlayer(new SkinDataPacket(name, false, new byte[0]), sender);
                } else {
                    NetworkHandler.sendToPlayer(new SkinDataPacket(name, data.slim(), data.bytes()), sender);
                }
            }));
        }
        ctx.get().setPacketHandled(true);
    }
}
