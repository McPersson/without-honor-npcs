package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NpcAdminActionPacket {

    public static final byte HEAL = 0;
    public static final byte CLEAR_EFFECTS = 1;

    private final int entityId;
    private final byte action;

    public NpcAdminActionPacket(int entityId, byte action) {
        this.entityId = entityId;
        this.action = action;
    }

    public static void encode(NpcAdminActionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeByte(packet.action);
    }

    public static NpcAdminActionPacket decode(FriendlyByteBuf buf) {
        return new NpcAdminActionPacket(buf.readVarInt(), buf.readByte());
    }

    public static void handle(NpcAdminActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }
            if (!(player.level().getEntity(packet.entityId) instanceof CompanionEntity npc)) {
                return;
            }
            if (packet.action == HEAL) {
                npc.setHealth(npc.getMaxHealth());
                npc.refreshTotem();
                player.displayClientMessage(
                        Component.translatable("wh_npcs.msg.npc_healed", npc.getName()), true);
            } else if (packet.action == CLEAR_EFFECTS) {
                npc.removeAllEffects();
                player.displayClientMessage(
                        Component.translatable("wh_npcs.msg.npc_cleared", npc.getName()), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
