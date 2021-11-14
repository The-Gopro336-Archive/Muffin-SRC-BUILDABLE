package me.han.muffin.client.mixin.mixins.optifine;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "Config", remap = false)
public abstract class MixinConfig {

    @Inject(method = "isFastRender", at = @At("HEAD"), cancellable = true, remap = false)
    @Dynamic
    private static void onIsFastRenderPre(CallbackInfoReturnable<Boolean> isFastRender) {
        isFastRender.setReturnValue(false);
    }

}