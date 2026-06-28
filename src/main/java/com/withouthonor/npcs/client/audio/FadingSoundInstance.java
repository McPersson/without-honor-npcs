package com.withouthonor.npcs.client.audio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class FadingSoundInstance extends AbstractTickableSoundInstance {

    private boolean fadingOut;
    private boolean fadingIn;
    private float fadeStep = 0.04F;
    private final float baseVolume;
    private boolean muted;

    public FadingSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, boolean loop) {
        super(sound, source, RandomSource.create());
        this.baseVolume = volume;
        this.volume = volume;
        this.pitch = pitch;
        this.looping = loop;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0.0D;
        this.y = 0.0D;
        this.z = 0.0D;
    }

    public void fadeOut(float step) {
        this.fadeStep = step;
        this.fadingOut = true;
    }

    public void fadeIn(float step) {
        this.fadeStep = step;
        this.fadingIn = true;
        this.volume = 0.0F;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (fadingOut) {
            this.volume = Math.max(0.0F, this.volume - fadeStep);
            if (this.volume <= 0.0F) {
                stop();
            }
        } else if (fadingIn) {
            float next = Math.min(baseVolume, this.volume + fadeStep);
            if (next >= baseVolume) {
                this.fadingIn = false;
            }
            this.volume = muted ? 0.0F : next;
        } else {
            this.volume = muted ? 0.0F : baseVolume;
        }
    }
}
