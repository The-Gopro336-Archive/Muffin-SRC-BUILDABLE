package me.han.muffin.client.utils.math.rotation

import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.module.modules.player.FastBowModule
import me.han.muffin.client.module.modules.render.TrajectoriesModule
import me.han.muffin.client.utils.entity.EntityUtil.prevPosVector
import me.han.muffin.client.utils.extensions.kotlin.*
import me.han.muffin.client.utils.extensions.mc.block.state
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.RayTraceUtils
import me.han.muffin.client.utils.render.RenderUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.*
import java.util.*
import kotlin.math.*

object RotationUtils {

    /**
     * Face target with bow
     *
     * @param target      your enemy
     * @param predict     predict new enemy position
     * @param predictSize predict size of predict
     */
    fun faceBow(target: EntityPlayer, predict: Boolean, predictSize: Double): Vec2f {
        val player = Globals.mc.player

        val localVector = player.positionVector
        val targetVector = target.positionVector

        val targetPredictedXZ = targetVector.add((if (predict) targetVector.subtract(target.prevPosVector).scale(predictSize) else Vec3d.ZERO))
        val localPlayerPredictedXZ = player.positionVector.add(if (predict) localVector.subtract(player.prevPosVector) else Vec3d.ZERO)
        val xzDiff = targetPredictedXZ.subtract(localPlayerPredictedXZ)

        val posX = xzDiff.x
        val posY = target.entityBoundingBox.minY + (if (predict) (target.entityBoundingBox.minY - target.prevPosY) * predictSize else 0.0) + target.eyeHeight - 0.15 - (player.entityBoundingBox.minY + if (predict) player.posY - player.prevPosY else 0.0) - player.eyeHeight
        val posZ = xzDiff.z

        val distance = hypot(posX, posZ)
        val itemUseCount = FastBowModule.bowCharge ?: if (Globals.mc.player.isHandActive) TrajectoriesModule.getInterpolatedCharge() else 0.0
        val useDuration = (72000.0 - itemUseCount) / 20.0

        var power = (useDuration.pow(2) + useDuration * 2.0) / 3.0
        if (power > 1.0) power = 1.0

        val yaw = normalizeAngle(atan2(posZ, posX).toDegree() - 90.0)
        val pitch = normalizeAngle((-(atan((power.pow(2) - sqrt(power.pow(4) - 0.006F * (0.006F * distance.pow(2) + 2 * posY * power.pow(2)))) /
            (0.006F * distance)).toDegree())))

        return Vec2f(yaw, pitch)
    }

    fun calculateLookAt(px: Double, py: Double, pz: Double, me: EntityPlayer): Vec2d {
        var dirx = me.posX - px
        var diry = me.posY - py
        var dirz = me.posZ - pz

        val len = sqrt(dirx * dirx + diry * diry + dirz * dirz)

        dirx /= len
        diry /= len
        dirz /= len
        var pitch = asin(diry)
        var yaw = atan2(dirz, dirx)

        //to degree
        pitch *= 180.0 / Math.PI
        yaw *= 180.0 / Math.PI
        yaw += 90.0
        return Vec2d(yaw, pitch)
    }

    fun calcAngle(from: Vec3d, to: Vec3d): Vec2f {
        val difX = to.x - from.x
        val difY = (to.y - from.y) * -1.0
        val difZ = to.z - from.z

        val dist = hypot(difX, difZ)
        return Vec2f(normalizeAngle(atan2(difZ, difX).toDegree() - 90.0), normalizeAngle(atan2(difY, dist).toDegree()))
    }

    /**
     * Get angle for block pos.
     */
    fun getAngleBlockPos(target: BlockPos): Vec2f {
        val xDiff = target.x - Globals.mc.player.posX
        val yDiff = target.y - Globals.mc.player.posY
        val zDiff = target.z - Globals.mc.player.posZ

        val downPos = target.y - 1.0
        val xzDiff = hypot(xDiff, zDiff)

        val eyesPos = Globals.mc.player.eyePosition

        val yaw = (atan2(zDiff, xDiff).toDegree()).toFloat() - 90.0F

        var pitch = (-atan2(downPos - eyesPos.y, xzDiff).toDegree()).toFloat()

        if (yDiff > -0.2 && yDiff < 0.2) {
            pitch = (-atan2(downPos - eyesPos.y, xzDiff).toDegree()).toFloat()
        } else if (yDiff > -0.2) {
            pitch = (-atan2(downPos - eyesPos.y, xzDiff).toDegree()).toFloat()
        } else if (yDiff < 0.3) {
            pitch = (-atan2(downPos - eyesPos.y, xzDiff).toDegree()).toFloat()
        }

        return Vec2f(yaw, pitch)
    }

    fun faceVectorWithPositionPacket(vector: Vec3d) {
        val rotation = getRotationTo(vector)
        Globals.mc.player.connection.sendPacket(CPacketPlayer.Rotation(rotation.x, rotation.y, Globals.mc.player.onGround))
    }

    fun getVectorForRotation(rotation: Vec2f): Vec3d {
        val (yaw, pitch) = rotation

        val x = sin(-yaw.toRadian() - PI_F)
        val z = cos(-yaw.toRadian() - PI_F)
        val xz = -cos(-pitch.toRadian())

        val y = sin(-pitch.toRadian())
        return Vec3d((x * xz).toDouble(), y.toDouble(), (z * xz).toDouble())
    }

    /**
     * Allows you to check if your crosshair is over your target entity
     *
     * @param blockReachDistance your reach
     * @return if crosshair is over target
     */
    fun isFaced(blockReachDistance: Double): Boolean {
        return RayTraceUtils.getRaytraceEntity(blockReachDistance, LocalMotionManager.serverSideRotation) != null
    }

    fun getUnderBlockRotationFuture(pos: BlockPos, facing: EnumFacing?): Vec2f {
        val bb = pos.state.getBoundingBox(Globals.mc.world, pos)

        var diffX = pos.x + (bb.minX + bb.maxX) / 2.0
        var diffY = pos.y + (bb.minY + bb.maxY) / 2.0
        var diffZ = pos.z + (bb.minZ + bb.maxZ) / 2.0

        if (facing != null) {
            diffX += facing.directionVec.x * ((bb.minX + bb.maxX) / 2.0)
            diffY += facing.directionVec.y * ((bb.minY + bb.maxY) / 2.0)
            diffZ += facing.directionVec.z * ((bb.minZ + bb.maxZ) / 2.0)
        }

        return getRotationTo(Vec3d(diffX, diffY, diffZ))
    }

    /**
     * Arguments: current rotation, intended rotation, max increment.
     */
    fun updateRotation(angle: Float, targetAngle: Float, maxIncrease: Float): Float {
        var f = normalizeAngle(targetAngle - angle)

        if (f > maxIncrease) f = maxIncrease
        if (f < -maxIncrease) f = -maxIncrease

        return angle + f
    }

    fun getRandomCenter(bb: AxisAlignedBB): Vec3d {
        return Vec3d(
            bb.minX + (bb.maxX - bb.minX) * 0.8 * Math.random(),
            bb.minY + (bb.maxY - bb.minY) * Math.random() + 0.1 * Math.random(),
            bb.minZ + (bb.maxZ - bb.minZ) * 0.8 * Math.random()
        )
    }

    /*
    fun getAngleDifference(direction: Float, rotationYaw: Float): Float {
        val phi = abs(rotationYaw - direction) % 360.0f
        return if (phi > 180.0f) (360.0f - phi) else phi
    }
     */

    /**
     * Calculate difference between two angle points
     *
     * @param a angle point
     * @param b angle point
     * @return difference between angle points
     */
    fun getAngleDifference(a: Float, b: Float): Float = ((a - b) % 360F + 540F) % 360F - 180F

    fun isEntityInFov(entity: Entity, angle: Double): Boolean {
        return getAngleDifference(LocalMotionManager.serverSideRotation.x, getRotationToEntityClosest(entity).x) < angle
    }

    fun getYawChange(yaw: Float, posX: Double, posZ: Double): Float {
        val deltaX = posX - Globals.mc.player.posX
        val deltaZ = posZ - Globals.mc.player.posZ
        var yawToEntity = 0.0

        if (deltaZ < 0.0 && deltaX < 0.0) {
            if (deltaX != 0.0) yawToEntity = 90.0 + atan(deltaZ / deltaX).toDegree()
        } else if (deltaZ < 0.0 && deltaX > 0.0) {
            if (deltaX != 0.0) yawToEntity = -90.0 + atan(deltaZ / deltaX).toDegree()
        } else {
            if (deltaZ != 0.0) yawToEntity = -atan(deltaX / deltaZ).toDegree()
        }

        return normalizeAngle(-(yaw - yawToEntity.toFloat()))
    }

    fun getPitchChange(pitch: Float, entity: Entity, posY: Double): Float {
        val deltaX = entity.posX - Globals.mc.player.posX
        val deltaZ = entity.posZ - Globals.mc.player.posZ
        val deltaY = posY - 2.2 + entity.eyeHeight - Globals.mc.player.posY

        val distanceXZ = hypot(deltaX, deltaZ)
        val pitchToEntity = -atan(deltaY / distanceXZ).toDegree()

        return -normalizeAngle(pitch - pitchToEntity.toFloat()) - 2.5f
    }

    fun getRelativeRotation(entity: Entity): Float {
        return getRelativeRotation(entity.entityBoundingBox.center)
    }

    private fun getRelativeRotation(posTo: Vec3d): Float {
        return getRotationDiff(getRotationTo(posTo), Vec2f(Globals.mc.player))
    }

    fun getPlayerRotation(pTicks: Float = 1f): Vec2f {
        val rotation = Vec2f(Globals.mc.player.rotationYaw, Globals.mc.player.rotationPitch)
        val prevRotation = Vec2f(Globals.mc.player.prevRotationYaw, Globals.mc.player.prevRotationPitch)
        return prevRotation.plus(rotation.minus(prevRotation).times(pTicks))
    }

    private fun getRotationDiff(r1: Vec2f, r2: Vec2f): Float {
        val r1Radians = r1.toRadians()
        val r2Radians = r2.toRadians()
        return acos(cos(r1Radians.y) * cos(r2Radians.y) * cos(r1Radians.x - r2Radians.x) + sin(r1Radians.y) * sin(r2Radians.y)).toDegree()
    }

    fun getRotationToEntityClosest(entity: Entity): Vec2f {
        val box = entity.entityBoundingBox
        val eyePos = Globals.mc.player.eyePosition

        if (Globals.mc.player.entityBoundingBox.intersects(box)) return getRotationTo(box.center)

        val x = eyePos.x.coerceIn(box.minX + 0.1, box.maxX - 0.1)
        val y = eyePos.y.coerceIn(box.minY + 0.1, box.maxY - 0.1)
        val z = eyePos.z.coerceIn(box.minZ + 0.1, box.maxZ - 0.1)

        val hitVec = Vec3d(x, y, z)

        return getRotationTo(hitVec)
    }

    fun getRotationToEntityClosestStrict(entity: Entity): Vec2f {
        val local = Globals.mc.player
        val eyePos = Globals.mc.player.eyeHeight

        val xDiff = entity.posX - local.posX
        val zDiff = entity.posZ - local.posZ
        var yDiff = entity.posY - local.posY

        val dist = hypot(xDiff, zDiff)

        val box = entity.entityBoundingBox.expand(0.1, 0.1, 0.1)

        val pitch: Float
        val close = dist < 2.0 && abs(yDiff) < 2.0
        if (close && eyePos > box.minY) {
            pitch = 60.0F
        } else {
            yDiff = if (eyePos > box.maxY) box.maxY - eyePos else if (eyePos < box.minY) box.minY - eyePos else 0.0
            pitch = -(atan2(yDiff, dist).toDegree()).toFloat()
        }

        var yaw = (atan2(zDiff, xDiff).toDegree()).toFloat() - 90.0F
        if (close) {
            val inc = if (dist < 1.0F) 180F else 90F
            yaw = round(yaw / inc) * inc
        }

        return Vec2f(yaw, pitch)
    }

    fun getRotationToEntity(entity: Entity): Vec2f {
        return getRotationTo(entity.positionVector)
    }

    /**
     * Get rotation from a player position to another position vector
     *
     * @param posTo Calculate rotation to this position vector
     * @param eyeHeight Use player eye position to calculate
     * @return [Pair]<Yaw, Pitch>
     */
    fun getRotationTo(posTo: Vec3d): Vec2f {
        return getRotationTo(Globals.mc.player.eyePosition, posTo)
    }

    /**
     * Get rotation from a position vector to another position vector
     *
     * @param posFrom Calculate rotation from this position vector
     * @param posTo Calculate rotation to this position vector
     * @return [Vec2f] <Yaw, Pitch>
     */
    fun getRotationTo(posFrom: Vec3d, posTo: Vec3d): Vec2f {
        return getRotationFromVec(posTo.subtract(posFrom))
    }

    /*
    fun getRotationFromVec(vec: Vec3d): Vec2d {
        val xz = sqrt(vec.x * vec.x + vec.z * vec.z)
        val yaw = normalizeAngle(Math.toDegrees(atan2(vec.z, vec.x)) - 90.0)
        val pitch = normalizeAngle(-Math.toDegrees(atan2(vec.y, xz)))
        return Vec2d(yaw, pitch)
    }
     */
    fun getRotationFromVec(vec: Vec3d): Vec2f {
        val xz = hypot(vec.x, vec.z)
        val yaw = (atan2(vec.z, vec.x).toDegree()) - 90.0
        val pitch = (-(atan2(vec.y, xz).toDegree()))

        var yawDiff = yaw - LocalMotionManager.serverSideRotation.x
        if (yawDiff < -180.0F || yawDiff > 180.0F) {
            val round = round(abs(yawDiff / 360.0F))
            yawDiff = if (yawDiff < 0.0f) yawDiff + 360.0F * round else yawDiff - 360.0F * round
        }

        return Vec2f(normalizeAngle(LocalMotionManager.serverSideRotation.x + yawDiff), normalizeAngle(pitch))
    }

//    fun normalizeAngle(angleIn: Double): Double {
//        var angle = angleIn
//        angle %= 360.0
//
//        if (angle >= 180.0) angle -= 360.0
//        if (angle < -180.0) angle += 360.0
//
//        return angle
//    }
//
//    fun normalizeAngle(angleIn: Float): Float {
//        var angle = angleIn
//        angle %= 360F
//
//        if (angle >= 180F) angle -= 360F
//        if (angle < -180F) angle += 360F
//
//        return angle
//    }

    fun normalizeAngle(angleIn: Double): Double {
        var angle = angleIn

        if (angle <= -360.0) angle = -((-angle) % 360.0)
        else if (angle >= 360.0) angle %= 360.0

        if (angle < -180.0) angle += 360.0
        else if (angle > 180.0) angle -= 360.0

        return angle
    }

    fun normalizeAngle(angleIn: Float): Float {
        var angle = angleIn

        if (angle <= -360.0F) angle = -((-angle) % 360.0F)
        else if (angle >= 360.0F) angle %= 360.0F

        if (angle < -180F) angle += 360F
        else if (angle > 180F) angle -= 360F

        return angle
    }

    fun resolveBestHitVec(entity: Entity, precision: Int, evadeBlocks: Boolean): Vec3d? {
        return try {
            val headVec = Globals.mc.player.eyePosition
            var bestHitVec: Vec3d? = Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)

            val interpolated = MathUtils.interpolateEntity(entity, RenderUtils.renderPartialTicks)

            val offset = precision / 3
            val height = entity.eyeHeight / precision
            val width = entity.width * 0.5f / offset

            for (offsetY in 0..precision) for (offsetX in -offset..offset) for (offsetZ in -offset..offset) {
                val possibleVec = Vec3d(
                    interpolated.x + width * offsetX,
                    interpolated.y + height * offsetY,
                    interpolated.z + width * offsetZ
                )

                if (evadeBlocks) {
                    val result = Globals.mc.player.world.rayTraceBlocks(headVec, possibleVec)
                    if (result != null) continue
                }

                if (headVec.distanceTo(possibleVec) < headVec.distanceTo(bestHitVec!!)) {
                    bestHitVec = possibleVec
                }
            }

            if (bestHitVec!!.x == Double.MAX_VALUE) bestHitVec = null
            bestHitVec
        } catch (t: Throwable) {
            t.printStackTrace()
            entity.positionVector
        }
    }


    private fun getDifference(a: Float, b: Float): Float {
        var r = ((a - b) % 360.0).toFloat()
        if (r < -180.0) {
            r += 360.0f
        }
        if (r >= 180.0) {
            r -= 360.0f
        }
        return r
    }

    fun smoothRotation(currentRotations: Vec2f, neededRotations: Vec2f, rotationSpeed: Float): Vec2f {
        val yawDiff = getDifference(neededRotations.x, currentRotations.x)
        val pitchDiff = getDifference(neededRotations.y, currentRotations.y)

        val rotationSpeedYaw =
            if (yawDiff > rotationSpeed) {
                rotationSpeed
            } else {
                max(yawDiff, -rotationSpeed)
            }

        val rotationSpeedPitch =
            if (pitchDiff > rotationSpeed) {
                rotationSpeed
            } else {
                max(pitchDiff, -rotationSpeed)
            }

        val newYaw = currentRotations.x + rotationSpeedYaw
        val newPitch = currentRotations.y + rotationSpeedPitch

        return Vec2f(newYaw, newPitch)
    }

    fun getRotationToSpecialEyeHeight(target: Entity): Vec2f {
        val player = Globals.mc.player

        val x = target.posX - player.posX
        val z = target.posZ - player.posZ
        val y = target.posY + target.eyeHeight * 0.75 - (player.posY + player.getEyeHeight())

        val distance = hypot(x, z)
        val yaw = (atan2(z, x).toDegree()).toFloat() - 90.0F
        val pitch = (-(atan2(y, distance).toDegree())).toFloat()

        return Vec2f(yaw, pitch)
    }

    fun faceEntitySmooth(curYaw: Double, curPitch: Double, intendedYaw: Double, intendedPitch: Double, yawSpeed: Double, pitchSpeed: Double): Vec2f {
        return Vec2f(
            updateRotation(curYaw.toFloat(), intendedYaw.toFloat(), yawSpeed.toFloat()),
            updateRotation(curPitch.toFloat(), intendedPitch.toFloat(), pitchSpeed.toFloat())
        )
    }

    fun getBowAngles(entity: Entity): Vec2f {
        val xDelta = (entity.posX - entity.lastTickPosX) * 0.4
        val zDelta = (entity.posZ - entity.lastTickPosZ) * 0.4
        var d = Globals.mc.player.getDistance(entity).toDouble()
        d -= d % 0.8

        var xMulti = 1.0
        var zMulti = 1.0

        val sprint = entity.isSprinting
        xMulti = d / 0.8 * xDelta * if (sprint) 1.25 else 1.0
        zMulti = d / 0.8 * zDelta * if (sprint) 1.25 else 1.0

        val x = entity.posX + xMulti - Globals.mc.player.posX
        val z = entity.posZ + zMulti - Globals.mc.player.posZ
        val y = Globals.mc.player.posY + Globals.mc.player.getEyeHeight() - (entity.posY + entity.eyeHeight)

        val dist = Globals.mc.player.getDistance(entity)
        val yaw = atan2(z, x).toDegree() - 90.0

        val distance = hypot(x, z)
        val pitch = (-(atan2(y, distance).toDegree())) + dist * 0.11

        var yawDiff = yaw - LocalMotionManager.serverSideRotation.x
        if (yawDiff < -180.0F || yawDiff > 180.0F) {
            val round = round(abs(yawDiff / 360.0F))
            yawDiff = if (yawDiff < 0.0f) yawDiff + 360.0F * round else yawDiff - 360.0F * round
        }

        return Vec2f(yawDiff, -pitch)
    }

    /**
     * Get the center of a box
     *
     * @param bb your box
     * @return center of box
     */
    fun getCenter(bb: AxisAlignedBB) =
        Vec3d(
            bb.minX + (bb.maxX - bb.minX) * 0.5,
            bb.minY + (bb.maxY - bb.minY) * 0.5,
            bb.minZ + (bb.maxZ - bb.minZ) * 0.5
        )


    /**
     * Calculate difference between the client rotation and your entity
     *
     * @param entity your entity
     * @return difference between rotation
     */
    fun getRotationDifference(entity: Entity): Double {
        val rotation = getRotationTo(getCenter(entity.entityBoundingBox))
        return getRotationDifference(rotation, Vec2f(Globals.mc.player))
    }

    /**
     * Calculate difference between the server rotation and your rotation
     *
     * @param this your rotation
     * @return difference between rotation
     */
    fun Vec2f.getRotationDifference() = getRotationDifference(this, LocalMotionManager.serverSideRotation)

    /**
     * Calculate difference between two rotations
     *
     * @param a rotation
     * @param b rotation
     * @return difference between rotation
     */
    fun getRotationDifference(a: Vec2f, b: Vec2f) =
        hypot(getAngleDifference(a.x, b.x).toDouble(), (a.y - b.y).toDouble())

    /**
     * Limit your rotation using a turn speed
     *
     * @param currentRotation your current rotation
     * @param targetRotation your goal rotation
     * @param turnSpeed your turn speed
     * @return limited rotation
     */
    fun limitAngleChange(currentRotation: Vec2f, targetRotation: Vec2f, turnSpeed: Float): Vec2f {
        val yawDifference = getAngleDifference(targetRotation.x, currentRotation.x)
        val pitchDifference = getAngleDifference(targetRotation.y, currentRotation.y)

        return Vec2f(
            currentRotation.x + if (yawDifference > turnSpeed) turnSpeed else max(yawDifference, -turnSpeed),
            currentRotation.y + if (pitchDifference > turnSpeed) turnSpeed else max(pitchDifference, -turnSpeed)
        )
    }


    //We could also write our own mc.world.raytraceBlocks?
    /**
     * Convenience method, calls
     * [RotationUtils.isLegit].
     */
    fun isLegit(vec3d: Vec3d): Boolean {
        return isLegit(vec3d.x, vec3d.y, vec3d.z)
    }

    /**
     * Convenience method calling
     * [RotationUtisl.isLegit].
     */
    fun isLegit(entity: Entity): Boolean {
        return isLegit(entity.posX, entity.posY + entity.eyeHeight / 2, entity.posZ)
    }

    /**
     * Convenience method calling
     * [RotationUtil.isLegit].
     */
    fun isLegit(pos: BlockPos): Boolean {
        val x = pos.x + 0.5
        val y = pos.y + 0.5
        val z = pos.z + 0.5
        return isLegit(x, y, z)
    }

    /**
     * Method to check if the last reported rotation
     * of the player looks in the proximity of the given coordinates.
     * It should be noted that this is just a simple approximation.
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return <tt>true</tt> if the player is looking at the block.
     */
    fun isLegit(x: Double, y: Double, z: Double): Boolean {
        return isLegit(x, y, z, LocalMotionManager.serverSideRotation)
    }

    /**
     * Method to check if the given yaw and pitch
     * result in a legit rotation to the block.
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @param yaw the yaw.
     * @param pitch the pitch.
     * @return <tt>true</tt> if yaw and pitch look at the block.
     */
    //TODO: raytrace would be way better here.
    private fun isLegit(x: Double, y: Double, z: Double, rotation: Vec2f): Boolean {
        val vec3d: Vec3d = MathUtils.fromTo(Globals.mc.player.eyePosition, x, y, z)
        val dist = vec3d.length()
        return if (dist < 0.5) {
            true
        } else {
            MathUtils.angle(vec3d, getVectorForRotation(rotation)) * dist < 35
        }
    }

    fun Entity.isEntityInSight(): Boolean {
        val serverRotation = LocalMotionManager.serverSideRotation

        val localEyesPos = Globals.mc.player.eyePosition
        val localLookVec = getVectorForRotation(serverRotation)

        val width = this.width
        val height = this.height

        val targetX = this.posX
        val targetZ = this.posZ

        val targetY = height / 2.0

        var directionLength = sqrt(localLookVec.x * localLookVec.x + localLookVec.y * localLookVec.y + localLookVec.z * localLookVec.z)
        if (directionLength == 0.0) directionLength = 1.0

        val xDiff = targetX - localEyesPos.x
        val zDiff = targetZ - localEyesPos.z
        val yDiff = targetY - localEyesPos.y

        val targetDist = sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
        // val minDist = max(height, width)

        val xPrediction = targetDist * localLookVec.x / directionLength
        val yPrediction = targetDist * localLookVec.y / directionLength
        val zPrediction = targetDist * localLookVec.z / directionLength

        val precision = 2.6

        var offset = 0.0
        offset += max(abs(xDiff - xPrediction) - (width / 2.0 + precision), 0.0)
        offset += max(abs(zDiff - zPrediction) - (width / 2.0 + precision), 0.0)
        offset += max(abs(yDiff - yPrediction) - (height / 2.0 + precision), 0.0)

        if (offset > 1.0) offset = sqrt(offset)

        return offset <= 0.1
    }

    fun Entity.isEntityInSightStrict(): Boolean {
        val anglePrecision = 80.0
        val blockPrecision = 0.5

        val serverRotation = LocalMotionManager.serverSideRotation
        val localEyesPos = Globals.mc.player.eyePosition
        val localLookVec = getVectorForRotation(serverRotation)

        val width = (this.width / 2.0) * 2.0
        val height = max(eyeHeight, height).toDouble()

        val targetX = this.posX
        val targetZ = this.posZ

        val targetY = height / 2.0
        val targetWidth = width * 2.0

        var directionLength = sqrt(localLookVec.x * localLookVec.x + localLookVec.y * localLookVec.y + localLookVec.z * localLookVec.z)
        if (directionLength == 0.0) directionLength = 1.0

        val xDiff = targetX - eyePosition.x
        val zDiff = targetZ - eyePosition.z
        val yDiff = targetY - eyePosition.y

        val targetDist = sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
        val minDist = max(height, targetWidth)

        if (targetDist > minDist && MathUtils.angleNCP(localEyesPos, Vec3d(targetX, targetY, targetZ), localLookVec) * radianToGradian > anglePrecision) {
            return targetDist - minDist <= 0.0
        }

        val xPrediction = targetDist * localLookVec.x / directionLength
        val yPrediction = targetDist * localLookVec.y / directionLength
        val zPrediction = targetDist * localLookVec.z / directionLength

        var offset = 0.0
        offset += max(abs(xDiff - xPrediction) - (width / 2.0 + blockPrecision), 0.0)
        offset += max(abs(yDiff - yPrediction) - (height / 2.0 + blockPrecision), 0.0)
        offset += max(abs(zDiff - zPrediction) - (width / 2.0 + blockPrecision), 0.0)

        if (offset > 1.0) offset = sqrt(offset)

        return offset <= 0.0
    }


}