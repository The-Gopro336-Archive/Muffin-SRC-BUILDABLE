package me.han.muffin.client.mixin.mixins.world;

import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker {

    @Redirect(method = "processTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"))
    private Entity onGetRenderOrFreecamEntity(Minecraft mc) {
        return FreecamModule.INSTANCE.isEnabled() ? FreecamModule.INSTANCE.getCameraGuy() == null ? mc.player : FreecamModule.INSTANCE.getCameraGuy() : mc.getRenderViewEntity();
    }

}