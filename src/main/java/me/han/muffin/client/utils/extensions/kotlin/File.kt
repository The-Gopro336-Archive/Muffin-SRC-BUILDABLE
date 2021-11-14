package me.han.muffin.client.utils.extensions.kotlin

import java.io.File
import java.net.URI

val URI.file get() = File(this)
val String.file get() = File(this)