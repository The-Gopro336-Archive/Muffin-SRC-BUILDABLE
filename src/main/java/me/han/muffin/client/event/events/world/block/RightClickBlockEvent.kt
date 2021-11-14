package me.han.muffin.client.event.events.world.block

import me.han.muffin.client.event.EventCancellable
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

data class RightClickBlockEvent(
    val pos: BlockPos,
    val facing: EnumFacing,
    val vector: Vec3d,
    val hand: EnumHand
): EventCancellable()