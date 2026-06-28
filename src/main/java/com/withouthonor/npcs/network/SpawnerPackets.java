package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.block.SpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SpawnerPackets {

    private SpawnerPackets() {
    }

    private static Path exportsDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("exports");
    }

    private static List<String> listFiles(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        Path dir = exportsDir(server);
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().forEach(p -> {
                String fn = p.getFileName().toString();
                out.add(fn.substring(0, fn.length() - 5));
            });
        } catch (IOException ignored) {

        }
        return out;
    }

    public static void openEditor(ServerPlayer sp, BlockPos pos) {
        if (sp.serverLevel().getBlockEntity(pos) instanceof SpawnerBlockEntity be) {
            NetworkHandler.sendToPlayer(new OpenEditor(pos, be.toConfig(), listFiles(sp.server)), sp);
        }
    }

    public static final class OpenEditor {

        private final BlockPos pos;
        private final SpawnerBlockEntity.Config config;
        private final List<String> available;

        public OpenEditor(BlockPos pos, SpawnerBlockEntity.Config config, List<String> available) {
            this.pos = pos;
            this.config = config;
            this.available = available;
        }

        public static void encode(OpenEditor p, FriendlyByteBuf buf) {
            buf.writeBlockPos(p.pos);
            SpawnerBlockEntity.Config.write(buf, p.config);
            buf.writeCollection(p.available, (b, s) -> b.writeUtf(s, 80));
        }

        public static OpenEditor decode(FriendlyByteBuf buf) {
            return new OpenEditor(buf.readBlockPos(), SpawnerBlockEntity.Config.read(buf),
                    buf.readCollection(ArrayList::new, b -> b.readUtf(80)));
        }

        public static void handle(OpenEditor p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                            com.withouthonor.npcs.client.ClientNetHandlers.openSpawner(
                                    p.pos, p.config, p.available)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class Save {

        private final BlockPos pos;
        private final SpawnerBlockEntity.Config config;

        public Save(BlockPos pos, SpawnerBlockEntity.Config config) {
            this.pos = pos;
            this.config = config;
        }

        public static void encode(Save p, FriendlyByteBuf buf) {
            buf.writeBlockPos(p.pos);
            SpawnerBlockEntity.Config.write(buf, p.config);
        }

        public static Save decode(FriendlyByteBuf buf) {
            return new Save(buf.readBlockPos(), SpawnerBlockEntity.Config.read(buf));
        }

        public static void handle(Save p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)
                    && sender.serverLevel().getBlockEntity(p.pos) instanceof SpawnerBlockEntity be) {
                be.applyConfig(p.config);
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
