package me.han.muffin.client.utils.extensions.kotlin

import me.han.muffin.client.utils.color.Colour

fun Int.toColour(): Colour {
    val a = this shr 24 and 0xFF
    val r = this shr 16 and 0xFF
    val g = this shr 8 and 0xFF
    val b = this and 0xFF

    return Colour(r, g, b, a)
}