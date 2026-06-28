package com.withouthonor.npcs.common.registry;

import com.withouthonor.npcs.WHCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WHCompanions.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wh_npcs"))
                    .icon(() -> new ItemStack(ModItems.NPC_SPAWNER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.NPC_SPAWNER.get());
                        output.accept(ModItems.MEMORION_FEATHER.get());
                        output.accept(ModItems.NPC_MOVER.get());
                        output.accept(ModItems.NPC_BOOK.get());
                        output.accept(ModItems.NPC_AUTO_SPAWNER.get());
                        output.accept(ModItems.TRIGGER.get());
                        output.accept(ModItems.TRIGGER_HELPER.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
