package me.han.muffin.client.event.events.client

import me.han.muffin.client.event.EventCancellable

data class StrafeEvent(var strafe: Float, var up: Float, var forward: Float, var friction: Float): EventCancellable()