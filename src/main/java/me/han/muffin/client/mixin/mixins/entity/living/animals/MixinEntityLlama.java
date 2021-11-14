package me.han.muffin.client.mixin.mixins.entity.living.animals;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.entity.SteerEntityEvent;
import net.minecraft.entity.passive.EntityLlama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityLlama.class)
public abstract class MixinEntityLlama extends MixinAbstractHorse {

    @Inject(method = "canBeSteered", at = @At("HEAD"), cancellable = true)
    public void onCanBeSteeredPre(CallbackInfoReturnable<Boolean> cir) {
        SteerEntityEvent event = new SteerEntityEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(true);
        }
    }

}
