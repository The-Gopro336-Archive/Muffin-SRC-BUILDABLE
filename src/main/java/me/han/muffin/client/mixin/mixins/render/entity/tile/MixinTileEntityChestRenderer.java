package me.han.muffin.client.mixin.mixins.render.entity.tile;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.render.entity.RenderTileEntityChestEvent;
import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityChestRenderer.class)
public class MixinTileEntityChestRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void onInjectChamsPre(TileEntityChest chest, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderTileEntityChestEvent(EventStageable.EventStage.PRE, chest, x, y, z, partialTicks, destroyStage, alpha));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void obInjectChamsPost(TileEntityChest chest, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderTileEntityChestEvent(EventStageable.EventStage.POST, chest, x, y, z, partialTicks, destroyStage, alpha));
    }

}
