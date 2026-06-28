package com.withouthonor.npcs.common.profile;

import com.withouthonor.npcs.common.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public final class ProfileSync {

    private ProfileSync() {
    }

    public static void applyToLoadedEntities(MinecraftServer server, CompanionProfile profile) {
        Component name = coloredName(profile);
        for (ServerLevel level : server.getAllLevels()) {
            level.getEntities(ModEntities.COMPANION.get(),
                            npc -> profile.getId().equals(npc.getProfileId()))
                    .forEach(npc -> {
                        npc.setCustomName(name);
                        npc.setSkinName(profile.getSkinPlayerName());
                        npc.setDisguise(profile.getDisguise());
                        npc.setTitle(profile.isShowTitle() ? profile.getTitle() : "");
                        npc.setRenderTransform(profile);
                        npc.setPose(profile);
                        npc.applyCombatProfile(profile);
                        npc.updateIndex();
                    });
        }
    }

    public static Component coloredName(CompanionProfile profile) {
        if (profile.getName().indexOf('§') >= 0) {
            return Component.literal(profile.getName());
        }
        ChatFormatting color = parseColor(profile.getNameColor());
        Component name = Component.literal(profile.getName());
        return color != null ? name.copy().withStyle(color) : name;
    }

    @Nullable
    public static ChatFormatting parseColor(@Nullable String name) {
        if (name == null) {
            return null;
        }
        ChatFormatting formatting = ChatFormatting.getByName(name);
        return formatting != null && formatting.isColor() ? formatting : null;
    }
}
