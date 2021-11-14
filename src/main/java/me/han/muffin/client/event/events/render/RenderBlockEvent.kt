package me.han.muffin.client.event.events.render

import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos

data class RenderBlockEvent(val state: IBlockState, val pos: BlockPos)