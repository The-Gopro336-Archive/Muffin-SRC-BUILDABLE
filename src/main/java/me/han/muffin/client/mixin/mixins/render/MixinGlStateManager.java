package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.utils.render.GlStateUtils;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlStateManager.class)
public abstract class MixinGlStateManager {

    @Inject(method = "color(FFF)V", at = @At("HEAD"), cancellable = true)
    private static void onColor3FPre(float colorRed, float colorGreen, float colorBlue, CallbackInfo ci) {
        if (GlStateUtils.getColorLock()) ci.cancel();
    }

    @Inject(method = "color(FFFF)V", at = @At("HEAD"), cancellable = true)
    private static void onColor4FPre(float colorRed, float colorGreen, float colorBlue, float colorAlpha, CallbackInfo ci) {
        if (GlStateUtils.getColorLock()) ci.cancel();
    }

    @Inject(method = "loadIdentity", at = @At("HEAD"))
    private static void onLoadIdentityPre(CallbackInfo ci) {
        GlStateUtils.colorLock(false);
    }

}