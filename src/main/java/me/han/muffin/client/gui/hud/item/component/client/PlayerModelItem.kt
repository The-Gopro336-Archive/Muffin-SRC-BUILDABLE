package me.han.muffin.client.gui.hud.item.component.client

import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils.renderPartialTicks
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

object PlayerModelItem : HudItem("PlayerModel", HudCategory.Client, 5, 30) {
    private val scale = NumberValue(35, 1, 100, 1, "Size")
    private val onlySelf = Value(true, "SelfOnly")
    private val noClamp = Value(false, "NoClamp")
    private val emulateYaw = Value({ !noClamp.value }, false, "Emulate Yaw")
    private val emulatePitch = Value({ !noClamp.value }, true, "Emulate Pitch")

    init {
        addSettings(scale, onlySelf, noClamp, emulateYaw, emulatePitch)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (EntityUtil.fullNullCheck() || Globals.mc.renderManager.renderViewEntity == null) return

        GlStateUtils.matrix(true)
        GlStateUtils.depth(true)
        GlStateUtils.resetColour()

        val lastAttackedEntity = Globals.mc.player.lastAttackedEntity
        val entity = if (!onlySelf.value && Globals.mc.player.ticksExisted - Globals.mc.player.lastAttackedEntityTime <= 80) lastAttackedEntity else Globals.mc.player

        val pitch = if (noClamp.value) getInterpolateAmount(Globals.mc.player.prevRotationPitch, Globals.mc.player.rotationPitch) else if (emulatePitch.value) getWrapInterpolateAmount(Globals.mc.player.prevRotationPitch, Globals.mc.player.rotationPitch) else 0.0f
        val yaw = if (noClamp.value) getInterpolateAmount(Globals.mc.player.prevRotationYaw, Globals.mc.player.rotationYaw) else if (emulateYaw.value) getWrapInterpolateAmount(Globals.mc.player.prevRotationYaw, Globals.mc.player.rotationYaw) else 0.0f

        GuiInventory.drawEntityOnScreen(x + 22, y + 72, scale.value, -yaw, -pitch, entity)
        GlStateUtils.depth(false)
        GlStateUtils.texture2d(true)
        GlStateUtils.blend(true)

        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableColorMaterial()
        GlStateUtils.matrix(false)

        width = scale.value + 10F
        height = scale.value + 40F
    }

    private fun getInterpolateAmount(prev: Float, current: Float): Float {
        return MathUtils.interpolate(current.toDouble(), prev.toDouble(), renderPartialTicks).toFloat()
    }

    private fun getWrapInterpolateAmount(prev: Float, current: Float): Float {
        return RotationUtils.normalizeAngle(MathUtils.interpolate(current.toDouble(), prev.toDouble(), renderPartialTicks)).toFloat()
    }

}