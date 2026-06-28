package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class DeleteCompanionPacket {

    private final UUID npcUuid;

    public DeleteCompanionPacket(UUID npcUuid) {
        this.npcUuid = npcUuid;
    }

    public static void encode(DeleteCompanionPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.npcUuid);
    }

    public static DeleteCompanionPacket decode(FriendlyByteBuf buf) {
        return new DeleteCompanionPacket(buf.readUUID());
    }

    public static void handle(DeleteCompanionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(packet.npcUuid);
            if (entity instanceof CompanionEntity npc) {
                String name = npc.getName().getString();
                npc.discard();
                player.sendSystemMessage(Component.translatable("wh_npcs.msg.npc.deleted", name));
            }
        });
        context.setPacketHandled(true);
    }
}
