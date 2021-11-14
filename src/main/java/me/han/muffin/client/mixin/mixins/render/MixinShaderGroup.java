package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.imixin.render.IShaderGroup;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;
import java.util.List;

@Mixin(value = ShaderGroup.class)
public abstract class MixinShaderGroup implements IShaderGroup {

    @Nonnull
    @Override
    @Accessor(value = "listShaders")
    public abstract List<Shader> getShaders();

    @Nonnull
    @Override
    @Accessor(value = "mainFramebuffer")
    public abstract Framebuffer getMainFramebuffer();

}