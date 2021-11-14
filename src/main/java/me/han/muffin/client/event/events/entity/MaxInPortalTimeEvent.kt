package me.han.muffin.client.event.events.entity

import me.han.muffin.client.event.EventCancellable

data class MaxInPortalTimeEvent(
    var time: Int
): EventCancellable()