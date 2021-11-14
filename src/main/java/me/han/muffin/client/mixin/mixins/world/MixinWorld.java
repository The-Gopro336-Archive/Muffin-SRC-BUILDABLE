package me.han.muffin.client.mixin.mixins.world;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.GetSkyColourEvent;
import me.han.muffin.client.event.events.world.RainStrengthEvent;
import me.han.muffin.client.event.events.world.RenderSkyLightEvent;
import me.han.muffin.client.event.events.world.WorldEntityEvent;
import me.han.muffin.client.event.events.world.WorldPlaySoundEvent;
import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(value = World.class)
public abstract class MixinWorld {

    @Inject(method = "onEntityAdded", at = @At("HEAD"))
    private void onEntityAddedPre(Entity entityIn, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new WorldEntityEvent.Add(entityIn));
    }

    @Inject(method = "onEntityRemoved", at = @At("HEAD"))
    private void onEntityRemovedPost(Entity entityIn, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new WorldEntityEvent.Remove(entityIn));
    }

    @Inject(method = "playSound(Lnet/minecraft/entity/player/EntityPlayer;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V", at = @At("HEAD"), cancellable = true)
    public void onWorldPlaySoundForPlayerPre(@Nullable EntityPlayer player, double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch, CallbackInfo ci) {
        WorldPlaySoundEvent event = new WorldPlaySoundEvent(soundIn, volume, pitch);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "checkLightFor", at = @At("HEAD"), cancellable = true)
    private void onCheckLightForPre(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        RenderSkyLightEvent event = new RenderSkyLightEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled() && lightType == EnumSkyBlock.SKY) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true)
    private void onGetRainStrengthPre(float delta, CallbackInfoReturnable<Float> cir) {
        RainStrengthEvent event = new RainStrengthEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderStrength", at = @At("HEAD"), cancellable = true)
    private void onGetThunderStrengthPre(float delta, CallbackInfoReturnable<Float> cir) {
        RainStrengthEvent event = new RainStrengthEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    public void onGetSkyColorPre(Entity entityIn, float partialTicks, CallbackInfoReturnable<Vec3d> cir) {
        GetSkyColourEvent event = new GetSkyColourEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.cancel();
            cir.setReturnValue(event.getVec3d());
        }
    }

//    @Inject(method = "checkNoEntityCollision(Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/entity/Entity;)Z", at = @At(value = "JUMP", opcode = Opcodes.IFNE, ordinal = 0, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
//    private void onCheckEntityCollisionPre(AxisAlignedBB bb, Entity entityIn, CallbackInfoReturnable<Boolean> cir, List<Entity> a, Entity foundedEntity) {
//        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() == foundedEntity) {
//            cir.setReturnValue(true);
//            cir.cancel();
//        }
//    }

    @Inject(method = "checkNoEntityCollision(Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/entity/Entity;)Z", at = @At(value = "RETURN", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void onCheckEntityCollisionPreReturn(AxisAlignedBB bb, Entity entityIn, CallbackInfoReturnable<Boolean> cir, List<Entity> excludingList, int iterator, Entity foundedEntity) {
        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() == foundedEntity) {
            cir.setReturnValue(true);
        }
    }

//    @ModifyArg(method = "getEntitiesInAABBexcluding", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getEntitiesWithinAABBForEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Lcom/google/common/base/Predicate;)V"), index = 0)
//    private Entity onGetEntitiesWithinAABBEntityPreInvokeArg(Entity paramEntity) {
//        if (FreecamModule.INSTANCE.isEnabled()) return FreecamModule.INSTANCE.getCameraGuy();
//        else return paramEntity;
//    }

//    @ModifyArg(method = "getEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getEntitiesOfTypeWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Lcom/google/common/base/Predicate;)V"), index = 0)
//    private <T extends Entity> Class <? extends T> onGetEntitiesWithinAABBPreInvokeArg(Class <? extends T> paramEntityClass) {
//        // noinspection ConstantModification
//        if (FreecamModule.INSTANCE.isEnabled()) return (Class<? extends T>) FreecamModule.INSTANCE.getCameraGuy().getClass();
//        else return paramEntityClass;
//    }

}