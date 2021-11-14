package me.han.muffin.client.mixin.mixins.misc;

import me.han.muffin.client.imixin.misc.ISoundHandler;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = SoundHandler.class)
public abstract class MixinSoundHandler implements ISoundHandler {
    @Nonnull
    @Override
    @Accessor(value = "sndManager")
    public abstract SoundManager getSoundManager();
}
