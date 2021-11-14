package me.han.muffin.client.event.events.world

import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

data class CollisionBoxEvent(val block: Block, val pos: BlockPos, val entity: Entity?, var boundingBox: AxisAlignedBB?) {
   // constructor (block: Block, pos: BlockPos, bb: AxisAlignedBB?): this(block, pos, null, bb)
}