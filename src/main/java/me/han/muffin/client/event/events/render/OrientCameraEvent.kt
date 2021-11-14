package me.han.muffin.client.event.events.render

import me.han.muffin.client.event.EventCancellable

data class OrientCameraEvent(
    var distance: Double
): EventCancellable()