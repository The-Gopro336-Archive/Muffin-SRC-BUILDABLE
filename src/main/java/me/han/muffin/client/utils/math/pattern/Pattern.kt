package me.han.muffin.client.utils.math.pattern

import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.mc.utils.toStringFormat
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.abs

val AIMING_PATTERNS = arrayOf(
    GaussianPattern
)

interface Pattern {
    fun update()
    fun spot(box: AxisAlignedBB): Vec3d
    fun spot(vec: Vec3d): Vec3d
}

/**
 * A very basic human-like rotation pattern
 *
 * So the idea is that, when a player is standing still he is unlikely to move his mouse.
 * It is more likely to choose a new spot when a player is moving.
 * Is it also very unlikely to snap to a new spot. So I've implemented a speed-limit on how fast a new spot is being applied.
 *
 * We're also using the gaussian algorithm to make the rotations more human-like.
 *
 * This is of course more like a random-set of thoughts about values of a human-like pattern. Not something very.
 * Might train a data-set in the future.
 *
 * Tested on:
 * AAC
 */
object GaussianPattern: Pattern {
    private val random = Random()

    private var spot = gaussianVec
    private var nextSpot = gaussianVec

    private const val STANDING_CHANCE: Double = 0.91
    private const val MOVING_CHANCE: Double = 0.32

    private const val SPEED_HORIZONTAL_LIMITER: Double = 0.04
    private const val SPEED_VERTICAL_LIMITER: Double = 0.0787

    private val randomGaussian: Double
        get() = abs(random.nextGaussian() % 1.0)

    private val gaussianVec: Vec3d
        get() = Vec3d(randomGaussian, 1 - randomGaussian, randomGaussian)

    override fun update() {
        // Chance of generating new spot
        val newSpotChance = if (!MovementUtils.isMovingSpeed) {
            STANDING_CHANCE
        } else {
            MOVING_CHANCE
        }

        if (random.nextDouble() > newSpotChance) {
            nextSpot = gaussianVec
        }

        // Check if spot has to be moved
        if (spot != nextSpot) {
            val xSpeed = randomGaussian * SPEED_HORIZONTAL_LIMITER
            val ySpeed = randomGaussian * SPEED_VERTICAL_LIMITER
            val zSpeed = randomGaussian * SPEED_HORIZONTAL_LIMITER

            val diffX = (nextSpot.x - spot.x)
            val x = spot.x + diffX.coerceIn(-xSpeed, xSpeed)

            val diffY = (nextSpot.y - spot.y)
            val y = spot.y + diffY.coerceIn(-ySpeed, ySpeed)

            val diffZ = (nextSpot.z - spot.z)
            val z = spot.z + diffZ.coerceIn(-zSpeed, zSpeed)

            spot = Vec3d(x, y, z)
        }
    }

    override fun spot(box: AxisAlignedBB) = Vec3d(
        box.minX + ((box.maxX - box.minX) * spot.x),
        box.minY + ((box.maxY - box.minY) * spot.y),
        box.minZ + ((box.maxZ - box.minZ) * spot.z)
    )

    override fun spot(vec: Vec3d): Vec3d = vec.add(spot)

}