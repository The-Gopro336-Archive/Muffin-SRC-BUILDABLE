package me.han.muffin.client.utils.extensions.kotlin

inline fun <reified T : Any> Any?.ifType(block: (T) -> Unit) {
    if (this is T) block(this)
}