package me.han.muffin.client.mixin.mixins.render.client;

import me.han.muffin.client.imixin.render.entity.IEntityRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = EntityRenderer.class)
public abstract class MixinEntityRendererVoid implements IEntityRenderer {

    @Override
    @Invoker(value = "orientCamera")
    public abstract void orientCameraVoid(float partialTicks);

    @Override
    @Invoker(value = "setupCameraTransform")
    public abstract void setupCameraTransformVoid(float partialTicks, int pass);

    @Override
    @Invoker(value = "renderWorldPass")
    public abstract void renderWorldPassVoid(int pass, float partialTicks, long finishTimeNano);

}
