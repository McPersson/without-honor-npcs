package com.withouthonor.npcs.compat;

import net.minecraft.world.item.ItemStack;

public final class NoopEtchedBridge implements EtchedBridge {

    public static final NoopEtchedBridge INSTANCE = new NoopEtchedBridge();

    private NoopEtchedBridge() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isEtchedDisc(ItemStack stack) {
        return false;
    }

    @Override
    public String extractUrl(ItemStack stack) {
        return "";
    }

    @Override
    public String extractTitle(ItemStack stack) {
        return "";
    }
}
