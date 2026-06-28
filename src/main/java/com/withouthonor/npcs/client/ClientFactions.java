package com.withouthonor.npcs.client;

import javax.annotation.Nullable;
import java.util.List;

public final class ClientFactions {

    public record Info(String id, String name, int color) {
    }

    public record Full(com.withouthonor.npcs.common.reputation.Faction faction, int usedBy) {
    }

    private static List<Info> factions = List.of();
    private static List<Full> full = List.of();

    private ClientFactions() {
    }

    public static void set(List<Info> list) {
        factions = List.copyOf(list);
    }

    public static void setFull(List<Full> list) {
        full = List.copyOf(list);
        List<Info> infos = new java.util.ArrayList<>();
        for (Full entry : list) {
            infos.add(new Info(entry.faction().getId(), entry.faction().getName(), entry.faction().getColor()));
        }
        factions = List.copyOf(infos);
    }

    public static List<Full> full() {
        return full;
    }

    public static List<Info> all() {
        return factions;
    }

    @Nullable
    public static Info byId(String id) {
        for (Info info : factions) {
            if (info.id().equals(id)) {
                return info;
            }
        }
        return null;
    }

    @Nullable
    public static String cycle(@Nullable String current) {
        if (factions.isEmpty()) {
            return null;
        }
        if (current == null) {
            return factions.get(0).id();
        }
        for (int i = 0; i < factions.size(); i++) {
            if (factions.get(i).id().equals(current)) {
                return i + 1 < factions.size() ? factions.get(i + 1).id() : null;
            }
        }
        return factions.get(0).id();
    }
}
