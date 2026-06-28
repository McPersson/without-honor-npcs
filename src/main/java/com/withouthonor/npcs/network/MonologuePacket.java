package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.dialogue.action.MonologueLine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MonologuePacket {

    private final List<MonologueLine> lines;
    private final boolean lockControl;

    public MonologuePacket(List<MonologueLine> lines, boolean lockControl) {
        this.lines = lines;
        this.lockControl = lockControl;
    }

    public static void encode(MonologuePacket packet, FriendlyByteBuf buf) {
        buf.writeCollection(packet.lines, (b, l) -> {
            b.writeUtf(l.name(), 128);
            b.writeUtf(l.portrait(), 64);
            b.writeUtf(l.text(), 1024);
        });
        buf.writeBoolean(packet.lockControl);
    }

    public static MonologuePacket decode(FriendlyByteBuf buf) {
        List<MonologueLine> lines = buf.readCollection(ArrayList::new,
                b -> new MonologueLine(b.readUtf(128), b.readUtf(64), b.readUtf(1024)));
        return new MonologuePacket(lines, buf.readBoolean());
    }

    public static void handle(MonologuePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (packet.lockControl) {
                com.withouthonor.npcs.client.gui.MonologueScreen.show(packet.lines);
            } else {
                com.withouthonor.npcs.client.ClientMonologue.start(packet.lines);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
