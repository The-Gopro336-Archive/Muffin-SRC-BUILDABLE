package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.RenderBlockEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher {

    @Inject(method = "renderBlock", at = @At(value = "HEAD"))
    private void onRenderBlockPre(IBlockState state, BlockPos pos, IBlockAccess blockAccess, BufferBuilder bufferBuilderIn, CallbackInfoReturnable<Boolean> cir) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderBlockEvent(state, pos));
    }

}