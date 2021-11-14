package me.han.muffin.client.mixin.mixins.render.entity;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.module.modules.render.EntityESPModule;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelPlayer.class)
public class MixinModelPlayer {

    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    public void onSetRotationAnglesPost(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        if (EntityESPModule.INSTANCE.isEnabled() && EntityESPModule.INSTANCE.getSkeletons().getValue() && Globals.mc.world != null && Globals.mc.player != null && entityIn instanceof EntityPlayer) {
            EntityESPModule.addEntity((EntityPlayer) entityIn, (ModelPlayer) (Object) this);
        }
    }

}