package me.han.muffin.client.mixin.mixins.entity.living.animals;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.entity.HorseSaddledEvent;
import me.han.muffin.client.event.events.entity.SteerEntityEvent;
import me.han.muffin.client.mixin.mixins.entity.living.MixinEntityLivingBase;
import net.minecraft.entity.passive.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractHorse.class)
public abstract class MixinAbstractHorse extends MixinEntityLivingBase {

    @Inject(method = "canBeSteered", at = @At("HEAD"), cancellable = true)
    public void onCanBeSteeredPre(CallbackInfoReturnable<Boolean> cir) {
        SteerEntityEvent event = new SteerEntityEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isHorseSaddled", at = @At("HEAD"), cancellable = true)
    public void onIsHorseSaddledPre(CallbackInfoReturnable<Boolean> cir) {
        HorseSaddledEvent event = new HorseSaddledEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(true);
        }
    }

}
