package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.selectedBox
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.VectorUtils
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.RotationUtils.isEntityInSight
import me.han.muffin.client.utils.math.rotation.RotationUtils.isEntityInSightStrict
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import net.minecraft.block.BlockEnderChest
import net.minecraft.util.math.AxisAlignedBB
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object DirectionTest: Module("DirectionTester", Category.COMBAT) {
    private val mode = EnumValue(Mode.Strict, "Mode")

    private enum class Mode {
        Normal, Strict
    }

    init {
        addSettings(mode)
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

//        val crystalList = CrystalUtils.getCrystalList(6.0)
//
//        crystalList.forEach {
//            val vec = MathUtils.getInterpolatedRenderPos(it, event.partialTicks)
//            val posX = vec.x
//            val posY = vec.y
//            val posZ = vec.z
//            val bb = AxisAlignedBB(0.0, 0.0, 0.0, it.width.toDouble(), it.height.toDouble(), it.width.toDouble()).offset(posX - it.width / 2.0f, posY, posZ - it.width / 2.0f)
//
//            when (mode.value) {
//                Mode.Normal -> RenderUtils.drawBoxFullESP(bb, if (RotationUtils.isLegit(it)) Colour(21, 197, 12, 85) else Colour(197, 21, 12, 85), 3.0F)
//                Mode.Strict -> RenderUtils.drawBoxFullESP(bb, if (it.isEntityInSightStrict()) Colour(21, 197, 12, 85) else Colour(197, 21, 12, 85), 3.0F)
//            }
//        }

//        val around = VectorUtils.getBlockPosInSphere(Globals.mc.player.eyePosition, 6.0F)
//        around.forEach {
//            if (it.block is BlockEnderChest) {
//                val boundingBox = it.state.getBoundingBox(Globals.mc.world, it)
//                val collisionBox = it.collisionBox // .offset(-RenderUtils.renderPosX, -RenderUtils.renderPosY, -RenderUtils.renderPosZ)
//                val selectedBox = it.selectedBox.offset(-RenderUtils.renderPosX, -RenderUtils.renderPosY, -RenderUtils.renderPosZ)
//
//                RenderUtils.drawBoxFullESP(boundingBox, Colour(21, 197, 12, 85), 3.0F)
//                RenderUtils.drawBoxFullESP(selectedBox, Colour(197, 21, 12, 85), 3.0F)
//
//                println("boundingbox = $boundingBox")
//                println("collisionbox = $collisionBox")
//
//                println("selectedbox = $selectedBox")
//
//            }
//        }

        val around = VectorUtils.getBlockPosInSphere(Globals.mc.player.eyePosition, 6.0F)
        around.forEach {
            if (it.block is BlockEnderChest) {
                val renderBox = it.selectedBox.offset(-RenderUtils.renderPosX, -RenderUtils.renderPosY, -RenderUtils.renderPosZ)
                val result = RotationUtils.isLegit(it)
                RenderUtils.drawBoxFullESP(renderBox, if (result) Colour(21, 197, 120, 85) else Colour(197, 21, 12, 85), 3.0F)
            }
        }

    }

}