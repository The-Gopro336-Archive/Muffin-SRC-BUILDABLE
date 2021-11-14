package me.han.muffin.client.mixin.mixins.render.entity.tile;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.render.entity.RenderTileEntityEnderChestEvent;
import net.minecraft.client.renderer.tileentity.TileEntityEnderChestRenderer;
import net.minecraft.tileentity.TileEntityEnderChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityEnderChestRenderer.class)
public class MixinTileEntityEnderChestRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void onInjectChamsPre(TileEntityEnderChest enderChest, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderTileEntityEnderChestEvent(EventStageable.EventStage.PRE, enderChest, x, y, z, partialTicks, destroyStage, alpha));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onInjectChamsPost(TileEntityEnderChest enderChest, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo callbackInfo) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderTileEntityEnderChestEvent(EventStageable.EventStage.POST, enderChest, x, y, z, partialTicks, destroyStage, alpha));
    }

}
