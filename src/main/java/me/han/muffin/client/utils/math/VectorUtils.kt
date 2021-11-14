package me.han.muffin.client.utils.math

import me.han.muffin.client.utils.extensions.kotlin.ceilToInt
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.isReplaceable
import me.han.muffin.client.utils.extensions.mc.block.state
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import net.minecraft.block.BlockAir
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.*

object VectorUtils {

    /**
     * Gets distance between two vectors
     *
     * @param vecA First Vector
     * @param vecB Second Vector
     * @return the distance between two vectors
     */
    fun getDistance(vecA: Vec3d, vecB: Vec3d): Double {
        return sqrt((vecA.x - vecB.x).pow(2.0) + (vecA.y - vecB.y).pow(2.0) + (vecA.z - vecB.z).pow(2.0))
    }

    /**
     * Gets vectors between two given vectors (startVec and destinationVec) every (distance between the given vectors) / steps
     *
     * @param startVec Beginning vector
     * @param destinationVec Ending vector
     * @param steps distance between given vectors
     * @return all vectors between startVec and destinationVec divided by steps
     */
    fun extendVec(startVec: Vec3d, destinationVec: Vec3d, steps: Int): ArrayList<Vec3d> {
        val returnList = ArrayList<Vec3d>(steps + 1)
        val stepDistance = getDistance(startVec, destinationVec) / steps
        for (i in 0 until max(steps, 1) + 1) {
            returnList.add(advanceVec(startVec, destinationVec, stepDistance * i))
        }
        return returnList
    }

    /**
     * Moves a vector towards a destination based on distance
     *
     * @param startVec Starting vector
     * @param destinationVec returned vector
     * @param distance distance to move startVec by
     * @return vector based on startVec that is moved towards destinationVec by distance
     */
    fun advanceVec(startVec: Vec3d, destinationVec: Vec3d, distance: Double): Vec3d {
        val advanceDirection = destinationVec.subtract(startVec).normalize()
        return if (destinationVec.distanceTo(startVec) < distance) destinationVec
        else advanceDirection.scale(distance)
    }

    /**
     * Get all rounded block positions inside a 3-dimensional area between pos1 and pos2.
     *
     * @param pos1 Starting vector
     * @param pos2 Ending vector
     * @return rounded block positions inside a 3d area between pos1 and pos2
     */
    fun getBlockPositionsInArea(pos1: Vec3d, pos2: Vec3d): List<BlockPos> {
        val minX = (min(pos1.x, pos2.x)).roundToInt()
        val maxX = (max(pos1.x, pos2.x)).roundToInt()
        val minY = (min(pos1.y, pos2.y)).roundToInt()
        val maxY = (max(pos1.y, pos2.y)).roundToInt()
        val minZ = (min(pos1.z, pos2.z)).roundToInt()
        val maxZ = (max(pos1.z, pos2.z)).roundToInt()
        return getBlockPos(minX, maxX, minY, maxY, minZ, maxZ)
    }

    /**
     * Get all block positions inside a 3d area between pos1 and pos2
     *
     * @param pos1 Starting blockPos
     * @param pos2 Ending blockPos
     * @return block positions inside a 3d area between pos1 and pos2
     */
    fun getBlockPositionsInArea(pos1: BlockPos, pos2: BlockPos): List<BlockPos> {
        val minX = min(pos1.x, pos2.x)
        val maxX = max(pos1.x, pos2.x)
        val minY = min(pos1.y, pos2.y)
        val maxY = max(pos1.y, pos2.y)
        val minZ = min(pos1.z, pos2.z)
        val maxZ = max(pos1.z, pos2.z)
        return getBlockPos(minX, maxX, minY, maxY, minZ, maxZ)
    }

    /**
     * Get a block pos with the Y level as the highest terrain level
     *
     * @param pos blockPos
     * @return blockPos with highest Y level terrain
     */
    fun getHighestTerrainPos(pos: BlockPos): BlockPos {
        for (i in pos.y downTo 0) {
            val block = BlockPos(pos.x, i, pos.z).block
            val replaceable = BlockPos(pos.x, i, pos.z).state.isReplaceable
            if (block !is BlockAir && !replaceable) {
                return BlockPos(pos.x, i, pos.z)
            }
        }
        return BlockPos(pos.x, 0, pos.z)
    }

    private fun getBlockPos(minX: Int, maxX: Int, minY: Int, maxY: Int, minZ: Int, maxZ: Int): List<BlockPos> {
        val returnList = ArrayList<BlockPos>()
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    returnList.add(BlockPos(x, y, z))
                }
            }
        }
        return returnList
    }

    /**
     * Get all block positions inside a sphere with given [radius]
     *
     * @param center Center of the sphere
     * @param radius Radius of the sphere
     * @return block positions inside a sphere with given [radius]
     */
    fun getBlockPosInSphere(center: Vec3d, radius: Float): ArrayList<BlockPos> {
        val squaredRadius = radius.pow(2)
        val posList = ArrayList<BlockPos>()
        for (x in getAxisRange(center.x, radius)) for (y in getAxisRange(center.y, radius)) for (z in getAxisRange(center.z, radius)) {
            val blockPos = BlockPos(x, y, z)
            if (blockPos.distanceSqToCenter(center.x, center.y, center.z) > squaredRadius) continue
            posList.add(blockPos)
        }
        return posList
    }

    private fun getAxisRange(d1: Double, d2: Float): IntRange {
        return IntRange((d1 - d2).floorToInt(), (d1 + d2).ceilToInt())
    }

    fun Vec2f.toViewVec(): Vec3d {
        return RotationUtils.getVectorForRotation(this)
    }

    fun Vec3d.toBlockPos(): BlockPos {
        return BlockPos(x.floorToInt(), y.floorToInt(), z.floorToInt())
    }

    fun Vec3d.toBlockPos(xOffset: Int, yOffset: Int, zOffset: Int): BlockPos {
        return BlockPos(x.floorToInt() + xOffset, y.floorToInt() + yOffset, z.floorToInt() + zOffset)
    }

    fun Vec3i.toVec3d(): Vec3d {
        return toVec3d(0.0, 0.0, 0.0)
    }

    fun Vec3i.toVec3d(offSet: Vec3d): Vec3d {
        return Vec3d(x+ offSet.x, y + offSet.y, z + offSet.z)
    }

    fun Vec3i.toVec3d(xOffset: Double, yOffset: Double, zOffset: Double): Vec3d {
        return Vec3d(x + xOffset, y + yOffset, z + zOffset)
    }

    fun Vec3i.toVec3dCenter(): Vec3d {
        return toVec3dCenter(0.0, 0.0, 0.0)
    }

    fun Vec3i.toVec3dCenter(offSet: Vec3d): Vec3d {
        return Vec3d(x + 0.5 + offSet.x, y + 0.5 + offSet.y, z + 0.5 + offSet.z)
    }

    fun Vec3i.toVec3dCenter(xOffset: Double, yOffset: Double, zOffset: Double): Vec3d {
        return Vec3d(x + 0.5 + xOffset, y + 0.5 + yOffset, z + 0.5 + zOffset)
    }

    fun Vec3i.distanceTo(vec3i: Vec3i): Double {
        val xDiff = vec3i.x - x
        val yDiff = vec3i.y - y
        val zDiff = vec3i.z - z
        return sqrt((xDiff * xDiff + yDiff * yDiff + zDiff * zDiff).toDouble())
    }

    fun Vec3d.distanceTo(vec3i: Vec3i): Double {
        val xDiff = vec3i.x + 0.5 - x
        val yDiff = vec3i.y + 0.5 - y
        val zDiff = vec3i.z + 0.5 - z
        return sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
    }

    fun Entity.distanceTo(vec3i: Vec3i): Double {
        val xDiff = vec3i.x + 0.5 - posX
        val yDiff = vec3i.y + 0.5 - posY
        val zDiff = vec3i.z + 0.5 - posZ
        return sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
    }

    fun Entity.distanceTo(vec3d: Vec3d): Double {
        val xDiff = vec3d.x - posX
        val yDiff = vec3d.y - posY
        val zDiff = vec3d.z - posZ
        return sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
    }

    fun Vec3i.squareDistanceTo(vec3i: Vec3i): Double {
        val xDiff = vec3i.x - x
        val yDiff = vec3i.y - y
        val zDiff = vec3i.z - z
        return (xDiff * xDiff + yDiff * yDiff + zDiff * zDiff).toDouble()
    }

    fun Vec3d.squareDistanceTo(vec3i: Vec3i): Double {
        val xDiff = vec3i.x + 0.5 - x
        val yDiff = vec3i.y + 0.5 - y
        val zDiff = vec3i.z + 0.5 - z
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff
    }

    fun Entity.squareDistanceTo(vec3i: Vec3i): Double {
        val xDiff = vec3i.x + 0.5 - posX
        val yDiff = vec3i.y + 0.5 - posY
        val zDiff = vec3i.z + 0.5 - posZ
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff
    }

    fun Entity.squareDistanceTo(vec3d: Vec3d): Double {
        val xDiff = vec3d.x - posX
        val yDiff = vec3d.y - posY
        val zDiff = vec3d.z - posZ
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff
    }

    fun Entity.distanceTo(chunkPos: ChunkPos): Double {
        return hypot(chunkPos.x * 16 + 8 - posX, chunkPos.z * 16 + 8 - posZ)
    }

    fun Vec3i.multiply(multiplier: Int): Vec3i {
        val a = 20
        return Vec3i(x * multiplier, y * multiplier, z * multiplier)
    }

    infix operator fun Vec3d.times(vec3d: Vec3d): Vec3d = Vec3d(x * vec3d.x, y * vec3d.y, z * vec3d.z)
    infix operator fun Vec3d.times(multiplier: Double): Vec3d = Vec3d(x * multiplier, y * multiplier, z * multiplier)

    infix operator fun Vec3d.plus(vec3d: Vec3d): Vec3d = add(vec3d)
    infix operator fun Vec3d.minus(vec3d: Vec3d): Vec3d = subtract(vec3d)

}