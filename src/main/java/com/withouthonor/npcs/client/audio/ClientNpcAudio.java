package com.withouthonor.npcs.client.audio;

import com.withouthonor.npcs.compat.Compat;
import com.withouthonor.npcs.compat.EtchedClientBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;

public final class ClientNpcAudio {

    private static final float MUSIC_VOLUME = 0.9F;
    private static final float VOICE_VOLUME = 0.9F;

    private ClientNpcAudio() {
    }

    @Nullable
    private static FadingSoundInstance voice;

    @Nullable
    private static SoundInstance musicSound;
    private static boolean musicIsEtched;
    private static boolean musicMuted;
    private static boolean musicFading;
    private static float musicGain = 1.0F;

    @Nullable
    private static String musicId;
    private static String etchedId = "";
    private static String etchedLabel = "";

    public static void playVoice(SoundEvent sound, float pitch) {
        stopNow(voice);
        voice = new FadingSoundInstance(sound, SoundSource.PLAYERS, VOICE_VOLUME, pitch, false);
        Minecraft.getInstance().getSoundManager().play(voice);
    }

    public static void playMusic(SoundEvent sound, String discItemId, boolean loop) {
        if (discItemId.equals(musicId) && musicSound != null && !musicFading) {
            return;
        }
        stopMusicNow();
        MusicSoundInstance inst = new MusicSoundInstance(sound, SoundSource.RECORDS, MUSIC_VOLUME, loop);
        musicSound = inst;
        musicIsEtched = false;
        musicId = discItemId;
        etchedId = "";
        musicMuted = false;
        musicFading = false;
        musicGain = 1.0F;
        Minecraft.getInstance().getSoundManager().play(inst);
    }

    public static void setEtchedMusic(String id, String label, @Nullable SoundInstance sound) {
        etchedLabel = label == null ? "" : label;
        if (id != null && id.equals(etchedId) && musicSound != null && !musicFading) {
            return;
        }
        stopMusicNow();
        musicSound = sound;
        musicIsEtched = true;
        etchedId = id == null ? "" : id;
        musicId = null;
        musicMuted = false;
        musicFading = false;
        musicGain = 1.0F;
    }

    public static void fadeMusic() {
        if (musicSound != null) {
            musicFading = true;
        }
        musicId = null;
        etchedId = "";
    }

    public static void toggleMusicMute() {
        musicMuted = !musicMuted;
    }

    public static boolean isMusicMuted() {
        return musicMuted;
    }

    public static void fadeAll() {
        if (voice != null) {
            voice.fadeOut(0.10F);
            voice = null;
        }
        fadeMusic();
    }

    public static void tickMusicVolume() {
        if (musicSound == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            stopMusicNow();
            return;
        }
        if (musicMuted && !musicFading) {
            // Полная тишина: ставим сам канал на паузу. Гасить громкостью бесполезно —
            // Etched переписывает её чаще нашего тика, и звук проскальзывает между тиками.
            ClientMusicChannel.setPaused(musicSound, true);
            return;
        }
        ClientMusicChannel.setPaused(musicSound, false);
        float target = musicFading ? 0.0F : 1.0F;
        float step = musicFading ? 0.05F : 0.2F;
        if (musicGain < target) {
            musicGain = Math.min(target, musicGain + step);
        } else if (musicGain > target) {
            musicGain = Math.max(target, musicGain - step);
        }
        float source = mc.options.getSoundSourceVolume(musicSound.getSource());
        float gain = musicGain * musicSound.getVolume() * source;
        ClientMusicChannel.setVolume(musicSound, gain);
        if (musicFading && musicGain <= 0.0F) {
            stopMusicNow();
        }
    }

    @Nullable
    public static String currentMusicId() {
        if (musicId != null) {
            return musicId;
        }
        return etchedId.isEmpty() ? null : etchedId;
    }

    public static String currentMusicLabel() {
        return etchedLabel;
    }

    private static void stopMusicNow() {
        if (musicSound != null) {
            Minecraft.getInstance().getSoundManager().stop(musicSound);
            if (musicIsEtched) {
                EtchedClientBridge etched = Compat.etchedClient();
                if (etched != null) {
                    etched.stopOnline();
                }
            }
        }
        musicSound = null;
        musicIsEtched = false;
        musicFading = false;
        musicGain = 1.0F;
    }

    private static void stopNow(@Nullable FadingSoundInstance inst) {
        if (inst != null) {
            Minecraft.getInstance().getSoundManager().stop(inst);
        }
    }
}
