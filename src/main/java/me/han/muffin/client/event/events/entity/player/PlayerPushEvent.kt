package me.han.muffin.client.event.events.entity.player

import me.han.muffin.client.event.EventCancellable

data class PlayerPushEvent(val type: Type): EventCancellable() {
    enum class Type {
        BLOCK, LIQUID
    }
}