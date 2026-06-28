package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.storage.ImageStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ImageListPacket {

    private final List<ImageStore.ImageInfo> images;

    public ImageListPacket(List<ImageStore.ImageInfo> images) {
        this.images = images;
    }

    public static void encode(ImageListPacket packet, FriendlyByteBuf buf) {
        buf.writeCollection(packet.images, (b, i) -> {
            b.writeUtf(i.name(), 80);
            b.writeVarInt(i.sizeKb());
            b.writeVarLong(i.mtime());
        });
    }

    public static ImageListPacket decode(FriendlyByteBuf buf) {
        return new ImageListPacket(buf.readCollection(ArrayList::new,
                b -> new ImageStore.ImageInfo(b.readUtf(80), b.readVarInt(), b.readVarLong())));
    }

    public static void handle(ImageListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.withouthonor.npcs.client.gui.editor.NodeImagesScreen.acceptServerList(packet.images);
            com.withouthonor.npcs.client.gui.editor.PortraitPickerScreen.acceptServerList(packet.images);
        }));
        ctx.get().setPacketHandled(true);
    }
}
