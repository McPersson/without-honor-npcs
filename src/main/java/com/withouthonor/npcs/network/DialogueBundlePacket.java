package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DialogueBundlePacket {

    public record BundleEntry(String id, String json) {

        public static void write(FriendlyByteBuf buf, BundleEntry e) {
            buf.writeUtf(e.id(), 64);
            buf.writeByteArray(e.json().getBytes(StandardCharsets.UTF_8));
        }

        public static BundleEntry read(FriendlyByteBuf buf) {
            return new BundleEntry(buf.readUtf(64),
                    new String(buf.readByteArray(EditorDataPacket.MAX_JSON_BYTES), StandardCharsets.UTF_8));
        }
    }

    private final List<BundleEntry> entries;

    public DialogueBundlePacket(List<BundleEntry> entries) {
        this.entries = entries;
    }

    public static void encode(DialogueBundlePacket p, FriendlyByteBuf buf) {
        buf.writeCollection(p.entries, BundleEntry::write);
    }

    public static DialogueBundlePacket decode(FriendlyByteBuf buf) {
        return new DialogueBundlePacket(buf.readCollection(ArrayList::new, BundleEntry::read));
    }

    public static void handle(DialogueBundlePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.withouthonor.npcs.client.ClientLocalFiles.writeDialogues(p.entries)));
        ctx.get().setPacketHandled(true);
    }
}
