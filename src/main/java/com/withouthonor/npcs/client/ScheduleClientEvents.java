package com.withouthonor.npcs.client;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WHCompanions.MODID, value = Dist.CLIENT)
public final class ScheduleClientEvents {

    private ScheduleClientEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || !event.getEntity().isShiftKeyDown()
                || !event.getItemStack().is(ModItems.MEMORION_FEATHER.get())) {
            return;
        }
        if (SchedulePointPicker.isPending()) {
            SchedulePointPicker.complete(event.getPos());
            event.setCanceled(true);
        } else if (RespawnHomePicker.isPending()) {
            RespawnHomePicker.complete(event.getPos());
            event.setCanceled(true);
        }
    }
}
