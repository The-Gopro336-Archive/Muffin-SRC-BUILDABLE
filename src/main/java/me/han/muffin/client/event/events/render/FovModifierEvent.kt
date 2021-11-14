package me.han.muffin.client.event.events.render

import me.han.muffin.client.event.EventCancellable

data class FovModifierEvent(
    var fov: Float
): EventCancellable()