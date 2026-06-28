package com.withouthonor.npcs.client.gui;

import com.withouthonor.npcs.client.audio.ClientNpcAudio;
import com.withouthonor.npcs.network.DialogueNodeData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.registries.ForgeRegistries;

public final class ClientDialogue {

    private ClientDialogue() {
    }

    public static void showNode(DialogueNodeData data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DialogueScreen screen) {
            screen.updateNode(data);
        } else {
            minecraft.setScreen(new DialogueScreen(data));
        }
        playVoice(data);
        playMusic(data);
    }

    private static void playVoice(DialogueNodeData data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (data.voiceSound().isEmpty() || minecraft.player == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(data.voiceSound());
        if (id == null) {
            return;
        }

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (sound == null) {
            sound = SoundEvent.createVariableRangeEvent(id);
        }
        float variation = 0.96F + minecraft.player.getRandom().nextFloat() * 0.08F;
        ClientNpcAudio.playVoice(sound, data.voicePitch() * variation);
    }

    private static void playMusic(DialogueNodeData data) {
        com.withouthonor.npcs.compat.EtchedClientBridge etched =
                com.withouthonor.npcs.compat.Compat.etchedClient();
        if (!data.musicUrl().isEmpty() && etched != null) {
            etched.playOnline(data.musicUrl(), data.musicTitle(), data.musicLoop());
            String label = data.musicTitle().isEmpty() ? data.musicUrl() : data.musicTitle();
            ClientNpcAudio.setEtchedMusic("etched:" + data.musicUrl(), label, etched.currentSound());
            return;
        }
        if (data.musicDisc().isEmpty()) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(data.musicDisc());
        Item item = id != null ? ForgeRegistries.ITEMS.getValue(id) : null;
        if (item instanceof RecordItem record) {
            ClientNpcAudio.playMusic(record.getSound(), data.musicDisc(), data.musicLoop());
        }
    }

    public static void closeScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DialogueScreen screen) {
            screen.closeFromServer();
        }
    }
}
