package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.RenderPutColorMultiplierEvent;
import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = BufferBuilder.class)
public abstract class MixinBufferBuilder {

    /*
    @Redirect(method = "putColorMultiplier", at = @At(value = "INVOKE", target = "Ljava/nio/IntBuffer;put(II)Ljava/nio/IntBuffer;", remap = false))
    private IntBuffer onPutColorMultiplier(IntBuffer intBuffer, int n, int oldAlpha) {
        RenderPutColorMultiplierEvent event = new RenderPutColorMultiplierEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) oldAlpha = ((int) event.getOpacity()) << 24 | oldAlpha & 0xFFFFFF;
        return intBuffer.put(n, oldAlpha);
    }
     */

    @ModifyArg(method = "putColorMultiplier", at = @At(value = "INVOKE", target = "Ljava/nio/IntBuffer;put(II)Ljava/nio/IntBuffer;", remap = false), index = 1)
    private int onPutColourMultiplier(int oldAlpha) {
        RenderPutColorMultiplierEvent event = new RenderPutColorMultiplierEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) oldAlpha = ((int) event.getOpacity()) << 24 | oldAlpha & 0xFFFFFF;
        return oldAlpha;
    }

}