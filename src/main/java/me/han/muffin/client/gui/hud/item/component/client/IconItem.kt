package me.han.muffin.client.gui.hud.item.component.client

import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.manager.managers.TextureManager
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11.*

object IconItem: HudItem("Icon", HudCategory.Client, 2, 11) {
    private val clientColor = Value(false, "ClientColor")
    private val size = NumberValue(10F, 1F, 15F, 0.1F, "Size")

    init {
        addSettings(clientColor, size)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (clientColor.value) RenderUtils.glColorClient(255) else GlStateUtils.resetColour()
        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

        GlStateUtils.depth(false)
        GlStateUtils.texture2d(true)

        glTranslatef(1.0F, 1.0F, 0.0F)
        TextureManager.drawMipmapIcon512(x.toDouble(), y.toDouble(), size.value * 8)

        GlStateUtils.texture2d(true)
        GlStateUtils.depth(true)
        GlStateUtils.blend(false)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        width = size.value * 8 + 2
        height = size.value * 8 + 2
    }

}