package me.han.muffin.client.mixin.mixins.entity.living;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.entity.HandleJumpWaterEvent;
import me.han.muffin.client.event.events.entity.living.*;
import me.han.muffin.client.event.events.entity.player.PlayerIsPotionActiveEvent;
import me.han.muffin.client.event.events.entity.player.PlayerIsPotionActivePostEvent;
import me.han.muffin.client.event.events.movement.SmoothElytraEvent;
import me.han.muffin.client.imixin.entity.IEntityLivingBase;
import me.han.muffin.client.mixin.mixins.entity.MixinEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityLivingBase.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityLivingBase extends MixinEntity implements IEntityLivingBase {

    @Override
    public boolean isElytraFlying() {
        return getFlag(7);
    }

    @Shadow
    protected void jump() {}

    @Shadow public float rotationYawHead;
    @Shadow public float renderYawOffset;

    @Shadow public float prevRotationYawHead;

    @Shadow public float prevRenderYawOffset;

    @Shadow
    public void setSprinting(boolean sprinting) {
    }

    @Shadow private int jumpTicks;
    @Shadow protected int ticksSinceLastSwing;
    @Shadow protected int activeItemStackUseCount;

    @Shadow public void onUpdate() {}

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void onHeadLiving(CallbackInfo callbackInfo) {
     //   if (Muffin.getInstance().getModuleManager().getModuleType(NoJumpDelay.class).getState())
        jumpTicks = 0;
    }

  /*
    @Inject(method = "getArmSwingAnimationEnd", at = @At(value = "HEAD"), cancellable = true)
    private void onGetArmSwingAnimationEnd(CallbackInfoReturnable<Integer> cir) {
        EntityLivingBase _this = (EntityLivingBase) (Object) this;
        if (_this == Globals.mc.player) {
            int niga = cir.getReturnValue();
            cir.cancel();
            cir.setReturnValue((int) (niga * ItemRender.INSTANCE.getSwingSpeedValue().getValue()));
        }
    }
   */


    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeathPre(DamageSource cause, CallbackInfo ci) {
        if ((EntityLivingBase) (Object) this instanceof EntityPlayer) Muffin.getInstance().getEventManager().dispatchEvent(new EntityDeathEvent((EntityLivingBase) (Object) this, cause));
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void onPreJump(CallbackInfo ci) {
        EntityJumpEvent event = new EntityJumpEvent(EventStageable.EventStage.PRE, (EntityLivingBase) (Object) this);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void onPostJump(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new EntityJumpEvent(EventStageable.EventStage.POST, (EntityLivingBase) (Object) this));
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "net/minecraft/entity/EntityLivingBase.travel(FFF)V"))
    private void onLivingTravelPre(EntityLivingBase entity, float strafe, float vertical, float forward) {
        EntityTravelEvent event = new EntityTravelEvent(EventStageable.EventStage.PRE, entity, strafe, vertical, forward);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (!event.isCanceled()) entity.travel(event.getStrafe(), event.getVertical(), event.getForward());
        Muffin.getInstance().getEventManager().dispatchEvent(new EntityTravelEvent(EventStageable.EventStage.POST, entity, strafe, vertical, forward));
    }

    @Inject(method = "onItemUseFinish", at = @At("HEAD"))
    public void onItemUseFinishPre(CallbackInfo info) {
        EntityLivingBase _this = (EntityLivingBase) (Object) this;
        if (_this instanceof EntityPlayer) {
            Muffin.getInstance().getEventManager().dispatchEvent(new EntityUseItemFinishEvent(_this));
        }
    }

    @Inject(method = "onItemPickup", at = @At("HEAD"))
    public void onItemPickUpPre(Entity entityIn, int quantity, CallbackInfo ci) {
        EntityItemPickupEvent event = new EntityItemPickupEvent(entityIn, quantity);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "isPotionActive", at = @At("HEAD"), cancellable = true)
    public void onIsPotionActivePre(Potion potionIn, final CallbackInfoReturnable<Boolean> ciR) {
        PlayerIsPotionActiveEvent event = new PlayerIsPotionActiveEvent((EntityLivingBase) (Object) this, potionIn);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ciR.setReturnValue(false);
    }

    @Inject(method = "isPotionActive", at = @At("RETURN"), cancellable = true)
    public void onIsPotionActivePost(Potion potionIn, final CallbackInfoReturnable<Boolean> ciR) {
        PlayerIsPotionActivePostEvent event = new PlayerIsPotionActivePostEvent((EntityLivingBase) (Object) this, potionIn);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ciR.setReturnValue(false);
    }

    @Redirect(method = "travel", at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isRemote:Z", ordinal = 1))
    private boolean isWorldRemoteWrapper(World world) {
        return Muffin.getInstance().getEventManager().dispatchEvent(new SmoothElytraEvent(world.isRemote)).isWorldRemote();
    }

    @Inject(method = "updateActiveHand", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;activeItemStackUseCount:I", ordinal = 0))
    public void onUpdateActiveHand(CallbackInfo ci) {
        EntityUseItemTickEvent event = new EntityUseItemTickEvent((EntityLivingBase) (Object) this);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "handleJumpWater", at = @At(value = "HEAD"), cancellable = true)
    private void onHandleJumpWaterPre(CallbackInfo ci) {
        HandleJumpWaterEvent event = new HandleJumpWaterEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }


}