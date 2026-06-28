package com.withouthonor.npcs.common.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.withouthonor.npcs.common.dialogue.condition.ConditionTypes;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;

import java.util.ArrayList;
import java.util.List;

public class DialogueNode {

    public record ImageRef(String file, String caption) {
    }

    public static final int MAX_IMAGES = 3;

    public record RandomOption(int weight, String next) {
    }

    private final List<String> pages = new ArrayList<>();
    private final List<DialogueChoice> choices = new ArrayList<>();
    private final List<ImageRef> images = new ArrayList<>();

    private boolean randomPage;

    private String type = "text";

    private String inputStoreVar = "";
    private String inputHint = "";
    private String inputFallbackNext = "";

    private final List<DialogueCondition> checkConditions = new ArrayList<>();
    private int checkChance = 100;
    private String checkSuccessNext = "";
    private String checkFailNext = "";

    private final List<RandomOption> randomOptions = new ArrayList<>();

    private String musicDisc = "";

    private String musicUrl = "";

    private String musicTitle = "";

    private boolean musicLoop = true;

    private String secondCharPreset = "";

    private String secondCharPortrait = "";

    private boolean secondCharPortraitShow;

    private boolean secondCharNameShow = true;

    public List<String> getPages() {
        return pages;
    }

    public List<DialogueChoice> getChoices() {
        return choices;
    }

    public List<ImageRef> getImages() {
        return images;
    }

    public boolean isRandomPage() {
        return randomPage;
    }

    public void setRandomPage(boolean randomPage) {
        this.randomPage = randomPage;
    }

    public String getMusicDisc() {
        return musicDisc;
    }

    public void setMusicDisc(String musicDisc) {
        this.musicDisc = musicDisc == null ? "" : musicDisc;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        this.musicUrl = musicUrl == null ? "" : musicUrl;
    }

    public String getMusicTitle() {
        return musicTitle;
    }

    public void setMusicTitle(String musicTitle) {
        this.musicTitle = musicTitle == null ? "" : musicTitle;
    }

    public boolean isMusicLoop() {
        return musicLoop;
    }

    public void setMusicLoop(boolean musicLoop) {
        this.musicLoop = musicLoop;
    }

    public String getSecondCharPreset() {
        return secondCharPreset;
    }

    public void setSecondCharPreset(String preset) {
        this.secondCharPreset = preset == null ? "" : preset;
    }

    public String getSecondCharPortrait() {
        return secondCharPortrait;
    }

    public void setSecondCharPortrait(String portrait) {
        this.secondCharPortrait = portrait == null ? "" : portrait;
    }

    public boolean isSecondCharPortraitShow() {
        return secondCharPortraitShow;
    }

    public void setSecondCharPortraitShow(boolean show) {
        this.secondCharPortraitShow = show;
    }

    public boolean isSecondCharNameShow() {
        return secondCharNameShow;
    }

    public void setSecondCharNameShow(boolean show) {
        this.secondCharNameShow = show;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null || type.isEmpty() ? "text" : type;
    }

    public String getInputStoreVar() {
        return inputStoreVar;
    }

    public void setInputStoreVar(String v) {
        this.inputStoreVar = v == null ? "" : v;
    }

    public String getInputHint() {
        return inputHint;
    }

    public void setInputHint(String v) {
        this.inputHint = v == null ? "" : v;
    }

    public String getInputFallbackNext() {
        return inputFallbackNext;
    }

    public void setInputFallbackNext(String v) {
        this.inputFallbackNext = v == null ? "" : v;
    }

    public List<DialogueCondition> getCheckConditions() {
        return checkConditions;
    }

    public int getCheckChance() {
        return checkChance;
    }

    public void setCheckChance(int chance) {
        this.checkChance = Math.max(0, Math.min(100, chance));
    }

    public String getCheckSuccessNext() {
        return checkSuccessNext;
    }

    public void setCheckSuccessNext(String v) {
        this.checkSuccessNext = v == null ? "" : v;
    }

    public String getCheckFailNext() {
        return checkFailNext;
    }

    public void setCheckFailNext(String v) {
        this.checkFailNext = v == null ? "" : v;
    }

    public List<RandomOption> getRandomOptions() {
        return randomOptions;
    }

    public static DialogueNode fromJson(JsonObject json) {
        DialogueNode node = new DialogueNode();
        JsonArray pages = json.getAsJsonArray("pages");
        if (pages != null) {
            for (JsonElement page : pages) {
                node.pages.add(page.getAsString());
            }
        }
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices != null) {
            for (JsonElement choice : choices) {
                node.choices.add(DialogueChoice.fromJson(choice.getAsJsonObject()));
            }
        }
        node.randomPage = json.has("page_mode") && "random".equals(json.get("page_mode").getAsString());
        if (json.has("node_type")) {
            node.type = json.get("node_type").getAsString();
        }
        if (json.has("input_store")) {
            node.inputStoreVar = json.get("input_store").getAsString();
        }
        if (json.has("input_hint")) {
            node.inputHint = json.get("input_hint").getAsString();
        }
        if (json.has("input_fallback")) {
            node.inputFallbackNext = json.get("input_fallback").getAsString();
        }
        if (json.has("check_conditions")) {
            node.checkConditions.addAll(ConditionTypes.parseList(json.getAsJsonArray("check_conditions")));
        }
        if (json.has("check_chance")) {
            node.checkChance = json.get("check_chance").getAsInt();
        }
        if (json.has("check_success")) {
            node.checkSuccessNext = json.get("check_success").getAsString();
        }
        if (json.has("check_fail")) {
            node.checkFailNext = json.get("check_fail").getAsString();
        }
        if (json.has("random_options")) {
            for (JsonElement e : json.getAsJsonArray("random_options")) {
                JsonObject o = e.getAsJsonObject();
                node.randomOptions.add(new RandomOption(
                        o.has("weight") ? o.get("weight").getAsInt() : 1,
                        o.has("next") ? o.get("next").getAsString() : ""));
            }
        }
        if (json.has("music_disc")) {
            node.musicDisc = json.get("music_disc").getAsString();
        }
        if (json.has("music_url")) {
            node.musicUrl = json.get("music_url").getAsString();
        }
        if (json.has("music_title")) {
            node.musicTitle = json.get("music_title").getAsString();
        }
        if (json.has("music_disc") || json.has("music_url")) {
            node.musicLoop = !json.has("music_loop") || json.get("music_loop").getAsBoolean();
        }
        if (json.has("second_char")) {
            node.secondCharPreset = json.get("second_char").getAsString();
        }
        if (json.has("second_char_portrait")) {
            node.secondCharPortrait = json.get("second_char_portrait").getAsString();
        }
        node.secondCharPortraitShow = json.has("second_char_portrait_show");
        node.secondCharNameShow = !json.has("second_char_name_off");
        JsonArray images = json.getAsJsonArray("images");
        if (images != null) {
            for (JsonElement e : images) {
                JsonObject img = e.getAsJsonObject();
                node.images.add(new ImageRef(img.get("file").getAsString(),
                        img.has("caption") ? img.get("caption").getAsString() : ""));
            }
        }
        return node;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonArray pagesJson = new JsonArray();
        pages.forEach(pagesJson::add);
        json.add("pages", pagesJson);
        JsonArray choicesJson = new JsonArray();
        choices.forEach(c -> choicesJson.add(c.toJson()));
        json.add("choices", choicesJson);
        if (randomPage) {
            json.addProperty("page_mode", "random");
        }
        if ("input".equals(type)) {
            json.addProperty("node_type", "input");
            if (!inputStoreVar.isEmpty()) {
                json.addProperty("input_store", inputStoreVar);
            }
            if (!inputHint.isEmpty()) {
                json.addProperty("input_hint", inputHint);
            }
            if (!inputFallbackNext.isEmpty()) {
                json.addProperty("input_fallback", inputFallbackNext);
            }
        } else if ("check".equals(type)) {
            json.addProperty("node_type", "check");
            if (!checkConditions.isEmpty()) {
                JsonArray cond = new JsonArray();
                checkConditions.forEach(c -> cond.add(c.toJson()));
                json.add("check_conditions", cond);
            }
            json.addProperty("check_chance", checkChance);
            if (!checkSuccessNext.isEmpty()) {
                json.addProperty("check_success", checkSuccessNext);
            }
            if (!checkFailNext.isEmpty()) {
                json.addProperty("check_fail", checkFailNext);
            }
        } else if ("random".equals(type)) {
            json.addProperty("node_type", "random");
            if (!randomOptions.isEmpty()) {
                JsonArray opts = new JsonArray();
                for (RandomOption opt : randomOptions) {
                    JsonObject o = new JsonObject();
                    o.addProperty("weight", opt.weight());
                    o.addProperty("next", opt.next());
                    opts.add(o);
                }
                json.add("random_options", opts);
            }
        }
        if (!musicDisc.isEmpty() || !musicUrl.isEmpty()) {
            if (!musicDisc.isEmpty()) {
                json.addProperty("music_disc", musicDisc);
            }
            if (!musicUrl.isEmpty()) {
                json.addProperty("music_url", musicUrl);
            }
            if (!musicTitle.isEmpty()) {
                json.addProperty("music_title", musicTitle);
            }
            json.addProperty("music_loop", musicLoop);
        }
        if (!secondCharPreset.isEmpty()) {
            json.addProperty("second_char", secondCharPreset);
        }
        if (!secondCharPortrait.isEmpty()) {
            json.addProperty("second_char_portrait", secondCharPortrait);
        }
        if (secondCharPortraitShow) {
            json.addProperty("second_char_portrait_show", true);
        }
        if (!secondCharNameShow) {
            json.addProperty("second_char_name_off", true);
        }
        if (!images.isEmpty()) {
            JsonArray imagesJson = new JsonArray();
            for (ImageRef image : images) {
                JsonObject img = new JsonObject();
                img.addProperty("file", image.file());
                if (!image.caption().isEmpty()) {
                    img.addProperty("caption", image.caption());
                }
                imagesJson.add(img);
            }
            json.add("images", imagesJson);
        }
        return json;
    }
}
