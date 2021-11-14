package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.module.modules.render.WallHackModule;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockModelRenderer.class)
public abstract class MixinBlockModelRenderer {
    @Shadow
    public abstract boolean renderModelSmooth(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder buffer, boolean checkSides, long rand);

    @Inject(method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z", at = @At(value = "HEAD"), cancellable = true)
    private void onRedirectRenderBlockSmooth(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder buffer, boolean checkSides, long rand, CallbackInfoReturnable<Boolean> cir) {
        if (WallHackModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(renderModelSmooth(worldIn, modelIn, stateIn, posIn, buffer, !WallHackModule.INSTANCE.containsBlock(stateIn.getBlock()), rand));
        }
    }

}