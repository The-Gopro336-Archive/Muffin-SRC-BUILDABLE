package me.han.muffin.client.event.events.render

import me.han.muffin.client.event.EventCancellable
import net.minecraft.client.renderer.ItemRenderer
import net.minecraft.util.EnumHandSide

data class RenderArmsEvent(
    val renderer: ItemRenderer,
    val handSide: EnumHandSide
): EventCancellable()