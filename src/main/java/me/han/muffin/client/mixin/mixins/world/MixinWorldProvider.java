package me.han.muffin.client.mixin.mixins.world;

import me.han.muffin.client.imixin.world.IWorldProvider;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = WorldProvider.class)
public abstract class MixinWorldProvider implements IWorldProvider {

    @Nonnull
    @Override
    @Accessor(value = "lightBrightnessTable")
    public abstract float[] getLightBrightnessTable();

    @Override
    @Accessor(value = "lightBrightnessTable")
    public abstract void setLightBrightnessTable(@Nonnull float[] lightBrightnessTable);

}