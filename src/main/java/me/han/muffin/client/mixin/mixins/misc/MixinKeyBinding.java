package me.han.muffin.client.mixin.mixins.misc;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyBinding.class)
public abstract class MixinKeyBinding {
    /*
    @Shadow
    private boolean pressed;

    @Inject(
            method = "isKeyDown",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onIsKeyDown(CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        cir.setReturnValue(pressed);
    }
     */

    @Shadow private boolean pressed;

    @Inject(method = "isKeyDown", at = @At(value = "RETURN"), cancellable = true)
    private void onIsKeyDownPost(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            cir.setReturnValue(pressed);
        }
    }

}