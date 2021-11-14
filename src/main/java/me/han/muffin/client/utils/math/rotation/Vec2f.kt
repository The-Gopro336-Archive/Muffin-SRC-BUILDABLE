package me.han.muffin.client.utils.math.rotation

import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import net.minecraft.entity.Entity
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow

data class Vec2f(var x: Float, var y: Float) {

    constructor(entity: Entity) : this(entity.rotationYaw, entity.rotationPitch)

    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())

    constructor(vec2d: Vec2d): this(vec2d.x.toFloat(), vec2d.y.toFloat())

    fun toRadians() = Vec2f(x.toRadian(), y.toRadian())

    fun length() = hypot(x, y)

    fun lengthSquared() = (x.pow(2) + y.pow(2))

    fun isReallyCloseTo(other: Vec2f): Boolean {
        return yawIsReallyClose(other) && abs(y - other.y) < 0.01
    }

    fun yawIsReallyClose(other: Vec2f): Boolean {
        val yawDiff = abs(RotationUtils.normalizeAngle(x) - RotationUtils.normalizeAngle(other.x)) // you cant fool me
        return yawDiff < 0.01 || yawDiff > 359.99
    }

    operator fun div(vec2f: Vec2f) = div(vec2f.x, vec2f.y)
    operator fun div(divider: Float) = div(divider, divider)

    fun div(x: Float, y: Float) = Vec2f(this.x / x, this.y / y)

    operator fun times(vec2f: Vec2f) = times(vec2f.x, vec2f.y)
    operator fun times(multiplier: Float) = times(multiplier, multiplier)

    fun times(x: Float, y: Float) = Vec2f(this.x * x, this.y * y)

    operator fun minus(vec2f: Vec2f) = minus(vec2f.x, vec2f.y)
    operator fun minus(value: Float) = minus(value, value)

    fun minus(x: Float, y: Float) = plus(-x, -y)

    operator fun plus(vec2f: Vec2f) = plus(vec2f.x, vec2f.y)
    operator fun plus(value: Float) = plus(value, value)

    fun plus(x: Float, y: Float) = Vec2f(this.x + x, this.y + y)

    fun toVec2d() = Vec2d(x.toDouble(), y.toDouble())

    /**
     * Patch gcd exploit in aim
     *
     * @see net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender
     */
    fun fixedSensitivity(): Vec2f {
        val sensitivity = Globals.mc.gameSettings.mouseSensitivity

        val f = sensitivity * 0.6F + 0.2F
        val gcd = f * f * f * 1.2F

        // get previous rotation
        val rotation = LocalMotionManager.serverSideRotation

        // fix yaw
        var deltaYaw = x - rotation.x
        deltaYaw -= deltaYaw % gcd
        val yaw = rotation.x + deltaYaw

        // fix pitch
        var deltaPitch = y - rotation.y
        deltaPitch -= deltaPitch % gcd
        val pitch = rotation.y + deltaPitch

        return Vec2f(yaw, pitch)
    }

    companion object {
        val ZERO = Vec2f(0.0F, 0.0F)
    }

    override fun toString(): String {
        return "x: $x, y: $y"
    }

}