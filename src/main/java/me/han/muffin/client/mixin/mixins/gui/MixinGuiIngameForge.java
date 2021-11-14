package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.render.overlay.RenderPortalIconEvent;
import me.han.muffin.client.event.events.render.overlay.RenderPotionIconsEvent;
import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngameForge.class, remap = false)
public class MixinGuiIngameForge {

    @Inject(method = "renderPotionIcons", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderPotionIcons(ScaledResolution sr, CallbackInfo ci) {
        RenderPotionIconsEvent event = new RenderPotionIconsEvent(sr);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderPortal", at = @At("HEAD"), cancellable = true)
    private void onRenderPortal(ScaledResolution res, float partialTicks, CallbackInfo ci) {
        final RenderPortalIconEvent event = new RenderPortalIconEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @ModifyVariable(method = "renderAir", at = @At(value = "STORE", ordinal = 0))
    private EntityPlayer onRenderAirViewEntity(EntityPlayer renderViewEntity) {
        return gerRenderViewEntity(renderViewEntity);
    }

    @ModifyVariable(method = "renderHealth", at = @At(value = "STORE", ordinal = 0))
    private EntityPlayer onRenderHealthViewEntity(EntityPlayer renderViewEntity) {
        return gerRenderViewEntity(renderViewEntity);
    }

    @ModifyVariable(method = "renderFood", at = @At(value = "STORE", ordinal = 0))
    private EntityPlayer onRenderFoodViewEntity(EntityPlayer renderViewEntity) {
        return gerRenderViewEntity(renderViewEntity);
    }

    @ModifyVariable(method = "renderHealthMount", at = @At(value = "STORE", ordinal = 0))
    private EntityPlayer onRenderHealthMountViewEntity(EntityPlayer renderViewEntity) {
        return gerRenderViewEntity(renderViewEntity);
    }

    private EntityPlayer gerRenderViewEntity(EntityPlayer renderViewEntity) {
        EntityPlayer player = Globals.mc.player;
        if (FreecamModule.INSTANCE.isEnabled() && player != null) {
            return player;
        } else {
            return renderViewEntity;
        }
    }

}