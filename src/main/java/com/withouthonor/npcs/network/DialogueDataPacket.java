package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class DialogueDataPacket {

    private final String dialogueId;
    private final byte[] json;

    public DialogueDataPacket(String dialogueId, byte[] json) {
        this.dialogueId = dialogueId;
        this.json = json;
    }

    public static void encode(DialogueDataPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.dialogueId, 64);
        buf.writeByteArray(packet.json);
    }

    public static DialogueDataPacket decode(FriendlyByteBuf buf) {
        return new DialogueDataPacket(buf.readUtf(64), buf.readByteArray(EditorDataPacket.MAX_JSON_BYTES));
    }

    public static void handle(DialogueDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.withouthonor.npcs.client.gui.editor.DialogueEditorScreen.openFromJson(
                        new String(packet.json, StandardCharsets.UTF_8))));
        ctx.get().setPacketHandled(true);
    }
}
