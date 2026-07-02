package com.withouthonor.npcs.client;

import com.withouthonor.npcs.network.GlobalSchedulePackets.Row;

import java.util.List;

public final class ClientGlobalSchedule {

    private static List<Row> rows = List.of();

    private ClientGlobalSchedule() {
    }

    public static void set(List<Row> r) {
        rows = r;
    }

    public static List<Row> get() {
        return rows;
    }
}
