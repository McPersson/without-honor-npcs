package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.PlayerStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class RestockTradesPacket {

    private final UUID profileId;

    public RestockTradesPacket(UUID profileId) {
        this.profileId = profileId;
    }

    public static void encode(RestockTradesPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.profileId);
    }

    public static RestockTradesPacket decode(FriendlyByteBuf buf) {
        return new RestockTradesPacket(buf.readUUID());
    }

    public static void handle(RestockTradesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)) {
            PlayerStateManager.get(sender.server).resetTradesForAll(packet.profileId.toString());
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.npc.restocked")
                    .withStyle(ChatFormatting.GREEN));
        }
        ctx.get().setPacketHandled(true);
    }
}
