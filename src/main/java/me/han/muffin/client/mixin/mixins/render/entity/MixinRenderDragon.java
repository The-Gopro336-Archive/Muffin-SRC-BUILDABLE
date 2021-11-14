package me.han.muffin.client.mixin.mixins.render.entity;

import me.han.muffin.client.module.modules.render.EntityESPModule;
import net.minecraft.client.renderer.entity.RenderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderDragon.class)
public class MixinRenderDragon {

    @Inject(method = "renderCrystalBeams", at = @At(value = "HEAD"), cancellable = true)
    private static void onRenderCrystalBeamsPre(double p_188325_0_, double p_188325_2_, double p_188325_4_, float p_188325_6_, double p_188325_7_, double p_188325_9_, double p_188325_11_, int p_188325_13_, double p_188325_14_, double p_188325_16_, double p_188325_18_, CallbackInfo ci) {
        if (!EntityESPModule.renderNameTags) ci.cancel();
    }

}