package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: развернуть NPC лицом к игроку (виден всем — поворот делается на сервере). */
public class NpcFacePlayerPacket {

    private final int entityId;

    public NpcFacePlayerPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(NpcFacePlayerPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
    }

    public static NpcFacePlayerPacket decode(FriendlyByteBuf buf) {
        return new NpcFacePlayerPacket(buf.readVarInt());
    }

    public static void handle(NpcFacePlayerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(packet.entityId);
            if (!(entity instanceof CompanionEntity npc)
                    || npc.level() != player.level()
                    || npc.distanceToSqr(player) >= 32.0D * 32.0D) {
                return;
            }
            double dx = player.getX() - npc.getX();
            double dz = player.getZ() - npc.getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            npc.setYRot(yaw);
            npc.yRotO = yaw;
            npc.yBodyRot = yaw;
            npc.yBodyRotO = yaw;
            npc.setYHeadRot(yaw);
            npc.yHeadRotO = yaw;
            npc.getNavigation().stop();
        });
        context.setPacketHandled(true);
    }
}
