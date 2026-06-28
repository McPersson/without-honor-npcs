package com.withouthonor.npcs.common.event;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.command.WhcCommand;
import com.withouthonor.npcs.common.dialogue.DialogueSessions;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WHCompanions.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        WhcCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DialogueSessions.remove(event.getEntity().getUUID());
        com.withouthonor.npcs.common.item.NpcMoverItem.clearSelection(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof com.withouthonor.npcs.common.item.NpcMoverItem)) {
            return;
        }
        event.setCanceled(true);

        if (event.getLevel().isClientSide) {
            com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                    new com.withouthonor.npcs.network.NpcMoverPacket(event.getPos()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getTickCount() % 20 == 0) {
            com.withouthonor.npcs.common.storage.Graveyard.get(server).tickRespawns(server);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof CompanionEntity companion) {
            companion.updateIndex();

            if (companion.getProfileId() != null) {
                CompanionProfile profile = ProfileManager.get().get(companion.getProfileId());
                if (profile != null) {
                    companion.setSkinName(profile.getSkinPlayerName());
                    companion.setDisguise(profile.getDisguise());
                    companion.setTitle(profile.isShowTitle() ? profile.getTitle() : "");
                    companion.setRenderTransform(profile);
                    companion.setPose(profile);
                    companion.applyCombatProfile(profile);
                }
            }
        }
    }
}
