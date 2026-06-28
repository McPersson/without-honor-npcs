package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.ImageStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RenameImagePacket {

    private final String name;
    private final String newName;
    private final boolean avatars;

    public RenameImagePacket(String name, String newName, boolean avatars) {
        this.name = name;
        this.newName = newName;
        this.avatars = avatars;
    }

    public static void encode(RenameImagePacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.name, 80);
        buf.writeUtf(p.newName, 80);
        buf.writeBoolean(p.avatars);
    }

    public static RenameImagePacket decode(FriendlyByteBuf buf) {
        return new RenameImagePacket(buf.readUtf(80), buf.readUtf(80), buf.readBoolean());
    }

    public static void handle(RenameImagePacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            ImageStore store = ImageStore.get();
            store.renameImage(p.name, p.newName);
            NetworkHandler.sendToPlayer(new ImageListPacket(
                    p.avatars ? store.listAvatars() : store.listDetailed()), sender);
        }
        ctx.get().setPacketHandled(true);
    }
}
