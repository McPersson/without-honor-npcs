package com.withouthonor.npcs.client;

import com.withouthonor.npcs.WHCompanions;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ClientLocalFiles {

    private ClientLocalFiles() {
    }

    public static Path dir(boolean dialogue) {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("wh_npcs").resolve(dialogue ? "local_dialogues" : "local_profiles");
    }

    public static void ensureDir(boolean dialogue) {
        try {
            Files.createDirectories(dir(dialogue));
        } catch (IOException e) {
            WHCompanions.LOGGER.warn("Cannot create local dir: {}", e.toString());
        }
    }

    public static List<String> list(boolean dialogue) {
        List<String> out = new ArrayList<>();
        Path d = dir(dialogue);
        if (!Files.isDirectory(d)) {
            return out;
        }
        try (Stream<Path> files = Files.list(d)) {
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        String fn = p.getFileName().toString();
                        out.add(fn.substring(0, fn.length() - 5));
                    });
        } catch (IOException e) {
            WHCompanions.LOGGER.warn("Cannot list local dir: {}", e.toString());
        }
        return out;
    }

    public static byte[] read(boolean dialogue, String name) throws IOException {
        return Files.readAllBytes(dir(dialogue).resolve(name + ".json"));
    }

    public static List<com.withouthonor.npcs.network.ProfileSharePackets.FileEntry> listEntries(
            boolean dialogue, String author) {
        List<com.withouthonor.npcs.network.ProfileSharePackets.FileEntry> out = new ArrayList<>();
        Path d = dir(dialogue);
        if (!Files.isDirectory(d)) {
            return out;
        }
        try (Stream<Path> files = Files.list(d)) {
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            String fn = p.getFileName().toString();
                            String name = fn.substring(0, fn.length() - 5);
                            int kb = (int) Math.max(1, Files.size(p) / 1024);
                            long mt = Files.getLastModifiedTime(p).toMillis();
                            out.add(new com.withouthonor.npcs.network.ProfileSharePackets.FileEntry(
                                    name, author == null ? "" : author, kb, mt));
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            WHCompanions.LOGGER.warn("Cannot list local dir: {}", e.toString());
        }
        return out;
    }

    public static boolean renameProfile(String oldBase, String newBase) {
        try {
            String safe = sanitize(newBase);
            if (safe.isEmpty()) {
                safe = "profile";
            }
            Path src = dir(false).resolve(oldBase + ".json");
            Path dst = dir(false).resolve(safe + ".json");
            if (!Files.isRegularFile(src) || Files.exists(dst)) {
                return false;
            }
            Files.move(src, dst);
            return true;
        } catch (IOException e) {
            WHCompanions.LOGGER.warn("Cannot rename local profile: {}", e.toString());
            return false;
        }
    }

    public static void delete(boolean dialogue, String name) {
        try {
            Files.deleteIfExists(dir(dialogue).resolve(name + ".json"));
        } catch (IOException e) {
            WHCompanions.LOGGER.warn("Cannot delete local file: {}", e.toString());
        }
    }

    public static Path writeProfile(String name, String json) throws IOException {
        ensureDir(false);
        String safe = sanitize(name);
        if (safe.isEmpty()) {
            safe = "profile";
        }
        Path file = dir(false).resolve(safe + ".json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }

    public static Path writeDialogue(String id, String json) throws IOException {
        ensureDir(true);
        String safe = sanitize(id);
        if (safe.isEmpty()) {
            safe = "dialogue";
        }
        Path file = dir(true).resolve(safe + ".json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }

    public static void openFolder(boolean dialogue) {
        ensureDir(dialogue);
        net.minecraft.Util.getPlatform().openFile(dir(dialogue).toFile());
    }

    public static void browseNamed(String title, String[] patterns, String filterDesc,
                                   java.util.function.BiConsumer<String, byte[]> onPicked) {
        Thread t = new Thread(() -> {
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                org.lwjgl.PointerBuffer filters = stack.mallocPointer(patterns.length);
                for (String p : patterns) {
                    filters.put(stack.UTF8(p));
                }
                filters.flip();
                String path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(
                        title, "", filters, filterDesc, false);
                if (path != null) {
                    byte[] bytes = Files.readAllBytes(Path.of(path));
                    String fn = Path.of(path).getFileName().toString();
                    int dot = fn.lastIndexOf('.');
                    String base = dot > 0 ? fn.substring(0, dot) : fn;
                    Minecraft.getInstance().execute(() -> onPicked.accept(base, bytes));
                }
            } catch (Throwable e) {
                WHCompanions.LOGGER.warn("File browse failed: {}", e.toString());
            }
        }, "wh_npcs-file-browse");
        t.setDaemon(true);
        t.start();
    }

    public static void browse(String title, String[] patterns, String filterDesc, Consumer<byte[]> onPicked) {
        browseNamed(title, patterns, filterDesc, (name, bytes) -> onPicked.accept(bytes));
    }

    public static void browseJson(Consumer<byte[]> onPicked) {
        browse("Without Honor: NPCs", new String[]{"*.json"}, "JSON (*.json)", onPicked);
    }

    public static void browsePng(Consumer<byte[]> onPicked) {
        browse("Without Honor: NPCs", new String[]{"*.png"}, "PNG (*.png)", onPicked);
    }

    public static void browsePngNamed(java.util.function.BiConsumer<String, byte[]> onPicked) {
        browseNamed("Without Honor: NPCs", new String[]{"*.png"}, "PNG (*.png)", onPicked);
    }

    public static void uploadSkin(String name, byte[] bytes) {
        int chunk = com.withouthonor.npcs.common.storage.ImageStore.CHUNK_SIZE;
        int total = Math.max(1, (bytes.length + chunk - 1) / chunk);
        for (int i = 0; i < total; i++) {
            int from = i * chunk;
            int to = Math.min(bytes.length, from + chunk);
            byte[] part = java.util.Arrays.copyOfRange(bytes, from, to);
            com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                    new com.withouthonor.npcs.network.SkinLibraryPackets.Upload(name, i, total, part));
        }
    }

    public static void writeDialogues(List<com.withouthonor.npcs.network.DialogueBundlePacket.BundleEntry> entries) {
        int n = 0;
        for (com.withouthonor.npcs.network.DialogueBundlePacket.BundleEntry e : entries) {
            try {
                writeDialogue(e.id(), e.json());
                n++;
            } catch (IOException ignored) {
            }
        }
        if (n > 0) {
            openFolder(true);
        }
        net.minecraft.client.Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "wh_npcs.msg.dialogue.exported_client", n), false);
        }
    }

    public static void uploadImage(byte[] bytes, boolean avatar) {
        int chunk = com.withouthonor.npcs.common.storage.ImageStore.CHUNK_SIZE;
        int total = Math.max(1, (bytes.length + chunk - 1) / chunk);
        for (int i = 0; i < total; i++) {
            int from = i * chunk;
            int to = Math.min(bytes.length, from + chunk);
            byte[] part = java.util.Arrays.copyOfRange(bytes, from, to);
            com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                    new com.withouthonor.npcs.network.ImageUploadPacket(avatar, i, total, part));
        }
    }

    private static String sanitize(String raw) {
        String s = raw == null ? "" : raw.replaceAll("[^A-Za-z0-9._-]", "");
        while (s.startsWith(".")) {
            s = s.substring(1);
        }
        return s;
    }
}
