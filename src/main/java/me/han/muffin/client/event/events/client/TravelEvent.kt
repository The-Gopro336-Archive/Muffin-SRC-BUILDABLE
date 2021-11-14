package me.han.muffin.client.event.events.client

import me.han.muffin.client.event.EventCancellable

data class TravelEvent(var strafe: Float, var vertical: Float, var forward: Float): EventCancellable()