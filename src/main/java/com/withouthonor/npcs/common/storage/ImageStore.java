package com.withouthonor.npcs.common.storage;

import com.withouthonor.npcs.WHCompanions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ImageStore {

    private static final Pattern NAME = Pattern.compile("[a-zA-Z0-9_\\-]{1,64}\\.png");

    public static final String AVATAR_PREFIX = "avatars/";

    public static final int MAX_BYTES = 4 * 1024 * 1024;

    public static final int CHUNK_SIZE = 28_000;

    private static ImageStore instance;

    private final Path imagesDir;

    private ImageStore(Path imagesDir) {
        this.imagesDir = imagesDir;
    }

    public static ImageStore get() {
        if (instance == null) {
            throw new IllegalStateException("ImageStore is not initialized (server not started?)");
        }
        return instance;
    }

    public static void init(MinecraftServer server) {
        instance = new ImageStore(server.getWorldPath(LevelResource.ROOT)
                .resolve("wh_npcs").resolve("images"));
    }

    public static void shutdown() {
        instance = null;
    }

    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches();
    }

    public record ImageInfo(String name, int sizeKb, long mtime) {
    }

    public List<ImageInfo> listDetailed() {
        return listIn(imagesDir, "");
    }

    public List<ImageInfo> listAvatars() {
        return listIn(imagesDir.resolve("avatars"), AVATAR_PREFIX);
    }

    private List<ImageInfo> listIn(Path dir, String namePrefix) {
        List<ImageInfo> infos = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return infos;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> isValidName(p.getFileName().toString()))
                    .sorted()
                    .forEach(p -> {
                        try {
                            infos.add(new ImageInfo(namePrefix + p.getFileName().toString(),
                                    (int) (Files.size(p) / 1024),
                                    Files.getLastModifiedTime(p).toMillis()));
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to list images in {}", dir, e);
        }
        return infos;
    }

    public enum UploadStatus {
        OK, EXISTS, TOO_BIG, BAD_FORMAT, ERROR
    }

    public record SaveResult(UploadStatus status, String name) {
    }

    private static final int MAX_DIM = 1024;

    public SaveResult saveUpload(byte[] bytes, boolean avatar) {
        if (bytes.length > MAX_BYTES) {
            return new SaveResult(UploadStatus.TOO_BIG, "");
        }
        byte[] png;
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(bytes));
            if (img == null) {
                WHCompanions.LOGGER.warn("Upload rejected: not a decodable image (len={})", bytes.length);
                return new SaveResult(UploadStatus.BAD_FORMAT, "");
            }
            img = downscale(img);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", out);
            png = out.toByteArray();
            if (png.length > MAX_BYTES) {
                return new SaveResult(UploadStatus.TOO_BIG, "");
            }
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Image normalize to PNG failed: {}", e.toString());
            return new SaveResult(UploadStatus.BAD_FORMAT, "");
        }
        try {
            String hash = sha256hex(png).substring(0, 16);
            String bare = "up_" + hash + ".png";
            Path dir = avatar ? imagesDir.resolve("avatars") : imagesDir;
            Files.createDirectories(dir);
            Path file = dir.resolve(bare);
            String full = avatar ? AVATAR_PREFIX + bare : bare;
            if (Files.isRegularFile(file)) {
                return new SaveResult(UploadStatus.EXISTS, full);
            }
            Files.write(file, png);
            return new SaveResult(UploadStatus.OK, full);
        } catch (Exception e) {
            WHCompanions.LOGGER.error("Failed to save uploaded image", e);
            return new SaveResult(UploadStatus.ERROR, "");
        }
    }

    private static java.awt.image.BufferedImage downscale(java.awt.image.BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int max = Math.max(w, h);
        if (max <= MAX_DIM) {
            return src;
        }
        double s = (double) MAX_DIM / max;
        int nw = Math.max(1, (int) Math.round(w * s));
        int nh = Math.max(1, (int) Math.round(h * s));
        java.awt.image.BufferedImage dst = new java.awt.image.BufferedImage(
                nw, nh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = dst.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    private static String sha256hex(byte[] b) throws java.security.NoSuchAlgorithmException {
        byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest(b);
        StringBuilder sb = new StringBuilder(h.length * 2);
        for (byte x : h) {
            sb.append(Character.forDigit((x >> 4) & 0xF, 16));
            sb.append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString();
    }

    public boolean deleteImage(String name) {
        Path file;
        if (name.startsWith(AVATAR_PREFIX)) {
            String bare = name.substring(AVATAR_PREFIX.length());
            if (!isValidName(bare)) {
                return false;
            }
            file = imagesDir.resolve("avatars").resolve(bare);
        } else {
            if (!isValidName(name)) {
                return false;
            }
            file = imagesDir.resolve(name);
        }
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to delete image '{}'", name, e);
            return false;
        }
    }

    public boolean renameImage(String name, String newName) {
        Path dir;
        String oldBare;
        if (name.startsWith(AVATAR_PREFIX)) {
            oldBare = name.substring(AVATAR_PREFIX.length());
            dir = imagesDir.resolve("avatars");
        } else {
            oldBare = name;
            dir = imagesDir;
        }
        if (!isValidName(oldBare)) {
            return false;
        }
        int dot = oldBare.lastIndexOf('.');
        String ext = dot >= 0 ? oldBare.substring(dot) : ".png";
        String base = newName == null ? "" : newName.replaceAll("[^A-Za-z0-9_.-]", "");
        int nd = base.lastIndexOf('.');
        if (nd >= 0) {
            base = base.substring(0, nd);
        }
        while (base.startsWith(".")) {
            base = base.substring(1);
        }
        if (base.isEmpty()) {
            base = "image";
        }
        String newBare = base + ext;
        if (!isValidName(newBare) || newBare.equals(oldBare)) {
            return false;
        }
        Path from = dir.resolve(oldBare);
        Path to = dir.resolve(newBare);
        try {
            if (!Files.isRegularFile(from) || Files.exists(to)) {
                return false;
            }
            Files.move(from, to);
            return true;
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to rename image '{}' -> '{}'", name, newBare, e);
            return false;
        }
    }

    @Nullable
    public byte[] read(String name) {
        Path file;
        if (name.startsWith(AVATAR_PREFIX)) {
            String bare = name.substring(AVATAR_PREFIX.length());
            if (!isValidName(bare)) {
                return null;
            }
            file = imagesDir.resolve("avatars").resolve(bare);
        } else {
            if (!isValidName(name)) {
                return null;
            }
            file = imagesDir.resolve(name);
        }
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) {
                if (Files.isRegularFile(file)) {
                    WHCompanions.LOGGER.warn("Image '{}' exceeds {} bytes, refusing to send", name, MAX_BYTES);
                }
                return null;
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            WHCompanions.LOGGER.error("Failed to read image '{}'", name, e);
            return null;
        }
    }
}
