package me.han.muffin.client.utils.extensions.mc.world

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.extensions.mc.block.isLiquid
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.floor

fun World.isLiquidBelow(entity: Entity): Boolean {
    val results = rayTraceBoundingBoxToGround(entity, true)
    if (results.all { it.typeOfHit == RayTraceResult.Type.MISS || it.hitVec?.y ?: 911.0 < 0.0 }) return true

    val pos = results.maxByOrNull { it.hitVec?.y ?: -69420.0 }?.blockPos ?: return false
    return pos.isLiquid
}

private fun World.rayTraceBoundingBoxToGround(entity: Entity, stopOnLiquid: Boolean): List<RayTraceResult> {
    val boundingBox = entity.entityBoundingBox
    val xArray = arrayOf(floor(boundingBox.minX), floor(boundingBox.maxX))
    val zArray = arrayOf(floor(boundingBox.minZ), floor(boundingBox.maxZ))

    val results = ArrayList<RayTraceResult>(4)

    for (x in xArray)  for (z in zArray) {
        val result = rayTraceToGround(Vec3d(x, boundingBox.minY, z), stopOnLiquid)
        if (result != null) {
            results.add(result)
        }
    }

    return results
}

fun World.getGroundPos(entity: Entity): Vec3d {
    val results = rayTraceBoundingBoxToGround(entity, false)
    if (results.all { it.typeOfHit == RayTraceResult.Type.MISS || it.hitVec?.y ?: 911.0 < 0.0 }) {
        return Vec3d(0.0, -999.0, 0.0)
    }

    return results.maxByOrNull { it.hitVec?.y ?: -69420.0 }?.hitVec ?: Vec3d(0.0, -69420.0, 0.0)
}

private fun World.rayTraceToGround(vec3d: Vec3d, stopOnLiquid: Boolean): RayTraceResult? {
    return this.rayTrace(vec3d, Vec3d(vec3d.x, -1.0, vec3d.z), stopOnLiquid, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false)
}

fun World.isVisible(entity: Entity? = Globals.mc.player, pos: BlockPos, tolerance: Double = 1.0): Boolean = entity?.let {
    val center = pos.toVec3dCenter()
    val result = rayTrace(it.eyePosition, center, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true)

    result != null && (result.blockPos == pos || (result.hitVec != null && result.hitVec.distanceTo(center) <= tolerance))
} ?: false

fun World.rayTrace(start: Vec3d, end: Vec3d, stopOnLiquid: Boolean = false, ignoreBlockWithoutBoundingBox: Boolean = false, returnLastUncollidableBlock: Boolean = false): RayTraceResult? =
    this.rayTraceBlocks(start, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock)