package me.han.muffin.client.mixin.mixins.render.entity.living;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.render.RenderEntityLayerEvent;
import me.han.muffin.client.event.events.render.RenderEntitySizeEvent;
import me.han.muffin.client.event.events.render.entity.RenderEntityModelEvent;
import me.han.muffin.client.event.events.render.entity.RenderPlayerTagsEvent;
import me.han.muffin.client.module.modules.other.ItemRender;
import me.han.muffin.client.module.modules.render.EntityESPModule;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderLivingBase.class, priority = Integer.MAX_VALUE)
public abstract class MixinRenderLivingBase<T extends EntityLivingBase> {

    @Shadow
    protected abstract float interpolateRotation(float prevYawOffset, float yawOffset, float partialTicks);

    @Shadow protected ModelBase mainModel;
/*
    private RenderLivingBaseEvent renderLivingEvent;
    private float prevRenderYawOffset;
    private float renderYawOffset;

    private float prevRotationYaw;
    private float rotationYawHead;

    private float prevRotationPitch;
    private float rotationPitch;


    @Inject(method = "doRender", at = @At("HEAD"))
    private void doRenderHead(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (entity == Globals.mc.player) {
            renderLivingEvent = new RenderLivingBaseEvent(entity.prevRenderYawOffset, entity.renderYawOffset, entity.prevRotationYawHead, entity.rotationYawHead, entity.prevRotationPitch, entity.rotationPitch);
            Muffin.getInstance().getEventManager().dispatchEvent(renderLivingEvent);
            entity.prevRenderYawOffset = renderLivingEvent.getPrevRenderYawOffset();
            entity.renderYawOffset = renderLivingEvent.getRenderYawOffset();
            entity.prevRotationYawHead = renderLivingEvent.getPrevRotationYawHead();
            entity.rotationYawHead = renderLivingEvent.getRotationYawHead();
            entity.prevRotationPitch = renderLivingEvent.getPrevRotationPitch();
            entity.rotationPitch = renderLivingEvent.getRotationPitch();
        }
    }

    @Inject(method = "doRender", at = @At("RETURN"))
    private void doRenderReturn(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (renderLivingEvent != null) {
            entity.prevRenderYawOffset = renderLivingEvent.getPrevRenderYawOffset();
            entity.renderYawOffset = renderLivingEvent.getRenderYawOffset();
            entity.prevRotationYawHead = renderLivingEvent.getPrevRotationYawHead();
            entity.rotationYawHead = renderLivingEvent.getRotationYawHead();
            entity.prevRotationPitch = renderLivingEvent.getPrevRotationPitch();
            entity.rotationPitch = renderLivingEvent.getRotationPitch();
            renderLivingEvent = null;
        }
    }
 */

    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void onRenderModelWrapperPrePost(ModelBase modelBase, Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        RenderEntityModelEvent preEvent = new RenderEntityModelEvent(EventStageable.EventStage.PRE, modelBase, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Muffin.getInstance().getEventManager().dispatchEvent(preEvent);

        if (!preEvent.isCanceled())  {
            modelBase.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        RenderEntityModelEvent postEvent = new RenderEntityModelEvent(EventStageable.EventStage.POST, modelBase, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Muffin.getInstance().getEventManager().dispatchEvent(postEvent);
    }

    /*
    @Inject(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void doRender(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        RenderEntityModelEvent preEvent = new RenderEntityModelEvent(EventStageable.EventStage.PRE, mainModel, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        Muffin.getInstance().getEventManager().dispatchEvent(preEvent);

        if (preEvent.isCanceled()) {
            preEvent.getModelBase().render(preEvent.getEntity(), preEvent.getLimbSwing(), preEvent.getLimbSwingAmount(), preEvent.getAge(), preEvent.getHeadYaw(), preEvent.getHeadPitch(), preEvent.getScale());
            return;
        }

        mainModel.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);

        RenderEntityModelEvent postEvent = new RenderEntityModelEvent(EventStageable.EventStage.POST, mainModel, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        Muffin.getInstance().getEventManager().dispatchEvent(postEvent);
    }
     */

  //  RenderRotationEvent renderRotationEvent;
    /*
    private float rotationYaw;
    private float renderYawOffset;
    private float prevRotationYaw;
    private float rotationYawHead;

    private float rotationPitch;
    private float prevRotationPitch;

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true, require = 1)
    private void doRenderHead(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        RenderEntityEvent event = new RenderEntityEvent(EventStageable.EventStage.PRE, entity, x, y, z, entityYaw, partialTicks);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

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

        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "doRender", at = @At("RETURN"), cancellable = true, require = 1)
    private void doRenderReturn(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {

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

        Muffin.getInstance().getEventManager().dispatchEvent(new RenderEntityEvent(EventStageable.EventStage.POST, entity, x, y, z, entityYaw, partialTicks));
    }
 */

    @Inject(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = At.Shift.BEFORE))
    private void onRenderSmallModelPre(T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        RenderEntitySizeEvent event = new RenderEntitySizeEvent(EventStageable.EventStage.PRE);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = At.Shift.AFTER))
    private void onRenderSmallModelPost(T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderEntitySizeEvent(EventStageable.EventStage.POST));
    }

    @Inject(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderLivingBase;renderLayers(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onRenderLayersSmall(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        RenderEntitySizeEvent event = new RenderEntitySizeEvent(EventStageable.EventStage.PRE);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderLivingBase;renderLayers(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V", shift = At.Shift.AFTER), cancellable = true)
    private void onRenderSmallPost(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderEntitySizeEvent(EventStageable.EventStage.POST));
    }

    @Inject(method = "renderName", at = @At(value = "HEAD"), cancellable = true)
    public void obRenderNamePre(T entity, double x, double y, double z, CallbackInfo ci) {
        if (entity instanceof EntityPlayer) {
            RenderPlayerTagsEvent event = new RenderPlayerTagsEvent();
            Muffin.getInstance().getEventManager().dispatchEvent(event);
            if (event.isCanceled()) ci.cancel();
        }
        if (!EntityESPModule.renderNameTags) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLayers", at = @At(value = "HEAD"), cancellable = true)
    public void onInjectRenderLayers(CallbackInfo ci) {
        if (!EntityESPModule.renderNameTags) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderLayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/LayerRenderer;doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V"))
    public void onRenderLayersDoLayers(LayerRenderer<EntityLivingBase> layer, EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn) {
        final RenderEntityLayerEvent event = new RenderEntityLayerEvent(entity, layer);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (!event.isCanceled()) layer.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scaleIn);
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0, remap = false), index = 0)
    private float onSetBrightnessRed(float oldValue) {
        return ItemRender.isCustomDamageColour() ? ItemRender.INSTANCE.getDamageRed().getValue() / 255F : oldValue;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1, remap = false), index = 0)
    private float onSetBrightnessGreen(float oldValue) {
        return ItemRender.isCustomDamageColour() ? ItemRender.INSTANCE.getDamageGreen().getValue() / 255F : oldValue;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2, remap = false), index = 0)
    private float onSetBrightnessBlue(float oldValue) {
        return ItemRender.isCustomDamageColour() ? ItemRender.INSTANCE.getDamageBlue().getValue() / 255F : oldValue;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3, remap = false), index = 0)
    private float onSetBrightnessAlpha(float oldValue) {
        return ItemRender.isCustomDamageColour() ? ItemRender.INSTANCE.getDamageAlpha().getValue() / 255F : oldValue;
    }

}