package com.withouthonor.npcs.compat.curios;

import com.withouthonor.npcs.compat.CuriosBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;

public final class CuriosBridgeImpl implements CuriosBridge {

    @Override
    public List<CurioSlotEntry> getCurios(LivingEntity entity) {
        List<CurioSlotEntry> out = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).ifPresent(handler ->
                handler.getCurios().forEach((slotType, stacksHandler) -> {
                    IDynamicStackHandler stacks = stacksHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        out.add(new CurioSlotEntry(slotType, i, stacks.getStackInSlot(i)));
                    }
                }));
        return out;
    }

    @Override
    public void setCurio(LivingEntity entity, String slotType, int index, ItemStack stack) {
        CuriosApi.getCuriosInventory(entity).ifPresent(handler ->
                handler.getStacksHandler(slotType).ifPresent(sh -> {
                    IDynamicStackHandler stacks = sh.getStacks();
                    if (index >= 0 && index < stacks.getSlots()) {
                        stacks.setStackInSlot(index, stack);
                    }
                }));
    }

    @Override
    public boolean isValidForSlot(LivingEntity entity, String slotType, int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return CuriosApi.isStackValid(new SlotContext(slotType, entity, index, false, true), stack);
    }

    @Override
    public void resetCurios(LivingEntity entity) {
        CuriosApi.getCuriosInventory(entity).ifPresent(
                top.theillusivec4.curios.api.type.capability.ICuriosItemHandler::reset);
    }
}
