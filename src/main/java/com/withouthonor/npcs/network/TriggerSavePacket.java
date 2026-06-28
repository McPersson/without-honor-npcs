package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.block.TriggerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TriggerSavePacket {

    private final BlockPos pos;
    private final boolean once;
    private final String actionsJson;
    private final String conditionsJson;
    private final byte enterDir;

    public TriggerSavePacket(BlockPos pos, boolean once, String actionsJson, String conditionsJson,
                             byte enterDir) {
        this.pos = pos;
        this.once = once;
        this.actionsJson = actionsJson;
        this.conditionsJson = conditionsJson;
        this.enterDir = enterDir;
    }

    public static void encode(TriggerSavePacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.once);
        buf.writeUtf(p.actionsJson, 32767);
        buf.writeUtf(p.conditionsJson, 32767);
        buf.writeByte(p.enterDir);
    }

    public static TriggerSavePacket decode(FriendlyByteBuf buf) {
        return new TriggerSavePacket(buf.readBlockPos(), buf.readBoolean(),
                buf.readUtf(32767), buf.readUtf(32767), buf.readByte());
    }

    public static void handle(TriggerSavePacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null || !sp.hasPermissions(2)) {
                return;
            }
            if (sp.level().getBlockEntity(p.pos) instanceof TriggerBlockEntity be) {
                be.apply(p.actionsJson, p.conditionsJson, p.once, p.enterDir);
            }
        });
        c.setPacketHandled(true);
    }
}
