package me.han.muffin.client.mixin.mixins.render.entity;

import me.han.muffin.client.module.modules.render.EntityESPModule;
import net.minecraft.client.renderer.entity.RenderGuardian;
import net.minecraft.entity.monster.EntityGuardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderGuardian.class)
public class MixinRenderGuardian {

    @Inject(method = "doRender", at = @At(value = "INVOKE", target = "net/minecraft/entity/monster/EntityGuardian.getAttackAnimationScale(F)F"), cancellable = true)
    private void onDoRenderAnimationScale(EntityGuardian entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (!EntityESPModule.renderNameTags) ci.cancel();
    }

}