package me.han.muffin.client.module.modules.render

import me.han.muffin.client.event.events.render.Render2DEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.Color

internal object CrosshairModule: Module("Crosshair", Category.RENDER, "Custom crosshair.") {
    private val dot = Value(true, "Dot")
    private val size = NumberValue(1F, 1F, 10F, 1F, "Length")
    private val gap = NumberValue(3F, 1F, 10F, 1F, "Gap")
    private val width = NumberValue(.5f, .1f, 2f, .1f, "Width")

    private val red = NumberValue(0, 0, 255, 1, "Red")
    private val green = NumberValue(255, 0, 255, 1, "Green")
    private val blue = NumberValue(0, 0, 255, 1, "Blue")
    private val alpha = NumberValue(255, 0, 255, 1, "Alpha")

    init {
        addSettings(dot, size, gap, width, red, green, blue, alpha)
    }

    @Listener
    private fun onRender2D(event: Render2DEvent) {
        if (fullNullCheck()) return

        val x = event.scaledResolution.scaledWidth / 2
        val y = event.scaledResolution.scaledHeight / 2

        val size = size.value
        val gap = gap.value
        val width = width.value

        val sizeGap = size + gap

        val color = ColourUtils.toRGBA(red.value, green.value, blue.value, alpha.value)
        val black = Color.BLACK.rgb

        if (width >= .5) {
            RenderUtils.drawBorderedRectReliant(x - sizeGap, y - width, x - gap, y + width, .5F, color, black)
            RenderUtils.drawBorderedRectReliant(x + gap, y - width, x + sizeGap, y + width, .5F, color, black)
            RenderUtils.drawBorderedRectReliant(x - width, y - sizeGap, x + width, y - gap, .5F, color, black)
            RenderUtils.drawBorderedRectReliant(x - width, y + gap, x + width, y + sizeGap, .5F, color, black)
            if (dot.value) RenderUtils.drawBorderedRectReliant(x - width, y - width, x + width, y + width, .5f, color, black)
        } else {
            RenderUtils.drawRect(x - sizeGap, y - width, x - gap, y + width, color)
            RenderUtils.drawRect(x + gap, y - width, x + sizeGap, y + width, color)
            RenderUtils.drawRect(x - width, y - sizeGap, x + width, y - gap, color)
            RenderUtils.drawRect(x - width, y + gap, x + width, y + sizeGap, color)
            if (dot.value) RenderUtils.drawRect(x - width, y - width, x + width, y + width, color)
        }
    }

}