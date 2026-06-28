package com.withouthonor.npcs.common.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.dialogue.DialogueGraph;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class DialogueManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static DialogueManager instance;

    private final Path dialoguesDir;
    private final Map<String, DialogueGraph> dialogues = new HashMap<>();

    private DialogueManager(Path dialoguesDir) {
        this.dialoguesDir = dialoguesDir;
    }

    public static DialogueManager get() {
        if (instance == null) {
            throw new IllegalStateException("DialogueManager is not initialized (server not started?)");
        }
        return instance;
    }

    public static void init(MinecraftServer server) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("wh_npcs").resolve("dialogues");
        instance = new DialogueManager(dir);
        instance.loadAll();
    }

    public static void shutdown() {
        instance = null;
    }

    public static boolean isValidId(String id) {
        return id.matches("[a-z0-9_]{1,64}");
    }

    private void loadAll() {
        dialogues.clear();
        if (!Files.isDirectory(dialoguesDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dialoguesDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(this::loadFile);
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to list dialogues in {}", dialoguesDir, e);
        }
        WHCompanions.LOGGER.info("Loaded {} dialogue(s)", dialogues.size());
    }

    public int reload() {
        loadAll();
        return dialogues.size();
    }

    public int loadMissing() {
        if (!Files.isDirectory(dialoguesDir)) {
            return 0;
        }
        int before = dialogues.size();
        try (Stream<Path> files = Files.list(dialoguesDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> {
                        String base = p.getFileName().toString();
                        base = base.substring(0, base.length() - 5);
                        return !dialogues.containsKey(base);
                    })
                    .forEach(this::loadFile);
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to list dialogues in {}", dialoguesDir, e);
        }
        return dialogues.size() - before;
    }

    private void loadFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            DialogueGraph graph = DialogueGraph.fromJson(GSON.fromJson(reader, JsonObject.class));
            dialogues.put(graph.getId(), graph);
        } catch (Exception e) {
            WHCompanions.LOGGER.error("Failed to read dialogue {}", file.getFileName(), e);
        }
    }

    @Nullable
    public DialogueGraph get(String id) {
        return dialogues.get(id);
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(dialogues.keySet());
    }

    public void save(DialogueGraph graph) {
        dialogues.put(graph.getId(), graph);
        Path target = dialoguesDir.resolve(graph.getId() + ".json");
        Path tmp = dialoguesDir.resolve(graph.getId() + ".json.tmp");
        try {
            Files.createDirectories(dialoguesDir);
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(graph.toJson(), writer);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to save dialogue {}", graph.getId(), e);
        }
    }

    public boolean rename(String oldId, String newId) {
        if (oldId.equals(newId) || !isValidId(newId) || dialogues.containsKey(newId)) {
            return false;
        }
        DialogueGraph old = dialogues.get(oldId);
        if (old == null) {
            return false;
        }
        JsonObject json = old.toJson();
        json.addProperty("id", newId);
        DialogueGraph renamed = DialogueGraph.fromJson(json);
        dialogues.remove(oldId);
        save(renamed);
        try {
            Files.deleteIfExists(dialoguesDir.resolve(oldId + ".json"));
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to delete old dialogue file {}", oldId, e);
        }
        return true;
    }

    public boolean delete(String id) {
        if (dialogues.remove(id) == null) {
            return false;
        }
        try {
            Files.deleteIfExists(dialoguesDir.resolve(id + ".json"));
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to delete dialogue file {}", id, e);
        }
        return true;
    }
}
