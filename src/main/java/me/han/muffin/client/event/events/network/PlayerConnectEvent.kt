package me.han.muffin.client.event.events.network

import java.util.*

open class PlayerConnectEvent(val username: String, val uuid: UUID) {
    class Join(username: String, uuid: UUID): PlayerConnectEvent(username, uuid)
    class Leave(username: String, uuid: UUID): PlayerConnectEvent(username, uuid)
}