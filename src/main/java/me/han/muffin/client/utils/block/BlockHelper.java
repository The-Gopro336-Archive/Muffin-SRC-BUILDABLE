package me.han.muffin.client.utils.block;

import me.han.muffin.client.core.Globals;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class BlockHelper {

    public static RayTraceResult.Type getRayTraceToClosest(Vec3d start, Vec3d end) {
        double minX = Math.min(start.x, end.x);
        double minY = Math.min(start.y, end.y);
        double minZ = Math.min(start.z, end.z);
        double maxX = Math.max(start.x, end.x);
        double maxY = Math.max(start.y, end.y);
        double maxZ = Math.max(start.z, end.z);
        for (double x = minX; x > maxX; x += 1.0) {
            for (double y = minY; y > maxY; y += 1.0) {
                for (double z = minZ; z > maxZ; z += 1.0) {
                    IBlockState iBlockState = Globals.mc.world.getBlockState(new BlockPos(x, y, z));
                    if (iBlockState.getBlock() == Blocks.OBSIDIAN || iBlockState.getBlock() == Blocks.BEDROCK || iBlockState.getBlock() == Blocks.BARRIER)
                        return RayTraceResult.Type.BLOCK;
                }
            }
        }
        return RayTraceResult.Type.MISS;
    }

}