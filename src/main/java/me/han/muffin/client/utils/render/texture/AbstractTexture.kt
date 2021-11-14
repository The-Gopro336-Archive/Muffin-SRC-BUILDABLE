package me.han.muffin.client.utils.render.texture

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.TextureUtil
import org.lwjgl.opengl.GL11.glGenTextures
import java.awt.image.BufferedImage

abstract class AbstractTexture {

    var textureID: Int = -1; private set

    abstract val width: Int
    abstract val height: Int

    fun genTexture() {
        textureID = glGenTextures()
    }

    fun genTexture(image: BufferedImage) {
        textureID = TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(), image, true, true)
    }

    fun bindTexture() {
        if (textureID != -1) {
            GlStateManager.bindTexture(textureID)
        }
    }

    fun unbindTexture() {
        GlStateManager.bindTexture(0)
    }

    fun deleteTexture() {
        if (textureID != -1) {
            GlStateManager.deleteTexture(textureID)
            textureID = -1
        }
    }

    override fun equals(other: Any?) =
        this === other
            || other is AbstractTexture
            && this.textureID == other.textureID

    override fun hashCode() = textureID

}