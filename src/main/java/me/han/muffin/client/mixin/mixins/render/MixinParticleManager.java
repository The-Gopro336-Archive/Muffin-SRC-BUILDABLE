package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.module.modules.render.NoRenderModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleManager.class)
public abstract class MixinParticleManager {

    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
    public void onAddEffectPre(Particle effect, CallbackInfo ci) {
        if (NoRenderModule.INSTANCE.isEnabled() && NoRenderModule.INSTANCE.handleParticle(effect)) {
            ci.cancel();
        }
    }

}
