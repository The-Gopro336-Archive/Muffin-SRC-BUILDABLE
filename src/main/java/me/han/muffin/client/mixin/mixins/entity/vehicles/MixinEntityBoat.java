package me.han.muffin.client.mixin.mixins.entity.vehicles;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.mixin.mixins.entity.MixinEntity;
import me.han.muffin.client.module.modules.movement.BoatFlyModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityBoat.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityBoat extends MixinEntity {

 //   @Shadow public abstract double getMountedYOffset();

    @ModifyVariable(method = "updateMotion", ordinal = 1, at = @At(value = "STORE", ordinal = 0))
    private double onUpdateMotion2(double old) {
        return BoatFlyModule.INSTANCE.isEnabled() && (BoatFlyModule.INSTANCE.getAntiKick().getValue() || BoatFlyModule.INSTANCE.getGlideSpeed().getValue() == 0) ? 0.0 : old;
    }

  //  @Redirect(method = "updateMotion", at = @At(value = "INVOKE", target = "net/minecraft/entity/item/EntityBoat.hasNoGravity()Z"))
  //  private boolean onUpdateMotion(EntityBoat boat) {
  //      return hasNoGravity() || BoatFlyModule.INSTANCE != null && BoatFlyModule.INSTANCE.isEnabled() && (BoatFlyModule.INSTANCE.getAntiKick().getValue() || BoatFlyModule.INSTANCE.getGlideSpeed().getValue() == 0);
  //  }

    @Inject(method = "applyOrientationToEntity", at = @At(value = "HEAD"), cancellable = true)
    private void onApplyOrientationToEntityPre(Entity passenger, CallbackInfo ci) {
        if (Globals.mc.player != null && Globals.mc.player.isRiding() && Globals.mc.player == passenger && BoatFlyModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

/*
    @Inject(method = "updateMotion", at = @At(value = "HEAD"), cancellable = true)
    private void updateMotion(CallbackInfo ci) {
        if (Globals.mc.player != null && BoatFlyModule.INSTANCE != null && BoatFlyModule.INSTANCE.isEnabled() && Globals.mc.player.isRiding() && (EntityBoat) (Object) this == Globals.mc.player.getRidingEntity()) {
           // BoatFlyModule.INSTANCE.moveEntity((EntityBoat) (Object) this);
      //      ci.cancel();
        }
    }


    @Inject(method = "applyOrientationToEntity", at = @At(value = "HEAD"), cancellable = true)
    private void applyOrientationToEntity(Entity passenger, CallbackInfo ci) {
        if (Globals.mc.player != null && Globals.mc.player.isRiding() && Globals.mc.player == passenger && BoatFlyModule.INSTANCE != null && BoatFlyModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
 */

    /*
    @Inject(method = "controlBoat", at = @At(value = "HEAD"), cancellable = true)
    private void controlBoat(CallbackInfo ci) {
        if (Globals.mc.player != null && BoatFlyModule.INSTANCE != null && BoatFlyModule.INSTANCE.isEnabled() && Globals.mc.player.isRiding() && (EntityBoat) (Object) this == Globals.mc.player.getRidingEntity()) {
            ci.cancel();
        }
    }


    @Inject(method = "updatePassenger", at = @At(value = "HEAD"), cancellable = true)
    private void updatePassenger(Entity passenger, CallbackInfo ci) {
        if (Globals.mc.player != null && Globals.mc.player.isRiding() && Globals.mc.player == passenger && BoatFlyModule.INSTANCE != null && BoatFlyModule.INSTANCE.isEnabled()) {
            ci.cancel();
            float f1 = (float)((this.isDead ? (double)0.01f : this.getMountedYOffset()) + passenger.getYOffset());
            Vec3d vec3d = new Vec3d(0.0, 0.0, 0.0).rotateYaw(-this.rotationYaw * ((float)Math.PI / 180) - 1.5707964f);
            passenger.setPosition(this.posX + vec3d.x, this.posY + (double)f1, this.posZ + vec3d.z);
        }
    }

     */

}