package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.item.RenderEnchantedEvent;
import me.han.muffin.client.module.modules.render.CustomEnchantModule;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderItem.class)
public abstract class MixinRenderItem {

    @ModifyArg(method = "renderEffect", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/RenderItem.renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;I)V"), index = 1)
    private int onRenderEffectModel(final int rgb) {
        return CustomEnchantModule.INSTANCE.isEnabled() ? CustomEnchantModule.INSTANCE.getColour().getRGB() : rgb;
    }

/*
    @Shadow protected abstract void renderModel(IBakedModel model, ItemStack stack);

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V", shift = At.Shift.AFTER))
    private void onRenderModelPre(ItemStack stack, IBakedModel model, CallbackInfo ci) {
        RenderItemModelEvent event = new RenderItemModelEvent(EventStageable.EventStage.PRE, stack, model);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        glPushAttrib(GL_ALL_ATTRIB_BITS);

        glPolygonMode(GL_FRONT, GL_LINE);

        GlStateUtils.texture2d(false);
        GlStateUtils.lighting(false);

        GlStateUtils.lineSmooth(true);
        GlStateUtils.blend(true);

        GlStateUtils.depth(false);
        GlStateUtils.depthMask(false);

        RenderUtils.glColor(ColourUtils.getClientColor(80));

        GlStateManager.glLineWidth(1F);
    }

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderItem;renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/item/ItemStack;)V", shift = At.Shift.BEFORE))
    private void e(ItemStack stack, IBakedModel model, CallbackInfo ci) {
        GlStateUtils.depth(true);
        GlStateUtils.depthMask(true);

        RenderUtils.glColor(ColourUtils.getClientColor());
        renderModel(model, stack);
    }

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;popMatrix()V", shift = At.Shift.BEFORE))
    private void onRenderModelPost(ItemStack stack, IBakedModel model, CallbackInfo ci) {

        GlStateUtils.lighting(true);
        GlStateUtils.lineSmooth(false);
        GlStateUtils.texture2d(true);
        GlStateUtils.blend(false);
        GlStateUtils.resetColour();
        glPopAttrib();

        RenderItemModelEvent event = new RenderItemModelEvent(EventStageable.EventStage.POST, stack, model);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }
 */

    @Inject(method = "renderEffect", at = @At("HEAD"), cancellable = true)
    public void onRenderEnchantedEffectPre(IBakedModel bakedModel, CallbackInfo ci) {
        final RenderEnchantedEvent event = new RenderEnchantedEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

}