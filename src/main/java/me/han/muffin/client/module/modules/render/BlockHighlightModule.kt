package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.render.DrawSelectionBoxEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.other.RenderModeModule
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.extensions.mc.block.state
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.NumberValue
import net.minecraft.block.material.Material
import net.minecraft.util.math.RayTraceResult
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object BlockHighlightModule : Module("BlockHighlight", Category.RENDER, true, "Highlight the block you looking at.") {
    private val alpha = NumberValue(255, 0, 255, 1, "Alpha")
    private val width = NumberValue(1.0F, 0.1F, 4.0F, 0.1F, "Width")

    init {
        addSettings(alpha, width)
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        val (r, g, b, a) = ColourUtils.getClientColour(alpha.value)

        val ray = Globals.mc.objectMouseOver ?: return
        val playerView = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return

        if (ray.typeOfHit == RayTraceResult.Type.BLOCK && ray.sideHit != null) {
            val pos = ray.blockPos
            val state = pos.state

            if (state.material != Material.AIR && Globals.mc.world.worldBorder.contains(pos)) {
                val interpView = MathUtils.interpolateEntity(playerView, RenderUtils.renderPartialTicks)

                val bb = state.getSelectedBoundingBox(Globals.mc.world, pos)
                    .grow(RenderUtils.BBGrow)
                    .offset(-interpView.x, -interpView.y, -interpView.z)

                when (RenderModeModule.blockHighLight.value) {
                    RenderModeModule.RenderMode.Solid -> RenderUtils.drawBoxESP(bb, r, g, b, a)
                    RenderModeModule.RenderMode.Outline -> RenderUtils.drawBoxOutlineESP(bb, r, g, b, a, width.value)
                    RenderModeModule.RenderMode.Full -> RenderUtils.drawBoxFullESP(bb, r, g, b, a, width.value)
                    else -> RenderUtils.drawBoxOutlineESP(bb, r, g, b, 255, width.value)
                }

            }
        }
    }

    @Listener
    private fun onDrawSelectionBox(event: DrawSelectionBoxEvent) {
        event.cancel()
    }

}