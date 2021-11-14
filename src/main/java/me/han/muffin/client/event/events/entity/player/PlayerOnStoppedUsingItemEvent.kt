package me.han.muffin.client.event.events.entity.player

import me.han.muffin.client.event.EventCancellable
import net.minecraft.entity.player.EntityPlayer

data class PlayerOnStoppedUsingItemEvent(val player: EntityPlayer): EventCancellable()