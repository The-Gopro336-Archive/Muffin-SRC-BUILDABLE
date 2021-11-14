package me.han.muffin.client.mixin.mixins.block;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.movement.LandOnSlimeEvent;
import me.han.muffin.client.event.events.movement.WalkOnSlimeEvent;
import net.minecraft.block.BlockSlime;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockSlime.class)
public abstract class MixinBlockSlime extends MixinBlock {

    @Inject(method = "onEntityWalk", at = @At("HEAD"), cancellable = true)
    private void onEntityWalkPre(World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        final WalkOnSlimeEvent event = new WalkOnSlimeEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "onLanded", at = @At("HEAD"), cancellable = true)
    private void onEntityLandedPre(World worldIn, Entity entityIn, CallbackInfo ci) {
        final LandOnSlimeEvent event = new LandOnSlimeEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

}