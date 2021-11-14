package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.value.NumberValue
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object AutoTrapRewriteModule: Module("InstantTrap", Category.COMBAT, "Automatically trap enemies with obsidian.") {
    private val range = NumberValue(5.5, 0.1, 8.0, 0.1, "Range")

    private var currentTarget: EntityPlayer? = null

    init {
        addSettings(range)
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        currentTarget = EntityUtil.findClosestTarget(range.value) ?: return

        val structure = getStructure()
        if (structure.isEmpty()) return

        println(structure)
    }

    private fun getStructure(): ArrayList<Pair<BlockPos, EnumFacing>> {
        val target = currentTarget ?: return arrayListOf()

        val structure = ArrayList<Pair<BlockPos, EnumFacing>>()
        val ourPosition = Globals.mc.player.positionVector
        val targetVector = target.positionVector
        val targetPosition = targetVector.toBlockPos()

        val thirdLayer = getFinalBlockLayer(targetPosition.up().up())

        structure.addAll(getLayers(targetPosition))
        structure.addAll(getLayers(targetPosition.up()))
        structure.addAll(thirdLayer)

        return structure
    }

    private fun getLayers(position: BlockPos): ArrayList<Pair<BlockPos, EnumFacing>> {
        val layer = ArrayList<Pair<BlockPos, EnumFacing>>()
        for (hFacing in EnumFacing.HORIZONTALS) {
            val offset = position.offset(hFacing)
        //    if (!BlockUtil.isPlaceable(offset)) continue
            layer.add(Pair(offset, hFacing))
        }
        return layer
    }

    private fun getFinalBlockLayer(position: BlockPos): ArrayList<Pair<BlockPos, EnumFacing>> {
        val layer = ArrayList<Pair<BlockPos, EnumFacing>>()
        for (hFacing in EnumFacing.HORIZONTALS) {
            if (hFacing != Globals.mc.player.horizontalFacing.opposite) continue
            val offset = position.offset(hFacing)
        //    if (!BlockUtil.isPlaceable(offset)) continue
            layer.add(Pair(offset, hFacing))
        }
        return layer
    }


}