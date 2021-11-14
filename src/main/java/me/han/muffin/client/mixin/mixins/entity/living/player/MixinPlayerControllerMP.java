package me.han.muffin.client.mixin.mixins.entity.living.player;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.client.AttackEvent;
import me.han.muffin.client.event.events.client.AttackSyncEvent;
import me.han.muffin.client.event.events.entity.HittingPositionEvent;
import me.han.muffin.client.event.events.entity.PlayerSyncCurrentItemEvent;
import me.han.muffin.client.event.events.entity.SyncCurrentPlayItemEvent;
import me.han.muffin.client.event.events.entity.player.PlayerOnStoppedUsingItemEvent;
import me.han.muffin.client.event.events.entity.player.RightClickEvent;
import me.han.muffin.client.event.events.world.AllowInteractEvent;
import me.han.muffin.client.event.events.world.block.*;
import me.han.muffin.client.imixin.entity.IPlayerControllerMP;
import me.han.muffin.client.module.modules.exploits.ReachModule;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP implements IPlayerControllerMP {

    @Shadow private GameType currentGameType;
    @Shadow private int blockHitDelay;
    @Shadow private float curBlockDamageMP;
    @Shadow private boolean isHittingBlock;
    @Shadow private int currentPlayerItem;

    @Override
    @Accessor(value = "curBlockDamageMP")
    public abstract float getCurBlockDamageMP();

    @Override
    @Accessor(value = "curBlockDamageMP")
    public abstract void setCurBlockDamageMP(float damageMP);

    @Override
    @Accessor(value = "blockHitDelay")
    public abstract int getBlockHitDelay();

    @Override
    @Accessor(value = "blockHitDelay")
    public abstract void setBlockHitDelay(int hitDelay);

    @Override
    @Accessor(value = "currentPlayerItem")
    public abstract int getCurrentPlayerItem();

    @Override
    @Accessor(value = "isHittingBlock")
    public abstract void setHittingBlock(boolean isHittingBlock);

    @Inject(method = "getBlockReachDistance", at = @At("HEAD"), cancellable = true)
    public void onGetBlockReachDistancePre(CallbackInfoReturnable<Float> cir) {
        if (ReachModule.INSTANCE.isEnabled()) {
            float attrib = (float) Globals.mc.player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue() + (ReachModule.INSTANCE.isEnabled() ? ReachModule.INSTANCE.getReach().getValue() : 0);
            cir.setReturnValue(this.currentGameType.isCreative() ? attrib : attrib - 0.5F);
        }
    }

    @Inject(method = "onPlayerDamageBlock", at = @At("HEAD"), cancellable = true)
    public void onPlayerDamageBlockPre(BlockPos posBlock, EnumFacing directionFacing, CallbackInfoReturnable<Boolean> ci) {
        DamageBlockEvent event = new DamageBlockEvent(posBlock, directionFacing);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            ci.setReturnValue(false);
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onPlayerDestroyBlockPre(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        DestroyBlockEvent event = new DestroyBlockEvent((PlayerControllerMP) (Object) this, pos);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Redirect(method = "onPlayerDamageBlock", at = @At(value = "INVOKE", target = "net/minecraft/client/multiplayer/PlayerControllerMP.syncCurrentPlayItem()V"))
    private void onPlayerDamageBlockAndSyncItem(PlayerControllerMP playerMP, BlockPos pos, EnumFacing facing) {
        PlayerSyncCurrentItemEvent event = new PlayerSyncCurrentItemEvent(pos.getX(), pos.getY(), pos.getZ(), blockHitDelay, curBlockDamageMP, facing, pos);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        syncCurrentPlayItemVoid();
        if (blockHitDelay != event.getBlockHitDelay()) {
            blockHitDelay = event.getBlockHitDelay();
        }
        if (curBlockDamageMP != event.getCurBlockDamage()) {
            curBlockDamageMP = event.getCurBlockDamage();
        }
    }

  //  @Redirect(method = "clickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/NetHandlerPlayClient;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 1))
  //  private void onAbort(NetHandlerPlayClient netHandlerPlayClient, Packet<?> packetIn) {
  //  }

 //   @Redirect(method = "resetBlockRemoving", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/NetHandlerPlayClient;sendPacket(Lnet/minecraft/network/Packet;)V"))
 //   private void onAbort(NetHandlerPlayClient netHandlerPlayClient, Packet<?> packetIn) {
 //   }

    @Inject(method = "clickBlock", at = @At("HEAD"), cancellable = true)
    public void onClickBlockPre(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        ClickBlockEvent event = new ClickBlockEvent(loc, face);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

   // @Inject(method = "clickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/NetHandlerPlayClient;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 2, shift = At.Shift.AFTER), cancellable = true)
   // public void clickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
   //     ClickBlockEvent event = new ClickBlockEvent(loc, face);
   //     Muffin.getInstance().getEventManager().dispatchEvent(event);
   //     if (event.isCanceled()) {
   //         cir.setReturnValue(false);
   //         cir.cancel();
  //      }
  //  }


    @Inject(method = "resetBlockRemoving", at = @At("HEAD"), cancellable = true)
    private void onResetBlockWrapperPre(CallbackInfo ci) {
        ResetBlockRemovingEvent event = new ResetBlockRemovingEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "getIsHittingBlock", at = @At("HEAD"), cancellable = true)
    private void onGetIsHittingBlockPre(CallbackInfoReturnable<Boolean> cir) {
        AllowInteractEvent event = new AllowInteractEvent(isHittingBlock);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        cir.setReturnValue(event.isUsingItem());
    }

    @Inject(method = "attackEntity", at = @At(value = "HEAD"), cancellable = true)
    private void onAttackEntityPre(EntityPlayer entityPlayer, Entity targetEntity, CallbackInfo ci) {
        if (targetEntity == null) return;
        AttackEvent event = new AttackEvent(targetEntity);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "attackEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;syncCurrentPlayItem()V"))
    private void onAttackEntitySync(EntityPlayer entityPlayer, Entity targetEntity, CallbackInfo ci) {
        if (targetEntity == null) return;
        AttackSyncEvent event = new AttackSyncEvent(targetEntity);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "syncCurrentPlayItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;connection:Lnet/minecraft/client/network/NetHandlerPlayClient;"))
    private void onSyncPlayerItemSendPacket(CallbackInfo ci) {
        SyncCurrentPlayItemEvent event = new SyncCurrentPlayItemEvent(currentPlayerItem);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "onPlayerDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playEvent(ILnet/minecraft/util/math/BlockPos;I)V"), cancellable = true)
    public void onPlayerDestroyBlockPos(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        PlayerBlockBreakEvent event = new PlayerBlockBreakEvent(pos);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(false);
            if (!cir.isCancelled()) cir.cancel();
        }
    }

    /*
    @Inject(method = "onPlayerDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;removedByPlayer(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/EntityPlayer;Z)Z"),remap = false, cancellable = true)
    @Dynamic
    private void onBlockRemovedByPlayer(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        OnBlockRemovedByPlayerEvent event = new OnBlockRemovedByPlayerEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) cir.setReturnValue(false);
    }
     */

    @Inject(method = "processRightClickBlock", at = @At(value= "HEAD"), cancellable = true)
    private void onProcessRightClickBlockPre(EntityPlayerSP player, WorldClient worldIn, BlockPos pos, EnumFacing direction, Vec3d vec, EnumHand hand, CallbackInfoReturnable<EnumActionResult> cir) {
        RightClickBlockEvent event = new RightClickBlockEvent(pos, direction, vec, hand);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(EnumActionResult.FAIL);
            cir.cancel();
        }
    }

    @Inject(method = "processRightClick", at = @At(value= "HEAD"), cancellable = true)
    private void onProcessRightClickPre(EntityPlayer player, World worldIn, EnumHand hand, CallbackInfoReturnable<EnumActionResult> cir) {
        RightClickEvent event = new RightClickEvent(hand);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(EnumActionResult.FAIL);
            cir.cancel();
        }
    }

    @Inject(method = "isHittingPosition", at = @At(value = "HEAD"), cancellable = true)
    private void onIsHittingPositionPre(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        HittingPositionEvent event = new HittingPositionEvent(pos);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "onStoppedUsingItem", at = @At(value = "HEAD"), cancellable = true)
    public void onPlayerStoppedUsingItemPre(EntityPlayer playerIn, CallbackInfo ci) {
        PlayerOnStoppedUsingItemEvent event = new PlayerOnStoppedUsingItemEvent(playerIn);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

}
