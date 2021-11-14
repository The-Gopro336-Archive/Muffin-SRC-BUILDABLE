package me.han.muffin.client.mixin.mixins.entity.living.player;

import com.mojang.authlib.GameProfile;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.client.TravelEvent;
import me.han.muffin.client.event.events.entity.PortalCooldownEvent;
import me.han.muffin.client.event.events.entity.player.PlayerApplyCollisionEvent;
import me.han.muffin.client.event.events.entity.player.PlayerPushEvent;
import me.han.muffin.client.event.events.entity.player.PlayerSlowEvent;
import me.han.muffin.client.imixin.entity.IEntityPlayer;
import me.han.muffin.client.mixin.mixins.entity.living.MixinEntityLivingBase;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityPlayer.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase implements IEntityPlayer {

    @Shadow public abstract GameProfile getGameProfile();
    @Shadow public abstract float getAbsorptionAmount();

    @Override
    @Accessor(value = "speedInAir")
    public abstract float getSpeedInAir();

    @Override
    @Accessor(value = "speedInAir")
    public abstract void setSpeedInAir(float speed);

    private PlayerSlowEvent.Attack event;
    private boolean shouldIgnoreAbsorption;

    @Inject(method = "getAbsorptionAmount", at = @At(value = "HEAD"), cancellable = true)
    private void onGetAbsorptionAmountPre(CallbackInfoReturnable<Float> cir) {
        if (!(shouldIgnoreAbsorption ^= true)) return;
        try {
            cir.setReturnValue(getAbsorptionAmount());
        } catch (ClassCastException e) {
            cir.setReturnValue(0.0F);
        }
    }

    @Redirect(method = "getEyeHeight", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isSneaking()Z"))
    private boolean onRedirectEyeHeightSneaking(EntityPlayer player) {
        return false;
    }

    @Inject(method = "isWearing", at = @At(value = "HEAD"), cancellable = true)
    private void onAlwaysWearingMenuPre(EnumPlayerModelParts part, CallbackInfoReturnable<Boolean> cir) {
        if (Globals.mc.currentScreen instanceof GuiMainMenu) {
            cir.setReturnValue(true);
        }
    }

    @ModifyConstant(method = "attackTargetEntityWithCurrentItem", constant = @Constant(doubleValue = 0.6))
    private double onModifyAttackSlow(double factor) {
        Muffin.getInstance().getEventManager().dispatchEvent(event = new PlayerSlowEvent.Attack(factor));
        if (event.isCanceled()) return 1.0;
        return event.getFactor();
    }

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"), cancellable = true)
    private void onInjectSprintingAttack(CallbackInfo ci) {
        if (event.isSprinting() || event.isCanceled()) ci.cancel();
    }

    @Inject(method = "applyEntityCollision", at = @At("HEAD"), cancellable = true)
    public void onApplyEntityCollisionPre(Entity entity, CallbackInfo ci) {
        PlayerApplyCollisionEvent event = new PlayerApplyCollisionEvent(entity);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void onTravelPre(float strafe, float vertical, float forward, CallbackInfo ci) {
        //noinspection ConstantConditions
        if (EntityPlayerSP.class.isAssignableFrom(this.getClass())) {
            TravelEvent event = new TravelEvent(strafe, vertical, forward);
            Muffin.getInstance().getEventManager().dispatchEvent(event);
            if (event.isCanceled()) {
                super.move(MoverType.SELF, motionX, motionY, motionZ);
                ci.cancel();
            }
        }
    }

    @Inject(method = "isPushedByWater", at = @At("HEAD"), cancellable = true)
    private void onIsPushedByWaterPre(CallbackInfoReturnable<Boolean> ciR) {
        PlayerPushEvent event = new PlayerPushEvent(PlayerPushEvent.Type.LIQUID);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ciR.setReturnValue(false);
    }

    @ModifyConstant(method = "getPortalCooldown", constant = @Constant(intValue = 10))
    private int onGetPortalCooldownHookPre(final int cooldown) {
        int time = cooldown;
        PortalCooldownEvent event = new PortalCooldownEvent(time);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) time = event.getCooldown();
        return time;
    }

}