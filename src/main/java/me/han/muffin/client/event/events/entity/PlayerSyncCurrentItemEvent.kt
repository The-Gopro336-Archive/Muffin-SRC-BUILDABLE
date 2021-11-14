package me.han.muffin.client.event.events.entity

import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

data class PlayerSyncCurrentItemEvent(var x: Int, var y: Int, var z: Int, var blockHitDelay: Int, var curBlockDamage: Float, var facing: EnumFacing, var pos: BlockPos)