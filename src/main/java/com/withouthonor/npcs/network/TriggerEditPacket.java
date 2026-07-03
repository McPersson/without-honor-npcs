package com.withouthonor.npcs.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class TriggerEditPacket {

    private final BlockPos pos;
    private final boolean once;
    private final String actionsJson;
    private final String conditionsJson;
    private final boolean viaHelper;
    private final byte enterDir;
    @Nullable
    private final UUID targetNpc;
    private final String targetNpcName;

    public TriggerEditPacket(BlockPos pos, boolean once, String actionsJson, String conditionsJson,
                             boolean viaHelper, byte enterDir,
                             @Nullable UUID targetNpc, String targetNpcName) {
        this.pos = pos;
        this.once = once;
        this.actionsJson = actionsJson;
        this.conditionsJson = conditionsJson;
        this.viaHelper = viaHelper;
        this.enterDir = enterDir;
        this.targetNpc = targetNpc;
        this.targetNpcName = targetNpcName;
    }

    public static void encode(TriggerEditPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.once);
        buf.writeUtf(p.actionsJson, 32767);
        buf.writeUtf(p.conditionsJson, 32767);
        buf.writeBoolean(p.viaHelper);
        buf.writeByte(p.enterDir);
        buf.writeBoolean(p.targetNpc != null);
        if (p.targetNpc != null) {
            buf.writeUUID(p.targetNpc);
        }
        buf.writeUtf(p.targetNpcName, 64);
    }

    public static TriggerEditPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean once = buf.readBoolean();
        String actionsJson = buf.readUtf(32767);
        String conditionsJson = buf.readUtf(32767);
        boolean viaHelper = buf.readBoolean();
        byte enterDir = buf.readByte();
        UUID targetNpc = buf.readBoolean() ? buf.readUUID() : null;
        String targetNpcName = buf.readUtf(64);
        return new TriggerEditPacket(pos, once, actionsJson, conditionsJson,
                viaHelper, enterDir, targetNpc, targetNpcName);
    }

    public static void handle(TriggerEditPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.gui.editor.TriggerEditorScreen.open(
                        p.pos, p.once, p.actionsJson, p.conditionsJson, p.viaHelper, p.enterDir,
                        p.targetNpc, p.targetNpcName)));
        ctx.get().setPacketHandled(true);
    }
}
