package me.han.muffin.client.mixin.mixins.entity;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.client.LandStepEvent;
import me.han.muffin.client.event.events.client.MoveEvent;
import me.han.muffin.client.event.events.client.StepEvent;
import me.han.muffin.client.event.events.client.StrafeEvent;
import me.han.muffin.client.event.events.entity.EntityHitBoxEvent;
import me.han.muffin.client.event.events.entity.MaxInPortalTimeEvent;
import me.han.muffin.client.event.events.entity.player.ShouldWalkOffEdge;
import me.han.muffin.client.event.events.render.IsEntityInsideOpaqueBlockEvent;
import me.han.muffin.client.imixin.entity.IEntity;
import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(value = Entity.class)
public abstract class MixinEntity implements IEntity {

    @Override
    @Accessor(value = "isInWeb")
    public abstract boolean isIsInWeb();

    @Override
    @Accessor(value = "isInWeb")
    public abstract void setIsInWeb(boolean inWeb);

    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public double motionX;
    @Shadow public double motionY;
    @Shadow public double motionZ;

    @Shadow public float rotationYaw;
    @Shadow public float rotationPitch;

    @Shadow public float prevRotationYaw;
    @Shadow public float prevRotationPitch;

    @Shadow public abstract boolean isSprinting();
    @Shadow public abstract boolean isRiding();
    @Shadow public boolean onGround;

    @Shadow public abstract AxisAlignedBB getEntityBoundingBox();

    @Shadow
    public void move(MoverType type, double x, double y, double z) {
    }

    @Shadow public World world;
    @Shadow public float width;

    @Shadow public abstract boolean isSneaking();
    @Shadow public abstract float getEyeHeight();

    @Shadow public abstract void setEntityBoundingBox(AxisAlignedBB bb);

    @Shadow public float height;
    @Shadow public float stepHeight;
    @Shadow public abstract UUID getUniqueID();

    @Shadow public abstract int getMaxInPortalTime();
    @Shadow private int entityId;
    @Shadow public abstract int getEntityId();
    @Shadow public boolean isDead;

    @Shadow public abstract boolean hasNoGravity();
    @Shadow @Nullable public abstract Entity getRidingEntity();
    @Shadow protected abstract boolean getFlag(int flag);

    @Inject(method = "isInvisibleToPlayer", at = @At("HEAD"), cancellable = true)
    public void onIsInvisibleToPlayerPre(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
        cir.cancel();
    }

    @Inject(method = "getCollisionBorderSize", at = @At("HEAD"), cancellable = true)
    public void onGetCollisionBorderSizePre(final CallbackInfoReturnable<Float> ciR) {
        EntityHitBoxEvent event = new EntityHitBoxEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            ciR.cancel();
            ciR.setReturnValue(event.getSize());
        }
    }

    @Redirect(method = "move", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z", ordinal = 0)), at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z", ordinal = 0))
    private boolean onIsSneakingWrapperInvoke(Entity entity) {
        return Muffin.getInstance().getEventManager().dispatchEvent(new ShouldWalkOffEdge(entity.isSneaking())).isSneaking();
    }

    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void onHandleRotationsPre(float strafe, float up, float forward, float friction, CallbackInfo ci) {
        if ((Entity) (Object) this == Globals.mc.player) {
            StrafeEvent event = new StrafeEvent(strafe, up, forward, friction);
            Muffin.getInstance().getEventManager().dispatchEvent(event);
            if (event.isCanceled()) ci.cancel();
        }
    }

    private StepEvent stepEvent;
    private float cachedStepHeight;

    @Inject(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;stepHeight:F", shift = At.Shift.BEFORE, ordinal = 3))
    private void onInjectStepPre(MoverType type, double x, double y, double z, CallbackInfo ci) {
        if ((Entity) (Object) this == Globals.mc.player) {
            cachedStepHeight = stepHeight;
            stepEvent = new StepEvent(EventStageable.EventStage.PRE, stepHeight);
            Muffin.getInstance().getEventManager().dispatchEvent(stepEvent);
        }
    }

    @Inject(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;stepHeight:F", ordinal = 4, shift = At.Shift.BEFORE), require = 1)
    private void onInjectStepPre(CallbackInfo ci) {
        if ((Entity) (Object) this == Globals.mc.player) {
            this.stepHeight = stepEvent.getHeight();
        }
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", shift = At.Shift.BEFORE, ordinal = 0))
    private void onInjectStepEventPost(CallbackInfo ci) {
        if ((Entity) (Object) this == Globals.mc.player) {
            stepHeight = cachedStepHeight;
            Muffin.getInstance().getEventManager().dispatchEvent(new StepEvent(EventStageable.EventStage.POST, stepHeight));
        }
    }

    //private AxisAlignedBB bbBeforeStep;

    /**
     * Gets the bounding box and store it into a variable
     */
    /*
    @Inject(
            method = "move",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/Entity;stepHeight:F",
                    shift = At.Shift.BEFORE,
                    ordinal = 3
            )
    )
    private void inject$bbGetter(MoverType type, double x, double y, double z, CallbackInfo info) {
        bbBeforeStep = boundingBox;
    }

    /**
     * Invokes the {@link me.han.muffin.client.event.events.client.StepEvent}
     */
    /*
    @Inject(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;resetPositionToBB()V",
                    shift = At.Shift.BEFORE,
                    ordinal = 1
            )
    )
    private void inject$stepEvent(MoverType type, double x, double y, double z, CallbackInfo info) {
        if ((Object) this == Globals.mc.player) {
            final double xDiff = boundingBox.minX - bbBeforeStep.minX;
            final double yDiff = boundingBox.minY - bbBeforeStep.minY;
            final double zDiff = boundingBox.minZ - bbBeforeStep.minZ;
            StepEvent event = new StepEvent(EventStageable.EventStage.POST, xDiff, yDiff, zDiff);
            Muffin.getInstance().getEventManager().dispatchEvent(event);
            if (event.isCanceled())
                setEntityBoundingBox(bbBeforeStep);
        }
    }
*/

    @Redirect(method = "onEntityUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getMaxInPortalTime()I"))
    private int onGetMaxInPortalTimeHook(final Entity entity) {
        int time = getMaxInPortalTime();
        MaxInPortalTimeEvent event = new MaxInPortalTimeEvent(time);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

        if (event.isCanceled()) time = event.getTime();
        return time;
    }

    @Inject(method = "isEntityInsideOpaqueBlock",at = @At("HEAD"), cancellable = true)
    private void onIsEntityInsideOpaqueBlockPre(CallbackInfoReturnable<Boolean> cir) {
        IsEntityInsideOpaqueBlockEvent event = new IsEntityInsideOpaqueBlockEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) cir.setReturnValue(false);
    }

    // Makes the camera guy instead of original player turn around when we move mouse
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    public void onTurnPre(float yaw, float pitch, CallbackInfo ci) {
        if (Globals.mc.player != null && this.entityId != Globals.mc.player.getEntityId()) return;
        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() != null) {
            FreecamModule.INSTANCE.getCameraGuy().turn(yaw, pitch);
            ci.cancel();
        }
    }

    private MoveEvent moveEvent;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void onMovePre(MoverType type, double x, double y, double z, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof EntityPlayerSP) {
            moveEvent = new MoveEvent(type, x, y, z, entity.isSneaking());
            Muffin.getInstance().getEventManager().dispatchEvent(moveEvent);
        }
    }

    @ModifyVariable(method = "move", at = @At(value = "HEAD"), ordinal = 0)
    private double onMoveX(double x) {
        if (moveEvent != null) x = moveEvent.getX();
        return x;
    }

    @ModifyVariable(method = "move", at = @At(value = "HEAD"), ordinal = 1)
    private double onMoveY(double y) {
        if (moveEvent != null) y = moveEvent.getY();
        return y;
    }

    @ModifyVariable(method = "move", at = @At(value = "HEAD"), ordinal = 2)
    private double onMoveZ(double z) {
        if (moveEvent != null) z = moveEvent.getZ();
        return z;
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "net/minecraft/entity/Entity.isSneaking()Z", ordinal = 0))
    private boolean onMoveSneaking(Entity entity) {
        return moveEvent != null ? moveEvent.isSneaking() : entity.isSneaking();
    }

    @Inject(method = "move", at = @At("RETURN"))
    public void onMovePost(MoverType type, double x, double y, double z, CallbackInfo ci) {
        moveEvent = null;
    }

    @Inject(method = "move", at = @At(value = "FIELD", target = "net/minecraft/entity/Entity.onGround:Z", ordinal = 1))
    private void onStepGround(MoverType type, double x, double y, double z, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof EntityPlayerSP) {
            LandStepEvent event = new LandStepEvent(getEntityBoundingBox(), entity.stepHeight);
            Muffin.getInstance().getEventManager().dispatchEvent(event);
            entity.stepHeight = event.getStepHeight();
        }
    }

}
