package com.withouthonor.npcs.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface CuriosBridge {

    List<CurioSlotEntry> getCurios(LivingEntity entity);

    void setCurio(LivingEntity entity, String slotType, int index, ItemStack stack);

    boolean isValidForSlot(LivingEntity entity, String slotType, int index, ItemStack stack);

    void resetCurios(LivingEntity entity);

    record CurioSlotEntry(String slotType, int index, ItemStack stack) {
    }
}
