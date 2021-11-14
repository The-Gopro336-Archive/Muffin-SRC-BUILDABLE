package me.han.muffin.client.mixin.mixins.world;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.render.ComputeVisibilityEvent;
import me.han.muffin.client.event.events.world.SetOpaqueCubeEvent;
import me.han.muffin.client.module.modules.player.FreecamModule;
import me.han.muffin.client.utils.math.VectorUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;
import java.util.Set;

@Mixin(value = VisGraph.class)
public abstract class MixinVisGraph {

    @Inject(method = "setOpaqueCube", at = @At("HEAD"), cancellable = true)
    public void onSetOpaqueCubePre(BlockPos pos, CallbackInfo ci) {
        SetOpaqueCubeEvent event = new SetOpaqueCubeEvent(); ///< pos is unused
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "computeVisibility", at = @At(value = "RETURN"), cancellable = true)
    private void onComputeVisibilityPost(CallbackInfoReturnable<SetVisibility> cir) {
        ComputeVisibilityEvent event = new ComputeVisibilityEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) cir.cancel();
    }

    @Inject(method = "getVisibleFacings", at = @At("HEAD"), cancellable = true)
    public void onGetVisibleFacingsPre(CallbackInfoReturnable<Set<EnumFacing>> cir) {
        if (FreecamModule.INSTANCE.isDisabled()) return;

        WorldClient world = Globals.mc.world;
        if (world == null) return;

        // Only do the hacky cave culling fix if inside of a block
        Vec3d camPos = RenderUtils.getCamPos();
        BlockPos pos = VectorUtils.INSTANCE.toBlockPos(camPos);
        if (world.getBlockState(pos).isFullBlock()) {
            cir.setReturnValue(EnumSet.allOf(EnumFacing.class));
        }
    }

}
