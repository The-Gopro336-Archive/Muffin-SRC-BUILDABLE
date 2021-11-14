package me.han.muffin.client.event.events.world.block

import me.han.muffin.client.event.EventCancellable
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

data class ClickBlockEvent(val pos: BlockPos, val facing: EnumFacing): EventCancellable()