package me.han.muffin.client.event.events.entity.player

import me.han.muffin.client.event.EventCancellable
import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.Potion

data class PlayerIsPotionActiveEvent(val entity: EntityLivingBase, val potion: Potion): EventCancellable()