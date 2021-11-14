package me.han.muffin.client.event.events.entity.player

import me.han.muffin.client.event.EventCancellable

data class ShouldWalkOffEdge(var isSneaking: Boolean): EventCancellable()