package me.han.muffin.client.mixin.mixins.render.entity;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.render.item.RenderItemEvent;
import me.han.muffin.client.module.modules.other.ItemRender;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderEntityItem.class)
public class MixinRenderEntityItem {

    @Inject(method = "doRender", at = @At("HEAD"), require = 1)
    private void onInjectChamsPre(CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderItemEvent(EventStageable.EventStage.PRE));
    }

    @Inject(method = "doRender", at = @At("RETURN"), require = 1)
    private void onInjectChamsPost(CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderItemEvent(EventStageable.EventStage.POST));
    }

    @Redirect(method = "transformModelCount", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"))
    private void doTransformModelCountRotate(float angle, float x, float y, float z) {
        if (ItemRender.INSTANCE.isEnabled() && ItemRender.INSTANCE.getItemsAngle().getValue()) ItemRender.INSTANCE.doItemAngle();
    }

}
