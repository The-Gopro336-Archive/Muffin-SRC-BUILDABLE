package me.han.muffin.client.event.events.world;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.util.SoundEvent;

public class WorldPlaySoundEvent extends EventCancellable {
    private final SoundEvent sound;
    private float volume;
    private float pitch;

    public WorldPlaySoundEvent(SoundEvent sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SoundEvent getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

}
