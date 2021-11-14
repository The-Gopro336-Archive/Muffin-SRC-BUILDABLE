package me.han.muffin.client.event.events.entity

import me.han.muffin.client.event.EventCancellable
import net.minecraft.util.math.BlockPos

data class HittingPositionEvent(val pos: BlockPos): EventCancellable()