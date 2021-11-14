package me.han.muffin.client.event.events.client

open class ClientEvent {
    class Start: ClientEvent()
    class ShutDown: ClientEvent()
    class FinalLoading: ClientEvent()
}