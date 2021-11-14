package me.han.muffin.client.event.events.world

import me.han.muffin.client.event.EventCancellable

data class AllowInteractEvent(var isUsingItem: Boolean): EventCancellable()