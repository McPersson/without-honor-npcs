package com.withouthonor.npcs;

import com.mojang.logging.LogUtils;
import com.withouthonor.npcs.common.dialogue.DialogueSessions;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.registry.ModBlockEntities;
import com.withouthonor.npcs.common.registry.ModBlocks;
import com.withouthonor.npcs.common.registry.ModCommandArguments;
import com.withouthonor.npcs.common.registry.ModCreativeTabs;
import com.withouthonor.npcs.common.registry.ModEntities;
import com.withouthonor.npcs.common.registry.ModItems;
import com.withouthonor.npcs.common.skin.SkinService;
import com.withouthonor.npcs.common.storage.DialogueManager;
import com.withouthonor.npcs.common.storage.ProfileManager;
import com.withouthonor.npcs.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WHCompanions.MODID)
public class WHCompanions {
    public static final String MODID = "wh_npcs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WHCompanions(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModEntities.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModCommandArguments.register(modEventBus);
        modEventBus.addListener(this::onEntityAttributes);
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(this);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                com.withouthonor.npcs.common.config.WhConfig.SPEC);

        if (com.withouthonor.npcs.compat.Compat.curiosLoaded()) {
            MinecraftForge.EVENT_BUS.register(new com.withouthonor.npcs.compat.curios.CuriosServerEvents());
        }

        if (com.withouthonor.npcs.compat.Compat.carryonLoaded()) {
            com.withouthonor.npcs.compat.carryon.CarryOnCompat.init();
        }

        if (com.withouthonor.npcs.compat.Compat.epicFightLoaded()) {
            com.withouthonor.npcs.compat.epicfight.EpicFightCompat.init(modEventBus);
        }
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.COMPANION.get(), CompanionEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        ProfileManager.init(event.getServer());
        DialogueManager.init(event.getServer());
        SkinService.init(event.getServer());
        com.withouthonor.npcs.common.skin.UrlSkinRegistry.init(event.getServer());
        com.withouthonor.npcs.common.storage.ImageStore.init(event.getServer());
        com.withouthonor.npcs.common.reputation.FactionRegistry.init(event.getServer());
        com.withouthonor.npcs.common.glossary.GlossaryManager.init(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ProfileManager.shutdown();
        DialogueManager.shutdown();
        SkinService.shutdown();
        com.withouthonor.npcs.common.skin.UrlSkinRegistry.shutdown();
        com.withouthonor.npcs.common.storage.ImageStore.shutdown();
        com.withouthonor.npcs.common.reputation.FactionRegistry.shutdown();
        com.withouthonor.npcs.common.glossary.GlossaryManager.shutdown();
        DialogueSessions.clearAll();
    }
}
