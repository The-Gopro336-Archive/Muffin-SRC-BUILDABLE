package me.han.muffin.client.mixin.mixins.block;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.movement.CollideSoulSandEvent;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockSoulSand.class)
public abstract class MixinBlockSoulSand extends MixinBlock {

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void onEntityCollisionPre(World worldIn, BlockPos pos, IBlockState state, Entity entityIn, CallbackInfo ci) {
        final CollideSoulSandEvent event = new CollideSoulSandEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

}
