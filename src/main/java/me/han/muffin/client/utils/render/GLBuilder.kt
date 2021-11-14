package me.han.muffin.client.utils.render

import me.han.muffin.client.utils.color.Colour
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.VertexFormat
import org.lwjgl.opengl.GL11.glBegin
import org.lwjgl.opengl.GL11.glEnd

/**
 * @author han & smallshen
 * Simple GLHelper
 */

val tessellator: Tessellator = Tessellator.getInstance()
val buffer: BufferBuilder = tessellator.buffer

fun BufferBuilder.pos(x: Number, y: Number, z: Number = 0.0, colour: Colour? = null, tex: Pair<Number, Number>? = null) = this.pos(x.toDouble(), y.toDouble(), z.toDouble()).apply {
    if (colour != null) {
        val (r, g, b, a) = colour
        color(r, g, b, a)
    }
    if (tex != null) tex(tex.first.toDouble(), tex.second.toDouble())
    endVertex()
}

fun BufferBuilder.color(colour: Colour, alphaIn: Int) = color(colour.r, colour.g, colour.b, alphaIn)

@BufferDSL
inline infix fun Pair<Int, VertexFormat>.drawBuffer(elements: BufferBuilder.() -> Unit) {
    buffer.begin(this.first, this.second)
    elements(buffer)
    tessellator.draw()
}

@BufferDSL
inline infix fun Int.drawImmediate(elements: () -> Unit) {
    glBegin(this)
    elements()
    glEnd()
}

@BufferDSL
inline infix fun Int.withVertexFormat(vertexFormat: VertexFormat) = this to vertexFormat

enum class Dimension {
    TwoD {
        override operator fun invoke(elements: () -> Unit) {
             RenderUtils.enableGL2D()
             elements()
             RenderUtils.disableGL2D()
         }
    },
    ThreeD {
        override operator fun invoke(elements: () -> Unit) {
            RenderUtils.prepareGL3D()
            elements()
            RenderUtils.releaseGL3D()
        }
    };
    abstract operator fun invoke(elements: () -> Unit)
}

@DslMarker annotation class BufferDSL