package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EditorGiveItemPacket {

    private final ItemStack stack;

    private final int slot;

    public EditorGiveItemPacket(ItemStack stack, int slot) {
        this.stack = stack;
        this.slot = slot;
    }

    public static void encode(EditorGiveItemPacket packet, FriendlyByteBuf buf) {
        buf.writeItem(packet.stack);
        buf.writeVarInt(packet.slot);
    }

    public static EditorGiveItemPacket decode(FriendlyByteBuf buf) {
        return new EditorGiveItemPacket(buf.readItem(), buf.readVarInt());
    }

    public static void handle(EditorGiveItemPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null && sender.hasPermissions(2) && !packet.stack.isEmpty()) {
            ItemStack stack = packet.stack.copy();

            if (packet.slot >= 0 && packet.slot < 36 && sender.getInventory().getItem(packet.slot).isEmpty()) {
                sender.getInventory().setItem(packet.slot, stack);
            } else if (!sender.getInventory().add(stack)) {
                sender.drop(stack, false);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
