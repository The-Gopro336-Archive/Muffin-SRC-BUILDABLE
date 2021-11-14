package me.han.muffin.client.event.events.world.block

import net.minecraft.util.math.BlockPos

class BlockBreakEvent(val breakId: Int, val position: BlockPos, val progress: Int)