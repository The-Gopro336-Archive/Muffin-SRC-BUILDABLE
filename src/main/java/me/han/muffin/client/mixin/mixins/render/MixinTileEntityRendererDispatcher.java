package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.module.modules.render.NoRenderModule;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcher {

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"), cancellable = true)
    public void onRenderTileEntityPre(TileEntity entity, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (NoRenderModule.INSTANCE.isEnabled()) {
            if (NoRenderModule.INSTANCE.tryReplaceEnchantingTable(entity) || NoRenderModule.INSTANCE.getEntityList().contains(entity.getClass())) {
                ci.cancel();
            }
        }
    }

}
