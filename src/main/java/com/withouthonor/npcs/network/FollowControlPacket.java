package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FollowControlPacket {

    public static final byte STOP = 0;
    public static final byte GOODBYE = 1;

    private final int entityId;
    private final byte action;

    public FollowControlPacket(int entityId, byte action) {
        this.entityId = entityId;
        this.action = action;
    }

    public static void encode(FollowControlPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeByte(packet.action);
    }

    public static FollowControlPacket decode(FriendlyByteBuf buf) {
        return new FollowControlPacket(buf.readVarInt(), buf.readByte());
    }

    public static void handle(FollowControlPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (player.level().getEntity(packet.entityId) instanceof CompanionEntity npc
                    && npc.isFollowingPlayer(player)) {
                if (packet.action == GOODBYE) {
                    npc.sayGoodbye();
                } else {
                    npc.waitHere();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
