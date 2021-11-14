package me.han.muffin.client.event.events.entity.player

import me.han.muffin.client.event.EventCancellable
import net.minecraft.util.EnumHand

data class PlayerSwingArmEvent(var hand: EnumHand): EventCancellable()