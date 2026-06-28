package com.withouthonor.npcs.client;

import com.withouthonor.npcs.network.PoseLibraryPackets.PoseEntry;

import java.util.List;

public final class ClientPoseLibrary {

    private static List<PoseEntry> list = List.of();

    private ClientPoseLibrary() {
    }

    public static void set(List<PoseEntry> updated) {
        list = updated;
    }

    public static List<PoseEntry> get() {
        return list;
    }
}
