package com.withouthonor.npcs.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.profile.ProfileSync;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SaveProfilePacket {

    private final byte[] profileJson;

    public SaveProfilePacket(byte[] profileJson) {
        this.profileJson = profileJson;
    }

    public SaveProfilePacket(JsonObject json) {
        this(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void encode(SaveProfilePacket packet, FriendlyByteBuf buf) {
        buf.writeByteArray(packet.profileJson);
    }

    public static SaveProfilePacket decode(FriendlyByteBuf buf) {
        return new SaveProfilePacket(buf.readByteArray(EditorDataPacket.MAX_JSON_BYTES));
    }

    public static void handle(SaveProfilePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            handleOnServer(packet, sender);
        }
        ctx.get().setPacketHandled(true);
    }

    private static void handleOnServer(SaveProfilePacket packet, ServerPlayer sender) {
        if (!sender.hasPermissions(2)) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.npc.no_permission")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(
                    new String(packet.profileJson, StandardCharsets.UTF_8)).getAsJsonObject();
            CompanionProfile updated = CompanionProfile.fromJson(json);
            if (ProfileManager.get().get(updated.getId()) == null) {
                sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.not_found")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            ProfileManager.get().save(updated);
            ProfileSync.applyToLoadedEntities(sender.server, updated);
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.saved", updated.getName())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Bad profile from {}: {}", sender.getGameProfile().getName(), e.getMessage());
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.profile.save_err", e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }
}
