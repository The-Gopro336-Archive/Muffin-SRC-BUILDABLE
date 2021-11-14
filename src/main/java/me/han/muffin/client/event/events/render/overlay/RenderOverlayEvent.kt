package me.han.muffin.client.event.events.render.overlay

import me.han.muffin.client.event.EventCancellable

data class RenderOverlayEvent
    (var type: OverlayType): EventCancellable()
{
    enum class OverlayType {
        BLOCK, LIQUID, FIRE
    }
}