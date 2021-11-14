package me.han.muffin.client.event.events.network

import me.han.muffin.client.event.EventCancellable

data class PacketExceptionEvent(
    val throwable: Throwable
): EventCancellable()