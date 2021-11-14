package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.core.Globals;
import net.minecraft.client.renderer.debug.DebugRendererChunkBorder;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = DebugRendererChunkBorder.class)
public abstract class MixinDebugRendererChunkBorder {

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0))
    public EntityPlayer onRenderFirstLocal(EntityPlayer entityPlayer) {
        if (Globals.mc.getRenderViewEntity() instanceof EntityPlayer) {
            return (EntityPlayer) Globals.mc.getRenderViewEntity();
        } else {
            return Globals.mc.player;
        }
    }

    /*
    @Redirect(method = "render", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;player:Lnet/minecraft/client/entity/EntityPlayerSP;"))
    private EntityPlayerSP onRenderPlayer(Minecraft mc) {
        if (FreecamModule.INSTANCE != null && FreecamModule.INSTANCE.isEnabled()) {
            Entity entity = FreecamModule.INSTANCE.getCameraGuy() != null ? FreecamModule.INSTANCE.getCameraGuy() : mc.player;
            if (entity instanceof EntityPlayerSP) {
                return (EntityPlayerSP) entity;
            }
        }
        return mc.player;
    }
     */

}