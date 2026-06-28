package com.withouthonor.npcs.common.registry;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.block.SpawnerBlockEntity;
import com.withouthonor.npcs.common.block.TriggerBlockEntity;
import com.withouthonor.npcs.common.block.TriggerHelperBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WHCompanions.MODID);

    public static final RegistryObject<BlockEntityType<TriggerBlockEntity>> TRIGGER =
            BLOCK_ENTITIES.register("trigger", () -> BlockEntityType.Builder
                    .of(TriggerBlockEntity::new, ModBlocks.TRIGGER.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<TriggerHelperBlockEntity>> TRIGGER_HELPER =
            BLOCK_ENTITIES.register("trigger_helper", () -> BlockEntityType.Builder
                    .of(TriggerHelperBlockEntity::new, ModBlocks.TRIGGER_HELPER.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<SpawnerBlockEntity>> SPAWNER =
            BLOCK_ENTITIES.register("npc_auto_spawner", () -> BlockEntityType.Builder
                    .of(SpawnerBlockEntity::new, ModBlocks.NPC_AUTO_SPAWNER.get())
                    .build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
