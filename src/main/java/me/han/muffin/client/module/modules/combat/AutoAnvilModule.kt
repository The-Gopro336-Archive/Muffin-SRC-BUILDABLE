package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.NumberValue
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object AutoAnvilModule: Module("AutoAnvil", Category.COMBAT, "Place anvil above enemy head.") {
    private val delay = NumberValue(0, 0, 1000, 5, "Delay")
    private val range = NumberValue(5.0, 0.1, 10.0, 0.1, "Range")
    private val wallRange = NumberValue(3.0, 0.1, 10.0, 0.1, "WallRange")

    init {
        addSettings(delay, range, wallRange)
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {

    }

}