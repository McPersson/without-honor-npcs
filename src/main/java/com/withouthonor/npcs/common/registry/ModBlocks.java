package com.withouthonor.npcs.common.registry;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.block.SpawnerBlock;
import com.withouthonor.npcs.common.block.TriggerBlock;
import com.withouthonor.npcs.common.block.TriggerHelperBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, WHCompanions.MODID);

    public static final RegistryObject<Block> TRIGGER = BLOCKS.register("trigger",
            () -> new TriggerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> TRIGGER_HELPER = BLOCKS.register("trigger_helper",
            () -> new TriggerHelperBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> NPC_AUTO_SPAWNER = BLOCKS.register("npc_auto_spawner",
            () -> new SpawnerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 6.0F)
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.METAL)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
