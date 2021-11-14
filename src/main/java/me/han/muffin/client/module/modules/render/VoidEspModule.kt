package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.isAir
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.Executors
import kotlin.concurrent.thread

internal object VoidEspModule: Module("VoidESP", Category.RENDER, "Highlights void blocks.") {

    private val detectMode = EnumValue(DetectMode.Block, "DetectMode")
    private val holeType = EnumValue(HoleType.Hole, "HoleType")
    private val radius = NumberValue(8, 1, 15, 1, "Radius")
    private val yHeight = NumberValue(14, 1, 256, 1, "YHeight")

    private val renderMode = EnumValue(RenderMode.Full, "RenderMode")
    private val red = NumberValue(30, 0, 255, 1, "Red")
    private val green = NumberValue(215, 0, 255, 1, "Green")
    private val blue = NumberValue(180, 0, 255, 1, "Blue")
    private val alpha = NumberValue(80, 0, 255, 1, "Alpha")
    private val renderHeight = NumberValue(0.05f, 0f, 1f, 0.01f, "RenderHeight")
    private val lineWidth = NumberValue(1.0f, 0f, 5f, 0.1f, "LineWidth")

    private val voidHoles = hashSetOf<BlockPos>().synchronized()

    private var updateThread: Thread? = null
    private val updateExecutor = Executors.newSingleThreadExecutor()

    init {
        addSettings(detectMode, holeType, radius, yHeight, renderMode, red, green, blue, alpha, renderHeight, lineWidth)
    }

    private enum class HoleType {
        Hole, Void
    }

    private enum class RenderMode {
        Solid, Outline, Full
    }

    private enum class DetectMode {
        Block, Air
    }

    override fun onEnable() {
    }

    override fun onDisable() {
    }

    private fun doVoidEsp() {
        val tempVoidHoles = hashSetOf<BlockPos>()

        for (x in -radius.value..radius.value) for (z in -radius.value..radius.value) {
            val pos = BlockPos(Globals.mc.player.posX + x, 0.0, Globals.mc.player.posZ + z)
            if (Globals.mc.player.distanceTo(pos) > radius.value) continue
            if (detectMode.value == DetectMode.Air && !isVoidAir(pos)) continue
            if (detectMode.value == DetectMode.Block && !isVoidBlock(pos)) continue
            val renderPos = if (holeType.value == HoleType.Void) pos.down() else pos
            tempVoidHoles.add(renderPos)
        }

        synchronized(voidHoles) {
            voidHoles.clear()
            voidHoles.addAll(tempVoidHoles)
        }

    }

    private fun updateVoids() {
        if (updateThread == null || !updateThread!!.isAlive || updateThread!!.isInterrupted) {
            updateThread = thread(start = false) {
                doVoidEsp()
            }
            updateExecutor.execute(updateThread!!)
        }
    }


    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.POST || fullNullCheck()) return
        if (Globals.mc.player.dimension == 1 || Globals.mc.player.posY > yHeight.value) return

        updateVoids()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        synchronized(voidHoles) {
            voidHoles.forEach {
                if (RenderUtils.isInViewFrustum(it)) {
                    val colour = Colour(red.value, green.value, blue.value, alpha.value)
                    val width = lineWidth.value
                    val renderHeight = renderHeight.value

                    val bb = AxisAlignedBB(
                        it.x - Globals.mc.renderManager.viewerPosX, it.y - Globals.mc.renderManager.viewerPosY, it.z - Globals.mc.renderManager.viewerPosZ, it.x + 1 - Globals.mc.renderManager.viewerPosX, it.y + 1 - Globals.mc.renderManager.viewerPosY, it.z + 1 - Globals.mc.renderManager.viewerPosZ
                    )

                    if (RenderUtils.isInViewFrustum(bb)) {
                        val renderBB = AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + renderHeight, bb.maxZ)
                        when (renderMode.value) {
                            RenderMode.Solid -> RenderUtils.drawBoxESP(renderBB, colour)
                            RenderMode.Outline -> RenderUtils.drawBoxOutlineESP(renderBB, colour, width)
                            RenderMode.Full -> RenderUtils.drawBoxFullESP(renderBB, colour, width)
                        }
                    }

                }
            }
        }

    }

    private fun isAnyBedrock(origin: BlockPos, offset: Array<BlockPos>): Boolean {
        for (pos in offset) {
            if (origin.add(pos).block == Blocks.BEDROCK) {
                return true
            }
        }
        return false
    }

    private fun isVoidAir(pos: BlockPos) = pos.isAir && pos.up().isAir && pos.up(2).isAir

    private fun isVoidBlock(pos: BlockPos) =
        pos.block != Blocks.BEDROCK && pos.up().block != Blocks.BEDROCK
    //    Globals.mc.world.isAirBlock(pos) &&
            //    (BlockUtil.getBlock(pos.add(0, 1, 0).north()) != Blocks.BEDROCK && BlockUtil.getBlock(pos.add(0, 2, 0).north()) != Blocks.BEDROCK ||
            //    BlockUtil.getBlock(pos.add(0, 1, 0).south()) != Blocks.BEDROCK && BlockUtil.getBlock(pos.add(0, 2, 0).south()) != Blocks.BEDROCK ||
            //    BlockUtil.getBlock(pos.add(0, 1, 0).east()) != Blocks.BEDROCK && BlockUtil.getBlock(pos.add(0, 2, 0).east()) != Blocks.BEDROCK ||
            //    BlockUtil.getBlock(pos.add(0, 1, 0).west()) != Blocks.BEDROCK && BlockUtil.getBlock(pos.add(0, 2, 0).east()) != Blocks.BEDROCK)



}