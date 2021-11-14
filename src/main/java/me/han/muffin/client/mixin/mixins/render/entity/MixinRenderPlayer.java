package me.han.muffin.client.mixin.mixins.render.entity;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.RenderRotationEvent;
import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderPlayer.class)
public class MixinRenderPlayer {

  //  @Inject(method = "renderEntityName", at = @At("HEAD"), cancellable = true)
   // public void renderLivingLabel(AbstractClientPlayer entityIn, double x, double y, double z, String name, double distanceSq, CallbackInfo ci) {
    //    if (Muffin.getInstance().getModuleManager().isModuleEnabled(NametagsModule.class)) {
    //       ci.cancel();
     //   }
  //  }

    /*
    @Inject(method = "applyRotations", at = @At("RETURN"))
    protected void applyRotations(AbstractClientPlayer entityLiving, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (entityLiving == Globals.mc.player && ElytraFlyModule.INSTANCE.shouldSwing()) {
            Vec3d vec3d = entityLiving.getLook(partialTicks);
            double d0 = entityLiving.motionX * entityLiving.motionX + entityLiving.motionZ * entityLiving.motionZ;
            double d1 = vec3d.x * vec3d.x + vec3d.z * vec3d.z;

            if (d0 > 0.0D && d1 > 0.0D) {
                double d2 = (entityLiving.motionX * vec3d.x + entityLiving.motionZ * vec3d.z) / (Math.sqrt(d0) * Math.sqrt(d1));
                double d3 = entityLiving.motionX * vec3d.z - entityLiving.motionZ * vec3d.x;
                GlStateManager.rotate(-((float) (Math.signum(d3) * Math.acos(d2)) * 180.0F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
            }
        }
    }
     */

    // Redirect it to the original player so the original player can be renderer correctly
    @Redirect(method = "doRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderViewEntity:Lnet/minecraft/entity/Entity;", opcode = Opcodes.GETFIELD))
    public Entity onGetRenderViewEntity(RenderManager renderManager) {
        return Globals.mc.player;
    }

    @Redirect(method = "doRender", at = @At(value = "INVOKE", target = "net/minecraft/client/entity/AbstractClientPlayer.isUser()Z"))
    private boolean onRenderLocalPlayer(AbstractClientPlayer entity) {
        if (entity.equals(Globals.mc.player) && FreecamModule.INSTANCE.isEnabled()) return false;
        return entity.isUser();
    }

    private float rotationPitch;
    private float prevRotationPitch;

    @Inject(method = "doRender", at= @At(value = "HEAD"))
    private void doRenderPre(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        RenderRotationEvent event = new RenderRotationEvent(EventStageable.EventStage.PRE, entity, x, y, z, entityYaw, partialTicks);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

        /*
        if (!entity.equals(Globals.mc.player) || Globals.mc.gameSettings.thirdPersonView == 0) return;

        if (entity instanceof EntityPlayerSP && entity == Globals.mc.player) {
            IEntityPlayerSP playerSP = ((IEntityPlayerSP) entity);
            if (playerSP.getLastMotionEvent() != null) {
                MotionUpdateEvent motionEvent = playerSP.getLastMotionEvent();
                if (motionEvent != null && motionEvent.getRotation().isRotating()) {
                    rotationPitch = entity.rotationPitch;
                    prevRotationPitch = entity.prevRotationPitch;
                    entity.rotationPitch = motionEvent.getRotation().getPitch();
                    entity.prevRotationPitch = motionEvent.getRotation().getPitch();
                }
            }
        }
         */

    }

    @Inject(method = "doRender" , at= @At(value = "RETURN"))
    private void doRenderPost(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        RenderRotationEvent event = new RenderRotationEvent(EventStageable.EventStage.POST, entity, x, y, z, entityYaw, partialTicks);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

/*
        if (!entity.equals(Globals.mc.player) || Globals.mc.gameSettings.thirdPersonView == 0) return;
        if (entity instanceof EntityPlayerSP && entity == Globals.mc.player) {
            IEntityPlayerSP playerSP = ((IEntityPlayerSP) entity);
            if (playerSP.getLastMotionEvent() != null) {
                MotionUpdateEvent motionEvent = playerSP.getLastMotionEvent();
                if (motionEvent != null && motionEvent.getRotation().isRotating()) {
                    entity.rotationPitch = rotationPitch;
                    entity.prevRotationPitch = prevRotationPitch;
                }
            }
        }
 */

    }

/*
    private float pitch;
    private float yaw;
    private float yawOffset;
    private float prevPitch;
    private float prevYawOffset;
    private float prevYaw;

    @Inject(method = "doRender", at= @At(value = "HEAD"))
    private void doRenderHead(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {

        if (!entity.equals(Globals.mc.player) || Globals.mc.gameSettings.thirdPersonView == 0) return;

        if (Globals.mc.currentScreen instanceof GuiInventory) return;

        RenderRotationEvent event = new RenderRotationEvent(entity.rotationYawHead, entity.rotationPitch);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

        float rotationYaw = event.getRotationYaw();
        float rotationPitch = event.getRotationPitch();

        this.pitch = entity.rotationPitch;

        this.yaw = entity.rotationYawHead;
        this.prevYaw = entity.prevRotationYawHead;

        this.prevPitch = entity.prevRotationPitch;
        this.yawOffset = entity.renderYawOffset;
        this.prevYawOffset = entity.prevRenderYawOffset;

        if (this.yaw == rotationYaw && this.pitch == rotationPitch) return;

        entity.rotationYawHead = rotationYaw;
        entity.prevRotationYawHead = rotationYaw;
        entity.renderYawOffset = rotationYaw;
        entity.prevRenderYawOffset = rotationYaw;
        entity.rotationPitch = rotationPitch;
        entity.prevRotationPitch = rotationPitch;

    }

    @Inject(method = "doRender" , at= @At(value = "RETURN"))
    private void doRenderReturn(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo callbackInfo) {
        if (!entity.equals(Globals.mc.player) || Globals.mc.gameSettings.thirdPersonView == 0) return;

        if (Globals.mc.currentScreen instanceof GuiInventory) return;

        entity.rotationYawHead = this.yaw;
        entity.prevRotationYawHead = this.prevYaw;
        entity.renderYawOffset = this.yawOffset;
        entity.prevRenderYawOffset = this.prevYawOffset;
        entity.rotationPitch = this.pitch;
        entity.prevRotationPitch = this.prevPitch;

    }
 */

/*
    @ModifyArg(method = "renderLeftArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V", ordinal = 0))
    private float leftArm(float oldLeft) {
        return Muffin.getInstance().getModuleManager().isModuleEnabled(Testst.class) ? 0F : oldLeft;
    }

    @ModifyArg(method = "renderRightArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V", ordinal = 0))
    private float rightArm(float oldLeft) {
        return Muffin.getInstance().getModuleManager().isModuleEnabled(Testst.class) ? 0F : oldLeft;
    }

    @ModifyArg(method = "renderLeftArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V", ordinal = 1))
    private float leftArm1(float oldLeft) {
        return Muffin.getInstance().getModuleManager().isModuleEnabled(Testst.class) ? 0F : oldLeft;
    }

    @ModifyArg(method = "renderRightArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V", ordinal = 1))
    private float rightArm1(float oldLeft) {
        return Muffin.getInstance().getModuleManager().isModuleEnabled(Testst.class) ? 0F : oldLeft;
    }

    @ModifyArg(method = "renderRightArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPlayer;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V"), index = 5)
    private float rightArm2(float oldLeft) {
        return Muffin.getInstance().getModuleManager().isModuleEnabled(Testst.class) ? 0F : oldLeft;
    }
 */

}