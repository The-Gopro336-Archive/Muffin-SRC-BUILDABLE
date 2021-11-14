package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.entity.EntityTurnEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import me.han.muffin.client.value.ValueListeners
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.util.EnumFacing
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.roundToLong

object RotationLockModule: Module("RotationLock", Category.PLAYER, "Lock your yaw.") {
    private val direction = EnumValue(Direction.Custom, "Direction")

    private enum class Direction {
        North, East, South, West, Custom
    }

    init {
        addSettings(direction)
        direction.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                if (!isEnabled) return
                recalculate()
            }
        }
    }

    override fun onEnable() {
        recalculate()
    }

    @Listener
    private fun onTurn(event: EntityTurnEvent.Pre) {
        if (fullNullCheck()) return

        if (event.entity is EntityPlayerSP) {
            val yaw = when (direction.value) {
                Direction.Custom -> ((event.yaw + 1.0f) / 45.0f).roundToLong() * 45F
                Direction.North -> 0F
                Direction.East -> 0F
                Direction.South -> 0F
                Direction.West -> 0F
                else -> throw IllegalStateException()
            }

            event.yaw = yaw//((Globals.mc.player.rotationYaw + 1.0f) / 45.0f).roundToLong() * 45.0f
        }
    }


    private fun recalculate() {
        if (fullNullCheck()) return

        val yaw = when (direction.value) {
            Direction.Custom -> ((Globals.mc.player.rotationYaw + 1.0f) / 45.0f).roundToLong() * 45.0f//return//((Globals.mc.player.rotationYaw + 1.0f) / 45.0f).roundToLong() * 45.0f
            Direction.North -> EnumFacing.NORTH.horizontalAngle
            Direction.East -> EnumFacing.EAST.horizontalAngle
            Direction.South -> EnumFacing.SOUTH.horizontalAngle
            Direction.West -> EnumFacing.WEST.horizontalAngle
            else -> throw IllegalStateException()
        }

        Globals.mc.player.rotationYaw = yaw
        Globals.mc.player.ridingEntity?.rotationYaw = yaw
    }

}