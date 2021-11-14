package me.han.muffin.client.mixin.mixins.render.client;

import me.han.muffin.client.imixin.render.IViewFrustum;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nonnull;

@Mixin(value = ViewFrustum.class)
public abstract class MixinViewFrustumVoid implements IViewFrustum {

    @Nonnull
    @Override
    @Invoker(value = "getRenderChunk")
    public abstract RenderChunk getRenderChunkVoid(@Nonnull BlockPos pos);

}