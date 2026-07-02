package com.withouthonor.npcs.common.registry;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.item.MemorionFeatherItem;
import com.withouthonor.npcs.common.item.NpcBookItem;
import com.withouthonor.npcs.common.item.NpcMoverItem;
import com.withouthonor.npcs.common.item.NpcSpawnerItem;
import com.withouthonor.npcs.common.item.TriggerToolItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WHCompanions.MODID);

    public static final RegistryObject<Item> NPC_SPAWNER =
            ITEMS.register("npc_spawner", () -> new NpcSpawnerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> MEMORION_FEATHER =
            ITEMS.register("memorion_feather",
                    () -> new MemorionFeatherItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> NPC_MOVER =
            ITEMS.register("npc_mover", () -> new NpcMoverItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> NPC_BOOK =
            ITEMS.register("npc_book", () -> new NpcBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> NPC_AUTO_SPAWNER =
            ITEMS.register("npc_auto_spawner",
                    () -> new BlockItem(ModBlocks.NPC_AUTO_SPAWNER.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRIGGER =
            ITEMS.register("trigger",
                    () -> new TriggerToolItem(ModBlocks.TRIGGER.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TRIGGER_HELPER =
            ITEMS.register("trigger_helper",
                    () -> new TriggerToolItem(ModBlocks.TRIGGER_HELPER.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
