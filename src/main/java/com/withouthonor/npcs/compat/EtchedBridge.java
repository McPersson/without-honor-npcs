package com.withouthonor.npcs.compat;

import net.minecraft.world.item.ItemStack;

public interface EtchedBridge {

    boolean isAvailable();

    boolean isEtchedDisc(ItemStack stack);

    String extractUrl(ItemStack stack);

    String extractTitle(ItemStack stack);
}
