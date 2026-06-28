package com.withouthonor.npcs.common.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class PlayerStateManager extends SavedData {

    private static final String DATA_NAME = "wh_npcs_player_state";

    public record FlagMeta(String source, long time) {
    }

    public record FlagInfo(String name, String source, long time) {
    }

    private static class PlayerState {
        final Map<String, FlagMeta> flags = new HashMap<>();

        final Set<String> visitedNodes = new HashSet<>();

        final Map<String, Integer> reputation = new HashMap<>();

        final Map<String, Integer> tradeUses = new HashMap<>();

        final Map<String, Long> tradeWindows = new HashMap<>();

        final Map<String, String> vars = new HashMap<>();

        boolean isEmpty() {
            return flags.isEmpty() && visitedNodes.isEmpty() && reputation.isEmpty()
                    && tradeUses.isEmpty() && tradeWindows.isEmpty() && vars.isEmpty();
        }
    }

    private final Map<UUID, PlayerState> states = new HashMap<>();

    private final Map<String, String> flagDescriptions = new HashMap<>();

    private final Map<String, Integer> globalTradeUses = new HashMap<>();

    private final Map<String, Long> globalTradeWindows = new HashMap<>();

    public static PlayerStateManager get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(PlayerStateManager::load, PlayerStateManager::new, DATA_NAME);
    }

    public boolean hasFlag(UUID player, String flag) {
        PlayerState state = states.get(player);
        return state != null && state.flags.containsKey(flag);
    }

    public void setFlag(UUID player, String flag, boolean value) {
        setFlag(player, flag, value, "");
    }

    public void setFlag(UUID player, String flag, boolean value, String source) {
        if (value) {
            states.computeIfAbsent(player, u -> new PlayerState()).flags.put(flag,
                    new FlagMeta(source == null ? "" : source, System.currentTimeMillis()));
            setDirty();
        } else {
            PlayerState state = states.get(player);
            if (state != null && state.flags.remove(flag) != null) {
                setDirty();
            }
        }
    }

    public Set<String> flagsOf(UUID player) {
        PlayerState state = states.get(player);
        return state == null ? Collections.emptySet() : new TreeSet<>(state.flags.keySet());
    }

    public List<FlagInfo> flagInfosOf(UUID player) {
        PlayerState state = states.get(player);
        List<FlagInfo> out = new ArrayList<>();
        if (state != null) {
            for (Map.Entry<String, FlagMeta> e : state.flags.entrySet()) {
                out.add(new FlagInfo(e.getKey(), e.getValue().source(), e.getValue().time()));
            }
        }
        out.sort(java.util.Comparator.comparing(FlagInfo::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public void removeFlag(UUID player, String flag) {
        PlayerState state = states.get(player);
        if (state != null && state.flags.remove(flag) != null) {
            setDirty();
        }
    }

    public void clearFlags(UUID player) {
        PlayerState state = states.get(player);
        if (state != null && !state.flags.isEmpty()) {
            state.flags.clear();
            setDirty();
        }
    }

    public Map<UUID, Integer> playersWithFlags() {
        Map<UUID, Integer> out = new HashMap<>();
        for (Map.Entry<UUID, PlayerState> e : states.entrySet()) {
            if (!e.getValue().flags.isEmpty()) {
                out.put(e.getKey(), e.getValue().flags.size());
            }
        }
        return out;
    }

    public String getFlagDescription(String flag) {
        return flagDescriptions.getOrDefault(flag, "");
    }

    public void setFlagDescription(String flag, String desc) {
        if (desc == null || desc.isBlank()) {
            if (flagDescriptions.remove(flag) != null) {
                setDirty();
            }
        } else {
            flagDescriptions.put(flag, desc);
            setDirty();
        }
    }

    public Map<String, String> allFlagDescriptions() {
        return new HashMap<>(flagDescriptions);
    }

    public String getVar(UUID player, String name) {
        PlayerState state = states.get(player);
        return state == null ? "" : state.vars.getOrDefault(name, "");
    }

    public void setVar(UUID player, String name, String value) {
        if (value == null || value.isEmpty()) {
            PlayerState state = states.get(player);
            if (state != null && state.vars.remove(name) != null) {
                setDirty();
            }
        } else {
            states.computeIfAbsent(player, u -> new PlayerState()).vars.put(name, value);
            setDirty();
        }
    }

    public void markVisited(UUID player, String dialogueId, String nodeId) {
        if (states.computeIfAbsent(player, u -> new PlayerState()).visitedNodes.add(dialogueId + "/" + nodeId)) {
            setDirty();
        }
    }

    public boolean hasVisited(UUID player, String dialogueId, String nodeId) {
        PlayerState state = states.get(player);
        return state != null && state.visitedNodes.contains(dialogueId + "/" + nodeId);
    }

    public void renameDialogue(String oldId, String newId) {
        String prefix = oldId + "/";
        boolean changed = false;
        for (PlayerState state : states.values()) {
            java.util.List<String> hits = new java.util.ArrayList<>();
            for (String v : state.visitedNodes) {
                if (v.startsWith(prefix)) {
                    hits.add(v);
                }
            }
            for (String v : hits) {
                state.visitedNodes.remove(v);
                state.visitedNodes.add(newId + "/" + v.substring(prefix.length()));
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public int getReputation(UUID player, String faction) {
        PlayerState state = states.get(player);
        return state == null ? 0 : state.reputation.getOrDefault(faction, 0);
    }

    public void setReputation(UUID player, String faction, int value) {
        PlayerState state = states.computeIfAbsent(player, u -> new PlayerState());
        if (value == 0) {
            state.reputation.remove(faction);
        } else {
            state.reputation.put(faction, value);
        }
        setDirty();
    }

    public int addReputation(UUID player, String faction, int delta) {
        int value = getReputation(player, faction) + delta;
        setReputation(player, faction, value);
        return value;
    }

    public Map<UUID, Integer> reputationsFor(String faction) {
        Map<UUID, Integer> result = new HashMap<>();
        for (Map.Entry<UUID, PlayerState> entry : states.entrySet()) {
            Integer value = entry.getValue().reputation.get(faction);
            if (value != null) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    public int getTradeUses(UUID player, String offerKey) {
        PlayerState state = states.get(player);
        return state == null ? 0 : state.tradeUses.getOrDefault(offerKey, 0);
    }

    public void addTradeUse(UUID player, String offerKey) {
        PlayerState state = states.computeIfAbsent(player, u -> new PlayerState());
        state.tradeUses.merge(offerKey, 1, Integer::sum);
        setDirty();
    }

    public long getTradeWindow(UUID player, String profileId) {
        PlayerState state = states.get(player);
        return state == null ? 0L : state.tradeWindows.getOrDefault(profileId, 0L);
    }

    public void setTradeWindow(UUID player, String profileId, long startMillis) {
        states.computeIfAbsent(player, u -> new PlayerState()).tradeWindows.put(profileId, startMillis);
        setDirty();
    }

    public void resetTrades(UUID player, String profileId) {
        PlayerState state = states.get(player);
        if (state == null) {
            return;
        }
        state.tradeUses.keySet().removeIf(key -> key.startsWith(profileId + "#"));
        state.tradeWindows.remove(profileId);
        setDirty();
    }

    public void resetTradesForAll(String profileId) {
        for (PlayerState state : states.values()) {
            state.tradeUses.keySet().removeIf(key -> key.startsWith(profileId + "#"));
            state.tradeWindows.remove(profileId);
        }
        resetGlobalTrades(profileId);
        setDirty();
    }

    public int getGlobalTradeUses(String offerKey) {
        return globalTradeUses.getOrDefault(offerKey, 0);
    }

    public void addGlobalTradeUse(String offerKey) {
        globalTradeUses.merge(offerKey, 1, Integer::sum);
        setDirty();
    }

    public long getGlobalTradeWindow(String profileId) {
        return globalTradeWindows.getOrDefault(profileId, 0L);
    }

    public void setGlobalTradeWindow(String profileId, long startMillis) {
        globalTradeWindows.put(profileId, startMillis);
        setDirty();
    }

    public void resetGlobalTrades(String profileId) {
        globalTradeUses.keySet().removeIf(key -> key.startsWith(profileId + "#"));
        globalTradeWindows.remove(profileId);
        setDirty();
    }

    private static PlayerStateManager load(CompoundTag tag) {
        PlayerStateManager manager = new PlayerStateManager();
        CompoundTag globalUses = tag.getCompound("GlobalTradeUses");
        globalUses.getAllKeys().forEach(key -> manager.globalTradeUses.put(key, globalUses.getInt(key)));
        CompoundTag globalWindows = tag.getCompound("GlobalTradeWindows");
        globalWindows.getAllKeys().forEach(key -> manager.globalTradeWindows.put(key, globalWindows.getLong(key)));
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag t = players.getCompound(i);
            PlayerState state = new PlayerState();
            Tag flagsRaw = t.get("Flags");
            if (flagsRaw instanceof ListTag flagsList) {
                if (flagsList.getElementType() == Tag.TAG_STRING) {
                    for (int k = 0; k < flagsList.size(); k++) {
                        state.flags.put(flagsList.getString(k), new FlagMeta("", 0L));
                    }
                } else {
                    for (int k = 0; k < flagsList.size(); k++) {
                        CompoundTag fc = flagsList.getCompound(k);
                        state.flags.put(fc.getString("n"), new FlagMeta(fc.getString("s"), fc.getLong("t")));
                    }
                }
            }
            t.getList("Visited", Tag.TAG_STRING).forEach(v -> state.visitedNodes.add(v.getAsString()));
            CompoundTag rep = t.getCompound("Rep");
            rep.getAllKeys().forEach(faction -> state.reputation.put(faction, rep.getInt(faction)));
            CompoundTag trades = t.getCompound("TradeUses");
            trades.getAllKeys().forEach(key -> state.tradeUses.put(key, trades.getInt(key)));
            CompoundTag windows = t.getCompound("TradeWindows");
            windows.getAllKeys().forEach(key -> state.tradeWindows.put(key, windows.getLong(key)));
            CompoundTag vars = t.getCompound("Vars");
            vars.getAllKeys().forEach(key -> state.vars.put(key, vars.getString(key)));
            if (!state.isEmpty()) {
                manager.states.put(t.getUUID("Id"), state);
            }
        }
        CompoundTag descs = tag.getCompound("FlagDescriptions");
        descs.getAllKeys().forEach(k -> manager.flagDescriptions.put(k, descs.getString(k)));
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (!globalTradeUses.isEmpty()) {
            CompoundTag globalUses = new CompoundTag();
            globalTradeUses.forEach(globalUses::putInt);
            tag.put("GlobalTradeUses", globalUses);
        }
        if (!globalTradeWindows.isEmpty()) {
            CompoundTag globalWindows = new CompoundTag();
            globalTradeWindows.forEach(globalWindows::putLong);
            tag.put("GlobalTradeWindows", globalWindows);
        }
        ListTag players = new ListTag();
        for (Map.Entry<UUID, PlayerState> entry : states.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", entry.getKey());
            ListTag flags = new ListTag();
            entry.getValue().flags.forEach((name, meta) -> {
                CompoundTag fc = new CompoundTag();
                fc.putString("n", name);
                if (meta.source() != null && !meta.source().isEmpty()) {
                    fc.putString("s", meta.source());
                }
                if (meta.time() > 0L) {
                    fc.putLong("t", meta.time());
                }
                flags.add(fc);
            });
            t.put("Flags", flags);
            ListTag visited = new ListTag();
            entry.getValue().visitedNodes.forEach(v -> visited.add(StringTag.valueOf(v)));
            t.put("Visited", visited);
            if (!entry.getValue().reputation.isEmpty()) {
                CompoundTag rep = new CompoundTag();
                entry.getValue().reputation.forEach(rep::putInt);
                t.put("Rep", rep);
            }
            if (!entry.getValue().tradeUses.isEmpty()) {
                CompoundTag trades = new CompoundTag();
                entry.getValue().tradeUses.forEach(trades::putInt);
                t.put("TradeUses", trades);
            }
            if (!entry.getValue().tradeWindows.isEmpty()) {
                CompoundTag windows = new CompoundTag();
                entry.getValue().tradeWindows.forEach(windows::putLong);
                t.put("TradeWindows", windows);
            }
            if (!entry.getValue().vars.isEmpty()) {
                CompoundTag vars = new CompoundTag();
                entry.getValue().vars.forEach(vars::putString);
                t.put("Vars", vars);
            }
            players.add(t);
        }
        tag.put("Players", players);
        if (!flagDescriptions.isEmpty()) {
            CompoundTag descs = new CompoundTag();
            flagDescriptions.forEach(descs::putString);
            tag.put("FlagDescriptions", descs);
        }
        return tag;
    }
}
