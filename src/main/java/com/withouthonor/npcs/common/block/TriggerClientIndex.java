package com.withouthonor.npcs.common.block;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerClientIndex {

    public static final Set<BlockEntity> ENTRIES = ConcurrentHashMap.newKeySet();

    private TriggerClientIndex() {
    }

    public static void add(BlockEntity be) {
        ENTRIES.add(be);
    }

    public static void remove(BlockEntity be) {
        ENTRIES.remove(be);
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
