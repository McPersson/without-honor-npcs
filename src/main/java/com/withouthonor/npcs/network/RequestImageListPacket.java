package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.ImageStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestImageListPacket {

    private final boolean avatars;

    public RequestImageListPacket() {
        this(false);
    }

    public RequestImageListPacket(boolean avatars) {
        this.avatars = avatars;
    }

    public static void encode(RequestImageListPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.avatars);
    }

    public static RequestImageListPacket decode(FriendlyByteBuf buf) {
        return new RequestImageListPacket(buf.readBoolean());
    }

    public static void handle(RequestImageListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            ImageStore store = ImageStore.get();
            NetworkHandler.sendToPlayer(new ImageListPacket(
                    packet.avatars ? store.listAvatars() : store.listDetailed()), sender);
        }
        ctx.get().setPacketHandled(true);
    }
}
