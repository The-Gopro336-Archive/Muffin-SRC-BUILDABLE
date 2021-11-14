package me.han.muffin.client.gui.particle

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution

class Particle(var x: Float, var y: Float) {

    val size = genRandom()
    private val ySpeed = RandomUtils.random.nextInt(5).toFloat()
    private val xSpeed = RandomUtils.random.nextInt(5).toFloat()

    val height = 0
    val width = 0

    private fun lint1(f: Float): Float {
        return 1.02.toFloat() * (1.0f - f) + 1.0.toFloat() * f
    }

    private fun lint2(f: Float): Float {
        return 1.02.toFloat() + f * (1.0.toFloat() - 1.02.toFloat())
    }

    fun connect(x: Float, y: Float) {
        RenderUtils.drawLine(this.x.toDouble(), this.y.toDouble(), x.toDouble(), y.toDouble(), 0.5F, -1)
    }

    fun interpolation() {
        for (n in 0..64) {
            val f = n / 64.0f
            val p1 = lint1(f)
            val p2 = lint2(f)
            if (p1 != p2) {
                y -= f
                x -= f
            }
        }
    }

    fun fall() {
        val sr = ScaledResolution(Globals.mc)
        y += ySpeed
        x += xSpeed
        if (y > Globals.mc.displayHeight) y = 1f
        if (x > Globals.mc.displayWidth) x = 1f
        if (x < 1) x = sr.scaledWidth.toFloat()
        if (y < 1) y = sr.scaledHeight.toFloat()
    }

    private fun genRandom(): Float {
        return (0.3F + Math.random() * (0.6F - 0.3F + 1.0F)).toFloat()
    }

}