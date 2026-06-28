package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDialogueNodePacket {

    private final DialogueNodeData data;

    public OpenDialogueNodePacket(DialogueNodeData data) {
        this.data = data;
    }

    public static void encode(OpenDialogueNodePacket packet, FriendlyByteBuf buf) {
        packet.data.write(buf);
    }

    public static OpenDialogueNodePacket decode(FriendlyByteBuf buf) {
        return new OpenDialogueNodePacket(DialogueNodeData.read(buf));
    }

    public static void handle(OpenDialogueNodePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.gui.ClientDialogue.showNode(packet.data)));
        ctx.get().setPacketHandled(true);
    }
}
