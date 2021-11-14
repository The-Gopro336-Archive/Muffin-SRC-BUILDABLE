package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.module.modules.movement.BoatFlyModule;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelBoat.class)
public abstract class MixinModelBoat {

    @Inject(method = "render", at = @At("HEAD"))
    public void onRenderPre(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (Globals.mc.player.getRidingEntity() == entityIn && BoatFlyModule.INSTANCE.isEnabled()) {
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, BoatFlyModule.INSTANCE.getBoatOpacity().getValue() / 255F);
         //   GlStateManager.enableBlend();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void onRenderPost(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (Globals.mc.player.getRidingEntity() == entityIn && BoatFlyModule.INSTANCE.isEnabled()) {
            GlStateManager.disableBlend();
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBoat;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V"), cancellable = true)
    private void onRenderAngles(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (Globals.mc.player.getRidingEntity() == entityIn && BoatFlyModule.INSTANCE.isEnabled()) {
            double boatScale = BoatFlyModule.INSTANCE.getBoatScale().getValue();
            if (boatScale != 1.0) {
                GlStateManager.scale(boatScale, boatScale, boatScale);
            }
        }
    }

    @Inject(method = "renderMultipass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;colorMask(ZZZZ)V", ordinal = 0))
    private void onRenderMultipassColorMask(Entity entityIn, float partialTicks, float p_187054_3_, float p_187054_4_, float p_187054_5_, float p_187054_6_, float scale, CallbackInfo ci) {
        if (Globals.mc.player.getRidingEntity() == entityIn && BoatFlyModule.INSTANCE.isEnabled()) {
            double boatScale = BoatFlyModule.INSTANCE.getBoatScale().getValue();
            if (boatScale != 1.0) {
                GlStateManager.scale(boatScale, boatScale, boatScale);
            }
        }
    }

}
