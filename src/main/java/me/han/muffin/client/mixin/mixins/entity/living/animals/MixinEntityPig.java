package me.han.muffin.client.mixin.mixins.entity.living.animals;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.entity.HorseSaddledEvent;
import me.han.muffin.client.event.events.entity.PigTravelEvent;
import me.han.muffin.client.event.events.entity.SteerEntityEvent;
import net.minecraft.entity.passive.EntityPig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityPig.class)
public abstract class MixinEntityPig {

    @Inject(method = "canBeSteered", at = @At("HEAD"), cancellable = true)
    public void onCanBeSteeredPre(CallbackInfoReturnable<Boolean> cir) {
        SteerEntityEvent event = new SteerEntityEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getSaddled", at = @At("HEAD"), cancellable = true)
    public void onGetSaddledPre(CallbackInfoReturnable<Boolean> cir) {
        HorseSaddledEvent event = new HorseSaddledEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void onTravelPre(float strafe, float vertical, float forward, CallbackInfo ci) {
        final PigTravelEvent event = new PigTravelEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

}