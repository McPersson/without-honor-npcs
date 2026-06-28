package com.withouthonor.npcs.common.dialogue;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.dialogue.action.DialogueAction;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;
import com.withouthonor.npcs.common.dialogue.condition.ItemsCondition;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.storage.DialogueManager;
import com.withouthonor.npcs.common.storage.PlayerStateManager;
import com.withouthonor.npcs.common.storage.ProfileManager;
import com.withouthonor.npcs.network.DialogueClosePacket;
import com.withouthonor.npcs.network.DialogueNodeData;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.OpenDialogueNodePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class DialogueRuntime {

    private static final double MAX_DISTANCE_SQR = 8 * 8;

    private DialogueRuntime() {
    }

    public static boolean tryOpen(ServerPlayer player, CompanionEntity npc) {
        if (npc.getProfileId() == null) {
            return false;
        }
        CompanionProfile profile = ProfileManager.get().get(npc.getProfileId());
        if (profile == null) {
            return false;
        }
        DialogueCondition.Context ctx = new DialogueCondition.Context(player, npc);
        for (EntryPoint entry : profile.getEntryPoints()) {
            if (entry.matches(ctx)) {
                return open(player, npc, entry.getDialogueId(), null);
            }
        }
        return false;
    }

    public static boolean open(ServerPlayer player, CompanionEntity npc, String dialogueId, @Nullable String nodeId) {
        DialogueGraph graph = DialogueManager.get().get(dialogueId);
        if (graph == null) {
            WHCompanions.LOGGER.warn("Dialogue '{}' not found (npc {})", dialogueId, npc.getUUID());
            return false;
        }
        return sendNode(player, npc, graph, nodeId != null ? nodeId : graph.getStart());
    }

    public static void handleChoice(ServerPlayer player, String dialogueId, String nodeId, int choiceIndex) {
        DialogueSessions.Session session = DialogueSessions.get(player.getUUID());
        if (session == null
                || !session.dialogueId().equals(dialogueId)
                || !session.nodeId().equals(nodeId)) {
            return;
        }
        if (choiceIndex < 0) {
            DialogueSessions.remove(player.getUUID());
            return;
        }
        CompanionEntity npc = resolveNpc(player, session);
        if (npc == null) {
            close(player);
            return;
        }
        DialogueGraph graph = DialogueManager.get().get(dialogueId);
        DialogueNode node = graph != null ? graph.getNode(nodeId) : null;
        if (node == null || choiceIndex >= node.getChoices().size()) {
            close(player);
            return;
        }
        DialogueChoice choice = node.getChoices().get(choiceIndex);
        DialogueCondition.Context ctx = new DialogueCondition.Context(player, npc);

        if (!DialogueCondition.testAll(choice.getConditions(), ctx)) {
            sendNode(player, npc, graph, nodeId);
            return;
        }

        for (DialogueCondition condition : choice.getConditions()) {
            if (condition instanceof ItemsCondition items && items.isConsume() && !items.consumeItems(ctx)) {
                sendNode(player, npc, graph, nodeId);
                return;
            }
        }

        DialogueAction.executeAll(choice.getActions(), ctx);

        if (choice.getNext() == null) {
            close(player);
        } else {
            sendNode(player, npc, graph, choice.getNext());
        }
    }

    public static void close(ServerPlayer player) {
        DialogueSessions.remove(player.getUUID());
        NetworkHandler.sendToPlayer(new DialogueClosePacket(), player);
    }

    private static boolean sendNode(ServerPlayer player, CompanionEntity npc, DialogueGraph graph, String nodeId) {
        DialogueCondition.Context ctx = new DialogueCondition.Context(player, npc);

        DialogueNode node = graph.getNode(nodeId);
        int guard = 0;
        while (node != null && ("check".equals(node.getType()) || "random".equals(node.getType()))) {
            if (++guard > 32) {
                WHCompanions.LOGGER.warn("Dialogue '{}': redirect loop near '{}'", graph.getId(), nodeId);
                close(player);
                return false;
            }
            String next = "check".equals(node.getType())
                    ? routeCheck(node, ctx) : routeRandom(node, player.getRandom());
            if (next == null || next.isEmpty()) {
                close(player);
                return true;
            }
            nodeId = next;
            node = graph.getNode(nodeId);
        }
        if (node == null) {
            WHCompanions.LOGGER.warn("Dialogue '{}': node '{}' not found", graph.getId(), nodeId);
            close(player);
            return false;
        }
        PlayerStateManager.get(player.server).markVisited(player.getUUID(), graph.getId(), nodeId);

        List<String> pages = new ArrayList<>(node.getPages().size());
        if (node.isRandomPage() && node.getPages().size() > 1) {

            String page = node.getPages().get(player.getRandom().nextInt(node.getPages().size()));
            pages.add(Placeholders.apply(page, ctx));
        } else {
            node.getPages().forEach(page -> pages.add(Placeholders.apply(page, ctx)));
        }

        java.util.Set<String> annIds = new java.util.LinkedHashSet<>();
        pages.forEach(p -> collectAnnotationIds(p, annIds));
        List<DialogueNodeData.Annotation> annotations = new ArrayList<>();
        for (String id : annIds) {
            var term = com.withouthonor.npcs.common.glossary.GlossaryManager.get().byId(id);
            if (term != null) {
                annotations.add(new DialogueNodeData.Annotation(id, term.getTitle(), term.getBody()));
            }
        }

        boolean inputMode = "input".equals(node.getType());
        String inputHint = inputMode ? Placeholders.apply(node.getInputHint(), ctx) : "";
        List<DialogueNodeData.ChoiceData> choices = new ArrayList<>();
        if (!inputMode) {
            for (int i = 0; i < node.getChoices().size(); i++) {
                DialogueChoice choice = node.getChoices().get(i);
                boolean ok = DialogueCondition.testAll(choice.getConditions(), ctx);
                if (ok) {
                    choices.add(new DialogueNodeData.ChoiceData(i, Placeholders.apply(choice.getText(), ctx), false, ""));
                } else if (choice.getLockedHint() != null) {
                    choices.add(new DialogueNodeData.ChoiceData(i, Placeholders.apply(choice.getText(), ctx),
                            true, Placeholders.apply(choice.getLockedHint(), ctx)));
                }
            }
        }

        String npcTitle = "";
        String npcNameColor = "";
        String voiceSound = "";
        float voicePitch = 1.0F;
        String npcPortrait = "";
        boolean npcPortraitShow = false;
        String factionId = null;
        if (npc.getProfileId() != null) {
            CompanionProfile profile = ProfileManager.get().get(npc.getProfileId());
            if (profile != null) {
                npcTitle = profile.getTitle();
                npcNameColor = profile.getNameColor() != null ? profile.getNameColor() : "";
                voiceSound = profile.getVoiceSound() != null ? profile.getVoiceSound() : "";
                voicePitch = profile.getVoicePitch();
                npcPortrait = profile.getPortraitImage();
                npcPortraitShow = profile.isPortraitShow();
                factionId = profile.getFaction();
            }
        }
        List<DialogueNodeData.ImageData> images = new ArrayList<>(node.getImages().size());
        node.getImages().forEach(img -> images.add(new DialogueNodeData.ImageData(
                img.file(), Placeholders.apply(img.caption(), ctx))));

        String secondCharSkin = "";
        String secondCharName = "";
        if (!node.getSecondCharPreset().isEmpty()) {
            CompanionProfile sc = com.withouthonor.npcs.network.ProfileSharePackets
                    .loadExportProfile(player.server, node.getSecondCharPreset());
            if (sc != null) {
                secondCharSkin = sc.getSkinPlayerName() != null ? sc.getSkinPlayerName() : "";
                secondCharName = sc.getName();
            }
        }

        DialogueSessions.put(player.getUUID(), new DialogueSessions.Session(npc.getUUID(), graph.getId(), nodeId));
        NetworkHandler.sendToPlayer(new OpenDialogueNodePacket(new DialogueNodeData(
                npc.getId(), npc.getName().getString(), npcNameColor, npcTitle,
                voiceSound, voicePitch,
                npcPortrait, npcPortraitShow,
                secondCharSkin, secondCharName,
                node.getSecondCharPortrait(), node.isSecondCharPortraitShow(), node.isSecondCharNameShow(),
                node.getMusicDisc(), node.getMusicUrl(), node.getMusicTitle(), node.isMusicLoop(),
                graph.getId(), nodeId, pages, choices, images,
                reputationOf(player, factionId), annotations, npc.isFollowingPlayer(player),
                inputMode, inputHint)), player);
        return true;
    }

    private static String routeCheck(DialogueNode node, DialogueCondition.Context ctx) {
        boolean ok = DialogueCondition.testAll(node.getCheckConditions(), ctx)
                && ctx.player().getRandom().nextInt(100) < node.getCheckChance();
        return ok ? node.getCheckSuccessNext() : node.getCheckFailNext();
    }

    private static String routeRandom(DialogueNode node, net.minecraft.util.RandomSource random) {
        int total = 0;
        for (DialogueNode.RandomOption opt : node.getRandomOptions()) {
            total += Math.max(0, opt.weight());
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (DialogueNode.RandomOption opt : node.getRandomOptions()) {
            roll -= Math.max(0, opt.weight());
            if (roll < 0) {
                return opt.next();
            }
        }
        return null;
    }

    public static void handleInput(ServerPlayer player, String dialogueId, String nodeId, String text) {
        DialogueSessions.Session session = DialogueSessions.get(player.getUUID());
        if (session == null
                || !session.dialogueId().equals(dialogueId)
                || !session.nodeId().equals(nodeId)) {
            return;
        }
        CompanionEntity npc = resolveNpc(player, session);
        if (npc == null) {
            close(player);
            return;
        }
        DialogueGraph graph = DialogueManager.get().get(dialogueId);
        DialogueNode node = graph != null ? graph.getNode(nodeId) : null;
        if (node == null || !"input".equals(node.getType())) {
            close(player);
            return;
        }
        String input = text == null ? "" : text.trim();
        if (input.length() > 256) {
            input = input.substring(0, 256);
        }
        DialogueCondition.Context ctx = new DialogueCondition.Context(player, npc);
        if (!node.getInputStoreVar().isEmpty()) {
            PlayerStateManager.get(player.server).setVar(player.getUUID(), node.getInputStoreVar(), input);
        }

        for (DialogueChoice choice : node.getChoices()) {
            if (!DialogueCondition.testAll(choice.getConditions(), ctx)) {
                continue;
            }
            if (input.equalsIgnoreCase(choice.getText() == null ? "" : choice.getText().trim())) {
                DialogueAction.executeAll(choice.getActions(), ctx);
                if (choice.getNext() == null) {
                    close(player);
                } else {
                    sendNode(player, npc, graph, choice.getNext());
                }
                return;
            }
        }
        String fallback = node.getInputFallbackNext();
        if (fallback.isEmpty()) {
            close(player);
        } else {
            sendNode(player, npc, graph, fallback);
        }
    }

    @Nullable
    private static DialogueNodeData.ReputationData reputationOf(ServerPlayer player, @Nullable String factionId) {
        if (factionId == null) {
            return null;
        }
        var faction = com.withouthonor.npcs.common.reputation.FactionRegistry.get().byId(factionId);
        if (faction == null) {
            return null;
        }
        int value = com.withouthonor.npcs.common.storage.PlayerStateManager.get(player.server)
                .getReputation(player.getUUID(), faction.getId());
        var tier = faction.tierFor(value);
        var tiers = faction.getTiers();
        int index = tiers.indexOf(tier);
        float progress = 1.0F;
        if (index < tiers.size() - 1) {
            int hi = tiers.get(index + 1).min();

            int lo = tier.min() == Integer.MIN_VALUE ? hi - 40 : tier.min();
            progress = hi > lo ? Math.max(0.0F, Math.min(1.0F, (value - lo) / (float) (hi - lo))) : 1.0F;
        }
        return new DialogueNodeData.ReputationData(
                faction.getName(), faction.getColor(), value, tier.name(), tier.color(), progress);
    }

    @Nullable
    private static CompanionEntity resolveNpc(ServerPlayer player, DialogueSessions.Session session) {
        Entity entity = player.serverLevel().getEntity(session.npcUuid());
        if (entity instanceof CompanionEntity npc && player.distanceToSqr(npc) <= MAX_DISTANCE_SQR) {
            return npc;
        }
        return null;
    }

    public static String stripAnnotations(String text) {
        if (text.indexOf('§') < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char n = text.charAt(i + 1);
                if (n == '{') {
                    int close = text.indexOf('}', i + 2);
                    if (close >= 0) {
                        i = close;
                        continue;
                    }
                } else if (n == '}') {
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    static void collectAnnotationIds(String text, java.util.Set<String> ids) {
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) == '§' && text.charAt(i + 1) == '{') {
                int close = text.indexOf('}', i + 2);
                if (close >= 0) {
                    ids.add(text.substring(i + 2, close));
                    i = close;
                }
            }
        }
    }
}
