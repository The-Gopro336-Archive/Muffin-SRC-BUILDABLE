package me.han.muffin.client.gui.font

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.render.texture.MipmapTexture
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.TextureUtil
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL14
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Generate new bitmap based font renderer
 */
class AWTFontRenderer(val font: Font, startChar: Int = 0, stopChar: Int = 256, var loadingScreen: Boolean = false) {
    companion object {
        const val TEXTURE_WIDTH = 1024
        const val TEXTURE_WIDTH_DOUBLE = 1024.0
        const val MAX_TEXTURE_HEIGHT = 4096

        var assumeNonVolatile: Boolean = false
        val activeFontRenderers: ArrayList<AWTFontRenderer> = ArrayList()

        private var gcTicks: Int = 0
        private const val GC_TICKS = 600 // Start garbage collection every 600 frames
        private const val CACHED_FONT_REMOVAL_TIME = 30000 // Remove cached texts after 30s of not being used

        fun garbageCollectionTick() {
            if (gcTicks++ > GC_TICKS) {
                activeFontRenderers.forEach { it.collectGarbage() }
                gcTicks = 0
            }
        }
    }

    private fun collectGarbage() {
        val currentTime = System.currentTimeMillis()

        cachedStrings
            .filter { currentTime - it.value.lastUsage > CACHED_FONT_REMOVAL_TIME }
            .forEach {
                glDeleteLists(it.value.displayList, 1)
                it.value.deleted = true
                cachedStrings.remove(it.key)
            }
    }

    private var fontHeight = -1
    private val charLocations = arrayOfNulls<CharLocation>(stopChar)

    private val cachedStrings: HashMap<String, CachedFont> = HashMap()

    private var textureID = -1
    // private var glyphChunk: TestGlyphChunk? = null
    private var textureWidth = 0
    private var textureHeight = 0

    val height: Int
        get() = (fontHeight - 8) / 2

    init {
        renderBitmap(startChar, stopChar)

        activeFontRenderers.add(this)
    }

    /**
     * Allows you to draw a string with the target font
     *
     * @param text  to render
     * @param x     location for target position
     * @param y     location for target position
     * @param color of the text
     */
    fun drawString(text: String, x: Double, y: Double, color: Int) {
        val scale = 0.25
        val reverse = 1 / scale

        glPushMatrix()
        glScaled(scale, scale, 1.0)
        glTranslated(x * 2F, y * 2.0 - 2.0, 0.0)

        bindGlTexture()

        val red = (color shr 16 and 0xff) / 255F
        val green = (color shr 8 and 0xff) / 255F
        val blue = (color and 0xff) / 255F
        val alpha = (color shr 24 and 0xff) / 255F

        glColor4f(red, green, blue, alpha)

        var currX = 0.0

        val cached: CachedFont? = cachedStrings[text]

        if (cached != null) {
            glCallList(cached.displayList)
            cached.lastUsage = System.currentTimeMillis()
            glPopMatrix()

            return
        }

        var list = -1

        if (assumeNonVolatile) {
            list = glGenLists(1)

            glNewList(list, GL_COMPILE_AND_EXECUTE)
        }

        glBegin(GL_QUADS)

        for (char in text.toCharArray()) {
            if (char.toInt() >= charLocations.size) {
                glEnd()

                // Ugly solution, because floating point numbers, but I think that shouldn't be that much of a problem
                glScaled(reverse, reverse, reverse)
                Globals.mc.fontRenderer.drawString("$char", currX.toFloat() * scale.toFloat() + 1, 2f, color, false)
                currX += Globals.mc.fontRenderer.getStringWidth("$char") * reverse

                glScaled(scale, scale, scale)

                bindGlTexture()

                glColor4f(red, green, blue, alpha)

                glBegin(GL_QUADS)
            } else {
                val fontChar = charLocations[char.toInt()] ?: continue

                drawChar(fontChar, currX.toFloat(), 0f)
                currX += fontChar.width - 8.0
            }
        }

        glEnd()

        if (assumeNonVolatile) {
            cachedStrings[text] = CachedFont(list, System.currentTimeMillis())
            glEndList()
        }

        glPopMatrix()
    }

    /**
     * Draw char from texture to display
     *
     * @param char target font char to render
     * @param x        target position x to render
     * @param y        target position y to render
     */
    private fun drawChar(char: CharLocation, x: Float, y: Float) {
        val width = char.width.toFloat()
        val height = char.height.toFloat()
        val srcX = char.x.toFloat()
        val srcY = char.y.toFloat()
        val renderX = srcX / textureWidth
        val renderY = srcY / textureHeight
        val renderWidth = width / textureWidth
        val renderHeight = height / textureHeight

        glTexCoord2f(renderX, renderY)
        glVertex2f(x, y)
        glTexCoord2f(renderX, renderY + renderHeight)
        glVertex2f(x, y + height)
        glTexCoord2f(renderX + renderWidth, renderY + renderHeight)
        glVertex2f(x + width, y + height)
        glTexCoord2f(renderX + renderWidth, renderY)
        glVertex2f(x + width, y)
    }

    /**
     * Render font chars to a bitmap
     */
    private fun renderBitmap(startChar: Int, stopChar: Int) {
        val fontImages = arrayOfNulls<BufferedImage>(stopChar)

//        val bufferedImage = BufferedImage(TEXTURE_WIDTH, MAX_TEXTURE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
//        val graphics2D = bufferedImage.graphics as Graphics2D
//        graphics2D.background = Color(0, 0, 0, 0)

        var rowHeight = 0
        var charX = 0
        var charY = 0

        for (targetChar in startChar until stopChar) {
            val fontImage = drawCharToImage(targetChar.toChar())
            val fontChar = CharLocation(charX, charY, fontImage.width, fontImage.height)

            if (fontChar.height > fontHeight) fontHeight = fontChar.height
            if (fontChar.height > rowHeight) rowHeight = fontChar.height

            charLocations[targetChar] = fontChar
            fontImages[targetChar] = fontImage

//            graphics2D.drawImage(fontImage, charX, charY, null)
            charX += fontChar.width

            if (charX > 2048) {
                if (charX > textureWidth) textureWidth = charX
                charX = 0
                charY += rowHeight
                rowHeight = 0
            }
        }

       textureHeight = charY + rowHeight
       val textureImage = BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB)
//        (textureImage.graphics as Graphics2D).drawImage(bufferedImage, 0, 0, null)

        val textureGraphics2D = textureImage.createGraphics()
        textureGraphics2D.font = font
        textureGraphics2D.color = Color(255, 255, 255, 0)
        textureGraphics2D.fillRect(0, 0, textureWidth, textureHeight)
        textureGraphics2D.color = Color.WHITE

        for (targetChar in startChar until stopChar)
            if (fontImages[targetChar] != null && charLocations[targetChar] != null)
                textureGraphics2D.drawImage(fontImages[targetChar], charLocations[targetChar]!!.x, charLocations[targetChar]!!.y, null)

        // glyphChunk = TestGlyphChunk(createTexture(textureImage))
        textureID = TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(), textureImage, true, true)
    }

    /**
     * Draw a char to a buffered image
     *
     * @param ch char to render
     * @return image of the char
     */
    private fun drawCharToImage(ch: Char): BufferedImage {
        val tempGraphics2D = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
        tempGraphics2D.font = font

        val fontMetrics = tempGraphics2D.fontMetrics
        tempGraphics2D.dispose()

        var charWidth = fontMetrics.charWidth(ch) + 8
        if (charWidth <= 0) charWidth = 7

        var charHeight = fontMetrics.height + 3
        if (charHeight <= 0) charHeight = font.size

        val fontImage = BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = fontImage.createGraphics()

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        // graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.font = font
        graphics.color = Color.WHITE
        graphics.drawString(ch.toString(), 3, 1 + fontMetrics.ascent)
        graphics.dispose()

        return fontImage
    }

    /**
     * Calculate the string width of a text
     *
     * @param text for width calculation
     * @return the width of the text
     */
    fun getStringWidth(text: String): Int {
        var width = 0
        val reverse = 1 / 0.25

        for (c in text.toCharArray()) {
            width += if (c.toInt() >= charLocations.size) {
                (Globals.mc.fontRenderer.getStringWidth("$c") * reverse).toInt()
            } else {
                val fontChar = charLocations[
                        if (c.toInt() < charLocations.size)
                            c.toInt()
                        else
                            '\u0003'.toInt()
                ] ?: continue

                fontChar.width - 8
            }
        }

        return width / 2
    }

    private fun bindGlTexture() {
//        glyphChunk?.texture?.bindTexture()
//        glyphChunk?.updateLodBias(0.0F * 0.25F - 0.5F)
        if (this.loadingScreen) glBindTexture(GL_TEXTURE_2D, textureID) else GlStateManager.bindTexture(textureID)
    }

    private fun createTexture(bufferedImage: BufferedImage) = MipmapTexture(bufferedImage, GL_ALPHA, 4).apply {
        bindTexture()
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glTexParameterf(GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F)
        unbindTexture()
    }

    fun delete() {
        if (textureID != -1) {
            glDeleteTextures(textureID)
            textureID = -1
        }

        activeFontRenderers.remove(this)
    }

    fun finalize() {
        delete()
    }

    /**
     * Data class for saving char location of the font image
     */
    private data class CharLocation(var x: Int, var y: Int, var width: Int, var height: Int)
}