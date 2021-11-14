package me.han.muffin.client.event.events.render

import me.han.muffin.client.event.EventCancellable

data class RenderHeldItemEvent(
    var x: Float,
    var y: Float,
    var z: Float
): EventCancellable()