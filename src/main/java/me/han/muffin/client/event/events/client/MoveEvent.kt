package me.han.muffin.client.event.events.client

import net.minecraft.entity.MoverType

data class MoveEvent(val type: MoverType, var x: Double, var y: Double, var z: Double, var isSneaking: Boolean) {
    fun resetMotion() {
        x = 0.0
        y = 0.0
        z = 0.0
    }
}