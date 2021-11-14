package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object HighJumpModule: Module("HighJump", Category.MOVEMENT, "Jump way higher than a normal jump.") {
    val height = NumberValue(1.4, 0.0, 5.0, 0.1, "Height")
    private val inAir = Value(true, "InAir")

    init {
        addSettings(height, inAir)
    }

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (fullNullCheck() || Globals.mc.player.isRiding || Globals.mc.player.isElytraFlying) return

        if (Globals.mc.player.movementInput.jump && (inAir.value || Globals.mc.player.onGround)) {
            //event.y = height.value
            Globals.mc.player.motionY = height.value
            event.y = Globals.mc.player.motionY
        }
    }

}