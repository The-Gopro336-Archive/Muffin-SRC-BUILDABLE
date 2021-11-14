package me.han.muffin.client.utils.extensions.mc.utils

import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.function.BiPredicate

fun RayTraceResult.isEqualTo(pos: BlockPos) = typeOfHit == RayTraceResult.Type.BLOCK && blockPos == pos

fun World.rayTraceBlockC(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50,
    predicate: BiPredicate<BlockPos, IBlockState> = BiPredicate { _, state -> state.block != Blocks.AIR && CrystalUtils.isResistant(state) }
): RayTraceResult? {
    if (start.isNaN() || end.isNaN()) {
        return null
    }

    // Int start position
    val startX = start.x.floorToInt()
    val startY = start.y.floorToInt()
    val startZ = start.z.floorToInt()

    // Raytrace start block
    val startResult = rayTraceBlock(start, end, startX, startY, startZ, predicate)
    if (startResult != null) return startResult

    // Int end position
    val endX = end.x.floorToInt()
    val endY = end.y.floorToInt()
    val endZ = end.z.floorToInt()

    var current = start
    var currentX = startX
    var currentY = startY
    var currentZ = startZ

    var count = maxAttempt

    while (count-- >= 0) {
        if (currentX == endX && currentY == endY && currentZ == endZ) {
            return null
        }

        var xFlag = true
        var yFlag = true
        var zFlag = true

        var nextX = 999
        var nextY = 999
        var nextZ = 999

        when {
            endX > currentX -> nextX = currentX + 1
            endX < currentX -> nextX = currentX
            else -> xFlag = false
        }

        when {
            endY > currentY -> nextY = currentY + 1
            endY < currentY -> nextY = currentY
            else -> yFlag = false
        }

        when {
            endZ > currentZ -> nextZ = currentZ + 1
            endZ < currentZ -> nextZ = currentZ
            else -> zFlag = false
        }

        var stepX = 999.0
        var stepY = 999.0
        var stepZ = 999.0
        val diffX = end.x - current.x
        val diffY = end.y - current.y
        val diffZ = end.z - current.z
        if (xFlag) {
            stepX = (nextX - current.x) / diffX
        }

        if (yFlag) {
            stepY = (nextY - current.y) / diffY
        }

        if (zFlag) {
            stepZ = (nextZ - current.z) / diffZ
        }

        if (stepX < stepY && stepX < stepZ) {
            val y = current.y + diffY * stepX
            val z = current.z + diffZ * stepX

            current = Vec3d(nextX.toDouble(), y, z)

            currentX = if (endX < currentX) nextX - 1 else nextX
            currentY = y.floorToInt()
            currentZ = z.floorToInt()
        } else if (stepY < stepZ) {
            val x = current.x + diffX * stepY
            val z = current.z + diffZ * stepY

            current = Vec3d(x, nextY.toDouble(), z)

            currentX = x.floorToInt()
            currentY = if (endY < currentY) nextY - 1 else nextY
            currentZ = z.floorToInt()
        } else {
            val x = current.x + diffX * stepZ
            val y = current.y + diffY * stepZ

            current = Vec3d(x, y, nextZ.toDouble())

            currentX = x.floorToInt()
            currentY = y.floorToInt()
            currentZ = if (endZ < currentZ) nextZ - 1 else nextZ
        }

        val result = rayTraceBlock(current, end, currentX, currentY, currentZ, predicate)
        if (result != null) return result
    }

    return null
}

private fun World.rayTraceBlock(start: Vec3d, end: Vec3d, x: Int, y: Int, z: Int, predicate: BiPredicate<BlockPos, IBlockState>): RayTraceResult? {
    val currentBlockPos = BlockPos(x, y, z)
    val blockState = this.getBlockState(currentBlockPos)

    return if (blockState.block != Blocks.AIR && predicate.test(currentBlockPos, blockState)) {
        blockState.collisionRayTrace(this, currentBlockPos, start, end)
    } else {
        null
    }
}