package me.han.muffin.client.mixin.mixins.gui;


import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.RenderVignetteEvent;
import me.han.muffin.client.event.events.render.overlay.RenderPortalIconEvent;
import me.han.muffin.client.event.events.render.overlay.RenderPotionEffectsEvent;
import me.han.muffin.client.event.events.render.overlay.RenderPumpkinEvent;
import me.han.muffin.client.module.modules.render.CrosshairModule;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngame.class)
public class MixinGuiIngame {

    @Inject(
            method = "renderAttackIndicator",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onRenderCrosshair(CallbackInfo ci) {
        if (CrosshairModule.INSTANCE != null && CrosshairModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderPotionEffects",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onRenderPotionEffects(CallbackInfo ci) {
        RenderPotionEffectsEvent event = new RenderPotionEffectsEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(
            method = "renderVignette",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onRenderVignettePre(CallbackInfo ci) {
        RenderVignetteEvent event = new RenderVignetteEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(
            method = "renderPumpkinOverlay",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void preRenderPumpkinOverlay(ScaledResolution scaledRes, CallbackInfo ci) {
        RenderPumpkinEvent event = new RenderPumpkinEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderPortal", at = @At("HEAD"), cancellable = true)
    private void onRenderPortal(float timeInPortal, ScaledResolution res, CallbackInfo ci) {
        final RenderPortalIconEvent event = new RenderPortalIconEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

}