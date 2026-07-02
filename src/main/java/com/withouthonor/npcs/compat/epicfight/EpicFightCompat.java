package com.withouthonor.npcs.compat.epicfight;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.registry.ModEntities;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

public final class EpicFightCompat {

    private EpicFightCompat() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(EpicFightCompat::onEntityPatchRegistry);
        modBus.addListener(EpicFightCompat::onCommonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EpicFightClientCompat.init(modBus));
    }

    private static void onEntityPatchRegistry(EntityPatchRegistryEvent event) {
        event.getTypeEntry().put(ModEntities.COMPANION.get(), entity -> CompanionMobPatch::new);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                Armatures.registerEntityTypeArmature(ModEntities.COMPANION.get(), Armatures.BIPED));
    }

    public static void installMobCombat(CompanionEntity npc) {
        HumanoidMobPatch<?> patch = EpicFightCapabilities.getEntityPatch(npc, HumanoidMobPatch.class);
        if (patch != null) {
            boolean ranged = npc.getMainHandItem().getItem() instanceof ProjectileWeaponItem;
            patch.setAIAsInfantry(ranged);
        }
    }
}
