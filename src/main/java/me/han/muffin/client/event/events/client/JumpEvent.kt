package me.han.muffin.client.event.events.client

import me.han.muffin.client.event.EventCancellable

data class JumpEvent(val motionX: Double,  val motionY: Double, val motionZ: Double, val rotationYaw: Float): EventCancellable()