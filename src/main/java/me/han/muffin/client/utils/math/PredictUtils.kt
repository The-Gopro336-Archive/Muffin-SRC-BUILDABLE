package me.han.muffin.client.utils.math

import me.han.muffin.client.core.Globals
import me.han.muffin.client.module.modules.combat.AutoCrystalHelper
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.entity.EntityUtil.prevPosVector
import me.han.muffin.client.utils.extensions.kotlin.ceilToInt
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.entity.getBodyY
import me.han.muffin.client.utils.extensions.mixin.misc.tickLength
import me.han.muffin.client.utils.extensions.mixin.misc.timer
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

object PredictUtils {

    fun getBlockMoveMotionPos(entity: Entity, predicted: Double): Vec3d {
        val posVector = entity.positionVector
        // try and get the block the player will be over
        val motionX = entity.motionX
        val motionZ = entity.motionZ
        // ignore y motion
        val vel = Vec3d(motionX, 0.0, motionZ).normalize()
        var directionVector = Vec3d.ZERO
        // must be moving
        if (vel.lengthSquared() > 0.0) {
            var modX = 0.0
            var modZ = 0.0

            if (abs(vel.x) > abs(vel.z)) {
                modX = if (vel.x < 0.0) -predicted else predicted
                modZ = 0.0
            } else {
                modX = 0.0
                modZ = if (vel.z < 0.0) -predicted else predicted
            }
            directionVector = Vec3d(modX, 0.0, modZ)
        }
        // val predictY = ((entity.entityBoundingBox.minY - entity.lastTickPosY) * predicted) + entity.eyeHeight - 0.15
        val isPredictedTarget = directionVector.lengthSquared() > 0.0
        return if (isPredictedTarget) posVector.add(directionVector.x, 0.0, directionVector.z) else posVector
    }

    fun getMoveMotionPos(entity: Entity, predictValue: Double): Vec3d {
        val predicted = predictValue * 10

        val vel = abs(entity.motionX) + abs(entity.motionZ)
        val movedPosX = entity.posX + entity.motionX * if (vel > 0.25) 100.0 else predicted
        val movedPosY = entity.posY + entity.motionY * predicted
        val movedPosZ = entity.posZ + entity.motionZ * if (vel > 0.25) 100.0 else predicted
        return Vec3d(movedPosX, movedPosY, movedPosZ)
    }

    fun getRewritePos(entity: Entity): Vec3d {
        val xDelta = (entity.posX - entity.lastTickPosX) * 0.4
        val zDelta = (entity.posZ - entity.lastTickPosZ) * 0.4
        var d = Globals.mc.player.getDistance(entity).toDouble()
        d -= d % 0.8

        var xMulti = 1.0
        var zMulti = 1.0

        val sprint = entity.isSprinting
        xMulti = d / 0.8 * xDelta * if (sprint) 1.25 else 1.0
        zMulti = d / 0.8 * zDelta * if (sprint) 1.25 else 1.0

        val x = entity.posX + xMulti
        val z = entity.posZ + zMulti
        val y = entity.posY
        return Vec3d(x, y, z)
    }

    fun getNewPrediction(tracker: MotionTracker, entity: Entity, pingSync: Boolean, predictTicks: Int) = AutoCrystalHelper.target?.let {
        val ticks = if (pingSync) (InfoUtils.ping() / 25.0F).ceilToInt() else predictTicks
        tracker.getPositionAndBBAhead(ticks) ?: it.positionVector to it.entityBoundingBox
    } ?: entity.positionVector to entity.entityBoundingBox

    fun getSkeletonPrediction(entity: Entity, predictValue: Float): Vec3d {
        val eyesPos = Globals.mc.player.eyePosition

        val diffX = entity.posX - eyesPos.x
        //val diffY = entity.entityBoundingBox.minY - eyesPos.y
        val diffY = entity.getBodyY(0.3333333333333333) - eyesPos.y
        val diffZ = entity.posZ - eyesPos.z
        val squaredDistance = hypot(diffX, diffZ)
        val vector = Vec3d(diffX, diffY + squaredDistance * 0.20000000298023224, diffZ)

        return entity.positionVector.add(MathUtils.getPredictedVelocity(vector, predictValue, ((14 - Globals.mc.world.difficulty.id * 4).toFloat())))
    }

    fun getPredictedBoundingBox(entity: Entity, vector: Vec3d): AxisAlignedBB {
        val halfWidth = entity.width / 2.0
        val height = entity.height.toDouble()
        return AxisAlignedBB(vector.x - halfWidth, vector.y, vector.z - halfWidth, vector.x + halfWidth, vector.y + height, vector.z + halfWidth)
    }

    fun getArrowMotion(paramX: Double, paramY: Double, paramZ: Double, velocity: Double, inaccuracy: Double): Vec3d {
        var x = paramX
        var y = paramY
        var z = paramZ
        val f = sqrt(x * x + y * y + z * z)
        x /= f
        y /= f
        z /= f
        x += RandomUtils.random.nextGaussian() * 0.007499999832361937 * inaccuracy
        y += RandomUtils.random.nextGaussian() * 0.007499999832361937 * inaccuracy
        z += RandomUtils.random.nextGaussian() * 0.007499999832361937 * inaccuracy
        x *= velocity
        y *= velocity
        z *= velocity
        return Vec3d(x, y, z)
    }

    fun getSquaredNewNormalDistance(entity: Entity, vectorDiff: Vec3d): Vec3d {
        // val vectorDiff = getDiffVector(entity, ((Globals.mc as IMinecraft).timer as ITimer).tickLength.div(1.5F))
        val predictedX = if (entity.collidedHorizontally) entity.posX else entity.posX + vectorDiff.x
        val predictedY = if (entity.collidedVertically) if (vectorDiff.y > 0.0) entity.posY + vectorDiff.y else entity.posY else entity.posY + vectorDiff.y
        val predictedZ = if (entity.collidedHorizontally) entity.posZ else entity.posZ + vectorDiff.z

        return Vec3d(predictedX, predictedY, predictedZ)
    }

    fun getDiffVector(target: Entity, divide: Float = Globals.mc.timer.tickLength): Vec3d {
        val scale = InfoUtils.ping() / divide
        return target.positionVector.subtract(target.prevPosVector).scale(scale.toDouble())
    }

    fun predictEntityLocation(entity: Entity, ticks: Double): Vec3d {
        val posVector = entity.positionVector
        if (posVector.x == entity.lastTickPosX && posVector.y == entity.lastTickPosY && posVector.z == entity.lastTickPosZ) return posVector

        return MathUtils.interpolateVector(
            Vec3d(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ),
            posVector.add(entity.motionX, entity.motionY, entity.motionZ),
            ticks
        )
    }


}