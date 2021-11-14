package me.han.muffin.client.event.events.world.block

import me.han.muffin.client.event.EventCancellable
import net.minecraft.block.Block
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

data class CanPlaceBlockAtEvent(val world: World, val pos: BlockPos, val block: Block): EventCancellable()