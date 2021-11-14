package me.han.muffin.client.utils.extensions.kotlin

import me.han.muffin.client.utils.extensions.kotlin.interfaces.DisplayEnum

fun <E : Enum<E>> E.next(): E = declaringClass.enumConstants.run {
    get((ordinal + 1) % size)
}

fun Enum<*>.readableName() = (this as? DisplayEnum)?.displayName
    ?: name.mapEach('_') { it.toLowerCase().capitalize() }.joinToString(" ")