package me.han.muffin.client.mixin.mixins.block;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.RenderFluidEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockFluidRenderer.class)
public class MixinBlockFluidRenderer {

    @Inject(method = "renderFluid", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderFluid(IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn, CallbackInfoReturnable<Boolean> cir) {
        RenderFluidEvent event = new RenderFluidEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }

}
