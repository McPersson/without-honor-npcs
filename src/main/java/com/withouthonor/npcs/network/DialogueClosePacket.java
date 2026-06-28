package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DialogueClosePacket {

    public DialogueClosePacket() {
    }

    public static void encode(DialogueClosePacket packet, FriendlyByteBuf buf) {
    }

    public static DialogueClosePacket decode(FriendlyByteBuf buf) {
        return new DialogueClosePacket();
    }

    public static void handle(DialogueClosePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.gui.ClientDialogue.closeScreen()));
        ctx.get().setPacketHandled(true);
    }
}
