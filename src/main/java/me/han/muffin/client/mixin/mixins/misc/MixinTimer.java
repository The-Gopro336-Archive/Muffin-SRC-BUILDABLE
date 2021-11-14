package me.han.muffin.client.mixin.mixins.misc;

import me.han.muffin.client.imixin.ITimer;
import me.han.muffin.client.manager.managers.TimerManager;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Timer.class)
public abstract class MixinTimer implements ITimer {
    @Shadow public float elapsedPartialTicks;

    @Override
    @Accessor(value = "tickLength")
    public abstract void setTickLength(float tickLength);

    @Override
    @Accessor(value = "tickLength")
    public abstract float getTickLength();

    @Override
    @Accessor(value = "lastSyncSysClock")
    public abstract long getLastSyncSysClock();

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onInitTimerPost(CallbackInfo ci) {
        TimerManager.INSTANCE.setTimer(1.0F);
    }

    @Inject(method = "updateTimer", at = @At(value = "FIELD", target = "Lnet/minecraft/util/Timer;elapsedPartialTicks:F", ordinal = 1))
    private void onUpdateTimerTicks(CallbackInfo ci) {
        elapsedPartialTicks *= TimerManager.getTimerSpeed();
    }

}