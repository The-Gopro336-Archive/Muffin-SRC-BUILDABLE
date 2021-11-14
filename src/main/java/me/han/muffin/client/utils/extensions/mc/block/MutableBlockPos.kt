package me.han.muffin.client.utils.extensions.mc.block

import net.minecraft.util.math.BlockPos

fun BlockPos.MutableBlockPos.setNull() = this.setPos(0, -69, 0)

val BlockPos.MutableBlockPos.isNull get() = this.y == -69
val BlockPos.MutableBlockPos.isNotNull get() = this.y != -69

// infix fun BlockPos.MutableBlockPos.set(pos: BlockPos): BlockPos.MutableBlockPos = setPos(pos)