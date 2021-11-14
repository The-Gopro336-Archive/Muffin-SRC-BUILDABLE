package me.han.muffin.client.mixin.mixins.misc;

import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MovementInputFromOptions.class, priority = 10000)
    public abstract class MixinMovementInputFromOptions extends MovementInput {

/*
    @Inject(method = "updatePlayerMoveState", at = @At("RETURN"))
    public void updatePlayerMoveStateReturn(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new PlayerUpdateMoveStateEvent(MovementInputFromOptions.class.cast(this)));
    }
 */


  //  @Redirect(method = "updatePlayerMoveState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z"))
  //  public boolean isKeyPressed(KeyBinding keyBinding) {
  //      return keyBinding.isKeyDown();
  //  }


    // We have to cancel this so original player doesn't move around, also reset movement input when we enable Freecam
    @Inject(method = "updatePlayerMoveState", at = @At("HEAD"), cancellable = true)
    public void onUpdatePlayerMoveStatePre(CallbackInfo ci) {
        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() != null) ci.cancel();
        if (FreecamModule.INSTANCE.getResetInput()) {
            this.moveForward = 0f;
            this.moveStrafe = 0f;
            this.forwardKeyDown = false;
            this.backKeyDown = false;
            this.leftKeyDown = false;
            this.rightKeyDown = false;
            this.jump = false;
            this.sneak = false;
            FreecamModule.INSTANCE.setResetInput(false);
        }
    }


}