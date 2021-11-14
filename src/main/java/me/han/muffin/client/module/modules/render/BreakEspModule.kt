package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.world.block.BlockBreakEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mc.block.isAir
import me.han.muffin.client.utils.extensions.mc.block.selectedBox
import me.han.muffin.client.utils.extensions.mc.utils.component1
import me.han.muffin.client.utils.extensions.mc.utils.component2
import me.han.muffin.client.utils.extensions.mc.utils.component3
import me.han.muffin.client.utils.extensions.mixin.entity.curBlockDamageMP
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object BreakEspModule: Module("BreakEsp", Category.RENDER, "Highlights blocks being broken near you") {
    private val mode = EnumValue(Mode.Shrink, "Mode")
    private val interpolation = Value(false, "Interpolation")
    private val onlySelf = Value(true, "OnlySelf")
    private val frustumCheck = Value(true, "FrustumCheck")
    private val obbyOnly = Value(true, "ObbyOnly")

    private val radius = NumberValue(16.0F, 0.0F, 32.0F, 1F, "Radius")

    private val targetRed = NumberValue({ !onlySelf.value },198, 0, 255, 2, "Red")
    private val targetGreen = NumberValue({ !onlySelf.value },176, 0, 255, 2, "Green")
    private val targetBlue = NumberValue({ !onlySelf.value },12, 0, 255, 2, "Blue")
    private val targetAlpha = NumberValue({ !onlySelf.value },78, 0, 255, 2, "Alpha")

    private val selfRed = NumberValue(12, 0, 255, 2, "SelfRed")
    private val selfGreen = NumberValue(176, 0, 255, 2, "SelfGreen")
    private val selfBlue = NumberValue(198, 0, 255, 2, "SelfBlue")
    private val selfAlpha = NumberValue(78, 0, 255, 2, "SelfAlpha")

    private val lineWidth = NumberValue(1.5F, 0.0F, 5.0F, 0.1F, "LineWidth")

    private val breakingBlockList = LinkedHashMap<Int, Triple<BlockPos, Int, Boolean>>() /* <BreakerID, <Position, Progress, Render> */

    // private var progress = 0F
    // private var lastProgress = 0F

    init {
        addSettings(
            mode,
            interpolation,
            onlySelf, frustumCheck, obbyOnly,
            radius,
            selfRed, selfGreen, selfBlue, selfAlpha,
            targetRed, targetGreen, targetBlue, targetAlpha,
            lineWidth
        )
    }

    private enum class Mode {
        Grow, Shrink
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        val playerView = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return
        val (viewX, viewY, viewZ) = MathUtils.interpolateEntity(playerView, RenderUtils.renderPartialTicks)

        var resizedBB: AxisAlignedBB? = null
        var selfBreaking: AxisAlignedBB? = null

        for ((breakID, triple) in breakingBlockList) {
            if (!triple.third) continue

            val box = triple.first.selectedBox
                .grow(RenderUtils.BBGrow)
                .offset(-viewX, -viewY, -viewZ)

            // lastProgress = progress
            val progress = triple.second / 9F

            val interpProgress = if (interpolation.value) Globals.mc.playerController.curBlockDamageMP else progress // MathUtils.interpolate(lastProgress, progress, event.partialTicks)
            val resizedBox = if (mode.value == Mode.Shrink) box.shrink((1F - interpProgress) * box.averageEdgeLength * 0.5)
            else box.shrink(interpProgress * box.averageEdgeLength * 0.5)

            if (Globals.mc.world.getEntityByID(breakID) == Globals.mc.player) {
                selfBreaking = resizedBox
                continue
            }

            resizedBB = resizedBox
        }

        if (resizedBB != null && (!frustumCheck.value || RenderUtils.isInViewFrustum(resizedBB))) {
            if (lineWidth.value > 0.0) RenderUtils.drawBoxFullESP(resizedBB, targetRed.value, targetGreen.value, targetBlue.value, targetAlpha.value, lineWidth.value)
            else RenderUtils.drawBoxESP(resizedBB, targetRed.value, targetGreen.value, targetBlue.value, targetAlpha.value)
        }

        if (selfBreaking != null && (!frustumCheck.value || RenderUtils.isInViewFrustum(selfBreaking))) {
            if (lineWidth.value > 0.0) RenderUtils.drawBoxFullESP(selfBreaking, selfRed.value, selfGreen.value, selfBlue.value, selfAlpha.value, lineWidth.value)
            else RenderUtils.drawBoxESP(selfBreaking, selfRed.value, selfGreen.value, selfBlue.value, selfAlpha.value)
        }

    }

    @Listener
    private fun onBlockBreak(event: BlockBreakEvent) {
        if (fullNullCheck() || Globals.mc.player.distanceTo(event.position) > radius.value) return

        val breaker = Globals.mc.world.getEntityByID(event.breakId) ?: return

        if (event.progress in 0..9) {
            val render = !onlySelf.value || Globals.mc.player == breaker

            breakingBlockList.putIfAbsent(event.breakId, Triple(event.position, event.progress, render))
            breakingBlockList.computeIfPresent(event.breakId) { _, triple -> Triple(event.position, event.progress, triple.third) }
        } else {
            breakingBlockList.remove(event.breakId)
        }
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (fullNullCheck()) return
        breakingBlockList.values.removeIf { it.first.isAir }
    }

}