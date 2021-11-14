package me.han.muffin.client.utils.entity;


import me.han.muffin.client.core.Globals;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.io.InputStream;
import java.util.Scanner;

public class PlayerUtil {

    public static BlockPos getPlayerPosFloored(Entity player) {
        return new BlockPos(Math.floor(player.posX), Math.floor(player.posY), Math.floor(player.posZ));
    }

    public static boolean isPlayerTrapped(EntityPlayer player) {
        BlockPos playerPos = getPlayerPosFloored(player);

        final BlockPos[] trapPos = {
                playerPos.down(),
                playerPos.up().up(),
                playerPos.north(),
                playerPos.south(),
                playerPos.east(),
                playerPos.west(),
                playerPos.north().up(),
                playerPos.south().up(),
                playerPos.east().up(),
                playerPos.west().up(),
        };

        for (BlockPos pos : trapPos) {
            IBlockState iBlockState = Globals.mc.world.getBlockState(pos);
            if (iBlockState.getBlock() != Blocks.OBSIDIAN && Globals.mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK) return false;
        }

        return true;
    }

    public static boolean isPlayerInHole(EntityPlayer player) {
        BlockPos blockPos = getPlayerPosFloored(player);

        IBlockState blockState = Globals.mc.world.getBlockState(blockPos);

        if (blockState.getBlock() != Blocks.AIR)
            return false;

        if (Globals.mc.world.getBlockState(blockPos.up()).getBlock() != Blocks.AIR)
            return false;

        if (Globals.mc.world.getBlockState(blockPos.down()).getBlock() == Blocks.AIR)
            return false;

        final BlockPos[] touchingBlocks = new BlockPos[]{
                blockPos.north(),
                blockPos.south(),
                blockPos.east(),
                blockPos.west()
        };

        int validHorizontalBlocks = 0;
        for (BlockPos touching : touchingBlocks) {
            final IBlockState touchingState = Globals.mc.world.getBlockState(touching);
            if ((touchingState.getBlock() != Blocks.AIR) && touchingState.isFullBlock())
                validHorizontalBlocks++;
        }

        return validHorizontalBlocks >= 4;
    }


    public static boolean isInsideBlock() {
        for (int x = MathHelper.floor(Globals.mc.player.getEntityBoundingBox().minX); x < MathHelper.floor(Globals.mc.player.getEntityBoundingBox().maxX) + 1; x++) {
            for (int y = MathHelper.floor(Globals.mc.player.getEntityBoundingBox().minY); y < MathHelper.floor(Globals.mc.player.getEntityBoundingBox().maxY) + 1; y++) {
                for (int z = MathHelper.floor(Globals.mc.player.getEntityBoundingBox().minZ); z < MathHelper.floor(Globals.mc.player.getEntityBoundingBox().maxZ) + 1; z++) {
                    final Block block = Globals.mc.world.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (block == null || block instanceof BlockAir) {
                        continue;
                    }
                    if (block instanceof BlockTallGrass) {
                        return false;
                    }
                    AxisAlignedBB bb = Globals.mc.world.getBlockState(new BlockPos(x, y, z)).getSelectedBoundingBox(Globals.mc.world, new BlockPos(x, y, z));
                    if (bb != null && Globals.mc.player.getEntityBoundingBox().intersects(bb)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public enum FacingDirection {
        North,
        South,
        East,
        West,
    }

    public static FacingDirection getLocalPlayerFacing() {
        switch (MathHelper.floor((double) (Globals.mc.player.rotationYaw * 8.0F / 360.0F) + 0.5D) & 7) {
            case 0:
            case 1:
                return FacingDirection.South;
            case 2:
            case 3:
                return FacingDirection.West;
            case 4:
            case 5:
                return FacingDirection.North;
            case 6:
            case 7:
                return FacingDirection.East;
        }
        return FacingDirection.North;
    }

    public static String convertStreamToString(final InputStream is) {
        final Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "/";
    }

    public static void damageHypixel() {
        final NetHandlerPlayClient connection = Globals.mc.getConnection();
        if (connection == null || !Globals.mc.player.onGround) return;

        final double x = Globals.mc.player.posX;
        final double y = Globals.mc.player.posY;
        final double z = Globals.mc.player.posZ;

        for (int i = 0; i < 9; i++) {
            connection.sendPacket(new CPacketPlayer.Position(x, y + 0.4122222218322211111111F, z, false));
            connection.sendPacket(new CPacketPlayer.Position(x, y + 0.000002737272, z, false));
            connection.sendPacket(new CPacketPlayer(false));
        }
        connection.sendPacket(new CPacketPlayer(true));
    }

}