package com.withouthonor.npcs.common.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DialogueGraph {

    private final String id;
    private String start;
    private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();

    private final Set<String> pinnedNodes = new LinkedHashSet<>();

    private String author;

    public DialogueGraph(String id, String start) {
        this.id = id;
        this.start = start;
    }

    public String getId() {
        return id;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public Map<String, DialogueNode> getNodes() {
        return nodes;
    }

    public DialogueNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Set<String> getPinnedNodes() {
        return pinnedNodes;
    }

    public boolean renameNode(String oldId, String newId) {
        if (oldId == null || newId == null || oldId.equals(newId)
                || !nodes.containsKey(oldId) || nodes.containsKey(newId)) {
            return false;
        }
        Map<String, DialogueNode> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, DialogueNode> e : nodes.entrySet()) {
            rebuilt.put(e.getKey().equals(oldId) ? newId : e.getKey(), e.getValue());
        }
        nodes.clear();
        nodes.putAll(rebuilt);

        if (oldId.equals(start)) {
            start = newId;
        }
        if (pinnedNodes.remove(oldId)) {
            pinnedNodes.add(newId);
        }
        for (DialogueNode n : nodes.values()) {
            for (DialogueChoice c : n.getChoices()) {
                if (oldId.equals(c.getNext())) {
                    c.setNext(newId);
                }
            }
            if (oldId.equals(n.getInputFallbackNext())) {
                n.setInputFallbackNext(newId);
            }
            if (oldId.equals(n.getCheckSuccessNext())) {
                n.setCheckSuccessNext(newId);
            }
            if (oldId.equals(n.getCheckFailNext())) {
                n.setCheckFailNext(newId);
            }
            List<DialogueNode.RandomOption> opts = n.getRandomOptions();
            for (int i = 0; i < opts.size(); i++) {
                if (oldId.equals(opts.get(i).next())) {
                    opts.set(i, new DialogueNode.RandomOption(opts.get(i).weight(), newId));
                }
            }
        }
        return true;
    }

    public String getAuthor() {
        return author == null ? "" : author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public static DialogueGraph fromJson(JsonObject json) {
        if (!json.has("id") || !json.has("start")) {
            throw new JsonParseException("Dialogue requires 'id' and 'start'");
        }
        DialogueGraph graph = new DialogueGraph(json.get("id").getAsString(), json.get("start").getAsString());
        JsonObject nodes = json.getAsJsonObject("nodes");
        if (nodes != null) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : nodes.entrySet()) {
                graph.nodes.put(entry.getKey(), DialogueNode.fromJson(entry.getValue().getAsJsonObject()));
            }
        }
        if (json.has("pinned")) {
            json.getAsJsonArray("pinned").forEach(p -> graph.pinnedNodes.add(p.getAsString()));
        }
        if (json.has("author")) {
            graph.author = json.get("author").getAsString();
        }
        return graph;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("start", start);
        JsonObject nodesJson = new JsonObject();
        for (Map.Entry<String, DialogueNode> entry : nodes.entrySet()) {
            nodesJson.add(entry.getKey(), entry.getValue().toJson());
        }
        json.add("nodes", nodesJson);
        if (!pinnedNodes.isEmpty()) {
            JsonArray pinned = new JsonArray();
            pinnedNodes.forEach(pinned::add);
            json.add("pinned", pinned);
        }
        if (author != null && !author.isEmpty()) {
            json.addProperty("author", author);
        }
        return json;
    }
}
