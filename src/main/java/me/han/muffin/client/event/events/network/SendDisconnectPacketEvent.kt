package me.han.muffin.client.event.events.network

import me.han.muffin.client.event.EventCancellable
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.WorldClient

class SendDisconnectPacketEvent(val world: WorldClient, val server: ServerData): EventCancellable()