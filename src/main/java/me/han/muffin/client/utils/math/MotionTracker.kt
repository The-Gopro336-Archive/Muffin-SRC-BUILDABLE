package me.han.muffin.client.utils.math

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.utils.render.RenderUtils
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

/**
 * Tracking the motion of an Entity tick by tick
 */
class MotionTracker(targetIn: Entity?, private val trackLength: Int = 20) {
    var target: Entity? = targetIn
        set(value) {
            if (value != field) {
                reset()
                field = value
            }
        }

    private val motionLog = ArrayDeque<Vec3d>()
    private var prevMotion = Vec3d.ZERO
    private var motion = Vec3d.ZERO
    private val lockObject = Any()

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.POST) return

        synchronized(lockObject) {
            target?.let { target ->
                motionLog.add(calcActualMotion(target))
                while (motionLog.size > trackLength) motionLog.removeFirstOrNull()
                prevMotion = motion
                motion = calcAverageMotion()
            }
        }
    }


    /**
     * Calculate the actual motion of given entity
     *
     * @param entity The entity for motion calculation
     * @return Actual motion vector
     */
    private fun calcActualMotion(entity: Entity): Vec3d {
        return entity.positionVector.subtract(entity.prevPosX, entity.prevPosY, entity.prevPosZ)
    }

    /**
     * Calculate the average motion of the target entity in [trackLength]
     *
     * @return Average motion vector
     */
    private fun calcAverageMotion(): Vec3d {
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        synchronized(lockObject) {
            for (motion in motionLog) {
                sumX += motion.x
                sumY += motion.y
                sumZ += motion.z
            }
            return Vec3d(sumX, sumY, sumZ).scale(1.0 / motionLog.size)
        }
    }

    fun getPositionAndBBAhead(ticksAhead: Int, interpolation: Boolean = false): Pair<Vec3d, AxisAlignedBB>? {
        return target?.let { entity ->
            calcPositionAhead(ticksAhead, interpolation)?.let {
                val halfWidth = entity.width / 2.0
                val height = entity.height.toDouble()

                it to AxisAlignedBB(it.x - halfWidth, it.y, it.z - halfWidth, it.x + halfWidth, it.y + height, it.z + halfWidth)
            }
        }
    }

    /**
     * Calculate the predicted position of the target entity based on [calcAverageMotion]
     *
     * @param [ticksAhead] Amount of prediction ahead
     * @param [interpolation] Whether to return interpolated position or not, default value is false (no interpolation)
     * @return Predicted position of the target entity
     */
    fun calcPositionAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3d? {
        return target?.let { target ->
            calcMovedVectorAhead(ticksAhead, interpolation)?.let {
                val partialTicks = if (interpolation) RenderUtils.renderPartialTicks else 1.0F
                MathUtils.interpolateEntity(target, partialTicks).add(it)
            }
        }
    }

    /**
     * Calculate the predicted moved vector of the target entity based on [calcAverageMotion]
     *
     * @param [ticksAhead] Amount of prediction ahead
     * @param [interpolation] Whether to return interpolated position or not, default value is false (no interpolation)
     * @return Predicted moved vector of the target entity
     */
    fun calcMovedVectorAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3d? {
        return Globals.mc.world?.let { world ->
            target?.let {
                val partialTicks = if (interpolation) RenderUtils.renderPartialTicks else 1f
                val averageMotion = prevMotion.add(motion.subtract(prevMotion).scale(partialTicks.toDouble()))
                var movedVec = Vec3d(0.0, 0.0, 0.0)
                for (ticks in 0..ticksAhead) {
                    movedVec = if (canMove(world, it.entityBoundingBox, movedVec.add(averageMotion))) { // Attempt to move with full motion
                        movedVec.add(averageMotion)
                    } else if (canMove(world, it.entityBoundingBox, movedVec.add(averageMotion.x, 0.0, averageMotion.z))) { // Attempt to move horizontally
                        movedVec.add(averageMotion.x, 0.0, averageMotion.z)
                    } else break
                }
                movedVec
            }
        }
    }

    private fun canMove(world: World, bbox: AxisAlignedBB, offset: Vec3d): Boolean {
        return !world.collidesWithAnyBlock(bbox.offset(offset))
    }

    /**
     * Reset motion tracker
     */
    fun reset() {
        synchronized(lockObject) {
            motionLog.clear()
            prevMotion = Vec3d(0.0, 0.0, 0.0)
            motion = Vec3d(0.0, 0.0, 0.0)
        }
    }

    init {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

}