package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.compat.Compat;
import com.withouthonor.npcs.compat.CuriosBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class CuriosPackets {

    private CuriosPackets() {
    }

    private static void writeEntries(FriendlyByteBuf buf, List<CuriosBridge.CurioSlotEntry> entries) {
        buf.writeCollection(entries, (b, e) -> {
            b.writeUtf(e.slotType(), 64);
            b.writeVarInt(e.index());
            b.writeItem(e.stack());
        });
    }

    private static List<CuriosBridge.CurioSlotEntry> readEntries(FriendlyByteBuf buf) {
        return buf.readCollection(ArrayList::new,
                b -> new CuriosBridge.CurioSlotEntry(b.readUtf(64), b.readVarInt(), b.readItem()));
    }

    public static final class Request {

        private final int entityId;

        public Request(int entityId) {
            this.entityId = entityId;
        }

        public static void encode(Request p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.entityId);
        }

        public static Request decode(FriendlyByteBuf buf) {
            return new Request(buf.readVarInt());
        }

        public static void handle(Request p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)
                    && sender.level().getEntity(p.entityId) instanceof CompanionEntity npc) {
                List<CuriosBridge.CurioSlotEntry> entries = Compat.curios().getCurios(npc);
                NetworkHandler.sendToPlayer(new ListResult(p.entityId, entries), sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class ListResult {

        private final int entityId;
        private final List<CuriosBridge.CurioSlotEntry> entries;

        public ListResult(int entityId, List<CuriosBridge.CurioSlotEntry> entries) {
            this.entityId = entityId;
            this.entries = entries;
        }

        public static void encode(ListResult p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.entityId);
            writeEntries(buf, p.entries);
        }

        public static ListResult decode(FriendlyByteBuf buf) {
            return new ListResult(buf.readVarInt(), readEntries(buf));
        }

        public static void handle(ListResult p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.withouthonor.npcs.client.gui.editor.CuriosEditorScreen.accept(p.entityId, p.entries)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class Save {

        private final int entityId;
        private final List<CuriosBridge.CurioSlotEntry> entries;

        public Save(int entityId, List<CuriosBridge.CurioSlotEntry> entries) {
            this.entityId = entityId;
            this.entries = entries;
        }

        public static void encode(Save p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.entityId);
            writeEntries(buf, p.entries);
        }

        public static Save decode(FriendlyByteBuf buf) {
            return new Save(buf.readVarInt(), readEntries(buf));
        }

        public static void handle(Save p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                if (!sender.hasPermissions(2)) {
                    sender.sendSystemMessage(Component.translatable("wh_npcs.msg.npc.no_permission")
                            .withStyle(ChatFormatting.RED));
                } else if (sender.level().getEntity(p.entityId) instanceof CompanionEntity npc) {
                    CuriosBridge bridge = Compat.curios();
                    for (CuriosBridge.CurioSlotEntry e : p.entries) {

                        if (e.stack().isEmpty() || bridge.isValidForSlot(npc, e.slotType(), e.index(), e.stack())) {
                            bridge.setCurio(npc, e.slotType(), e.index(), e.stack());
                        }
                    }
                }
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
