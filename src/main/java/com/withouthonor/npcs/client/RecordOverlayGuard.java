package com.withouthonor.npcs.client;

import com.withouthonor.npcs.WHCompanions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = WHCompanions.MODID, value = Dist.CLIENT)
public final class RecordOverlayGuard {

    private static Field MESSAGE;
    private static Field MESSAGE_TIME;
    private static boolean resolved;

    private RecordOverlayGuard() {
    }

    @SubscribeEvent
    public static void onPreOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.RECORD_OVERLAY.id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Gui gui = mc.gui;
        if (gui == null) {
            return;
        }
        resolve();
        if (MESSAGE == null || MESSAGE_TIME == null) {
            return;
        }
        try {
            if (MESSAGE_TIME.getInt(gui) <= 0) {
                return;
            }
            Component message = (Component) MESSAGE.get(gui);
            if (message == null) {
                MESSAGE_TIME.setInt(gui, 0);
                event.setCanceled(true);
                return;
            }
            mc.font.width(message);
        } catch (ReflectiveOperationException ignored) {

        } catch (Throwable broken) {
            try {
                MESSAGE_TIME.setInt(gui, 0);
            } catch (Throwable ignored) {

            }
            event.setCanceled(true);
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            MESSAGE = ObfuscationReflectionHelper.findField(Gui.class, "f_92990_");
            MESSAGE_TIME = ObfuscationReflectionHelper.findField(Gui.class, "f_92991_");
        } catch (Throwable t) {
            MESSAGE = null;
            MESSAGE_TIME = null;
        }
    }
}
