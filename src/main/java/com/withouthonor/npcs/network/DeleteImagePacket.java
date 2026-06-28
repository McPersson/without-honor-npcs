package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.ImageStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteImagePacket {

    private final String name;
    private final boolean avatars;

    public DeleteImagePacket(String name, boolean avatars) {
        this.name = name;
        this.avatars = avatars;
    }

    public static void encode(DeleteImagePacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.name, 80);
        buf.writeBoolean(p.avatars);
    }

    public static DeleteImagePacket decode(FriendlyByteBuf buf) {
        return new DeleteImagePacket(buf.readUtf(80), buf.readBoolean());
    }

    public static void handle(DeleteImagePacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            ImageStore store = ImageStore.get();
            store.deleteImage(p.name);
            NetworkHandler.sendToPlayer(new ImageListPacket(
                    p.avatars ? store.listAvatars() : store.listDetailed()), sender);
        }
        ctx.get().setPacketHandled(true);
    }
}
