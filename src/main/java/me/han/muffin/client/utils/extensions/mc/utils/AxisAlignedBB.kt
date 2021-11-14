package me.han.muffin.client.utils.extensions.mc.utils

import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.math.VectorUtils.plus
import me.han.muffin.client.utils.math.VectorUtils.times
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d

val AxisAlignedBB.xLength get() = maxX - minX
val AxisAlignedBB.yLength get() = maxY - minY
val AxisAlignedBB.zLength get() = maxY - minY

val AxisAlignedBB.lengths get() = Vec3d(xLength, yLength, zLength)

fun AxisAlignedBB.expand(vec: Vec3d) = this.expand(vec.x, vec.y, vec.z)

fun AxisAlignedBB.corners(scale: Double) : Array<Vec3d> {
    val growSizes = lengths * (scale - 1.0)
    return grow(growSizes.x, growSizes.y, growSizes.z).corners()
}

fun AxisAlignedBB.corners() = arrayOf(
    Vec3d(minX, minY, minZ),
    Vec3d(minX, minY, maxZ),
    Vec3d(minX, maxY, minZ),
    Vec3d(minX, maxY, maxZ),
    Vec3d(maxX, minY, minZ),
    Vec3d(maxX, minY, maxZ),
    Vec3d(maxX, maxY, minZ),
    Vec3d(maxX, maxY, maxZ)
)

fun AxisAlignedBB.side(side: EnumFacing, scale: Double = 0.5) : Vec3d {
    val lengths = lengths
    val sideDirectionVec = side.directionVec.toVec3d()
    return lengths * sideDirectionVec * scale + center
}

/**
 * Check if a block is in sight
 */
fun AxisAlignedBB.isInSight(
    posFrom: Vec3d = Globals.mc.player?.eyePosition ?: Vec3d.ZERO,
    rotation: Vec2f = LocalMotionManager.serverSideRotation, // Globals.mc.player?.let { Vec2f(it) } ?: Vec2f.ZERO,
    range: Double = 4.25,
    tolerance: Double = 1.05
): RayTraceResult? = isInSight(posFrom, RotationUtils.getVectorForRotation(rotation), range, tolerance)

/**
 * Check if a block is in sight
 */
fun AxisAlignedBB.isInSight(
    posFrom: Vec3d,
    viewVec: Vec3d,
    range: Double = 4.25,
    tolerance: Double = 1.05
): RayTraceResult? {
    val sightEnd = posFrom.add(viewVec.scale(range))
    return grow(tolerance - 1.0).calculateIntercept(posFrom, sightEnd)
}

fun BlockPos.isPosInSight(): RayTraceResult? {
    val startVec = Globals.mc.player.eyePosition
    val lookingDirection = RotationUtils.getVectorForRotation(LocalMotionManager.serverSideRotation)

    val distanceTo = startVec.distanceTo(this.toVec3dCenter())
    val endVec = startVec.add(lookingDirection.scale(distanceTo))

    return AxisAlignedBB(this.x - 0.1, this.y - 0.1, this.z - 0.1, this.x + 1.1, this.y + 1.1, this.z + 1.1)
        .calculateIntercept(startVec, endVec)
}