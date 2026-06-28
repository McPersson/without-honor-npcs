package com.withouthonor.npcs.network;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveEquipmentPacket {

    public static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private final int entityId;
    private final ItemStack[] functional;
    private final ItemStack[] cosmetic;
    private final boolean hideArmor;
    private final boolean hideMainhand;
    private final boolean hideOffhand;
    private final ItemStack arrow;

    public SaveEquipmentPacket(int entityId, ItemStack[] functional, ItemStack[] cosmetic,
                               boolean hideArmor, boolean hideMainhand, boolean hideOffhand, ItemStack arrow) {
        this.entityId = entityId;
        this.functional = functional;
        this.cosmetic = cosmetic;
        this.hideArmor = hideArmor;
        this.hideMainhand = hideMainhand;
        this.hideOffhand = hideOffhand;
        this.arrow = arrow;
    }

    public static void encode(SaveEquipmentPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        for (int i = 0; i < SLOTS.length; i++) {
            buf.writeItem(packet.functional[i]);
            buf.writeItem(packet.cosmetic[i]);
        }
        buf.writeBoolean(packet.hideArmor);
        buf.writeBoolean(packet.hideMainhand);
        buf.writeBoolean(packet.hideOffhand);
        buf.writeItem(packet.arrow);
    }

    public static SaveEquipmentPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ItemStack[] functional = new ItemStack[SLOTS.length];
        ItemStack[] cosmetic = new ItemStack[SLOTS.length];
        for (int i = 0; i < SLOTS.length; i++) {
            functional[i] = buf.readItem();
            cosmetic[i] = buf.readItem();
        }
        return new SaveEquipmentPacket(entityId, functional, cosmetic,
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readItem());
    }

    public static void handle(SaveEquipmentPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            handleOnServer(packet, sender);
        }
        ctx.get().setPacketHandled(true);
    }

    private static void handleOnServer(SaveEquipmentPacket packet, ServerPlayer sender) {
        if (!sender.hasPermissions(2)) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.npc.no_permission")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!(sender.level().getEntity(packet.entityId) instanceof CompanionEntity npc)) {
            sender.sendSystemMessage(Component.translatable("wh_npcs.msg.npc.not_loaded")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        for (int i = 0; i < SLOTS.length; i++) {
            npc.setItemSlot(SLOTS[i], packet.functional[i]);

            npc.setDropChance(SLOTS[i], 0.0F);
            npc.setCosmeticItem(SLOTS[i], packet.cosmetic[i]);
        }
        npc.setHideArmor(packet.hideArmor);
        npc.setHideMainhand(packet.hideMainhand);
        npc.setHideOffhand(packet.hideOffhand);
        npc.setArrowItem(packet.arrow);

        if (npc.getProfileId() != null) {
            var profile = com.withouthonor.npcs.common.storage.ProfileManager.get()
                    .get(npc.getProfileId());
            if (profile != null) {
                npc.applyCombatProfile(profile);
            }
        }
    }
}
