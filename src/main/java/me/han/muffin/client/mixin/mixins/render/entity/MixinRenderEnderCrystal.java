package me.han.muffin.client.mixin.mixins.render.entity;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.render.RenderCrystalSizeEvent;
import me.han.muffin.client.event.events.render.entity.RenderCrystalModelEvent;
import me.han.muffin.client.event.events.render.entity.RenderEnderCrystalEvent;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderEnderCrystal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderEnderCrystal.class)
public abstract class MixinRenderEnderCrystal {

    @Inject(method = "doRender", at = @At("HEAD"), require = 1)
    private void onInjectChamsPre(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderEnderCrystalEvent(EventStageable.EventStage.PRE));
    }

    @Inject(method = "doRender", at = @At("RETURN"), require = 1)
    private void onInjectChamsPost(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderEnderCrystalEvent(EventStageable.EventStage.POST));
    }

    @Redirect(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", ordinal = 0))
    private void onInjectModelsPre(ModelBase modelBase, Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        RenderCrystalModelEvent preEvent = new RenderCrystalModelEvent(EventStageable.EventStage.PRE, modelBase, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Muffin.getInstance().getEventManager().dispatchEvent(preEvent);

        if (!preEvent.isCanceled()) modelBase.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

        RenderCrystalModelEvent postEvent = new RenderCrystalModelEvent(EventStageable.EventStage.POST, modelBase, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Muffin.getInstance().getEventManager().dispatchEvent(postEvent);
    }

    @Redirect(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", ordinal = 1))
    private void onInjectModelsPre2(ModelBase modelBase, Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        RenderCrystalModelEvent preEvent = new RenderCrystalModelEvent(EventStageable.EventStage.PRE, modelBase, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Muffin.getInstance().getEventManager().dispatchEvent(preEvent);

        if (!preEvent.isCanceled()) modelBase.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

        RenderCrystalModelEvent postEvent = new RenderCrystalModelEvent(EventStageable.EventStage.POST, modelBase, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Muffin.getInstance().getEventManager().dispatchEvent(postEvent);
    }

    @Inject(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", ordinal = 0, shift = At.Shift.BEFORE))
    private void onRenderSmallPre1(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        RenderCrystalSizeEvent event = new RenderCrystalSizeEvent(EventStageable.EventStage.PRE);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onRenderSmallPost1(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderCrystalSizeEvent(EventStageable.EventStage.POST));
    }

    @Inject(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void onRenderSmallPre2(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        RenderCrystalSizeEvent event = new RenderCrystalSizeEvent(EventStageable.EventStage.PRE);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onRenderSmallPost2(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderCrystalSizeEvent(EventStageable.EventStage.POST));
    }

}
