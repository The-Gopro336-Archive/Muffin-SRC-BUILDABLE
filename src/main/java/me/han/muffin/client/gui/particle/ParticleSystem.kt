package me.han.muffin.client.gui.particle

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils

class ParticleSystem(private val amount: Int) {
    private val particles = ArrayList<Particle>()

    private var prevWidth = 0
    private var prevHeight = 0

    fun draw(mouseX: Int, mouseY: Int) {
        if (particles.isEmpty() || prevWidth != Globals.mc.displayWidth || prevHeight != Globals.mc.displayHeight) {
            particles.clear()
            create()
        }

        prevWidth = Globals.mc.displayWidth
        prevHeight = Globals.mc.displayHeight

        for (particle in particles) {
            particle.fall()
            particle.interpolation()

            val range = 50
            val mouseOver = mouseX >= particle.x - range && mouseY >= particle.y - range && mouseX <= particle.x + range && mouseY <= particle.y + range

            if (mouseOver) {
                particles.filter { part ->
                    part.x > particle.x &&
                        part.x - particle.x < range &&
                        particle.x - part.x < range &&
                        (part.y > particle.y && part.y - particle.y < range || particle.y > part.y && particle.y - part.y < range)
                }.forEach { connectable -> particle.connect(connectable.x, connectable.y) }
            }

            GlStateUtils.blend(true)
            GlStateUtils.depth(false)
            GlStateUtils.texture2d(false)
            GlStateUtils.lineSmooth(true)
            GlStateUtils.depthMask(false)

            RenderUtils.drawCircle(particle.x, particle.y, particle.size, -0x1)

            GlStateUtils.depthMask(true)
            GlStateUtils.lineSmooth(false)
            GlStateUtils.texture2d(true)
            GlStateUtils.depth(true)
            GlStateUtils.blend(false)        }
    }

    private fun create() {
        for (i in 0 until amount) particles.add(Particle(RandomUtils.random.nextInt(Globals.mc.displayWidth).toFloat(), RandomUtils.random.nextInt(Globals.mc.displayHeight).toFloat()))
    }

}