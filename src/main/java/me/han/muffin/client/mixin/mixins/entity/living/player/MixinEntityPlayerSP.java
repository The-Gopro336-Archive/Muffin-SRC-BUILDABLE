package me.han.muffin.client.mixin.mixins.entity.living.player;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.PosRotationEvent;
import me.han.muffin.client.event.events.PositionEvent;
import me.han.muffin.client.event.events.client.*;
import me.han.muffin.client.event.events.entity.CancelSprintEvent;
import me.han.muffin.client.event.events.entity.PortalScreenEvent;
import me.han.muffin.client.event.events.entity.player.*;
import me.han.muffin.client.event.events.movement.PlayerUpdateMoveStateEvent;
import me.han.muffin.client.imixin.entity.IEntityPlayerSP;
import me.han.muffin.client.module.modules.movement.SprintModule;
import me.han.muffin.client.module.modules.player.FreecamModule;
import me.han.muffin.client.utils.Location;
import me.han.muffin.client.utils.math.rotation.Vec2f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.util.EnumHand;
import net.minecraft.util.MovementInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;

@Mixin(value = EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends MixinAbstractClientPlayer implements IEntityPlayerSP {
    @Override
    @Accessor(value = "prevOnGround")
    public abstract boolean isPrevOnGround();

    @Override
    @Accessor(value = "prevOnGround")
    public abstract void setPrevOnGround(boolean isPrevOnGround);

    @Override
    @Accessor(value = "lastReportedPosX")
    public abstract double getLastReportedPosX();

    @Override
    @Accessor(value = "lastReportedPosY")
    public abstract double getLastReportedPosY();

    @Override
    @Accessor(value = "lastReportedPosZ")
    public abstract double getLastReportedPosZ();

    @Override
    @Accessor(value = "lastReportedPosX")
    public abstract void setLastReportedPosX(double x);

    @Override
    @Accessor(value = "lastReportedPosY")
    public abstract void setLastReportedPosY(double y);

    @Override
    @Accessor(value = "lastReportedPosZ")
    public abstract void setLastReportedPosZ(double z);

    @Override
    @Accessor(value = "lastReportedYaw")
    public abstract float getLastReportedYaw();

    @Override
    @Accessor(value = "lastReportedPitch")
    public abstract float getLastReportedPitch();

    @Shadow
    @Final
    public NetHandlerPlayClient connection;

    @Shadow
    private float lastReportedYaw;
    @Shadow
    private float lastReportedPitch;

    @Shadow
    public abstract boolean isSneaking();

    @Shadow public MovementInput movementInput;

    @Shadow private double lastReportedPosX;
    @Shadow private double lastReportedPosY;
    @Shadow private double lastReportedPosZ;
    @Shadow private int positionUpdateTicks;

    @Shadow protected Minecraft mc;

    @Shadow protected abstract void onUpdateWalkingPlayer();

    private double cachedX;
    private double cachedY;
    private double cachedZ;

    private float cachedRotationPitch;
    private float cachedRotationYaw;

    private boolean cachedMoving;
    private boolean cachedOnGround;

    /*
    @Inject(method = "updateRidden", at = @At(value = "RETURN"))
    private void updateRiddenPost(CallbackInfo info) {
        if (BoatFlyModule.INSTANCE != null && BoatFlyModule.INSTANCE.isEnabled() && getRidingEntity() instanceof EntityBoat) {
            EntityBoat entityboat = (EntityBoat)  this.getRidingEntity();
            entityboat.updateInputs(this.movementInput.leftKeyDown, this.movementInput.rightKeyDown, this.movementInput.moveForward > 0.0f, this.movementInput.backKeyDown);
            this.rowingBoat |= this.movementInput.leftKeyDown || this.movementInput.rightKeyDown || this.movementInput.moveForward > 0.0f || this.movementInput.backKeyDown;
        }
    }
     */

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V"))
    private void onMoveStateUpdate(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new PlayerUpdateMoveStateEvent(movementInput));
    }

    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true)
    private void onPreUpdate(CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinPrePlayerUpdate");
        UpdateEvent preUpdateEvent = new UpdateEvent(EventStageable.EventStage.PRE);
        Muffin.getInstance().getEventManager().dispatchEvent(preUpdateEvent);
        if (preUpdateEvent.isCanceled()) ci.cancel();
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void onPostUpdate(CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinPostPlayerUpdate");
        Muffin.getInstance().getEventManager().dispatchEvent(new UpdateEvent(EventStageable.EventStage.POST));
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "onLivingUpdate",at = @At("HEAD"))
    private void onPreLivingUpdate(CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinPreLivingUpdate");
        Muffin.getInstance().getEventManager().dispatchEvent(new LivingUpdateEvent(EventStageable.EventStage.PRE));
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "onLivingUpdate", at = @At("RETURN"))
    private void onPostLivingUpdate(CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinPostLivingUpdate");
        Muffin.getInstance().getEventManager().dispatchEvent(new LivingUpdateEvent(EventStageable.EventStage.POST));
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "sendChatMessage",at = @At("HEAD"), cancellable = true)
    private void onSendChatMessagePre(String msg, CallbackInfo ci) {
        SendChatMessageEvent event = new SendChatMessageEvent(msg);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "closeScreen", at = @At("HEAD"), cancellable = true)
    private void onCloseScreenPre(CallbackInfo ci) {
        final CloseScreenEvent event = new CloseScreenEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant (floatValue = 0.2F))
    private float onModifyItemSpeed(float factor) {
        final PlayerSlowEvent.ActiveHand event = new PlayerSlowEvent.ActiveHand(factor);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) return 1.0F;
        return event.getFactor();
    }


    private boolean isMoving() {
        double xDiff = this.posX - this.lastReportedPosX;
        double yDiff = this.getEntityBoundingBox().minY - this.lastReportedPosY;
        double zDiff = this.posZ - this.lastReportedPosZ;

        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 9.0E-4D || this.positionUpdateTicks >= 20;
    }


    /*
    private Location location;
    private Rotation rotation;

    @Inject(method = "onUpdateWalkingPlayer", at = @At(value = "HEAD"), cancellable = true)
    private void onUpdateWalkingPlayerPre(CallbackInfo ci) {
        MotionUpdateEvent motionEvent = new MotionUpdateEvent(EventStageable.EventStage.PRE, getLocation(), getRotation());
        Muffin.getInstance().getEventManager().dispatchEvent(motionEvent);
        if (motionEvent.isCanceled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "net/minecraft/client/entity/EntityPlayerSP.posX:D"))
    private double onUpdateWalkingPlayerPosX(EntityPlayerSP entityPlayerSP) {
        return location.getX();
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "net/minecraft/util/math/AxisAlignedBB.minY:D"))
    private double onUpdateWalkingPlayerPosY(AxisAlignedBB axisAlignedBB) {
        return location.getY();
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "net/minecraft/client/entity/EntityPlayerSP.posZ:D"))
    private double onUpdateWalkingPlayerPosZ(EntityPlayerSP entityPlayerSP) {
        return location.getZ();
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "net/minecraft/client/entity/EntityPlayerSP.rotationYaw:F"))
    private float onUpdateWalkingPlayerYaw(EntityPlayerSP entityPlayerSP) {
        return rotation.getYaw();
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "net/minecraft/client/entity/EntityPlayerSP.rotationPitch:F"))
    private float onUpdateWalkingPlayerPitch(EntityPlayerSP entityPlayerSP) {
        return rotation.getPitch();
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "net/minecraft/client/entity/EntityPlayerSP.onGround:Z"))
    private boolean onUpdateWalkingPlayerOnGround(EntityPlayerSP entityPlayerSP) {
        return location.isOnGround();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At(value = "RETURN"))
    private void onUpdateWalkingPlayerPost(CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new MotionUpdateEvent(EventStageable.EventStage.POST, getLocation(), getRotation()));
    }

    public Location getLocation() {
        AxisAlignedBB bb = getEntityBoundingBox();
        if (location == null) location = new Location(posX, bb.minY, posZ, onGround, isMoving());
        location.setX(posX);
        location.setY(bb.minY);
        location.setZ(posZ);
        location.setOnGround(onGround);
        location.setMoving(isMoving());
        return location;
    }

    public Rotation getRotation() {
        if (rotation == null) rotation = new Rotation(rotationYaw, rotationPitch, prevRotationYaw, prevRotationPitch);
        rotation.setYaw(rotationYaw);
        rotation.setPitch(rotationPitch);
        return rotation;
    }
     */

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "net/minecraft/client/entity/EntityPlayerSP.onUpdateWalkingPlayer()V", ordinal = 0, shift = At.Shift.AFTER))
    private void onMotionPostUpdateFactor(final CallbackInfo ci) {
        final MotionUpdateMultiplierEvent event = new MotionUpdateMultiplierEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);

        final int factorIn = event.getFactor() - 1;

        for (int i = 0; i < factorIn; ++i) {
            final EntityPlayerSP local = (EntityPlayerSP) (Object) this;

            final int cacheSinceLastSwing = this.ticksSinceLastSwing;
            final int cacheActiveItemStackUseCount = this.activeItemStackUseCount;

            final int cacheHurtTime = local.hurtTime;

            final float cachePrevSwingProgress = local.prevSwingProgress;
            final float cacheSwingProgress = local.swingProgress;
            final int cacheSwingProgressInt = local.swingProgressInt;
            final boolean cacheIsSwingInProgress = local.isSwingInProgress;

            final float cacheRotationYaw = local.rotationYaw;
            final float cachePrevRotationYaw = local.prevRotationYaw;
            final float cacheRenderYawOffset = local.renderYawOffset;
            final float cachePrevRenderYawOffset = local.prevRenderYawOffset;

            final float cacheRotationYawHead = local.rotationYawHead;
            final float cachePrevRotationYawHead = local.prevRotationYawHead;

            final float cacheCameraYaw = local.cameraYaw;
            final float cachePrevCameraYaw = local.prevCameraYaw;

            final float cacheRenderArmYaw = local.renderArmYaw;
            final float cachePrevRenderArmYaw = local.prevRenderArmYaw;

            final float cacheRenderArmPitch = local.renderArmPitch;
            final float cachePrevRenderArmPitch = local.prevRenderArmPitch;

            final float cacheDistanceWalkedModified = local.distanceWalkedModified;
            final float cachePrevDistanceWalkedModified = local.prevDistanceWalkedModified;

            final double cacheChasingPosX = local.chasingPosX;
            final double cachePrevChasingPosX = local.prevChasingPosX;

            final double cacheChasingPosY = local.chasingPosY;
            final double cachePrevChasingPosY = local.prevChasingPosY;

            final double cacheChasingPosZ = local.chasingPosZ;
            final double cachePrevChasingPosZ = local.prevChasingPosZ;
            
            final float cacheLimbSwingAmount = local.limbSwingAmount;
            final float cachePrevLimbSwingAmount = local.prevLimbSwingAmount;
            final float cacheLimbSwing = local.limbSwing;

            super.onUpdate();

            this.ticksSinceLastSwing = cacheSinceLastSwing;
            this.activeItemStackUseCount = cacheActiveItemStackUseCount;
            local.hurtTime = cacheHurtTime;

            local.prevSwingProgress = cachePrevSwingProgress;
            local.swingProgress = cacheSwingProgress;
            local.swingProgressInt = cacheSwingProgressInt;
            local.isSwingInProgress = cacheIsSwingInProgress;

            local.rotationYaw = cacheRotationYaw;
            local.prevRotationYaw = cachePrevRotationYaw;
            local.renderYawOffset = cacheRenderYawOffset;
            local.prevRenderYawOffset = cachePrevRenderYawOffset;

            local.rotationYawHead = cacheRotationYawHead;
            local.prevRotationYawHead = cachePrevRotationYawHead;

            local.cameraYaw = cacheCameraYaw;
            local.prevCameraYaw = cachePrevCameraYaw;

            local.renderArmYaw = cacheRenderArmYaw;
            local.prevRenderArmYaw = cachePrevRenderArmYaw;

            local.renderArmPitch = cacheRenderArmPitch;
            local.prevRenderArmPitch = cachePrevRenderArmPitch;

            local.distanceWalkedModified = cacheDistanceWalkedModified;
            local.prevDistanceWalkedModified = cachePrevDistanceWalkedModified;

            local.chasingPosX = cacheChasingPosX;
            local.prevChasingPosX = cachePrevChasingPosX;

            local.chasingPosY = cacheChasingPosY;
            local.prevChasingPosY = cachePrevChasingPosY;

            local.chasingPosZ = cacheChasingPosZ;
            local.prevChasingPosZ = cachePrevChasingPosZ;

            local.limbSwingAmount = cacheLimbSwingAmount;
            local.prevLimbSwingAmount = cachePrevLimbSwingAmount;
            local.limbSwing = cacheLimbSwing;

            this.onUpdateWalkingPlayer();
        }

        for (int i2 = 0; i2 < event.getFactor() - 1 - factorIn; ++i2) this.onUpdateWalkingPlayer();
    }

    //private MotionUpdateEvent motionEvent;
    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdateWalkingPlayerPre(CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinPreMotionUpdate");
        cachedX = posX;
        cachedY = posY;
        cachedZ = posZ;

        cachedRotationYaw = rotationYaw;
        cachedRotationPitch = rotationPitch;
        cachedOnGround = onGround;

        Location location = new Location(posX, posY, posZ, onGround, isMoving());

        Vec2f prevRotation = new Vec2f(prevRotationYaw, prevRotationPitch);
        Vec2f rotation = new Vec2f(rotationYaw, rotationPitch);

        MotionUpdateEvent motionEvent = new MotionUpdateEvent(EventStageable.EventStage.PRE, location, rotation, prevRotation);
        Muffin.getInstance().getEventManager().dispatchEvent(motionEvent);

        posX = motionEvent.getLocation().getX();
        posY = motionEvent.getLocation().getY();
        posZ = motionEvent.getLocation().getZ();

        rotationYaw = motionEvent.getRotation().getX();
        rotationPitch = motionEvent.getRotation().getY();
        onGround = motionEvent.getLocation().isOnGround();

        if (motionEvent.getRotating()) {
            rotationYawHead = motionEvent.getRotation().getX();
            renderYawOffset = motionEvent.getRotation().getX();
        }

        if (motionEvent.isCanceled()) ci.cancel();
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    private void onUpdateWalkingPlayerPost(CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinPostMotionUpdate");
        posX = cachedX;
        posZ = cachedZ;
        posY = cachedY;
        rotationYaw = cachedRotationYaw;
        rotationPitch = cachedRotationPitch;
        onGround = cachedOnGround;

        Location location = new Location(posX, posY, posZ, onGround, isMoving());

        Vec2f prevRotation = new Vec2f(prevRotationYaw, prevRotationPitch);
        Vec2f rotation = new Vec2f(rotationYaw, rotationPitch);

        Muffin.getInstance().getEventManager().dispatchEvent(new MotionUpdateEvent(EventStageable.EventStage.POST, location, rotation, prevRotation));
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/NetHandlerPlayClient;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 5))
    private void onUpdateWalkingPlayerPosRotate(CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinMotionPosRotationUpdate");
        Muffin.getInstance().getEventManager().dispatchEvent(new PosRotationEvent());
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/NetHandlerPlayClient;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 6))
    private void onUpdateWalkingPlayerPos(CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinMotionPosUpdate");
        Muffin.getInstance().getEventManager().dispatchEvent(new PositionEvent());
        Globals.mc.profiler.endSection();
    }

/*
    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdateWalkingPlayerPre(CallbackInfo ci) {
        AxisAlignedBB bb = getEntityBoundingBox();
        cachedX = posX;
        cachedY = bb.minY;
        cachedZ = posZ;
        cachedRotationYaw = rotationYaw;
        cachedRotationPitch = rotationPitch;
        cachedOnGround = onGround;

        Location location = new Location(posX, bb.minY, posZ, onGround, isMoving());
        Rotation rotation = new Rotation(rotationYaw, rotationPitch, prevRotationYaw, prevRotationPitch);

        motionEvent = new MotionUpdateEvent(EventStageable.EventStage.PRE, location, rotation);
        Muffin.getInstance().getEventManager().dispatchEvent(motionEvent);

        posX = motionEvent.getLocation().getX();
        posY = motionEvent.getLocation().getY();
        posZ = motionEvent.getLocation().getZ();

        rotationYaw = motionEvent.getRotation().getYaw();
        rotationPitch = motionEvent.getRotation().getPitch();
        onGround = motionEvent.getLocation().isOnGround();

        if (motionEvent.getRotation().isRotating()) {
            rotationYawHead = motionEvent.getRotation().getYaw();
            renderYawOffset = motionEvent.getRotation().getYaw();
        }

        if (motionEvent.isCanceled()) ci.cancel();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    private void onUpdateWalkingPlayerPost(CallbackInfo ci) {
        posX = cachedX;
        posZ = cachedZ;
        posY = cachedY;
        rotationYaw = cachedRotationYaw;
        rotationPitch = cachedRotationPitch;
        onGround = cachedOnGround;

        Location location = new Location(posX, posY, posZ, onGround, isMoving());
        Rotation rotation = new Rotation(rotationYaw, rotationPitch, prevRotationYaw, prevRotationPitch);
        Muffin.getInstance().getEventManager().dispatchEvent(new MotionUpdateEvent(EventStageable.EventStage.POST, location, rotation));
    }
 */

    // We have to return true here so it would still update movement inputs from Baritone and send packets
    @Inject(method = "isCurrentViewEntity", at = @At("HEAD"), cancellable = true)
    protected void onIsCurrentViewEntityPre(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() != null) {
            cir.setReturnValue(mc.getRenderViewEntity() == FreecamModule.INSTANCE.getCameraGuy());
        }
    }

    @Inject(method = "isUser", at = @At(value = "HEAD"), cancellable = true)
    private void isFreecamOrUser(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() != null) {
            cir.setReturnValue(false);
        }
    }

    /*
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void move(MoverType type, double x, double y, double z, CallbackInfo ci) {
        MoveEvent event = new MoveEvent(type, x, y, z);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            super.move(type, event.getX(), event.getY(), event.getZ());
            ci.cancel();
        }
    }

    @Inject(method = "move", at = @At("RETURN"), cancellable = true)
    public void postMove(MoverType type, double x, double y, double z, CallbackInfo ci) {
        PostMoveEvent event = new PostMoveEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }
     */

    @Inject(method = "isHandActive", at = @At("HEAD"), cancellable = true)
    public void onIsHandActivePre(CallbackInfoReturnable<Boolean> cir) {
        PlayerIsHandActiveEvent event = new PlayerIsHandActiveEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    public void onPushOutOfBlocksPre(double x, double y, double z, CallbackInfoReturnable<Boolean> ci) {
        PlayerPushEvent event = new PlayerPushEvent(PlayerPushEvent.Type.BLOCK);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.setReturnValue(false);
    }

    PlayerSwingArmEvent swingArmEvent;

    @Inject(method = "swingArm", at = @At(value = "HEAD"), cancellable = true)
    public void onSwingArmPre(EnumHand hand, CallbackInfo ci) {
        swingArmEvent = new PlayerSwingArmEvent(hand);
        Muffin.getInstance().getEventManager().dispatchEvent(swingArmEvent);
        if (swingArmEvent.isCanceled()) ci.cancel();
    }

    @ModifyArg(method = "swingArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;swingArm(Lnet/minecraft/util/EnumHand;)V"), index = 0)
    public EnumHand onModifyClientSwingArm(EnumHand defaultHand) {
        if (swingArmEvent != null) return swingArmEvent.getHand();
        return defaultHand;
    }

    @ModifyArg(method = "swingArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/NetHandlerPlayClient;sendPacket(Lnet/minecraft/network/Packet;)V"), index = 0)
    public Packet<?> onModifyServerSwingArm(Packet<?> defaultPacket) {
        if (swingArmEvent != null) return new CPacketAnimation(swingArmEvent.getHand());
        return defaultPacket;
    }

    @Inject(method = "swingArm", at = @At(value = "RETURN"))
    public void onSwingArmPost(EnumHand hand, CallbackInfo ci) {
        swingArmEvent = null;
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;collidedHorizontally:Z"))
    private boolean onOverrideCollidedHorizontally(EntityPlayerSP player) {
        CancelSprintEvent event = new CancelSprintEvent(player.collidedHorizontally);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.getShouldCancel();
    }

    @ModifyArg(method = "setSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;setSprinting(Z)V"), index = 0)
    public boolean onModifySprintingState(boolean sprinting) {
        if (SprintModule.INSTANCE.isEnabled() && SprintModule.INSTANCE.canSprint()) return true;
        return sprinting;
    }

    /*
    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;closeScreen()V"))
    public void onPortalCloseScreen(final EntityPlayerSP player) {
        PortalScreenEvent event = new PortalScreenEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (!event.isCanceled()) player.closeScreen();
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
    public void displayGuiScreenHook(final Minecraft mc, final GuiScreen screen) {
        PortalScreenEvent event = new PortalScreenEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (!event.isCanceled()) Globals.mc.displayGuiScreen(screen);
    }
     */

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;doesGuiPauseGame()Z"))
    public boolean onInvokeDoesGuiPauseGame(GuiScreen guiScreen) {
        PortalScreenEvent event = new PortalScreenEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.isCanceled();
    }

    @Override
    public void jump() {
        JumpEvent event = new JumpEvent(motionX, motionY, motionZ, rotationYaw);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (!event.isCanceled()) super.jump();
    }

    @Nonnull
    @Override
    public MotionUpdateEvent getLastMotionEvent() {
    //    if (motionEvent != null && motionEvent.getStage() == EventStageable.EventStage.PRE) return motionEvent;
        return null;
    }


/*
    private boolean isMoving() {
        double xDiff = this.posX - this.lastReportedPosX;
        double yDiff = this.getEntityBoundingBox().minY - this.lastReportedPosY;
        double zDiff = this.posZ - this.lastReportedPosZ;

        if (this.isRiding())
            return false;

        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 9.0E-4D || this.positionUpdateTicks >= 20;
    }
 */


}
