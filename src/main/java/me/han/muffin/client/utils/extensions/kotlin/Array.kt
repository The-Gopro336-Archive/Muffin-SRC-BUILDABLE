package me.han.muffin.client.utils.extensions.kotlin

import java.util.*

fun BooleanArray.stream() = Arrays.stream(this.toTypedArray())

fun ByteArray.stream() = Arrays.stream(this.toTypedArray())

fun ShortArray.stream() = Arrays.stream(this.toTypedArray())

fun IntArray.stream() = Arrays.stream(this)

fun LongArray.stream() = Arrays.stream(this)

fun FloatArray.stream() = Arrays.stream(this.toTypedArray())

fun DoubleArray.stream() = Arrays.stream(this)

fun CharArray.stream() = Arrays.stream(this.toTypedArray())

fun <T> Array<out T>.stream() = Arrays.stream(this)