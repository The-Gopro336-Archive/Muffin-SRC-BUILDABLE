package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBow
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object BowAimModule: Module("BowAim", Category.COMBAT, "Automatically aims at players when using a bow.", 280) {

    private val range = NumberValue(6.0, 0.1, 65.0, 0.1, "Range")
    private val targetPriority = EnumValue(TargetPriority.Direction, "TargetPriority")
    private val predict = Value(true, "Predict")
    private val predictTicks = NumberValue(2.0, 0.1, 5.0, 0.1, "PredictTicks")

    private var target: EntityPlayer? = null
    private var targetThread: Thread? = null

    private enum class TargetPriority {
        Distance, Health, Direction
    }

    init {
        addSettings(range, targetPriority, predict, predictTicks)
    }

    private fun updateTarget() {
        if (targetThread == null || !targetThread!!.isAlive || targetThread!!.isInterrupted) {
            targetThread = Thread { target = getTarget() }
            targetThread!!.start()
        }
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return
        updateTarget()

        val currentTarget = target ?: return
        if (Globals.mc.player.isHandActive && Globals.mc.player.activeItemStack.item is ItemBow) {
            val rotation = RotationUtils.faceBow(currentTarget, predict.value, predictTicks.value)
            addMotion { rotate(rotation) }
        }
    }

    private fun getTarget(): EntityPlayer? {
        val targets = Globals.mc.world.playerEntities.filter { !EntityUtil.isntValid(it, range.value) }

        return when (targetPriority.value) {
            TargetPriority.Distance -> targets.minByOrNull { Globals.mc.player.getDistance(it) }
            TargetPriority.Health -> targets.minByOrNull { it.health }
            TargetPriority.Direction -> targets.minByOrNull { RotationUtils.getRotationDifference(it) }
            else -> null
        }
    }

    fun hasTarget() = target != null && Globals.mc.player.canEntityBeSeen(target!!)

}