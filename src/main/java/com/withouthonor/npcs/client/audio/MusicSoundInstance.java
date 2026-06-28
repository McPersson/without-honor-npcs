package com.withouthonor.npcs.client.audio;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class MusicSoundInstance extends AbstractSoundInstance {

    public MusicSoundInstance(SoundEvent sound, SoundSource source, float volume, boolean loop) {
        super(sound, source, RandomSource.create());
        this.volume = volume;
        this.pitch = 1.0F;
        this.looping = loop;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0.0D;
        this.y = 0.0D;
        this.z = 0.0D;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
