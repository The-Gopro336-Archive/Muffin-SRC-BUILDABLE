package me.han.muffin.client.event.events.entity

import me.han.muffin.client.event.EventCancellable

data class PortalCooldownEvent(
    var cooldown: Int
): EventCancellable()