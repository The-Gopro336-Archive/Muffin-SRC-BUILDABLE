package me.han.muffin.client.utils.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.Quad
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11.*

object GlStateUtils {

    private var lastScissor: Quad<Int, Int, Int, Int>? = null
    private val scissorList = ArrayList<Quad<Int, Int, Int, Int>>()

    fun scissor(x: Int, y: Int, width: Int, height: Int) {
        lastScissor = Quad(x, y, width, height)
        glScissor(x, y, width, height)
    }

    fun pushScissor() {
        lastScissor?.let {
            scissorList.add(it)
        }
    }

    fun popScissor() {
        scissorList.removeLastOrNull()?.let {
            scissor(it.first, it.second, it.third, it.fourth)
        }
    }

    @JvmStatic
    var colorLock = false
        private set

    @JvmStatic
    fun useVbo(): Boolean {
        return Globals.mc.gameSettings.useVbo
    }

    @JvmStatic
    fun matrix(state: Boolean) {
        if (state) {
            glPushMatrix()
        } else {
            glPopMatrix()
        }
    }

    @JvmStatic
    fun blend(state: Boolean) {
        if (state) {
            GlStateManager.enableBlend()
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        //    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        } else {
            GlStateManager.disableBlend()
        }
    }

    @JvmStatic
    fun alpha(state: Boolean) {
        if (state) {
            GlStateManager.enableAlpha()
        } else {
            GlStateManager.disableAlpha()
        }
    }

    @JvmStatic
    fun smooth(state: Boolean) {
        if (state) {
            GlStateManager.shadeModel(GL_SMOOTH)
        } else {
            GlStateManager.shadeModel(GL_FLAT)
        }
    }

    @JvmStatic
    fun lineSmooth(state: Boolean) {
        if (state) {
            glEnable(GL_LINE_SMOOTH)
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        } else {
            glDisable(GL_LINE_SMOOTH)
            glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE)
        }
    }

    @JvmStatic
    fun hintPolygon(state: Boolean) {
        if (state) {
            glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST)
        } else {
            glHint(GL_POLYGON_SMOOTH_HINT, GL_DONT_CARE)
        }
    }

    @JvmStatic
    fun depth(state: Boolean) {
        if (state) {
            GlStateManager.enableDepth()
        } else {
            GlStateManager.disableDepth()
        }
    }

    fun depthMask(state: Boolean) {
        if (state) {
            GlStateManager.depthMask(true)
        } else {
            GlStateManager.depthMask(false)
        }
    }

    @JvmStatic
    fun texture2d(state: Boolean) {
        if (state) {
            GlStateManager.enableTexture2D()
        } else {
            GlStateManager.disableTexture2D()
        }
    }

    fun cull(state: Boolean) {
        if (state) {
            GlStateManager.enableCull()
        } else {
            GlStateManager.disableCull()
        }
    }

    @JvmStatic
    fun lighting(state: Boolean) {
        if (state) {
            GlStateManager.enableLighting()
        } else {
            GlStateManager.disableLighting()
        }
    }

    fun polygon(state: Boolean) {
        if (state) {
            GlStateManager.enablePolygonOffset()
            GlStateManager.doPolygonOffset(1.0F, -1500000.0F)
        } else {
            GlStateManager.disablePolygonOffset()
            GlStateManager.doPolygonOffset(1.0F, 1500000.0F)
        }
    }


    @JvmStatic
    fun resetColour() {
        RenderUtils.glColor(1F, 1F, 1F, 1F)
    }

    @JvmStatic
    fun colorLock(state: Boolean) {
        colorLock = state
    }

    @JvmStatic
    fun rescale(width: Double, height: Double) {
        GlStateManager.clear(256)
        GlStateManager.viewport(0, 0, Globals.mc.displayWidth, Globals.mc.displayHeight)
        GlStateManager.matrixMode(GL_PROJECTION)
        GlStateManager.loadIdentity()
        GlStateManager.ortho(0.0, width, height, 0.0, 1000.0, 3000.0)
        GlStateManager.matrixMode(GL_MODELVIEW)
        GlStateManager.loadIdentity()
        GlStateManager.translate(0.0f, 0.0f, -2000.0f)
    }

    @JvmStatic
    fun rescaleActual() {
        rescale(Globals.mc.displayWidth.toDouble(), Globals.mc.displayHeight.toDouble())
    }

    @JvmStatic
    fun rescaleMc() {
        val resolution = ScaledResolution(Globals.mc)
        rescale(resolution.scaledWidth_double, resolution.scaledHeight_double)
    }

}