package com.withouthonor.npcs.client;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.client.render.CompanionRenderer;
import com.withouthonor.npcs.client.render.SpawnerBlockEntityRenderer;
import com.withouthonor.npcs.common.registry.ModBlockEntities;
import com.withouthonor.npcs.common.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WHCompanions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMPANION.get(), CompanionRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SPAWNER.get(), SpawnerBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {

        event.enqueueWork(() -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                com.withouthonor.npcs.common.registry.ModBlocks.NPC_AUTO_SPAWNER.get(),
                net.minecraft.client.renderer.RenderType.cutout()));

        event.enqueueWork(() -> com.withouthonor.npcs.client.gui.VanillaUIHelper.setTheme(
                themeFromPref(ClientPrefs.get().getUiTheme())));
    }

    private static com.withouthonor.npcs.client.gui.VanillaUIHelper.Theme themeFromPref(String pref) {
        return switch (pref == null ? "" : pref) {
            case "vanilla" -> com.withouthonor.npcs.client.gui.VanillaUIHelper.Theme.VANILLA;
            case "coffee" -> com.withouthonor.npcs.client.gui.VanillaUIHelper.Theme.COFFEE;
            default -> com.withouthonor.npcs.client.gui.VanillaUIHelper.Theme.DARK;
        };
    }
}
