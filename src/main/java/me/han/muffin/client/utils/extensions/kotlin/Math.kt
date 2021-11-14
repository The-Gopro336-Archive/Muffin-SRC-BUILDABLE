package me.han.muffin.client.utils.extensions.kotlin

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor

const val radianToGradian = 360.0 / (2.0 * PI)
const val PI_F = 3.14159265358979323846F

fun Double.floorToInt() = floor(this).toInt()
fun Float.floorToInt() = floor(this).toInt()

fun Double.ceilToInt() = ceil(this).toInt()
fun Float.ceilToInt() = ceil(this).toInt()

fun Int.toRadian() = this * PI / 180.0
fun Float.toRadian() = this * PI_F / 180.0F
fun Double.toRadian() = this * PI / 180.0

fun Int.toDegree() = this * 180.0 / PI
fun Float.toDegree() = this * 180.0F / PI_F
fun Double.toDegree() = this * 180.0 / PI

