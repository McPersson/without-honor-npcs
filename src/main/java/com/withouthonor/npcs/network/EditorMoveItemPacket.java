package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EditorMoveItemPacket {

    private final int from;
    private final int to;

    public EditorMoveItemPacket(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public static void encode(EditorMoveItemPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.from);
        buf.writeVarInt(packet.to);
    }

    public static EditorMoveItemPacket decode(FriendlyByteBuf buf) {
        return new EditorMoveItemPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(EditorMoveItemPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2)
                && packet.from >= 0 && packet.from < 36
                && packet.to >= 0 && packet.to < 36
                && packet.from != packet.to) {
            Inventory inventory = sender.getInventory();
            ItemStack moved = inventory.getItem(packet.from);
            inventory.setItem(packet.from, inventory.getItem(packet.to));
            inventory.setItem(packet.to, moved);
        }
        ctx.get().setPacketHandled(true);
    }
}
