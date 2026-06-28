package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.item.NpcMoverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NpcMoverPacket {

    private final BlockPos pos;

    public NpcMoverPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(NpcMoverPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }

    public static NpcMoverPacket decode(FriendlyByteBuf buf) {
        return new NpcMoverPacket(buf.readBlockPos());
    }

    public static void handle(NpcMoverPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sp = ctx.get().getSender();
        if (sp != null && sp.getMainHandItem().getItem() instanceof NpcMoverItem) {
            NpcMoverItem.tryMove(sp, p.pos.above());
        }
        ctx.get().setPacketHandled(true);
    }
}
