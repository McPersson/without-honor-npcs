package com.withouthonor.npcs.compat.carryon;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.config.WhConfig;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public final class CarryOnCompat {

    private CarryOnCompat() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void init() {
        try {
            Class<?> eventClass = Class.forName("tschipp.carryon.events.EntityPickupEvent");
            Field targetField = eventClass.getDeclaredField("target");
            targetField.setAccessible(true);
            Consumer<Event> listener = event -> {
                if (WhConfig.allowCarryOnPickup()) {
                    return;
                }
                try {
                    if (targetField.get(event) instanceof CompanionEntity) {
                        event.setCanceled(true);
                    }
                } catch (IllegalAccessException ignored) {

                }
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                    (Class<Event>) eventClass, listener);
            WHCompanions.LOGGER.info("Carry On detected: companion NPC pickup is blocked (config: allowCarryOnPickup).");
        } catch (ReflectiveOperationException | RuntimeException e) {
            WHCompanions.LOGGER.warn("Carry On compat: could not hook EntityPickupEvent ({}). "
                    + "Carrying NPCs may be possible.", e.toString());
        }
    }
}
