package com.withouthonor.npcs.common.glossary;

import com.google.gson.JsonObject;

public class GlossaryTerm {

    private final String id;
    private String title;
    private String body;

    private String author = "";

    public GlossaryTerm(String id, String title, String body) {
        this.id = id;
        this.title = title;
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author == null ? "" : author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public static GlossaryTerm fromJson(JsonObject json) {
        GlossaryTerm term = new GlossaryTerm(
                json.get("id").getAsString(),
                json.has("title") ? json.get("title").getAsString() : "",
                json.has("body") ? json.get("body").getAsString() : "");
        if (json.has("author")) {
            term.author = json.get("author").getAsString();
        }
        return term;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("title", title);
        json.addProperty("body", body);
        if (!author.isEmpty()) {
            json.addProperty("author", author);
        }
        return json;
    }
}
