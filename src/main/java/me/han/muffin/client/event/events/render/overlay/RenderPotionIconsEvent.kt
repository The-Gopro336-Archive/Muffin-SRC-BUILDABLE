package me.han.muffin.client.event.events.render.overlay

import me.han.muffin.client.event.EventCancellable
import net.minecraft.client.gui.ScaledResolution

data class RenderPotionIconsEvent(
    val sr: ScaledResolution
): EventCancellable()