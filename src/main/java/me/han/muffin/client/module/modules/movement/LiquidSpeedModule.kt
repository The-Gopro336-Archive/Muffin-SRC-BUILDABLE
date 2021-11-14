package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.value.NumberValue
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object LiquidSpeedModule: Module("LiquidSpeed", Category.MOVEMENT, "Makes you swim faster.") {

    private val waterHorizontal = NumberValue(0.7, 0.0, 2.0, 0.1, "WaterHorizontal")
    private val waterVertical = NumberValue(1.0, 0.0, 2.0, 0.1, "WaterVertical")

    private val lavaHorizontal = NumberValue(0.6, 0.0, 2.0, 0.1, "LavaHorizontal")
    private val lavaVertical = NumberValue(0.9, 0.0, 2.0, 0.1, "LavaVertical")

    init {
        addSettings(waterHorizontal, waterVertical, lavaHorizontal, lavaVertical)
    }

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (fullNullCheck()) return

        if (EntityUtil.isInWater(Globals.mc.player) && !Globals.mc.player.onGround) {
            if (Globals.mc.player.isInWater) {
                event.x *= 1.0 + waterHorizontal.value
                event.z *= 1.0 + waterHorizontal.value
                event.y *= 1.0 + waterVertical.value
            } else if (Globals.mc.player.isInLava) {
                event.x *= 1.0 + lavaHorizontal.value
                event.z *= 1.0 + lavaHorizontal.value
                event.y *= 1.0 + lavaVertical.value
            }
        }
    }

}