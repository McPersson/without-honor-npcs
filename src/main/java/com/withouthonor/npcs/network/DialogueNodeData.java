package com.withouthonor.npcs.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record DialogueNodeData(int npcEntityId, String npcName, String npcNameColor, String npcTitle,
                               String voiceSound, float voicePitch,
                               String npcPortrait, boolean npcPortraitShow,
                               String secondCharSkin, String secondCharName,
                               String secondCharPortrait, boolean secondCharPortraitShow,
                               boolean secondCharNameShow,
                               String musicDisc, String musicUrl, String musicTitle, boolean musicLoop,
                               String dialogueId, String nodeId,
                               List<String> pages, List<ChoiceData> choices, List<ImageData> images,
                               @javax.annotation.Nullable ReputationData reputation,
                               List<Annotation> annotations, boolean followingViewer,
                               boolean inputMode, String inputHint) {

    public record ChoiceData(int index, String text, boolean locked, String hint) {
    }

    public record Annotation(String id, String title, String body) {
    }

    public record ImageData(String file, String caption) {
    }

    public record ReputationData(String factionName, int factionColor, int value,
                                 String tierName, int tierColor, float progress) {

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(factionName, 48);
            buf.writeInt(factionColor);
            buf.writeVarInt(value);
            buf.writeUtf(tierName, 48);
            buf.writeInt(tierColor);
            buf.writeFloat(progress);
        }

        public static ReputationData read(FriendlyByteBuf buf) {
            return new ReputationData(buf.readUtf(48), buf.readInt(), buf.readVarInt(),
                    buf.readUtf(48), buf.readInt(), buf.readFloat());
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(npcEntityId);
        buf.writeUtf(npcName);
        buf.writeUtf(npcNameColor);
        buf.writeUtf(npcTitle);
        buf.writeUtf(voiceSound, 128);
        buf.writeFloat(voicePitch);
        buf.writeUtf(npcPortrait, 80);
        buf.writeBoolean(npcPortraitShow);
        buf.writeUtf(secondCharSkin, 128);
        buf.writeUtf(secondCharName, 64);
        buf.writeUtf(secondCharPortrait, 80);
        buf.writeBoolean(secondCharPortraitShow);
        buf.writeBoolean(secondCharNameShow);
        buf.writeUtf(musicDisc, 80);
        buf.writeUtf(musicUrl, 256);
        buf.writeUtf(musicTitle, 128);
        buf.writeBoolean(musicLoop);
        buf.writeUtf(dialogueId);
        buf.writeUtf(nodeId);
        buf.writeCollection(pages, FriendlyByteBuf::writeUtf);
        buf.writeCollection(choices, (b, c) -> {
            b.writeVarInt(c.index());
            b.writeUtf(c.text());
            b.writeBoolean(c.locked());
            b.writeUtf(c.hint());
        });
        buf.writeCollection(images, (b, i) -> {
            b.writeUtf(i.file(), 80);
            b.writeUtf(i.caption());
        });
        buf.writeBoolean(reputation != null);
        if (reputation != null) {
            reputation.write(buf);
        }
        buf.writeCollection(annotations, (b, a) -> {
            b.writeUtf(a.id(), 64);
            b.writeUtf(a.title(), 128);
            b.writeUtf(a.body(), 2048);
        });
        buf.writeBoolean(followingViewer);
        buf.writeBoolean(inputMode);
        buf.writeUtf(inputHint, 128);
    }

    public static DialogueNodeData read(FriendlyByteBuf buf) {
        return new DialogueNodeData(
                buf.readVarInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(128),
                buf.readFloat(),
                buf.readUtf(80),
                buf.readBoolean(),
                buf.readUtf(128),
                buf.readUtf(64),
                buf.readUtf(80),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(80),
                buf.readUtf(256),
                buf.readUtf(128),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                buf.readCollection(ArrayList::new,
                        b -> new ChoiceData(b.readVarInt(), b.readUtf(), b.readBoolean(), b.readUtf())),
                buf.readCollection(ArrayList::new,
                        b -> new ImageData(b.readUtf(80), b.readUtf())),
                buf.readBoolean() ? ReputationData.read(buf) : null,
                buf.readCollection(ArrayList::new,
                        b -> new Annotation(b.readUtf(64), b.readUtf(128), b.readUtf(2048))),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(128));
    }
}
