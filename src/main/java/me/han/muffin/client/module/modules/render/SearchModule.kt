package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.render.RenderBlockEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.kotlin.toColour
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mc.block.id
import me.han.muffin.client.utils.extensions.mc.block.shulkerList
import me.han.muffin.client.utils.extensions.mc.block.state
import me.han.muffin.client.utils.extensions.mixin.render.orientCamera
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import me.han.muffin.client.utils.render.*
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.Blocks
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.sqrt

internal object SearchModule: Module("Search", Category.RENDER) {

    private val defaultSearchList = hashSetOf(
        Blocks.BED,
        Blocks.MOB_SPAWNER,
        Blocks.PORTAL,
        Blocks.END_PORTAL_FRAME,
        Blocks.END_PORTAL,
        Blocks.DISPENSER,
        Blocks.DROPPER,
        Blocks.HOPPER,
        Blocks.FURNACE,
        Blocks.LIT_FURNACE,
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.ENDER_CHEST,
    ).apply {
        addAll(shulkerList)
    }

    private fun getColour(state: IBlockState): Int {
        val id = state.block.id

        if (id == 56) return 9480789
        if (id == 57) return 9480789
        if (id == 14) return -1869610923
        if (id == 41) return -1869610923
        if (id == 15) return -2140123051
        if (id == 42) return -2140123051
        if (id == 16) return 0x20202055
        if (id == 21) return 3170389
        if (id == 73) return 0x60000055
        if (id == 74) return 0x60000055
        if (id == 129) return 8396885
        if (id == 98) return 9480789
        if (id == 354) return 9480789
        if (id == 49) return 1696715042
        if (id == 90) return 1696715076
        if (id == 10) return -7141377
        if (id == 11) return -7141547
        if (id == 52) return 8051029
        if (id == 26) return -16777131
        if (id == 5) return -1517671851
        if (id == 17) return -1517671851
        if (id == 162) return -1517671851
        if (id == 112) return 16728862

        val colourValue = state.material.materialMapColor.colorValue

        val red = colourValue shr 16 and 0xFF
        val green = colourValue shr 8 and 0xFF
        val blue = colourValue and 0xFF

        return Colour(red, green, blue, 100).toHex()
    }

    private val blocksCache = arrayListOf<BlockData>().synchronized()

    private val boundingBox = Value(true, "BoundingBox")
    private val fillings = Value(true, "Fillings")
    private val tracers = Value(true, "Tracers")
    private val lineWidth = NumberValue(0.6F, 0.1F, 10.0F, 0.1F, "LineWidth")

    init {
        addSettings(boundingBox, fillings, tracers, lineWidth)
    }

    @Listener
    private fun onBlockRender(event: RenderBlockEvent) {
        if (blocksCache.size >= 100000) blocksCache.clear()

        val block = event.state.block
        if (defaultSearchList.contains(block)) {
            val data = BlockData(event.pos.toVec3d())
            if (!blocksCache.contains(data) && Globals.mc.player.getDistance(data.vector.x, data.vector.y, data.vector.z) <= 256 && block != Blocks.AIR) {
                blocksCache.add(data)
            }
        }

    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        val viewEntity = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return

        synchronized(blocksCache) {
            for (cache in blocksCache) {
                val pos = cache.vector.toBlockPos()
                val state = pos.state

                if (viewEntity.getDistance(cache.vector.x, cache.vector.y, cache.vector.z) > 256.0 || !defaultSearchList.contains(state.block)) continue

                val oreColour = getColour(state)
                val colour = oreColour.toColour()

                if (boundingBox.value || fillings.value) {
                    val axisBB = state.getBoundingBox(Globals.mc.world, pos)
                        .offset(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                        .offset(-RenderUtils.renderPosX, -RenderUtils.renderPosY, -RenderUtils.renderPosZ)

                    if (boundingBox.value) RenderUtils.drawBoxOutlineESP(axisBB, colour, 1.5F)
                    if (fillings.value) RenderUtils.drawBoxESP(axisBB, colour)
                }

                if (!tracers.value) continue

                val posX = cache.vector.x - RenderUtils.renderPosX
                val posY = cache.vector.y - RenderUtils.renderPosY
                val posZ = cache.vector.z - RenderUtils.renderPosZ

                Dimension.ThreeD {
                    glLoadIdentity()

                    val bobbing = Globals.mc.gameSettings.viewBobbing
                    glLineWidth(lineWidth.value)

                    Globals.mc.gameSettings.viewBobbing = false
                    Globals.mc.entityRenderer.orientCamera(event.partialTicks)

                    val eyeVector = Vec3d(0.0, 0.0, 1.0)
                        .rotatePitch(-viewEntity.rotationPitch.toRadian())
                        .rotateYaw(-viewEntity.rotationYaw.toRadian())

                    GL_LINES withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
                        pos(eyeVector.x, viewEntity.eyeHeight + eyeVector.y, eyeVector.z, colour = colour)
                        pos(posX + 0.5, posY + 0.5, posZ + 0.5, colour = colour)
                    }

                    Globals.mc.gameSettings.viewBobbing = bobbing
                }
            }
        }

        GlStateUtils.resetColour()
    }

    private class BlockData(val vector: Vec3d) {

        override fun equals(other: Any?): Boolean {
            if (other is BlockData) return compareValues(other.vector.x, vector.x) == 0 && compareValues(other.vector.y, vector.y) == 0 && compareValues(other.vector.z, vector.z) == 0
            return super.equals(other)
        }

        fun distanceTo(data: BlockData): Double {
            val xDiff = vector.x - data.vector.x
            val yDiff = vector.y - data.vector.y
            val zDiff = vector.z - data.vector.z
            return sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
        }

        override fun hashCode(): Int {
            return vector.hashCode()
        }

    }


}