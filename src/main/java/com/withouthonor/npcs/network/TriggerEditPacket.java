package com.withouthonor.npcs.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TriggerEditPacket {

    private final BlockPos pos;
    private final boolean once;
    private final String actionsJson;
    private final String conditionsJson;
    private final boolean viaHelper;
    private final byte enterDir;

    public TriggerEditPacket(BlockPos pos, boolean once, String actionsJson, String conditionsJson,
                             boolean viaHelper, byte enterDir) {
        this.pos = pos;
        this.once = once;
        this.actionsJson = actionsJson;
        this.conditionsJson = conditionsJson;
        this.viaHelper = viaHelper;
        this.enterDir = enterDir;
    }

    public static void encode(TriggerEditPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.once);
        buf.writeUtf(p.actionsJson, 32767);
        buf.writeUtf(p.conditionsJson, 32767);
        buf.writeBoolean(p.viaHelper);
        buf.writeByte(p.enterDir);
    }

    public static TriggerEditPacket decode(FriendlyByteBuf buf) {
        return new TriggerEditPacket(buf.readBlockPos(), buf.readBoolean(),
                buf.readUtf(32767), buf.readUtf(32767), buf.readBoolean(), buf.readByte());
    }

    public static void handle(TriggerEditPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.gui.editor.TriggerEditorScreen.open(
                        p.pos, p.once, p.actionsJson, p.conditionsJson, p.viaHelper, p.enterDir)));
        ctx.get().setPacketHandled(true);
    }
}
