package me.han.muffin.client.utils.math

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.extensions.kotlin.step
import me.han.muffin.client.utils.extensions.mc.block.getHitVec
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.utils.expand
import me.han.muffin.client.utils.extensions.mc.utils.rayTraceBlockC
import me.han.muffin.client.utils.math.VectorUtils.times
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d

object RayTraceUtils {
//    fun raytraceEntity(range: Double, rotation: Vec2f, filter: (Entity) -> Boolean): Entity? {
//        val entity: Entity = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return null
//
//        val cameraVec = entity.eyePosition
//        val rotationVec = RotationUtils.getVectorForRotation(rotation)
//
//        val vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)
//        val box = entity.entityBoundingBox.expand(rotationVec * range).expand(1.0, 1.0, 1.0)
//
//        val entityHitResult = Globals.mc.world.getEntitiesInAABBexcluding(entity, cameraVec, vec3d3, box, { !it.isSpectator && it.collides() && filter(it) }, range * range)
//
//        return entityHitResult?.entity
//    }

    /**
     * Allows you to check if a point is behind a wall
     */
    fun isVisible(eyes: Vec3d, vec: Vec3d): Boolean {
        val result = Globals.mc.world.rayTraceBlocks(eyes, vec)
        return result?.typeOfHit == RayTraceResult.Type.MISS // result == null || result.typeOfHit == RayTraceResult.Type.MISS
    }

    /**
     * Allows you to check if a point is behind a wall
     */
    fun facingBlock(eyes: Vec3d, vec: Vec3d, blockPos: BlockPos): Boolean {
        val searchedPos = Globals.mc.world.rayTraceBlocks(eyes, vec) ?: return false
        if (searchedPos.typeOfHit != RayTraceResult.Type.BLOCK) return false
        return searchedPos.blockPos == blockPos
    }

    fun getRaytraceEntity(range: Double, rotation: Vec2f): Entity? {
        val renderViewEntity = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return null

        var pointedEntity: Entity? = null
        var blockReachDistance = range

        val startVec = renderViewEntity.eyePosition
        val lookingVec = RotationUtils.getVectorForRotation(rotation)
        val endVec = startVec.add(lookingVec.x * blockReachDistance, lookingVec.y * blockReachDistance, lookingVec.z * blockReachDistance)

        val entityList = Globals.mc.world.getEntitiesInAABBexcluding(renderViewEntity,
            renderViewEntity.entityBoundingBox.expand(
                lookingVec.x * blockReachDistance,
                lookingVec.y * blockReachDistance,
                lookingVec.z * blockReachDistance)
                .expand(1.0, 1.0, 1.0)) {
            it != null && (it !is EntityPlayer || !it.isSpectator) && it.canBeCollidedWith()
        }

        for (entity in entityList) {
            val collisionBorderSize = entity.collisionBorderSize.toDouble()

            val hitbox = entity.entityBoundingBox.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize)
            val interceptResult = hitbox.calculateIntercept(startVec, endVec)

            if (hitbox.contains(startVec)) {
                if (blockReachDistance >= 0.0) {
                    pointedEntity = entity
                    blockReachDistance = 0.0
                }
            } else if (interceptResult != null) {
                val eyeDistance = startVec.distanceTo(interceptResult.hitVec)

                if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                    if (entity == renderViewEntity.ridingEntity && !renderViewEntity.canRiderInteract()) {
                        if (blockReachDistance == 0.0) pointedEntity = entity
                    } else {
                        pointedEntity = entity
                        blockReachDistance = eyeDistance
                    }
                }

            }
        }

        return pointedEntity
    }

    fun rayTracePlaceCheck(entity: Entity, pos: BlockPos) = isHitVecVisibleToFacing(entity, pos, false) != null

    fun isHitVecVisibleToFacing(entity: Entity, pos: BlockPos, verticals: Boolean): EnumFacing? {
        return EnumFacing.values().firstOrNull { facing ->
            val hitVec = pos.getHitVec(facing)
            val result = Globals.mc.world.rayTraceBlocks(entity.eyePosition, hitVec, false, true, false)
            result != null && result.typeOfHit == RayTraceResult.Type.BLOCK && result.blockPos == pos
        } ?: if (verticals) {
            if (pos.y > Globals.mc.player.posY + Globals.mc.player.eyeHeight) {
                EnumFacing.DOWN
            } else {
                EnumFacing.UP
            }
        } else {
            null
        }
    }

    fun rayTraceTo(pos: BlockPos): RayTraceResult? {
        return Globals.mc.world.rayTraceBlocks(Globals.mc.player.eyePosition, pos.toVec3d())
    }

    fun rayTraceTo(vector: Vec3d): RayTraceResult? {
        return Globals.mc.world.rayTraceBlocks(Globals.mc.player.eyePosition, vector)
    }

    fun getRayTraceResult(rotation: Vec2f): RayTraceResult {
        return getRayTraceResult(rotation, Globals.mc.playerController.blockReachDistance)
    }

    fun getRayTraceResult(rotation: Vec2f, distance: Float): RayTraceResult {
        val entity = Globals.mc.renderViewEntity ?: Globals.mc.player ?:
                     return RayTraceResult(RayTraceResult.Type.MISS, Vec3d(0.5, 1.0, 0.5), EnumFacing.UP, BlockPos.ORIGIN)

        val start = entity.eyePosition
        val lookVec = RotationUtils.getVectorForRotation(rotation)
        val end = start.add(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance)

        return Globals.mc.world.rayTraceBlockC(start, end)
            ?: RayTraceResult(RayTraceResult.Type.MISS, Vec3d(0.5, 1.0, 0.5), EnumFacing.UP, BlockPos.ORIGIN)
    }

    fun getStrictResult(vector: Vec3d): RayTraceResult {
        var rayTraceResult: RayTraceResult? = null

        val eyesPos = Globals.mc.player.eyePosition

        for (xSearch in 0.1..0.9 step 0.1) for (ySearch in 0.1..0.9 step 0.1) for (zSearch in 0.1..0.9 step 0.1) {
            val posVector = vector.add(xSearch, ySearch, zSearch)
            val dist = eyesPos.distanceTo(posVector)

            val rotationTo = RotationUtils.getRotationTo(posVector)
            val lookVec = RotationUtils.getVectorForRotation(rotationTo)
            val rotationVector = eyesPos.add(lookVec.x * dist, lookVec.y * dist, lookVec.z * dist)
            // val visibleFacing = if (strict) BlockUtil.getVisibleSidesStrict(rotationVector.toBlockPos(), rotationVector) else BlockUtil.getVisibleSides(rotationVector.toBlockPos(), rotationVector)

            val result = Globals.mc.world.rayTraceBlockC(eyesPos, rotationVector)
            if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) rayTraceResult = result
        }

        return rayTraceResult ?: RayTraceResult(RayTraceResult.Type.MISS, Vec3d(0.5, 1.0, 0.5), EnumFacing.UP, BlockPos.ORIGIN)
    }

}