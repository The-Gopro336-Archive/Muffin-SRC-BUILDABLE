package me.han.muffin.client.mixin.mixins.world;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.SetupBoundingBoxInFrustumEvent;
import me.han.muffin.client.imixin.render.IFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Frustum.class)
public abstract class MixinFrustum implements IFrustum {

    @Override
    @Accessor(value = "x")
    public abstract double getX();

    @Override
    @Accessor(value = "y")
    public abstract double getY();

    @Override
    @Accessor(value = "z")
    public abstract double getZ();

    @Inject(method = "isBoundingBoxInFrustum", at = @At("HEAD"), cancellable = true)
    public void onIsBoundingBoxEtcPre(AxisAlignedBB ignore, CallbackInfoReturnable<Boolean> cir) {
        SetupBoundingBoxInFrustumEvent event = new SetupBoundingBoxInFrustumEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) cir.setReturnValue(true);
    }

}