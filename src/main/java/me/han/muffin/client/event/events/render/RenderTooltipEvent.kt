package me.han.muffin.client.event.events.render

import me.han.muffin.client.event.EventCancellable
import net.minecraft.item.ItemStack

data class RenderTooltipEvent(
    val item: ItemStack,
    val x: Int,
    val y: Int
): EventCancellable()