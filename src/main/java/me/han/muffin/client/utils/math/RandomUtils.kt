package me.han.muffin.client.utils.math

import java.util.*

object RandomUtils {
    val random = Random()

    fun nextInt(startInclusive: Int, endExclusive: Int): Int {
        return if (endExclusive - startInclusive <= 0) startInclusive else startInclusive + random.nextInt(endExclusive - startInclusive)
    }

    fun nextDouble(startInclusive: Double, endInclusive: Double): Double {
        return if (startInclusive == endInclusive || endInclusive - startInclusive <= 0.0) startInclusive else startInclusive + (endInclusive - startInclusive) * Math.random()
    }

    fun nextFloat(startInclusive: Float, endInclusive: Float): Float {
        return if (startInclusive == endInclusive || endInclusive - startInclusive <= 0f) startInclusive else (startInclusive + (endInclusive - startInclusive) * Math.random()).toFloat()
    }

    fun randomNumber(length: Int): String {
        return random(length, "123456789")
    }

    fun randomString(length: Int): String {
        return random(length, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
    }

    fun random(length: Int, chars: String): String {
        return random(length, chars.toCharArray())
    }

    fun random(length: Int, chars: CharArray): String {
        return StringBuilder().run {
            for (i in 0 until length) append(chars[random.nextInt(chars.size)])
            toString()
        }
    }

    fun randomDelay(minDelay: Int, maxDelay: Int): Long {
        return nextInt(minDelay, maxDelay).toLong()
    }

    fun randomClickDelay(minCPS: Int, maxCPS: Int): Long {
        return ((Math.random() * (1000 / minCPS - 1000 / maxCPS + 1)) + 1000 / maxCPS).toLong()
    }

}