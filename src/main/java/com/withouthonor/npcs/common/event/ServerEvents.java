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
    public static void onServerAboutToStart(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
        // Сброс счётчика «атакуемых» NPC — от дрейфа между мирами в одном процессе (одиночная игра).
        com.withouthonor.npcs.common.entity.ai.CreatureAggroState.reset();
    }

    /**
     * Дружественный огонь (#6). Отменяем ДО нокбека/звука на LivingAttackEvent. Атакер — владелец
     * снаряда/кастер (event.getSource().getEntity()), покрывает стрелы/копья(setOwner)/ISS-касты.
     * Ранние выходы по дешевизне: атакер-NPC → жертва-NPC своей фракции без friendly-fire ИЛИ
     * жертва-сопровождаемый игрок. Саммоны ISS (source=саммон, не NPC) вне охвата v1 (осознанно).
     */
    @SubscribeEvent
    public static void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof CompanionEntity npc)) {
            return;
        }
        net.minecraft.world.entity.LivingEntity victim = event.getEntity();
        // Гейт А — фракция: свои не бьют своих (если у фракции не разрешён friendly fire).
        if (victim instanceof CompanionEntity vc && vc != npc
                && npc.isSameFaction(vc) && !npc.factionAllowsFriendlyFire()) {
            event.setCanceled(true);
            return;
        }
        // Гейт Б — напарник не ранит сопровождаемого игрока (оба пути следования).
        if (victim instanceof net.minecraft.server.level.ServerPlayer sp && npc.escortNoHarm()) {
            net.minecraft.world.entity.player.Player esc = npc.escortedPlayer();
            if (esc != null && esc.getUUID().equals(sp.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DialogueSessions.remove(event.getEntity().getUUID());
        com.withouthonor.npcs.common.item.NpcMoverItem.clearSelection(event.getEntity().getUUID());
        com.withouthonor.npcs.network.ImageUploadPacket.onLogout(event.getEntity().getUUID());
        com.withouthonor.npcs.network.SkinLibraryPackets.Upload.onLogout(event.getEntity().getUUID());
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
        if (server != null) {
            if (server.getTickCount() % 20 == 0) {
                com.withouthonor.npcs.common.storage.Graveyard.get(server).tickRespawns(server);
            }
            com.withouthonor.npcs.common.storage.GlobalScheduleManager.get(server).tick(server);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof CompanionEntity companion) {
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
            return;
        }
        // 0.9.5 #4: чужому мобу вешаем цели по «Типу существа». Дёшевы при отсутствии подходящих NPC
        // (canUse гейтится счётчиком); предикаты решают по типу конкретного NPC. Дедуп — на повторный join.
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
            if (com.withouthonor.npcs.common.entity.ai.WhCreatureAggroGoal.isPotentialAttacker(mob)
                    && mob.targetSelector.getAvailableGoals().stream().noneMatch(
                            wg -> wg.getGoal() instanceof com.withouthonor.npcs.common.entity.ai.WhCreatureAggroGoal)) {
                mob.targetSelector.addGoal(
                        com.withouthonor.npcs.common.entity.ai.WhCreatureAggroGoal.PRIORITY,
                        new com.withouthonor.npcs.common.entity.ai.WhCreatureAggroGoal(mob));
            }
            if (com.withouthonor.npcs.common.entity.ai.WhCreatureDefendGoal.isPotentialDefender(mob)
                    && mob.targetSelector.getAvailableGoals().stream().noneMatch(
                            wg -> wg.getGoal() instanceof com.withouthonor.npcs.common.entity.ai.WhCreatureDefendGoal)) {
                mob.targetSelector.addGoal(
                        com.withouthonor.npcs.common.entity.ai.WhCreatureDefendGoal.PRIORITY,
                        new com.withouthonor.npcs.common.entity.ai.WhCreatureDefendGoal(mob));
            }
        }
    }
}
