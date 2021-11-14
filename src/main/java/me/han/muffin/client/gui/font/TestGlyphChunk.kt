package me.han.muffin.client.gui.font

import me.han.muffin.client.utils.render.texture.MipmapTexture
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14

class TestGlyphChunk(val texture: MipmapTexture) {
    private var lodbias = 0.0f

    fun updateLodBias(input: Float) {
        if (input != lodbias) {
            lodbias = input
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, input)
        }
    }

    override fun equals(other: Any?) =
        this === other || other is GlyphChunk && texture == other.texture

}