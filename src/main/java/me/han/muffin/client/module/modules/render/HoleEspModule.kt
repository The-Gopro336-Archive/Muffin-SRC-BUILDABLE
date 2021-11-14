package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.HoleManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.block.HoleInfo
import me.han.muffin.client.utils.block.HoleType
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.Executors
import kotlin.concurrent.thread

internal object HoleEspModule: Module("HoleESP", Category.RENDER, "Highlights holes for crystal pvp.") {

    private val page = EnumValue(Pages.General, "Page")

    private val bottom = Value({ page.value == Pages.General }, true, "Bottom")
    private val hideOwn = Value({ page.value == Pages.General }, true, "HideOwn")
    private val frustumCheck = Value({ page.value == Pages.General }, true, "FrustumCheck")

    private val renderDistance = NumberValue({ page.value == Pages.General }, 8, 1, 15, 1, "Radius")
    private val delay = NumberValue({ page.value == Pages.General }, 2, 0, 40, 1, "Delay")
    private val maxHoles = NumberValue({ page.value == Pages.General }, 10, 0, 50, 1, "MaxHoles")

    private val renderMode = EnumValue({ page.value == Pages.General }, RenderMode.Full, "RenderMode")

    private val flatOutline = Value({ page.value == Pages.General && renderMode.value == RenderMode.Glow }, true, "FlatOutline")
    private val outlineAlpha = NumberValue({ page.value == Pages.General && renderMode.value == RenderMode.Glow }, 255, 0, 255, 1, "OutlineAlpha")
    private val glowHeight = NumberValue({ page.value == Pages.General && renderMode.value == RenderMode.Glow }, 0.5, 0.0, 3.0, 0.01, "GlowHeight")

    private val alpha = NumberValue({ page.value == Pages.General }, 75, 0, 255, 1, "Alpha")

    private val lineWidth = NumberValue({ page.value == Pages.General }, 1.0F, 0.0F, 5.0F, 0.1F, "LineWidth")
    private val renderWidth = NumberValue({ page.value == Pages.General }, 1.0F, 0.0F, 1.0F, 0.1F, "RenderWidth")

    private val renderHeight = NumberValue({ page.value == Pages.General }, 1.0F, 0.0F, 1.0F, 0.01F, "RenderHeight")
    private val holeType = EnumValue({ page.value == Pages.Normal }, HoleTypes.Both, "Blocks")
    private val redObby = NumberValue({ page.value == Pages.Normal && (holeType.value == HoleTypes.Obby || holeType.value == HoleTypes.Both) }, 210, 0, 255, 1, "Obby-Red")
    private val greenObby = NumberValue({ page.value == Pages.Normal && (holeType.value == HoleTypes.Obby || holeType.value == HoleTypes.Both) }, 0, 0, 255, 1, "Obby-Green")
    private val blueObby = NumberValue({ page.value == Pages.Normal && (holeType.value == HoleTypes.Obby || holeType.value == HoleTypes.Both) }, 0, 0, 255, 1, "Obby-Blue")

    private val redBed = NumberValue({ page.value == Pages.Normal && (holeType.value == HoleTypes.Bedrock || holeType.value == HoleTypes.Both) }, 110, 0, 255, 1, "Bed-Red")
    private val greenBed = NumberValue({ page.value == Pages.Normal && (holeType.value == HoleTypes.Bedrock || holeType.value == HoleTypes.Both) }, 255, 0, 255, 1, "Bed-Green")
    private val blueBed = NumberValue({ page.value == Pages.Normal && (holeType.value == HoleTypes.Bedrock || holeType.value == HoleTypes.Both) }, 0, 0, 255, 1, "Bed-Blue")

    private val doubleHoles = Value({ page.value == Pages.Double }, false, "DoubleHoles")
    private val redDouble = NumberValue({ page.value == Pages.Double && doubleHoles.value }, 20, 0, 255, 1, "Double-Red")
    private val greenDouble = NumberValue({ page.value == Pages.Double && doubleHoles.value }, 255, 0, 255, 1, "Double-Green")
    private val blueDouble = NumberValue({ page.value == Pages.Double && doubleHoles.value }, 110, 0, 255, 1, "Double-Blue")

    private val quadHoles = Value({ page.value == Pages.Quad }, false, "QuadHoles")
    private val redQuad = NumberValue({ page.value == Pages.Quad && quadHoles.value }, 20, 0, 255, 1, "Quad-Red")
    private val greenQuad = NumberValue({ page.value == Pages.Quad && quadHoles.value }, 255, 0, 255, 1, "Quad-Green")
    private val blueQuad = NumberValue({ page.value == Pages.Quad && quadHoles.value }, 110, 0, 255, 1, "Quad-Blue")

    //    private val TEST = ColourValue(BetterColour(255.0, 255.0, 255.0, 255.0), "TESTING")

    private var safeHoles = hashMapOf<AxisAlignedBB, Colour>().synchronized()

    private val timer = Timer()
    private var updateThread: Thread? = null
    private val threadPool = Executors.newCachedThreadPool()

    private enum class Pages {
        General, Normal, Double, Quad
    }

    private enum class HoleTypes {
        None, Obby, Bedrock, Both
    }

    private enum class RenderMode {
        Solid, Outline, Full, Glow
    }

    init {
        addSettings(
            // General //
            page, bottom, hideOwn, frustumCheck, renderDistance, delay, maxHoles, renderMode, flatOutline, outlineAlpha, glowHeight, alpha, lineWidth, renderHeight,
            // Normal //
            holeType, redObby, greenObby, blueObby, redBed, greenBed, blueBed,
            // Double //
            doubleHoles, redDouble, greenDouble, blueDouble,
            // Quad //
            quadHoles, redQuad, greenQuad, blueQuad
        )
    }

    override fun getHudInfo(): String? {
        if (safeHoles.isNotEmpty()) return safeHoles.size.toString()
        return null
    }

    private fun doHoleESP() {

        val eyesPos = Globals.mc.player.eyePosition
        val flooredPosition = Globals.mc.player.flooredPosition

        val tempSafeHoles = hashMapOf<AxisAlignedBB, Colour>()
        // val tempSafeHoles = arrayListOf<Pair<AxisAlignedBB, Colour>>()
        val possibleWideHoles = arrayListOf<Pair<BlockPos, BlockPos>>()

        if (!timer.passedTicks(delay.value)) return

        for (holeInfo in HoleManager.holeInfosNearFiltered) {
            val pos = holeInfo.origin

            if (hideOwn.value && pos == flooredPosition) continue

            if (eyesPos.squareDistanceTo(holeInfo.center) > renderDistance.value.square) continue
            if (frustumCheck.value && !RenderUtils.isInViewFrustum(pos)) continue

            val colour = getHoleColour(holeInfo) ?: continue

            if (maxHoles.value == 0 || tempSafeHoles.size < maxHoles.value) {
                tempSafeHoles[holeInfo.boundingBox.offset(0.0, if (bottom.value) -1.0 else 0.0, 0.0)] = colour
            }
        }

        synchronized(safeHoles) {
            safeHoles.clear()
            safeHoles.putAll(tempSafeHoles)
        }

        timer.reset()
    }

    private fun updateHoleESPMT() {
        if (updateThread == null || !updateThread!!.isAlive || updateThread!!.isInterrupted) {
            updateThread = thread(start = false) { doHoleESP() }
            threadPool.execute(updateThread!!)
        }
    }

    private fun getHoleColour(info: HoleInfo): Colour? {
        return when (info.type) {
            HoleType.Obsidian -> if (shouldAddObby()) Colour(redObby.value, greenObby.value, blueObby.value) else null
            HoleType.Bedrock -> if (shouldAddBedrock()) Colour(redBed.value, greenBed.value, blueBed.value) else null
            HoleType.Two -> if (doubleHoles.value) Colour(redDouble.value, greenDouble.value, blueDouble.value) else null
            HoleType.Four -> if (quadHoles.value) Colour(redQuad.value, greenQuad.value, blueQuad.value) else null
            else -> null
        }
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.POST || fullNullCheck()) return
        updateHoleESPMT()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck() || safeHoles.isEmpty()) return

        synchronized(safeHoles) {
            for ((bb, colour) in safeHoles) {
                val renderBB = AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + renderHeight.value, bb.maxZ)
                    .offset(-RenderUtils.renderPosX, -RenderUtils.renderPosY, -RenderUtils.renderPosZ)

                if (RenderUtils.isInViewFrustum(renderBB)) {
                    val (r, g, b) = colour
                    val a = alpha.value
                    val width = lineWidth.value
                    when (renderMode.value) {
                        RenderMode.Solid -> RenderUtils.drawBoxESP(renderBB, r, g, b, a)
                        RenderMode.Outline -> RenderUtils.drawBoxOutlineESP(renderBB, r, g, b, a, width)
                        RenderMode.Full -> RenderUtils.drawBoxFullESP(renderBB, r, g, b, a, width)
                        RenderMode.Glow -> {
                            colour.a = alpha.value
                            RenderUtils.drawBBGlowEspBox(renderBB, colour, outlineAlpha.value, width, glowHeight.value, flatOutline.value)
                        }
                    }
                }
            }
        }

    }

    private fun shouldAddObby(): Boolean {
        return holeType.value == HoleTypes.Obby || holeType.value == HoleTypes.Both
    }

    private fun shouldAddBedrock(): Boolean {
        return holeType.value == HoleTypes.Bedrock || holeType.value == HoleTypes.Both
    }

}