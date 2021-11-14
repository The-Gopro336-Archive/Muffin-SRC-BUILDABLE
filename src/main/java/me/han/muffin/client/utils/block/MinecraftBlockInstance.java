package me.han.muffin.client.utils.block;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.module.modules.player.InteractionTweaksModule;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public class MinecraftBlockInstance {

    /**
     * Essentially the same as {@link net.minecraft.world.World#getBlockDensity(Vec3d, AxisAlignedBB)}
     * but with an option to ignore webs and beds.
     *
     * @param vec we check the blocks along this vector.
     * @param bb the bounding box inside which we check.
     * @param ignoreWebs if you want to ignore webs.
     * @return the percentage of real blocks within the given parameters.
     */
    public static float getBlockDensity(World world, Vec3d vec, AxisAlignedBB bb, boolean ignoreWebs, boolean ignoreBeds, boolean ignoreTripwire, boolean ignorePortal) {
        double x = 1.0D / ((bb.maxX - bb.minX) * 2.0D + 1.0D);
        double y = 1.0D / ((bb.maxY - bb.minY) * 2.0D + 1.0D);
        double z = 1.0D / ((bb.maxZ - bb.minZ) * 2.0D + 1.0D);
        double xFloor = (1.0D - Math.floor(1.0D / x) * x) / 2.0D;
        double zFloor = (1.0D - Math.floor(1.0D / z) * z) / 2.0D;

        if (x >= 0.0D && y >= 0.0D && z >= 0.0D) {
            int air = 0;
            int traced = 0;

            for (float a = 0.0F; a <= 1.0F; a = (float) ((double) a + x)) {
                for (float b = 0.0F; b <= 1.0F; b = (float) ((double) b + y)) {
                    for (float c = 0.0F; c <= 1.0F; c = (float) ((double) c + z)) {
                        double xOff = bb.minX + (bb.maxX - bb.minX) * (double) a;
                        double yOff = bb.minY + (bb.maxY - bb.minY) * (double) b;
                        double zOff = bb.minZ + (bb.maxZ - bb.minZ) * (double) c;

                        RayTraceResult result = rayTraceBlocks(world, new Vec3d(xOff + xFloor, yOff, zOff + zFloor), vec, false, false, false, InteractionTweaksModule.isLiquidInteractEnabled(), ignoreWebs, ignoreBeds, ignoreTripwire, ignorePortal);
                        if (result == null) {
                            air++;
                        }
                        traced++;
                    }
                }
            }
            return (float) air / (float) traced;
        } else {
            return 0.0F;
        }
    }

    /**
     * Essentially the same as {@link World#rayTraceBlocks(Vec3d, Vec3d, boolean, boolean, boolean)} (Vec3d, AxisAlignedBB)}
     * but with an option to ignore webs and beds.
     *
     * @param start same as the original param.
     * @param end same as the original param.
     * @param stopOnLiquid same as the original param.
     * @param ignoreBlockWithoutBoundingBox same as the original param.
     * @param returnLastUncollidableBlock same as the original param.
     * @param ignoreWebs handles webs like air.
     * @param ignoreBeds handles beds like air.
     * @return {@link World#rayTraceBlocks(Vec3d, Vec3d, boolean, boolean, boolean)} (Vec3d, AxisAlignedBB)} for the given params.
     */
    public static RayTraceResult rayTraceBlocks(World world,  Vec3d start, Vec3d end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, boolean isLiquidInteractOn, boolean ignoreWebs, boolean ignoreBeds, boolean ignoreTripWire, boolean ignorePortal) {
        if (!Double.isNaN(start.x) && !Double.isNaN(start.y) && !Double.isNaN(start.z)) {
            if (!Double.isNaN(end.x) && !Double.isNaN(end.y) && !Double.isNaN(end.z)) {
                int currentX = MathHelper.floor(start.x);
                int currentY = MathHelper.floor(start.y);
                int currentZ = MathHelper.floor(start.z);

                int endX = MathHelper.floor(end.x);
                int endY = MathHelper.floor(end.y);
                int endZ = MathHelper.floor(end.z);

                BlockPos currentBlockPos = new BlockPos(currentX, currentY, currentZ);
                IBlockState startBlockState = world.getBlockState(currentBlockPos);
                Block startBlock = startBlockState.getBlock();

                if ((!ignoreBlockWithoutBoundingBox || startBlockState.getCollisionBoundingBox(world, currentBlockPos) != Block.NULL_AABB) &&
                        (startBlock.canCollideCheck(startBlockState, stopOnLiquid)
                                && !(
                                                isLiquidInteractOn && startBlock instanceof BlockLiquid ||
                                                ignoreBeds && startBlock instanceof BlockBed ||
                                                ignoreWebs && startBlock instanceof BlockWeb ||
                                                ignoreTripWire && startBlock instanceof BlockTripWire ||
                                                ignorePortal && startBlock instanceof BlockPortal)))
                {
                    RayTraceResult result = startBlockState.collisionRayTrace(world, currentBlockPos, start, end);
                    // noinspection ConstantConditions
                    if (result != null) return result;
                }

                RayTraceResult lastResult = null;
                int k1 = 200;

                while (k1-- >= 0) {
                    if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z)) {
                        return null;
                    }

                    if (currentX == endX && currentY == endY && currentZ == endZ) {
                        return returnLastUncollidableBlock ? lastResult : null;
                    }

                    boolean xFlag = true;
                    boolean yFlag = true;
                    boolean zFlag = true;

                    double nextX = 999.0D;
                    double nextY = 999.0D;
                    double nextZ = 999.0D;

                    if (endX > currentX) {
                        nextX = (double) currentX + 1.0D;
                    } else if (endX < currentX) {
                        nextX = (double) currentX + 0.0D;
                    } else {
                        xFlag = false;
                    }

                    if (endY > currentY) {
                        nextY = (double)currentY + 1.0D;
                    } else if (endY < currentY) {
                        nextY = (double)currentY + 0.0D;
                    } else {
                        yFlag = false;
                    }

                    if (endZ > currentZ) {
                        nextZ = (double) currentZ + 1.0D;
                    } else if (endZ < currentZ) {
                        nextZ = (double) currentZ + 0.0D;
                    } else {
                        zFlag = false;
                    }

                    double diffX = 999.0D;
                    double diffY = 999.0D;
                    double diffZ = 999.0D;

                    double totalDiffX = end.x - start.x;
                    double totalDiffY = end.y - start.y;
                    double totalDiffZ = end.z - start.z;

                    if (xFlag) diffX = (nextX - start.x) / totalDiffX;
                    if (yFlag) diffY = (nextY - start.y) / totalDiffY;
                    if (zFlag) diffZ = (nextZ - start.z) / totalDiffZ;


                    if (diffX == -0.0D) diffX = -1.0E-4D;
                    if (diffY == -0.0D) diffY = -1.0E-4D;
                    if (diffZ == -0.0D) diffZ = -1.0E-4D;

                    EnumFacing side;
                    if (diffX < diffY && diffX < diffZ) {
                        side = endX > currentX ? EnumFacing.WEST : EnumFacing.EAST;
                        start = new Vec3d(nextX, start.y + totalDiffY * diffX, start.z + totalDiffZ * diffX);
                    } else if (diffY < diffZ) {
                        side = endY > currentY ? EnumFacing.DOWN : EnumFacing.UP;
                        start = new Vec3d(start.x + totalDiffX * diffY, nextY, start.z + totalDiffZ * diffY);
                    } else {
                        side = endZ > currentZ ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        start = new Vec3d(start.x + totalDiffX * diffZ, start.y + totalDiffY * diffZ, nextZ);
                    }

                    currentX = MathHelper.floor(start.x) - (side == EnumFacing.EAST ? 1 : 0);
                    currentY = MathHelper.floor(start.y) - (side == EnumFacing.UP ? 1 : 0);
                    currentZ = MathHelper.floor(start.z) - (side == EnumFacing.SOUTH ? 1 : 0);
                    currentBlockPos = new BlockPos(currentX, currentY, currentZ);

                    IBlockState state = world.getBlockState(currentBlockPos);
                    Block block = state.getBlock();

                    if (!ignoreBlockWithoutBoundingBox || state.getMaterial() == Material.PORTAL || state.getCollisionBoundingBox(world, currentBlockPos) != Block.NULL_AABB) {
                        if (block.canCollideCheck(state, stopOnLiquid) &&
                                !(      isLiquidInteractOn && block instanceof BlockLiquid ||
                                        ignoreBeds && block instanceof BlockBed ||
                                        ignoreWebs && block instanceof BlockWeb ||
                                        ignoreTripWire && startBlock instanceof BlockTripWire ||
                                        ignorePortal && startBlock instanceof BlockPortal
                                )) {
                            RayTraceResult result = state.collisionRayTrace(world, currentBlockPos, start, end);
                            //noinspection ConstantConditions
                            if (result != null) return result;
                        } else {
                            lastResult = new RayTraceResult(RayTraceResult.Type.MISS, start, side, currentBlockPos);
                        }
                    }
                }

                return returnLastUncollidableBlock ? lastResult : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean canBeBlown(BlockPos pos) {
        return Globals.mc.world.getBlockState(pos).getBlock().getExplosionResistance(Globals.mc.world, pos, null, null) >= 19.7;
    }

}