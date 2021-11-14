package me.han.muffin.client.manager.managers;

import me.han.muffin.client.core.Globals;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class BlockManager {
    private static BlockPos blockPos = null;
    private static boolean _started = false;

    public static void SetCurrentBlock(BlockPos block) {
        blockPos = block;
        _started = false;
    }

    public static BlockPos GetCurrBlock() {
        return blockPos;
    }

    public static boolean GetState() {
        if (blockPos != null) return IsDoneBreaking(Globals.mc.world.getBlockState(blockPos));
        return false;
    }

    private static boolean IsDoneBreaking(IBlockState blockState) {
        return blockState.getBlock() == Blocks.BEDROCK
                || blockState.getBlock() == Blocks.AIR
                || blockState.getBlock() instanceof BlockLiquid;
    }

    public static boolean Update(float range, boolean rayTrace) {
        if (blockPos == null) return false;

        IBlockState state = Globals.mc.world.getBlockState(blockPos);

        if (IsDoneBreaking(state) || Globals.mc.player.getDistanceSq(blockPos) > Math.pow(range, range)) {
            blockPos = null;
            return false;
        }

        // CPacketAnimation
        Globals.mc.player.swingArm(EnumHand.MAIN_HAND);

        EnumFacing facing = EnumFacing.UP;

        if (rayTrace) {
            RayTraceResult result = Globals.mc.world.rayTraceBlocks(
                    new Vec3d(Globals.mc.player.posX, Globals.mc.player.posY + Globals.mc.player.getEyeHeight(), Globals.mc.player.posZ),
                    new Vec3d(blockPos.getX() + 0.5, blockPos.getY() - 0.5,
                            blockPos.getZ() + 0.5));

            if (result != null && result.sideHit != null)
                facing = result.sideHit;
        }

        if (!_started) {
            _started = true;
            // Start Break

            Globals.mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, facing));
        } else {
            Globals.mc.playerController.onPlayerDamageBlock(blockPos, facing);
        }

        return true;
    }
}