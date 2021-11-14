package me.han.muffin.client.mixin.mixins.render.entity;

import me.han.muffin.client.module.modules.render.EntityESPModule;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.entity.item.EntityMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderMinecart.class)
public abstract class MixinRenderMinecart<T extends EntityMinecart> {

    @Inject(method = "renderCartContents", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderCartContentsPre(EntityMinecart p_188319_1_, float partialTicks, IBlockState p_188319_3_, CallbackInfo ci) {
        if (!EntityESPModule.renderNameTags) ci.cancel();
    }

}