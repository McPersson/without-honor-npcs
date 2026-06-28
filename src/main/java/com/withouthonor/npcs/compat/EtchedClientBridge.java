package com.withouthonor.npcs.compat;

public interface EtchedClientBridge {

    void playOnline(String url, String title, boolean loop);

    void stopOnline();

    String currentUrl();

    @javax.annotation.Nullable
    net.minecraft.client.resources.sounds.SoundInstance currentSound();
}
