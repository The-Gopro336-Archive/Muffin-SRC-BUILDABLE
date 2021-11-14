package me.han.muffin.client.gui.font

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.render.TextEvent
import me.han.muffin.client.utils.color.ColorUtils
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper.glUseProgram
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.glTranslated
import java.awt.Font

class MinecraftFontRenderer(val font: Font): FontRenderer(Globals.mc.gameSettings, ResourceLocation("textures/font/ascii.png"), Globals.mc.textureManager, false) {
    val fontHeight: Int

    private val defaultFont = AWTFontRenderer(font)

    private var renderFont = AWTFontRenderer(font)
    private val boldFont = AWTFontRenderer(font.deriveFont(Font.BOLD))
    private val italicFont = AWTFontRenderer(font.deriveFont(Font.ITALIC))
    private val boldItalicFont = AWTFontRenderer(font.deriveFont(Font.BOLD or Font.ITALIC))

    val height: Int get() = renderFont.height / 2
    val size: Int get() = renderFont.font.size

    init {
        fontHeight = height
        FONT_HEIGHT = height
    }

    enum class FontStyle {
        Default, Plain, Italic, Bold, BoldItalic
    }

    fun setFontStyle(fontStyle: FontStyle) {
        val style = when (fontStyle) {
            FontStyle.Default -> defaultFont.font.style
            FontStyle.Plain -> Font.PLAIN
            FontStyle.Italic -> Font.ITALIC
            FontStyle.Bold -> Font.BOLD
            FontStyle.BoldItalic -> Font.BOLD or Font.ITALIC
        }
        renderFont = AWTFontRenderer(font.deriveFont(style))
    }

    fun drawString(s: String?, x: Float, y: Float, color: Int) = drawString(s, x, y, color, false)

    override fun drawStringWithShadow(text: String?, x: Float, y: Float, color: Int) = drawString(text, x, y, color, true)

    fun drawCenteredString(s: String, x: Float, y: Float, color: Int, shadow: Boolean) = drawString(s, x - getStringWidth(s) / 2F, y, color, shadow)

    fun drawCenteredStringWithShadow(s: String, x: Float, y: Float, color: Int) = drawString(s, x - getStringWidth(s) / 2F, y, color, true)

    fun drawCenteredString(s: String, x: Float, y: Float, color: Int) =
        drawStringWithShadow(s, x - getStringWidth(s) / 2F, y, color)

    override fun drawString(text: String?, x: Float, y: Float, color: Int, shadow: Boolean): Int {
        var currentText = text

        val event = TextEvent(currentText)
        Muffin.getInstance().eventManager.dispatchEvent(event)
        currentText = event.text ?: return 0

        val currY = y - 3F
        if (currentText.contains("\n")) {
            val parts = currentText.split("\n")
            var newY = 0.0f
            for (s in parts) {
                drawText(s, x, currY + newY, color, shadow)
                newY += height
            }
            return 0
        }

        if (shadow) {
            glUseProgram(0)
            val alpha = 1 - (color shr 24 and 0xFF) / 255
            drawText(currentText, x + 1f, currY + 1f, 0xFF000000.toInt(), true)
            //   drawText(currentText, x + 1f, currY + 1f, Color(0, 0, 0, alpha * 150).rgb, true)
        }

        return drawText(currentText, x, currY, color, false)
    }

    private fun drawText(text: String?, x: Float, y: Float, color: Int, ignoreColor: Boolean): Int {
        if (text == null) return 0

        if (text.isNullOrEmpty()) return x.toInt()

        glTranslated(x - 1.5, y + 0.5, 0.0)
        GlStateManager.disableOutlineMode()
        GlStateUtils.texture2d(true)
        GlStateUtils.lighting(false)

        GlStateUtils.alpha(false)
        GlStateUtils.blend(true)

        GlStateUtils.cull(false)
        GlStateUtils.lineSmooth(true)
        GlStateUtils.hintPolygon(true)
        GlStateUtils.smooth(true)

        //        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        //        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        //        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        //        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        //        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, -0.25f)

        var currentColour = color

        if (currentColour == 553648127) {
            currentColour = 16777215
        }

        if (currentColour and -0x4000000 == 0)
            currentColour = currentColour or -16777216

        val defaultColor = currentColour

        val alpha = (currentColour shr 24 and 0xff)

        if (text.contains("§")) {
            val parts = text.split("§")
            var currentFont = renderFont

            var width = 0.0

            // Color code states
            var randomCase = false
            var bold = false
            var italic = false
            var strikeThrough = false
            var underline = false

            parts.forEachIndexed { index, part ->
                if (part.isEmpty()) return@forEachIndexed

                if (index == 0) {
                    currentFont.drawString(part, width, 0.0, currentColour)
                    width += currentFont.getStringWidth(part)
                } else {
                    val words = part.substring(1)
                    val type = part[0]
                    when (val colorIndex = "0123456789abcdefklmnor".indexOf(type)) {
                        in 0..15 -> {
                            if (!ignoreColor) currentColour = ColorUtils.hexColors[colorIndex] or (alpha shl 24)
                            bold = false
                            italic = false
                            randomCase = false
                            underline = false
                            strikeThrough = false
                        }
                        16 -> randomCase = true
                        17 -> bold = true
                        18 -> strikeThrough = true
                        19 -> underline = true
                        20 -> italic = true
                        21 -> {
                            currentColour = color

                            if (currentColour and -67108864 == 0) currentColour = currentColour or -16777216

                            if (currentColour and -0x4000000 == 0) {
                                currentColour = currentColour or -0x1000000
                            }

                            bold = false
                            italic = false
                            randomCase = false
                            underline = false
                            strikeThrough = false
                        }
                    }

                    currentFont = if (bold && italic)
                        boldItalicFont
                    else if (bold)
                        boldFont
                    else if (italic)
                        italicFont
                    else
                        renderFont

                    if (randomCase) {
                        currentFont.drawString(ColorUtils.randomMagicText(words), width, 0.0, currentColour)
                    } else {
                        currentFont.drawString(words, width, 0.0, currentColour)
                    }

                    if (strikeThrough)
                        RenderUtils.drawLine(width / 2.0 + 1, currentFont.height / 3.0, (width + currentFont.getStringWidth(words)) / 2.0 + 1, currentFont.height / 3.0, fontHeight / 16F)

                    if (underline)
                        RenderUtils.drawLine(width / 2.0 + 1, currentFont.height / 2.0, (width + currentFont.getStringWidth(words)) / 2.0 + 1, currentFont.height / 2.0, fontHeight / 16F)

                    width += currentFont.getStringWidth(words)
                }
            }
        } else {
            // Color code states
            renderFont.drawString(text, 0.0, 0.0, currentColour)
        }

        GlStateUtils.hintPolygon(false)

        GlStateUtils.smooth(false)
        GlStateUtils.lineSmooth(false)
        GlStateUtils.blend(false)

        GlStateUtils.alpha(true)
        GlStateUtils.cull(true)

        //   GlStateUtils.resetTexParam()

        glTranslated(-(x - 1.5), -(y + 0.5), 0.0)

        GlStateUtils.resetColour()

        return (x + getStringWidth(text)).toInt()
    }

    override fun getColorCode(charCode: Char) =
        ColorUtils.hexColors[getColorIndex(charCode)]

    override fun getStringWidth(text: String?): Int {
        var currentText = text

        val event = TextEvent(currentText)
        Muffin.getInstance().eventManager.dispatchEvent(event)
        currentText = event.text ?: return 0

        return if (currentText.contains("§")) {
            val parts = currentText.split("§")

            var currentFont = renderFont
            var width = 0
            var bold = false
            var italic = false

            parts.forEachIndexed { index, part ->
                if (part.isEmpty())
                    return@forEachIndexed

                if (index == 0) {
                    width += currentFont.getStringWidth(part)
                } else {
                    val words = part.substring(1)
                    val type = part[0]
                    val colorIndex = getColorIndex(type)
                    when {
                        colorIndex < 16 -> {
                            bold = false
                            italic = false
                        }
                        colorIndex == 17 -> bold = true
                        colorIndex == 20 -> italic = true
                        colorIndex == 21 -> {
                            bold = false
                            italic = false
                        }
                    }

                    currentFont = if (bold && italic)
                        boldItalicFont
                    else if (bold)
                        boldFont
                    else if (italic)
                        italicFont
                    else
                        renderFont

                    width += currentFont.getStringWidth(words)
                }
            }

            width / 2
        } else
            renderFont.getStringWidth(currentText) / 2
    }

    override fun getCharWidth(character: Char) = getStringWidth(character.toString())

    companion object {
        @JvmStatic
        fun getColorIndex(type: Char): Int {
            return when (type) {
                in '0'..'9' -> type - '0'
                in 'a'..'f' -> type - 'a' + 10
                in 'k'..'o' -> type - 'k' + 16
                'r' -> 21
                else -> -1
            }
        }
    }
}