package com.withouthonor.npcs.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.WHCompanions;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ClientPrefs {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientPrefs instance;

    private final Set<String> favoriteImages = new HashSet<>();
    private final Set<String> pinnedImages = new HashSet<>();
    private final Set<String> favoriteDialogues = new HashSet<>();
    private final Set<String> pinnedDialogues = new HashSet<>();
    private final Set<String> favoriteSkins = new HashSet<>();
    private final Set<String> pinnedFactions = new HashSet<>();
    private final Set<String> favoriteTerms = new HashSet<>();
    private final Set<String> pinnedTerms = new HashSet<>();
    private final Set<String> favoriteVoices = new HashSet<>();
    private final Set<String> pinnedVoices = new HashSet<>();
    private final Set<String> favoriteProfiles = new HashSet<>();
    private final Set<String> pinnedProfiles = new HashSet<>();
    private final Set<String> pinnedFlagPlayers = new HashSet<>();
    private final Set<String> favoritePoses = new HashSet<>();
    private final Set<String> pinnedPoses = new HashSet<>();
    private final Set<String> favoriteEmotes = new HashSet<>();
    private String uiTheme = "dark";

    public static ClientPrefs get() {
        if (instance == null) {
            instance = new ClientPrefs();
            instance.load();
        }
        return instance;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("wh_npcs-client.json");
    }

    private void load() {
        if (!Files.isRegularFile(file())) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            readSet(json, "favorite_images", favoriteImages);
            readSet(json, "pinned_images", pinnedImages);
            readSet(json, "favorite_dialogues", favoriteDialogues);
            readSet(json, "pinned_dialogues", pinnedDialogues);
            readSet(json, "favorite_skins", favoriteSkins);
            readSet(json, "pinned_factions", pinnedFactions);
            readSet(json, "favorite_terms", favoriteTerms);
            readSet(json, "pinned_terms", pinnedTerms);
            readSet(json, "favorite_voices", favoriteVoices);
            readSet(json, "pinned_voices", pinnedVoices);
            readSet(json, "favorite_profiles", favoriteProfiles);
            readSet(json, "pinned_profiles", pinnedProfiles);
            readSet(json, "pinned_flag_players", pinnedFlagPlayers);
            readSet(json, "favorite_poses", favoritePoses);
            readSet(json, "pinned_poses", pinnedPoses);
            readSet(json, "favorite_emotes", favoriteEmotes);
            if (json.has("ui_theme")) {
                uiTheme = json.get("ui_theme").getAsString();
            }
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to read client prefs", e);
        }
    }

    private static void readSet(JsonObject json, String key, Set<String> target) {
        if (json.has(key)) {
            json.getAsJsonArray(key).forEach(e -> target.add(e.getAsString()));
        }
    }

    private void save() {
        JsonObject json = new JsonObject();
        json.add("favorite_images", toArray(favoriteImages));
        json.add("pinned_images", toArray(pinnedImages));
        json.add("favorite_dialogues", toArray(favoriteDialogues));
        json.add("pinned_dialogues", toArray(pinnedDialogues));
        json.add("favorite_skins", toArray(favoriteSkins));
        json.add("pinned_factions", toArray(pinnedFactions));
        json.add("favorite_terms", toArray(favoriteTerms));
        json.add("pinned_terms", toArray(pinnedTerms));
        json.add("favorite_voices", toArray(favoriteVoices));
        json.add("pinned_voices", toArray(pinnedVoices));
        json.add("favorite_profiles", toArray(favoriteProfiles));
        json.add("pinned_profiles", toArray(pinnedProfiles));
        json.add("pinned_flag_players", toArray(pinnedFlagPlayers));
        json.add("favorite_poses", toArray(favoritePoses));
        json.add("pinned_poses", toArray(pinnedPoses));
        json.add("favorite_emotes", toArray(favoriteEmotes));
        json.addProperty("ui_theme", uiTheme);
        try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (Exception e) {
            WHCompanions.LOGGER.warn("Failed to save client prefs", e);
        }
    }

    private static JsonArray toArray(Set<String> set) {
        JsonArray array = new JsonArray();
        set.stream().sorted().forEach(array::add);
        return array;
    }

    private boolean toggle(Set<String> set, String value) {
        if (!set.remove(value)) {
            set.add(value);
        }
        save();
        return set.contains(value);
    }

    public boolean isFavoriteImage(String name) {
        return favoriteImages.contains(name);
    }

    public void toggleFavoriteImage(String name) {
        toggle(favoriteImages, name);
    }

    public boolean isPinnedImage(String name) {
        return pinnedImages.contains(name);
    }

    public void togglePinnedImage(String name) {
        toggle(pinnedImages, name);
    }

    public boolean isFavoriteDialogue(String id) {
        return favoriteDialogues.contains(id);
    }

    public void toggleFavoriteDialogue(String id) {
        toggle(favoriteDialogues, id);
    }

    public boolean isPinnedDialogue(String id) {
        return pinnedDialogues.contains(id);
    }

    public void togglePinnedDialogue(String id) {
        toggle(pinnedDialogues, id);
    }

    public boolean isFavoriteSkin(String spec) {
        return favoriteSkins.contains(spec);
    }

    public java.util.Set<String> favoriteSkinSet() {
        return java.util.Collections.unmodifiableSet(favoriteSkins);
    }

    public void toggleFavoriteSkin(String spec) {
        toggle(favoriteSkins, spec);
    }

    public boolean isPinnedFaction(String id) {
        return pinnedFactions.contains(id);
    }

    public void togglePinnedFaction(String id) {
        toggle(pinnedFactions, id);
    }

    public boolean isFavoriteTerm(String id) {
        return favoriteTerms.contains(id);
    }

    public void toggleFavoriteTerm(String id) {
        toggle(favoriteTerms, id);
    }

    public boolean isPinnedTerm(String id) {
        return pinnedTerms.contains(id);
    }

    public void togglePinnedTerm(String id) {
        toggle(pinnedTerms, id);
    }

    public boolean isFavoriteVoice(String id) {
        return favoriteVoices.contains(id);
    }

    public void toggleFavoriteVoice(String id) {
        toggle(favoriteVoices, id);
    }

    public boolean isPinnedVoice(String id) {
        return pinnedVoices.contains(id);
    }

    public void togglePinnedVoice(String id) {
        toggle(pinnedVoices, id);
    }

    public boolean isFavoriteProfile(String file) {
        return favoriteProfiles.contains(file);
    }

    public void toggleFavoriteProfile(String file) {
        toggle(favoriteProfiles, file);
    }

    public boolean isPinnedFlagPlayer(String uuid) {
        return pinnedFlagPlayers.contains(uuid);
    }

    public void togglePinnedFlagPlayer(String uuid) {
        toggle(pinnedFlagPlayers, uuid);
    }

    public boolean isPinnedProfile(String file) {
        return pinnedProfiles.contains(file);
    }

    public void togglePinnedProfile(String file) {
        toggle(pinnedProfiles, file);
    }

    public boolean isFavoritePose(String name) {
        return favoritePoses.contains(name);
    }

    public void toggleFavoritePose(String name) {
        toggle(favoritePoses, name);
    }

    public boolean isPinnedPose(String name) {
        return pinnedPoses.contains(name);
    }

    public void togglePinnedPose(String name) {
        toggle(pinnedPoses, name);
    }

    public boolean isFavoriteEmote(String id) {
        return favoriteEmotes.contains(id);
    }

    public void toggleFavoriteEmote(String id) {
        toggle(favoriteEmotes, id);
    }

    public String getUiTheme() {
        return uiTheme;
    }

    public void setUiTheme(String theme) {
        uiTheme = theme;
        save();
    }
}
