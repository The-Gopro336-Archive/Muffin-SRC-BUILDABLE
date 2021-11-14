package me.han.muffin.client.mixin.mixins.render.entity.tile;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.render.entity.RenderTileEntityShulkerBoxEvent;
import net.minecraft.client.renderer.tileentity.TileEntityShulkerBoxRenderer;
import net.minecraft.tileentity.TileEntityShulkerBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityShulkerBoxRenderer.class)
public class MixinTileEntityShulkerBoxRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void onInjectChamsPre(TileEntityShulkerBox shulkerBox, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderTileEntityShulkerBoxEvent(EventStageable.EventStage.PRE, shulkerBox, x, y, z, partialTicks, destroyStage, alpha));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onInjectChamsPost(TileEntityShulkerBox shulkerBox, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderTileEntityShulkerBoxEvent(EventStageable.EventStage.POST, shulkerBox, x, y, z, partialTicks, destroyStage, alpha));
    }

}
