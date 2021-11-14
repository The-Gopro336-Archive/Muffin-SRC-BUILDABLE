package me.han.muffin.client.utils.extensions.mc.utils

import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import net.minecraft.util.math.Vec3d

operator fun Vec3d.component1() = this.x
operator fun Vec3d.component2() = this.y
operator fun Vec3d.component3() = this.z

fun Vec3d.multiply(multX: Double, multY: Double, multZ: Double) = Vec3d(this.x * multX, this.y * multY, this.z * multZ)
fun Vec3d.multiply(mult: Vec3d) = this.multiply(mult.x, mult.y, mult.z)
fun Vec3d.multiply(mult: Double) = this.multiply(mult, mult, mult)

fun Vec3d.isNaN() = this.x.isNaN() || this.y.isNaN() || this.z.isNaN()

fun Vec3d.toStringFormat(floorToInt: Boolean = true, comma: Boolean = true): String {
    return StringBuilder().run {
        append(if (floorToInt) this@toStringFormat.x.floorToInt() else this@toStringFormat.x)

        append(if (comma) ", " else " ")
        append(if (floorToInt) this@toStringFormat.y.floorToInt() else this@toStringFormat.y)

        append(if (comma) ", " else " ")
        append(if (floorToInt) this@toStringFormat.z.floorToInt() else this@toStringFormat.z)

        toString()
    }
}