package me.han.muffin.client.utils.math

import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mc.utils.multiply
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.rotation.RotationUtils.normalizeAngle
import me.han.muffin.client.utils.math.rotation.Vec2d
import me.han.muffin.client.utils.render.RenderUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import java.util.*
import java.util.Map.Entry.comparingByValue
import kotlin.math.*

object MathUtils {

    fun ceilToPOT(valueIn: Int): Int {
        // Magical bit shifting
        var i = valueIn
        i--
        i = i or (i shr 1)
        i = i or (i shr 2)
        i = i or (i shr 4)
        i = i or (i shr 8)
        i = i or (i shr 16)
        i++
        return i
    }

    fun roundVec(vec3d: Vec3d, places: Int): Vec3d {
        return Vec3d(round(vec3d.x, places), round(vec3d.y, places), round(vec3d.z, places))
    }

    fun interpolateEntity(entity: Entity, time: Float): Vec3d {
        return Vec3d(
            entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * time,
            entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * time,
            entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * time
        )
    }

    fun getInterpolatedRenderPos(entity: Entity, ticks: Float): Vec3d {
        return interpolateEntity(entity, ticks).subtract(RenderUtils.renderPosX, RenderUtils.renderPosY, RenderUtils.renderPosZ)
    }

    fun interpolateEntityInt(entity: Entity, time: Float): Vec3i {
        return Vec3i(
            entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * time,
            entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * time,
            entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * time
        )
    }

    fun interpolate(now: Double, then: Double, time: Float): Double {
        return then + (now - then) * time
    }

    fun interpolate(now: Float, then: Float, time: Float): Float {
        return then + (now - then) * time
    }

    fun interpolate(now: Int, then: Int, time: Float): Float {
        return then + (now - then) * time
    }

    fun direction(yaw: Float): Vec2d {
        return Vec2d(cos((yaw + 90.0).toRadian()), sin((yaw + 90.0).toRadian()))
    }

    fun round(value: Float, places: Int): Double {
        val scale = 10.0.pow(places.toDouble())
        return round(value * scale) / scale
    }

    fun round(value: Double, places: Int): Double {
        val scale = 10.0.pow(places.toDouble())
        return round(value * scale) / scale
    }

    fun isNumberEven(i: Int): Boolean {
        return i and 1 == 0
    }

    fun reverseNumber(num: Int, min: Int, max: Int): Int {
        return max + min - num
    }

    fun isBetween(min: Int, max: Int, value: Int): Boolean {
        return value in min..max
    }

    fun isBetween(min: Double, max: Double, value: Double): Boolean {
        return value in min..max
    }

    /**
     * Draws a Vec3d between 2 Vec3ds, from first
     * parameter to second.
     *
     * @param from the start vec3d.
     * @param to the end vec3d.
     * @return return a vec3d looking from "from" to "to".
     */
    fun fromTo(from: Vec3d, to: Vec3d): Vec3d {
        return fromTo(from.x, from.y, from.z, to)
    }

    /**
     * Convenience method.
     */
    fun fromTo(from: Vec3d, x: Double, y: Double, z: Double): Vec3d {
        return fromTo(from.x, from.y, from.z, x, y, z)
    }

    /**
     * Convenience method.
     */
    fun fromTo(x: Double, y: Double, z: Double, to: Vec3d): Vec3d {
        return fromTo(x, y, z, to.x, to.y, to.z)
    }

    /**
     * Convenience method.
     */
    fun fromTo(x: Double, y: Double, z: Double, x2: Double, y2: Double, z2: Double): Vec3d {
        return Vec3d(x2 - x, y2 - y, z2 - z)
    }

    fun angle(a: Vec3d, b: Vec3d): Double {
        val lengthSq = a.length() * b.length()
        if (lengthSq < 1.0E-4) return 0.0

        val dot = a.dotProduct(b)
        val arg = dot / lengthSq

        if (arg > 1) return 0.0 else if (arg < -1) return 180.0

        return acos(arg) * 180.0f / Math.PI
    }

    /**
     * Angle between 2 non-zero vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return angle (radians).
     */
    fun angleNCP(a: Vec3d, b: Vec3d): Double {
        val theta = min(1.0, max(a.dotProduct(b) / (a.length() * b.length()), -1.0))
        return acos(theta)
    }

    /**
     * Positive angle between vector from source to target and the vector for
     * the given direction [0...PI].
     *
     * @param source vector from
     * @param target vector to
     * @param direction from direction to
     * @return Positive angle between vector from source to target and the
     *         vector for the given direction [0...PI].
     */
    fun angleNCP(source: Vec3d, target: Vec3d, direction: Vec3d): Double {
        // var dirLength = sqrt(direction.x * direction.x + direction.y * direction.y + direction.z * direction.z)
        // if (dirLength == 0.0) dirLength = 1.0
        val diff = target.subtract(source)
        return angleNCP(direction, diff)
    }

    fun getPlayerCardinal(player: EntityPlayer): Cardinal {
        return if (isBetween(-22.5, 22.5, normalizeAngle(player.rotationYaw.toDouble()))) {
            Cardinal.POS_Z
        } else if (isBetween(22.6, 67.5, normalizeAngle(player.rotationYaw.toDouble()))) {
            Cardinal.NEG_X_POS_Z
        } else if (isBetween(67.6, 112.5, normalizeAngle(player.rotationYaw.toDouble()))) {
            Cardinal.NEG_X
        } else if (isBetween(112.6, 157.5, normalizeAngle(player.rotationYaw.toDouble()))) {
            Cardinal.NEG_X_NEG_Z
        } else if (normalizeAngle(player.rotationYaw.toDouble()) >= 157.6 || normalizeAngle(player.rotationYaw.toDouble()) <= -157.5) {
            Cardinal.NEG_Z
        } else if (isBetween(-157.6, -112.5, normalizeAngle(player.rotationYaw.toDouble()))) {
            Cardinal.POS_X_NEG_Z
        } else if (isBetween(-112.5, -67.5, normalizeAngle(player.rotationYaw.toDouble()))) {
            Cardinal.POS_X
        } else if (isBetween(-67.6, -22.6, normalizeAngle(player.rotationYaw.toDouble()))) {
            Cardinal.POS_X_POS_Z
        } else {
            Cardinal.ERROR
        }
    }

    fun getPlayerMainCardinal(player: EntityPlayer): CardinalMain {
        return when (Character.toUpperCase(player.horizontalFacing.toString()[0])) {
            'N' -> CardinalMain.NEG_Z
            'S' -> CardinalMain.POS_Z
            'E' -> CardinalMain.POS_X
            'W' -> CardinalMain.NEG_X
            else -> CardinalMain.NULL
        }
    }

    fun convertRange(valueIn: Int, minIn: Int, maxIn: Int, minOut: Int, maxOut: Int): Int {
        return convertRange(valueIn.toDouble(), minIn.toDouble(), maxIn.toDouble(), minOut.toDouble(), maxOut.toDouble()).toInt()
    }

    fun convertRange(valueIn: Float, minIn: Float, maxIn: Float, minOut: Float, maxOut: Float): Float {
        return convertRange(valueIn.toDouble(), minIn.toDouble(), maxIn.toDouble(), minOut.toDouble(), maxOut.toDouble()).toFloat()
    }

    fun convertRange(valueIn: Double, minIn: Double, maxIn: Double, minOut: Double, maxOut: Double): Double {
        val rangeIn = maxIn - minIn
        val rangeOut = maxOut - minOut
        val convertedIn = (valueIn - minIn) * (rangeOut / rangeIn) + minOut
        val actualMin = min(minOut, maxOut)
        val actualMax = max(minOut, maxOut)
        return min(max(convertedIn, actualMin), actualMax)
    }

    fun areVec3dsAligned(from: Vec3d, to: Vec3d): Boolean {
        return from.toBlockPos() == to.toBlockPos()
    }

    enum class Cardinal(var cardinalName: String) {
        POS_Z("+Z"),
        NEG_X_POS_Z("-X / +Z"),
        NEG_X("-X"),
        NEG_X_NEG_Z("-X / -Z"),
        NEG_Z("-Z"),
        POS_X_NEG_Z("+X / -Z"),
        POS_X("+X"),
        POS_X_POS_Z("+X / +Z"),
        ERROR("ERROR_CALC_DIRECT");
    }

    enum class CardinalMain(var cardinalName: String) {
        POS_Z("+Z"),
        NEG_X("-X"),
        NEG_Z("-Z"),
        POS_X("+X"),
        NULL("N/A");
    }

    fun <K, V : Comparable<V>> sortByValue(map: Map<K, V>, descending: Boolean): HashMap<K, V> {
        val list = LinkedList(map.entries).sortedWith(if (descending) comparingByValue(Comparator.reverseOrder()) else comparingByValue())

        return linkedMapOf<K, V>().apply {
            list.forEach { (key, value) -> this[key] = value }
        }
    }

    fun getPredictedVelocity(vec3d: Vec3d, speed: Float, divergence: Float): Vec3d {
        return vec3d.normalize().add(
            RandomUtils.random.nextGaussian() * 0.007499999832361937 * divergence.toDouble(),
            RandomUtils.random.nextGaussian() * 0.007499999832361937 * divergence.toDouble(),
            RandomUtils.random.nextGaussian() * 0.007499999832361937 * divergence.toDouble()
        ).multiply(speed.toDouble())
    }

    fun interpolateVector(from: Vec3d, to: Vec3d, pct: Double): Vec3d {
        val x = from.x + (to.x - from.x) * pct
        val y = from.y + (to.y - from.y) * pct
        val z = from.z + (to.z - from.z) * pct
        return Vec3d(x, y, z)
    }

}