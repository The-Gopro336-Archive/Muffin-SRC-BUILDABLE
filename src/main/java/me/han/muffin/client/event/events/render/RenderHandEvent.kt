package me.han.muffin.client.event.events.render

import me.han.muffin.client.event.EventCancellable

data class RenderHandEvent(val partialTicks: Float, val pass: Int): EventCancellable() {
    constructor(): this(1.0F, 0)
}