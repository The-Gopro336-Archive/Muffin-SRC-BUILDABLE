package me.han.muffin.client.mixin.mixins.render.entity.living;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.RenderLeashEvent;
import net.minecraft.client.renderer.entity.RenderLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderLiving.class)
public abstract class MixinRenderLiving {

    @Inject(method = "renderLeash", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderLeashPre(CallbackInfo ci) {
        final RenderLeashEvent event = new RenderLeashEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

}