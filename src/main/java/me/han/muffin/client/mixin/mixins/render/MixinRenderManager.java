package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.render.entity.RenderEntityEvent;
import me.han.muffin.client.imixin.render.IRenderManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderManager.class)
public abstract class MixinRenderManager implements IRenderManager {
    @Shadow private boolean renderOutlines;

    @Override
    @Accessor(value = "renderPosX")
    public abstract double getRenderPosX();

    @Override
    @Accessor(value = "renderPosY")
    public abstract double getRenderPosY();

    @Override
    @Accessor(value = "renderPosZ")
    public abstract double getRenderPosZ();

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    public void onRenderEntityPre(Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean debug, CallbackInfo ci) {
        RenderEntityEvent event = new RenderEntityEvent(EventStageable.EventStage.PRE, entity, x, y, z, yaw, partialTicks);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderEntity", at = @At("RETURN"))
    public void onRenderEntityPostReturn(Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean debug, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderEntityEvent(EventStageable.EventStage.POST, entity, x, y, z, yaw, partialTicks));
    }

    // Weird way around because don't wanna mess up with shadow and hitbox rendering
    /*
    @Inject(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRenderShadowAndFire(Lnet/minecraft/entity/Entity;DDDFF)V"))
    public void renderEntityPostShadow(Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean debug, CallbackInfo ci) {
        RenderFireAndShadowEvent event = new RenderFireAndShadowEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled())
            ci.cancel();
    }

    @Inject(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderDebugBoundingBox(Lnet/minecraft/entity/Entity;DDDFF)V"))
    public void renderEntityPostDebugBB(Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean debug, CallbackInfo ci) {
        if (!this.renderOutlines)
            return;

        Muffin.getInstance().getEventManager().dispatchEvent(new RenderEntityEvent(EventStageable.EventStage.POST, (EntityLivingBase) entity, x, y, z, yaw, partialTicks));
    }

     */

}