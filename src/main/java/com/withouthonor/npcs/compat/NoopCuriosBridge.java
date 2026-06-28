package com.withouthonor.npcs.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class NoopCuriosBridge implements CuriosBridge {

    public static final NoopCuriosBridge INSTANCE = new NoopCuriosBridge();

    private NoopCuriosBridge() {
    }

    @Override
    public List<CurioSlotEntry> getCurios(LivingEntity entity) {
        return List.of();
    }

    @Override
    public void setCurio(LivingEntity entity, String slotType, int index, ItemStack stack) {

    }

    @Override
    public boolean isValidForSlot(LivingEntity entity, String slotType, int index, ItemStack stack) {
        return false;
    }

    @Override
    public void resetCurios(LivingEntity entity) {

    }
}
